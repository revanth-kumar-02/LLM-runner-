package com.example.domain.adapter

import com.example.data.model.ModelMetadata

/**
 * Encapsulates architecture-specific prompt framing, special tokens, and stopping criteria.
 */
interface ModelAdapter {
    val architecture: String
    fun formatPrompt(rawPrompt: String, systemPrompt: String? = null): String
    fun getStopTokens(): List<String>
}

/**
 * Generic GGUF adapter handling standard chat templates (ChatML / Llama / Gemma / Phi).
 */
class GenericGGUFAdapter(override val architecture: String = "generic") : ModelAdapter {

    override fun formatPrompt(rawPrompt: String, systemPrompt: String?): String {
        return when {
            architecture.contains("llama", ignoreCase = true) -> {
                val sys = if (!systemPrompt.isNullOrBlank()) "<|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>" else ""
                "$sys<|start_header_id|>user<|end_header_id|>\n\n$rawPrompt<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
            }
            architecture.contains("gemma", ignoreCase = true) -> {
                "<start_of_turn>user\n$rawPrompt<end_of_turn>\n<start_of_turn>model\n"
            }
            architecture.contains("phi", ignoreCase = true) -> {
                "<|user|>\n$rawPrompt<|end|>\n<|assistant|>\n"
            }
            else -> {
                // Default ChatML format (Qwen, Mistral, Smollm, Deepseek, etc.)
                val sys = if (!systemPrompt.isNullOrBlank()) "<|im_start|>system\n$systemPrompt<|im_end|>\n" else ""
                "$sys<|im_start|>user\n$rawPrompt<|im_end|>\n<|im_start|>assistant\n"
            }
        }
    }

    override fun getStopTokens(): List<String> = when {
        architecture.contains("llama", ignoreCase = true) -> listOf("<|eot_id|>", "<|end_of_text|>")
        architecture.contains("gemma", ignoreCase = true) -> listOf("<end_of_turn>")
        architecture.contains("phi", ignoreCase = true) -> listOf("<|end|>")
        else -> listOf("<|im_end|>", "<|endoftext|>", "</s>")
    }
}

class ModelAdapterFactory {
    companion object {
        fun getAdapter(model: ModelMetadata): ModelAdapter {
            return GenericGGUFAdapter(model.architecture)
        }
    }
}
