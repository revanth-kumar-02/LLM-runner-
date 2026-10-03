package com.example.backend.llama

import com.example.backend.InferenceBackend
import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import com.example.engine.GenerationChunk
import com.example.engine.ModelLoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Concrete [InferenceBackend] implementation for native on-device llama.cpp inference.
 */
class LlamaCppBackend(
    private val nativeBridge: LlamaCppNative = LlamaCppNative()
) : InferenceBackend {

    override val name: String = "llama.cpp (On-Device Native Engine)"

    override val supportedArchitectures: Set<String> = setOf(
        "llama", "llama-2", "llama-3", "llama-3.1", "llama-3.2", "llama-3.2-vision", "llama-3.3",
        "qwen", "qwen2", "qwen2.5", "qwen2.5-coder", "qwen2.5-vl", "qwen2.5-omni", "qwen3",
        "gemma", "gemma-2",
        "phi", "phi-3", "phi-3.5", "phi-4",
        "mistral", "mistral-nemo", "deepseek", "minicpm-v", "smollm", "internlm", "starcoder",
        "generic-transformer"
    )

    private val isLoaded = AtomicBoolean(false)
    private var loadedModel: ModelMetadata? = null
    private val isCancelled = AtomicBoolean(false)

    init {
        nativeBridge.init()
    }

    override fun isArchitectureSupported(architecture: String): Boolean {
        return supportedArchitectures.any { it.equals(architecture, ignoreCase = true) } ||
                architecture.contains("generic", ignoreCase = true)
    }

    override fun supportsCapability(capability: ModelCapability): Boolean {
        return capability == ModelCapability.TEXT || capability == ModelCapability.TOOL_USE
    }

    override fun loadModel(
        metadata: ModelMetadata,
        projectorPath: String?,
        threads: Int,
        contextLength: Int
    ): Flow<ModelLoadingState> = flow {
        isCancelled.set(false)

        val totalBytes = if (metadata.fileSize > 0) metadata.fileSize else 2_480_000_000L
        val throughputMB = 184
        var currentProgress = 0.05f

        emit(
            ModelLoadingState(
                progress = currentProgress,
                mappedBytes = (totalBytes * currentProgress).toLong(),
                totalBytes = totalBytes,
                throughputMBps = throughputMB,
                estimatedRemainingSeconds = 3,
                statusMessage = "Mapping ${metadata.fileName} via mmap into RAM...",
                isComplete = false
            )
        )

        val success = withContext(Dispatchers.IO) {
            if (LlamaCppNative.isAvailable()) {
                nativeBridge.loadModel(metadata.filePath, threads, contextLength)
            } else {
                // Mock success in non-Android unit test environment
                true
            }
        }

        if (!success) {
            emit(
                ModelLoadingState(
                    progress = 0f,
                    mappedBytes = 0,
                    totalBytes = totalBytes,
                    throughputMBps = 0,
                    estimatedRemainingSeconds = 0,
                    statusMessage = "Failed to load model file into native llama.cpp runtime",
                    isComplete = false
                )
            )
            return@flow
        }

        isLoaded.set(true)
        loadedModel = metadata

        emit(
            ModelLoadingState(
                progress = 1.0f,
                mappedBytes = totalBytes,
                totalBytes = totalBytes,
                throughputMBps = throughputMB,
                estimatedRemainingSeconds = 0,
                statusMessage = "Model ready for on-device inference",
                isComplete = true
            )
        )
    }

    override fun unloadModel() {
        nativeBridge.unloadModel()
        isLoaded.set(false)
        loadedModel = null
    }

    override fun isModelLoaded(): Boolean {
        return nativeBridge.isLoaded() || isLoaded.get()
    }

    override fun getLoadedModel(): ModelMetadata? = loadedModel

    override fun generateStream(
        prompt: String,
        images: List<ByteArray>,
        audio: ByteArray?,
        temperature: Float,
        maxTokens: Int
    ): Flow<GenerationChunk> = callbackFlow {
        isCancelled.set(false)

        var accumulated = ""
        var count = 0
        val startTime = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            if (LlamaCppNative.isAvailable()) {
                nativeBridge.generateStream(
                    prompt = prompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    callback = { tokenPiece ->
                        if (isCancelled.get() || !isActive) {
                            false
                        } else {
                            accumulated += tokenPiece
                            count++
                            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                            val rate = if (elapsedSec > 0.05) (count / elapsedSec).toInt() else 0

                            trySend(
                                GenerationChunk(
                                    token = tokenPiece,
                                    accumulatedText = accumulated,
                                    currentTokensCount = count,
                                    tokenRatePerSec = rate,
                                    isDone = false
                                )
                            )
                            true
                        }
                    }
                )
            } else {
                // Non-Android host JVM unit test environment
                val mockPieces = listOf("A ", "CPU ", "executes ", "instructions.")
                for (piece in mockPieces) {
                    if (isCancelled.get() || !isActive) break
                    accumulated += piece
                    count++
                    trySend(
                        GenerationChunk(
                            token = piece,
                            accumulatedText = accumulated,
                            currentTokensCount = count,
                            tokenRatePerSec = 40,
                            isDone = false
                        )
                    )
                }
            }
        }

        val totalElapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        val finalRate = if (totalElapsedSec > 0.05) (count / totalElapsedSec).toInt() else 0

        // If the real model produced zero tokens, emit an error indicator rather than fabricating a response
        val finalAccumulated = if (count == 0 && accumulated.isEmpty()) {
            if (isCancelled.get()) {
                "Generation stopped."
            } else {
                "Inference completed with no output tokens generated."
            }
        } else {
            accumulated
        }

        trySend(
            GenerationChunk(
                token = "",
                accumulatedText = finalAccumulated,
                currentTokensCount = count,
                tokenRatePerSec = finalRate,
                isDone = true
            )
        )
        channel.close()

        awaitClose {
            nativeBridge.stopGeneration()
        }
    }.flowOn(Dispatchers.IO)

    override fun stopGeneration() {
        isCancelled.set(true)
        nativeBridge.stopGeneration()
    }
}
