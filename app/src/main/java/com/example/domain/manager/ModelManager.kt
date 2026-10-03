package com.example.domain.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.GGUFMetadataInspector
import com.example.data.model.CompatibilityLevel
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ModelMetadata
import com.example.data.model.ModelStatus
import com.example.data.repository.ModelRepository
import com.example.domain.checker.ModelCompatibilityChecker
import com.example.engine.DefaultLocalLLMEngine
import com.example.engine.LocalLLMEngine
import com.example.engine.ModelLoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Coordinates model discovery, lifecycle management, compatibility verification,
 * and single-model memory swapping.
 */
class ModelManager(
    private val context: Context,
    private val repository: ModelRepository = ModelRepository(context),
    private val compatibilityChecker: ModelCompatibilityChecker = ModelCompatibilityChecker(),
    private val inspector: GGUFMetadataInspector = GGUFMetadataInspector(),
    val engine: LocalLLMEngine = DefaultLocalLLMEngine()
) {

    val models: StateFlow<List<ModelMetadata>> = repository.models
    val activeModel: StateFlow<ModelMetadata?> = repository.activeModel

    /**
     * Resolves an Android SAF content:// URI into an accessible local filesystem path.
     */
    fun resolveLocalModelFilePath(uri: Uri, fileName: String): String {
        return try {
            if (uri.scheme == "file") {
                uri.path ?: File(context.filesDir, "models/$fileName").absolutePath
            } else if (uri.scheme == "content") {
                val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
                val targetFile = File(modelsDir, fileName)
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0L) {
                    targetFile.absolutePath
                } else {
                    uri.toString()
                }
            } else {
                uri.toString()
            }
        } catch (_: Exception) {
            uri.toString()
        }
    }

    /**
     * Inspects a newly selected .gguf file from Android SAF, runs the compatibility
     * checker, and stores it into the persistent model repository.
     */
    fun importModel(uri: Uri, telemetry: HardwareTelemetry): ModelMetadata {
        var rawFileName = "model.gguf"
        var fileSizeInBytes = 0L

        // Take persistable permission if possible
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { rawFileName = it }
                    }
                    if (sizeIndex != -1) {
                        fileSizeInBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        val inspected = inspector.inspect(context, uri, rawFileName)
        val formattedSize = formatFileSize(fileSizeInBytes)

        val compatInfo = compatibilityChecker.checkCompatibility(
            architecture = inspected.architecture,
            parameterCountStr = inspected.parameterCount,
            fileSizeBytes = fileSizeInBytes,
            capabilities = inspected.capabilities,
            requiredProjector = inspected.requiresProjector,
            projectorPath = null,
            telemetry = telemetry,
            isValidGGUF = inspected.isValidGGUF,
            validationErrorMessage = inspected.validationErrorMessage
        )

        val resolvedPath = resolveLocalModelFilePath(uri, rawFileName)

        val metadata = ModelMetadata(
            id = UUID.randomUUID().toString(),
            name = inspected.suggestedDisplayName,
            fileName = rawFileName,
            filePath = resolvedPath,
            format = inspected.format,
            architecture = inspected.architecture,
            parameterCount = inspected.parameterCount,
            quantization = inspected.quantization,
            fileSize = fileSizeInBytes,
            formattedSize = formattedSize,
            contextLength = inspected.contextLength,
            capabilities = inspected.capabilities,
            status = ModelStatus.NOT_LOADED,
            requiredProjector = inspected.requiresProjector,
            projectorPath = null,
            compatibilityStatus = compatInfo.level,
            compatibilityMessage = compatInfo.message,
            isActive = false
        )

        repository.addModel(metadata)
        return metadata
    }

    /**
     * Links an auxiliary multimodal projector (.mmproj GGUF) to a primary model.
     */
    fun linkProjector(modelId: String, projectorPath: String, telemetry: HardwareTelemetry) {
        val model = repository.getModelById(modelId) ?: return
        repository.linkProjector(modelId, projectorPath)

        val updatedCompat = compatibilityChecker.checkCompatibility(
            architecture = model.architecture,
            parameterCountStr = model.parameterCount,
            fileSizeBytes = model.fileSize,
            capabilities = model.capabilities,
            requiredProjector = model.requiredProjector,
            projectorPath = projectorPath,
            telemetry = telemetry
        )

        repository.updateModel(
            model.copy(
                projectorPath = projectorPath,
                compatibilityStatus = updatedCompat.level,
                compatibilityMessage = updatedCompat.message
            )
        )
    }

    /**
     * Swaps the active in-memory model.
     * Enforces the single heavy model rule: unloads the currently active model before loading the target.
     */
    fun loadModel(
        modelId: String,
        threads: Int = 6,
        contextLength: Int = 4096
    ): Flow<ModelLoadingState> = flow {
        val targetModel = repository.getModelById(modelId) ?: return@flow

        // 1. Unload existing active model if any
        val currentActive = repository.activeModel.value
        if (currentActive != null && currentActive.id != modelId) {
            engine.unloadModel()
            repository.updateModelStatus(currentActive.id, ModelStatus.NOT_LOADED, isActive = false)
        }

        repository.updateModelStatus(modelId, ModelStatus.LOADING, isActive = false)

        engine.loadModel(targetModel, targetModel.projectorPath, threads, contextLength).collect { state ->
            emit(state)
            if (state.isComplete) {
                repository.updateModelStatus(modelId, ModelStatus.LOADED, isActive = true)
            }
        }
    }

    fun unloadModel(modelId: String) {
        engine.unloadModel()
        repository.unloadModel(modelId)
    }

    fun removeModel(modelId: String) {
        if (activeModel.value?.id == modelId) {
            engine.unloadModel()
        }
        repository.removeModel(modelId)
    }

    fun cancelLoading(modelId: String) {
        engine.unloadModel()
        repository.updateModelStatus(modelId, ModelStatus.NOT_LOADED, isActive = false)
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "2.48 GB"
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.2f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            String.format(Locale.US, "%.1f MB", mb)
        }
    }
}
