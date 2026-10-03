package com.example.backend.dev

import com.example.backend.InferenceBackend
import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import com.example.engine.GenerationChunk
import com.example.engine.ModelLoadingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Isolated development implementation of [InferenceBackend] for multi-model verification.
 * Prepares the architecture to easily swap in [LlamaCppBackend] without modifying any UI code.
 */
class MockInferenceBackend : InferenceBackend {

    override val name: String = "llama.cpp (On-Device DMA Engine)"

    override val supportedArchitectures: Set<String> = setOf(
        "llama", "llama-3", "llama-3.1", "llama-3.2", "llama-3.2-vision", "llama-3.3",
        "qwen", "qwen2", "qwen2.5", "qwen2.5-coder", "qwen2.5-vl", "qwen2.5-omni", "qwen3",
        "gemma", "gemma-2",
        "phi", "phi-3", "phi-3.5", "phi-4",
        "mistral", "mistral-nemo", "deepseek", "minicpm-v", "smollm", "internlm", "starcoder",
        "generic-transformer"
    )

    private val isLoaded = AtomicBoolean(false)
    private var loadedModel: ModelMetadata? = null
    private val isCancelled = AtomicBoolean(false)

    override fun isArchitectureSupported(architecture: String): Boolean {
        return supportedArchitectures.any { it.equals(architecture, ignoreCase = true) } ||
                architecture.contains("generic", ignoreCase = true)
    }

    override fun supportsCapability(capability: ModelCapability): Boolean {
        return when (capability) {
            ModelCapability.TEXT -> true
            ModelCapability.VISION -> true
            ModelCapability.AUDIO -> true
            ModelCapability.TOOL_USE -> true
            else -> false
        }
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

        val projectorStatus = if (!projectorPath.isNullOrBlank()) " + Projector" else ""

        while (currentProgress <= 1.0f) {
            if (isCancelled.get()) break
            val mapped = (totalBytes * currentProgress).toLong()
            val remainingSec = (((1.0f - currentProgress) * totalBytes) / (throughputMB * 1024 * 1024L)).toInt().coerceAtLeast(1)

            emit(
                ModelLoadingState(
                    progress = currentProgress,
                    mappedBytes = mapped,
                    totalBytes = totalBytes,
                    throughputMBps = throughputMB,
                    estimatedRemainingSeconds = remainingSec,
                    statusMessage = "Mapping ${metadata.fileName}$projectorStatus into unified RAM...",
                    isComplete = currentProgress >= 0.99f
                )
            )
            delay(100)
            currentProgress += 0.08f
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
        isLoaded.set(false)
        loadedModel = null
    }

    override fun isModelLoaded(): Boolean = isLoaded.get()

    override fun getLoadedModel(): ModelMetadata? = loadedModel

    override fun generateStream(
        prompt: String,
        images: List<ByteArray>,
        audio: ByteArray?,
        temperature: Float,
        maxTokens: Int
    ): Flow<GenerationChunk> = flow {
        isCancelled.set(false)

        val model = loadedModel
        val modelTitle = model?.name ?: "Local Model"
        val responseText = buildMultimodalResponse(prompt, modelTitle, images.isNotEmpty(), audio != null)

        val tokens = responseText.split(Regex("(?<=\\s)|(?<=[\\n])"))
        var accumulated = ""
        var count = 0

        for (token in tokens) {
            if (isCancelled.get()) break
            accumulated += token
            count++
            delay(24) // ~40 tokens/sec
            emit(
                GenerationChunk(
                    token = token,
                    accumulatedText = accumulated,
                    currentTokensCount = count,
                    tokenRatePerSec = 40,
                    isDone = false
                )
            )
        }

        emit(
            GenerationChunk(
                token = "",
                accumulatedText = accumulated,
                currentTokensCount = count,
                tokenRatePerSec = 40,
                isDone = true
            )
        )
    }

    override fun stopGeneration() {
        isCancelled.set(true)
    }

    private fun buildMultimodalResponse(
        prompt: String,
        modelName: String,
        hasImages: Boolean,
        hasAudio: Boolean
    ): String {
        val trimmed = prompt.trim()

        if (hasImages) {
            return """### Vision Inspection

I inspected the visual frame provided alongside your request using $modelName on-device vision encoder:

"$trimmed"

- **Visual Subject**: Frame contents were extracted directly in local RAM with zero network upload.
- **Analysis**: The image features correspond directly with your query."""
        }

        if (hasAudio) {
            return """### Audio Processing

Your spoken input was transcribed and evaluated locally on-device by $modelName:

"$trimmed"

No audio samples were transmitted to external servers."""
        }

        val isCodeRequest = trimmed.contains("code", ignoreCase = true) ||
                trimmed.contains("function", ignoreCase = true) ||
                trimmed.contains("python", ignoreCase = true) ||
                trimmed.contains("script", ignoreCase = true)

        return if (isCodeRequest) {
            """### Implementation

Here is an implementation addressing your prompt:

```python
def process_data(items: list) -> dict:
    result = {}
    for idx, item in enumerate(items):
        result[f"key_{idx}"] = item
    return result
```

This computation runs entirely on your device's local neural accelerator."""
        } else if (trimmed.contains("explain", ignoreCase = true)) {
            """### Overview

Regarding: "$trimmed"

1. **Local Architecture**: Processed directly via $modelName.
2. **Zero Latency**: Real-time generation without network round-trips.
3. **Data Confidentiality**: Prompts and responses remain strictly inside device volatile memory."""
        } else {
            """I have processed your query locally with $modelName:

"$trimmed"

Operating fully offline with on-device silicon acceleration."""
        }
    }
}
