package com.example.backend

import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import com.example.engine.GenerationChunk
import com.example.engine.ModelLoadingState
import kotlinx.coroutines.flow.Flow

/**
 * Backend abstraction layer decoupling local AI execution from specific native libraries (e.g. llama.cpp).
 *
 * Architecture:
 * UI -> ViewModel -> ModelManager -> InferenceBackend -> Native runtime (llama.cpp)
 */
interface InferenceBackend {
    val name: String
    val supportedArchitectures: Set<String>

    fun isArchitectureSupported(architecture: String): Boolean
    fun supportsCapability(capability: ModelCapability): Boolean

    /**
     * Loads a primary model file and optional multimodal projector into unified memory.
     */
    fun loadModel(
        metadata: ModelMetadata,
        projectorPath: String?,
        threads: Int = 6,
        contextLength: Int = 4096
    ): Flow<ModelLoadingState>

    /**
     * Unloads the active model from RAM and releases all buffers.
     */
    fun unloadModel()

    fun isModelLoaded(): Boolean

    fun getLoadedModel(): ModelMetadata?

    /**
     * Executes prompt inference with optional multimodal attachments (images or audio).
     */
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
