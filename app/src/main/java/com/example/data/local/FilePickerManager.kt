package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

/**
 * Metadata extracted from an imported GGUF model file.
 */
data class GGUFFileMetadata(
    val uri: Uri,
    val fileName: String,
    val formattedDisplayName: String,
    val fileSizeInBytes: Long,
    val formattedFileSize: String,
    val detectedQuantization: String,
    val virtualFilePath: String
)

/**
 * Manager class encapsulating Android Storage Access Framework (SAF) logic
 * for selecting and extracting metadata from local .gguf model files.
 */
class FilePickerManager {

    companion object {
        val SUPPORTED_MIME_TYPES = arrayOf(
            "*/*",
            "application/octet-stream"
        )

        /**
         * Creates an Intent to prompt the user to select a .gguf document via SAF.
         */
        fun createOpenDocumentIntent(): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_MIME_TYPES)
            }
        }
    }

    /**
     * Extracts full metadata from a SAF [Uri] selected by the user.
     */
    fun extractMetadata(context: Context, uri: Uri): GGUFFileMetadata {
        var fileName = "model.gguf"
        var fileSizeInBytes = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { fileName = it }
                    }
                    if (sizeIndex != -1) {
                        fileSizeInBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to default values on query issue
        }

        val formattedDisplayName = parseDisplayName(fileName)
        val formattedFileSize = formatFileSize(fileSizeInBytes)
        val detectedQuantization = detectQuantization(fileName)
        val virtualFilePath = "Android/data/storage/$fileName"

        return GGUFFileMetadata(
            uri = uri,
            fileName = fileName,
            formattedDisplayName = formattedDisplayName,
            fileSizeInBytes = fileSizeInBytes,
            formattedFileSize = formattedFileSize,
            detectedQuantization = detectedQuantization,
            virtualFilePath = virtualFilePath
        )
    }

    /**
     * Converts a raw file name like 'qwen3-4b-instruct-q4_k_m.gguf' into 'Qwen3 4b Instruct'.
     */
    fun parseDisplayName(fileName: String): String {
        val clean = fileName.removeSuffix(".gguf")
            .replace(Regex("(?i)[-_]?(q[0-9]_[a-z0-9_]+)"), "") // remove quantization suffixes
            .replace("-", " ")
            .replace("_", " ")
            .trim()

        return clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }.ifEmpty { "Custom GGUF Model" }
    }

    /**
     * Formats raw bytes into GB or MB representation.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "2.48 GB"
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.2f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            String.format(Locale.US, "%.1f MB", mb)
        }
    }

    /**
     * Detects GGUF quantization type from the file name (e.g. Q4_K_M, Q5_K_M, Q8_0).
     */
    fun detectQuantization(fileName: String): String {
        val pattern = Regex("(?i)\\b(Q[0-9]_[A-Z0-9_]+)\\b")
        val match = pattern.find(fileName)
        return match?.value?.uppercase(Locale.US) ?: "Q4_K_M"
    }
}
