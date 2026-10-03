package com.example.domain.checker

import com.example.data.model.CompatibilityLevel
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ModelCapability
import com.example.data.model.ModelCompatibilityInfo

/**
 * Validates whether an imported model is technically viable on the host device.
 * Enforces the policy: "Run compatible AI models under approximately 8B parameters directly on Android."
 */
class ModelCompatibilityChecker {

    fun checkCompatibility(
        architecture: String,
        parameterCountStr: String,
        fileSizeBytes: Long,
        capabilities: Set<ModelCapability>,
        requiredProjector: Boolean,
        projectorPath: String?,
        telemetry: HardwareTelemetry,
        isValidGGUF: Boolean = true,
        validationErrorMessage: String? = null
    ): ModelCompatibilityInfo {
        // 0. Invalid GGUF format check
        if (!isValidGGUF) {
            return ModelCompatibilityInfo(
                level = CompatibilityLevel.INCOMPATIBLE,
                message = validationErrorMessage ?: "Invalid or unsupported GGUF file: magic bytes mismatch."
            )
        }

        // 1. RAM footprint check
        val requiredRamGB = (fileSizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)) + 0.8
        if (requiredRamGB > telemetry.totalRamGB) {
            return ModelCompatibilityInfo(
                level = CompatibilityLevel.INCOMPATIBLE,
                message = "Model requires ~${String.format(java.util.Locale.US, "%.1f", requiredRamGB)} GB RAM, which exceeds total device unified memory (${Math.round(telemetry.totalRamGB)} GB)."
            )
        }

        // 2. Parameter count evaluation (~8B threshold)
        val paramNum = extractParameterNumber(parameterCountStr)
        if (paramNum > 8.5) {
            return ModelCompatibilityInfo(
                level = CompatibilityLevel.WARNING,
                message = "Large model (${parameterCountStr}) — exceeds the recommended ~8B threshold; may cause high thermal throttling or memory pressure."
            )
        }

        // 3. Multimodal projector check
        if (requiredProjector && projectorPath.isNullOrBlank()) {
            val modality = when {
                capabilities.contains(ModelCapability.VISION) && capabilities.contains(ModelCapability.AUDIO) -> "vision & audio"
                capabilities.contains(ModelCapability.VISION) -> "vision"
                capabilities.contains(ModelCapability.AUDIO) -> "audio"
                else -> "multimodal"
            }
            return ModelCompatibilityInfo(
                level = CompatibilityLevel.WARNING,
                message = "Multimodal ($modality) model requires an auxiliary projector (.mmproj GGUF). Text inference remains functional."
            )
        }

        // 4. Memory headroom warning (if free RAM is very tight)
        val freeRam = telemetry.freeRamGB
        if (requiredRamGB > freeRam * 1.2) {
            return ModelCompatibilityInfo(
                level = CompatibilityLevel.WARNING,
                message = "Model memory (~${String.format(java.util.Locale.US, "%.1f", requiredRamGB)} GB) exceeds currently available free RAM (~${String.format(java.util.Locale.US, "%.1f", freeRam)} GB). Other background apps will be suspended."
            )
        }

        // 5. Fully compatible
        return ModelCompatibilityInfo(
            level = CompatibilityLevel.COMPATIBLE,
            message = "Fully compatible with device on-device NPU/CPU accelerator."
        )
    }

    private fun extractParameterNumber(paramStr: String): Double {
        val clean = paramStr.lowercase().replace("b", "").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }
}
