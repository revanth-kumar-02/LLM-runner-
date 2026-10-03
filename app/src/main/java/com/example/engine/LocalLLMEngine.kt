package com.example.engine

import com.example.backend.InferenceBackend
import com.example.backend.llama.LlamaCppBackend
import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Interface representing the on-device AI inference engine runtime.
 */
interface LocalLLMEngine {

    val backendName: String

    fun isArchitectureSupported(architecture: String): Boolean

    fun supportsCapability(capability: ModelCapability): Boolean

    fun loadModel(
        metadata: ModelMetadata,
        projectorPath: String? = null,
        threads: Int = 6,
        contextLength: Int = 4096
    ): Flow<ModelLoadingState>

    fun unloadModel()

    fun isModelLoaded(): Boolean

    fun getLoadedModel(): ModelMetadata?

    fun generateStream(
        prompt: String,
        images: List<ByteArray> = emptyList(),
        audio: ByteArray? = null,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048,
        enableThinking: Boolean = false
    ): Flow<GenerationChunk>

    fun stopGeneration()
}

data class ModelLoadingState(
    val progress: Float,
    val mappedBytes: Long,
    val totalBytes: Long,
    val throughputMBps: Int,
    val estimatedRemainingSeconds: Int,
    val statusMessage: String,
    val isComplete: Boolean = false
)

data class GenerationChunk(
    val token: String,
    val accumulatedText: String,
    val currentTokensCount: Int,
    val tokenRatePerSec: Int,
    val isDone: Boolean = false
)

/**
 * Default implementation of [LocalLLMEngine] delegating to an [InferenceBackend].
 */
class DefaultLocalLLMEngine(
    private val backend: InferenceBackend = LlamaCppBackend()
) : LocalLLMEngine {

    override val backendName: String
        get() = backend.name

    override fun isArchitectureSupported(architecture: String): Boolean =
        backend.isArchitectureSupported(architecture)

    override fun supportsCapability(capability: ModelCapability): Boolean =
        backend.supportsCapability(capability)

    override fun loadModel(
        metadata: ModelMetadata,
        projectorPath: String?,
        threads: Int,
        contextLength: Int
    ): Flow<ModelLoadingState> =
        backend.loadModel(metadata, projectorPath, threads, contextLength)

    override fun unloadModel() =
        backend.unloadModel()

    override fun isModelLoaded(): Boolean =
        backend.isModelLoaded()

    override fun getLoadedModel(): ModelMetadata? =
        backend.getLoadedModel()

    override fun generateStream(
        prompt: String,
        images: List<ByteArray>,
        audio: ByteArray?,
        temperature: Float,
        maxTokens: Int,
        enableThinking: Boolean
    ): Flow<GenerationChunk> =
        backend.generateStream(prompt, images, audio, temperature, maxTokens, enableThinking)

    override fun stopGeneration() =
        backend.stopGeneration()
}
