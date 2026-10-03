package com.example.data.repository

import android.content.Context
import com.example.data.model.CompatibilityLevel
import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import com.example.data.model.ModelStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ModelRepository(private val context: Context) {

    private val modelsFile = File(context.filesDir, "installed_models_v2.json")
    val modelsDirectory = File(context.filesDir, "models").apply { mkdirs() }

    private val _models = MutableStateFlow<List<ModelMetadata>>(emptyList())
    val models: StateFlow<List<ModelMetadata>> = _models.asStateFlow()

    private val _activeModel = MutableStateFlow<ModelMetadata?>(null)
    val activeModel: StateFlow<ModelMetadata?> = _activeModel.asStateFlow()

    init {
        loadPersistedModels()
    }

    private fun loadPersistedModels() {
        if (!modelsFile.exists()) {
            _models.value = emptyList()
            _activeModel.value = null
            return
        }

        try {
            val jsonStr = modelsFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ModelMetadata>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusStr = obj.optString("status", ModelStatus.NOT_LOADED.name)
                val status = try {
                    ModelStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    ModelStatus.NOT_LOADED
                }

                val compatStr = obj.optString("compatibilityStatus", CompatibilityLevel.COMPATIBLE.name)
                val compat = try {
                    CompatibilityLevel.valueOf(compatStr)
                } catch (_: Exception) {
                    CompatibilityLevel.COMPATIBLE
                }

                val caps = mutableSetOf<ModelCapability>()
                val capsArray = obj.optJSONArray("capabilities")
                if (capsArray != null) {
                    for (c in 0 until capsArray.length()) {
                        try {
                            caps.add(ModelCapability.valueOf(capsArray.getString(c)))
                        } catch (_: Exception) {}
                    }
                }
                if (caps.isEmpty()) {
                    caps.add(ModelCapability.TEXT)
                }

                list.add(
                    ModelMetadata(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        fileName = obj.getString("fileName"),
                        filePath = obj.getString("filePath"),
                        format = obj.optString("format", "GGUF"),
                        architecture = obj.optString("architecture", "generic"),
                        parameterCount = obj.optString("parameterCount", "Unknown"),
                        quantization = obj.optString("quantization", "Q4_K_M"),
                        fileSize = obj.optLong("fileSize", 0L),
                        formattedSize = obj.getString("formattedSize"),
                        contextLength = obj.optInt("contextLength", 4096),
                        capabilities = caps,
                        status = status,
                        requiredProjector = obj.optBoolean("requiredProjector", false),
                        projectorPath = if (obj.has("projectorPath") && !obj.isNull("projectorPath")) obj.getString("projectorPath") else null,
                        compatibilityStatus = compat,
                        compatibilityMessage = obj.optString("compatibilityMessage", "Compatible with device on-device NPU/CPU accelerator."),
                        isActive = obj.optBoolean("isActive", false),
                        throughput = obj.optString("throughput", "184 MB/s (DMA)"),
                        cpuThreads = obj.optInt("cpuThreads", 6),
                        temperature = obj.optDouble("temperature", 0.7).toFloat(),
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis())
                    )
                )
            }
            _models.value = list
            _activeModel.value = list.find { it.isActive }
        } catch (_: Exception) {
            _models.value = emptyList()
            _activeModel.value = null
        }
    }

    private fun persistModels() {
        try {
            val array = JSONArray()
            _models.value.forEach { model ->
                val obj = JSONObject().apply {
                    put("id", model.id)
                    put("name", model.name)
                    put("fileName", model.fileName)
                    put("filePath", model.filePath)
                    put("format", model.format)
                    put("architecture", model.architecture)
                    put("parameterCount", model.parameterCount)
                    put("quantization", model.quantization)
                    put("fileSize", model.fileSize)
                    put("formattedSize", model.formattedSize)
                    put("contextLength", model.contextLength)
                    put("capabilities", JSONArray(model.capabilities.map { it.name }))
                    put("status", model.status.name)
                    put("requiredProjector", model.requiredProjector)
                    put("projectorPath", model.projectorPath)
                    put("compatibilityStatus", model.compatibilityStatus.name)
                    put("compatibilityMessage", model.compatibilityMessage)
                    put("isActive", model.isActive)
                    put("throughput", model.throughput)
                    put("cpuThreads", model.cpuThreads)
                    put("temperature", model.temperature.toDouble())
                    put("dateAdded", model.dateAdded)
                }
                array.put(obj)
            }
            modelsFile.writeText(array.toString())
        } catch (_: Exception) {}
    }

    fun addModel(model: ModelMetadata) {
        _models.update { listOf(model) + it }
        persistModels()
    }

    fun updateModel(model: ModelMetadata) {
        _models.update { list ->
            list.map { if (it.id == model.id) model else it }
        }
        if (model.isActive) {
            _activeModel.value = model
        } else if (_activeModel.value?.id == model.id) {
            _activeModel.value = null
        }
        persistModels()
    }

    fun updateModelStatus(modelId: String, status: ModelStatus, isActive: Boolean = false) {
        _models.update { list ->
            list.map { model ->
                if (model.id == modelId) {
                    model.copy(status = status, isActive = isActive)
                } else {
                    if (isActive) model.copy(isActive = false) else model
                }
            }
        }
        if (isActive && status == ModelStatus.LOADED) {
            _activeModel.value = _models.value.find { it.id == modelId }
        } else if (!isActive && _activeModel.value?.id == modelId) {
            _activeModel.value = null
        }
        persistModels()
    }

    fun linkProjector(modelId: String, projectorPath: String) {
        _models.update { list ->
            list.map {
                if (it.id == modelId) {
                    it.copy(projectorPath = projectorPath)
                } else it
            }
        }
        if (_activeModel.value?.id == modelId) {
            _activeModel.value = _models.value.find { it.id == modelId }
        }
        persistModels()
    }

    fun unloadModel(modelId: String) {
        updateModelStatus(modelId, ModelStatus.NOT_LOADED, isActive = false)
    }

    fun removeModel(modelId: String) {
        if (_activeModel.value?.id == modelId) {
            _activeModel.value = null
        }
        _models.update { list -> list.filterNot { it.id == modelId } }
        persistModels()
    }

    fun getModelById(id: String): ModelMetadata? {
        return _models.value.find { it.id == id }
    }
}
