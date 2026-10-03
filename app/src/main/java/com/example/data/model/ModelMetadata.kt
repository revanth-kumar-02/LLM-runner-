package com.example.data.model

import java.util.UUID

/**
 * Fundamental capabilities that a local AI model may exhibit.
 */
enum class ModelCapability {
    TEXT,
    VISION,
    AUDIO,
    IMAGE_GENERATION,
    EMBEDDING,
    TOOL_USE;

    val label: String
        get() = when (this) {
            TEXT -> "Text"
            VISION -> "Vision"
            AUDIO -> "Audio"
            IMAGE_GENERATION -> "Image Gen"
            EMBEDDING -> "Embedding"
            TOOL_USE -> "Tool Calling"
        }
}

/**
 * Runtime hardware & software compatibility tier.
 */
enum class CompatibilityLevel {
    COMPATIBLE,
    WARNING,
    INCOMPATIBLE
}

data class ModelCompatibilityInfo(
    val level: CompatibilityLevel,
    val message: String
)

/**
 * Lifecycle state of a local model in device memory.
 */
enum class ModelStatus {
    NOT_LOADED,
    LOADING,
    LOADED,
    ERROR
}

/**
 * Model-agnostic representation of an on-device neural weight file (GGUF, etc.).
 * Designed to accommodate any model architecture (Qwen, Gemma, Llama, Phi, Mistral, Smollm, etc.)
 * under or around 8B parameters.
 */
data class ModelMetadata(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fileName: String,
    val filePath: String,
    val format: String = "GGUF",
    val architecture: String = "generic",
    val parameterCount: String = "Unknown",
    val quantization: String = "Q4_K_M",
    val fileSize: Long = 0L,
    val formattedSize: String = "0 B",
    val contextLength: Int = 4096,
    val capabilities: Set<ModelCapability> = setOf(ModelCapability.TEXT),
    val status: ModelStatus = ModelStatus.NOT_LOADED,
    val requiredProjector: Boolean = false,
    val projectorPath: String? = null,
    val compatibilityStatus: CompatibilityLevel = CompatibilityLevel.COMPATIBLE,
    val compatibilityMessage: String = "Compatible with on-device NPU/CPU accelerator.",
    val isActive: Boolean = false,
    val throughput: String = "184 MB/s (DMA)",
    val cpuThreads: Int = 6,
    val temperature: Float = 0.7f,
    val dateAdded: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: String,
    val isStreaming: Boolean = false,
    val tokenRate: String? = null,
    val tokenCount: Int = 0,
    val modelName: String = "Local Model",
    val codeSnippet: String? = null,
    val codeLanguage: String? = null,
    val attachedImageUri: String? = null,
    val attachedAudioDurationSec: Int? = null
)

data class AppSettings(
    val activeModelId: String? = null,
    val contextLength: Int = 4096,
    val temperature: Float = 0.7f,
    val maxResponseTokens: Int = 2048,
    val cpuThreads: Int = 6,
    val inferenceMode: String = "CPU / NPU Accelerated",
    val conversationHistoryEnabled: Boolean = true,
    val themeName: String = "Warm Light",
    val readingTextSize: String = "Medium (16px)",
    val isOnboardingCompleted: Boolean = false,
    val enableThinking: Boolean = false
)

data class HardwareTelemetry(
    val hostDevice: String,
    val socName: String,
    val cpuCoreCount: Int,
    val totalRamGB: Double,
    val freeRamGB: Double,
    val ramUsagePercent: Float,
    val totalStorageGB: Double,
    val freeStorageGB: Double,
    val storageUsagePercent: Float,
    val modelsStorageBytes: Long,
    val chatsStorageBytes: Long,
    val npuTemperatureCelsius: Int = 44
)
