package com.example.backend.llama

/**
 * JNI bindings for the native on-device llama.cpp C++ inference runtime.
 */
class LlamaCppNative {

    fun interface TokenCallback {
        /**
         * Invoked on each generated token piece.
         * Return true to continue generation, or false to stop early.
         */
        fun onToken(token: String): Boolean
    }

    companion object {
        private val isLibraryLoaded: Boolean

        init {
            isLibraryLoaded = try {
                System.loadLibrary("llama-android")
                true
            } catch (_: Throwable) {
                false
            }
        }

        fun isAvailable(): Boolean = isLibraryLoaded
    }

    fun init(): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeInit()
            } catch (_: Throwable) {
                false
            }
        } else false
    }

    fun loadModel(modelPath: String, threads: Int, contextLength: Int): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeLoadModel(modelPath, threads, contextLength)
            } catch (_: Throwable) {
                false
            }
        } else false
    }

    fun unloadModel() {
        if (isLibraryLoaded) {
            try {
                nativeUnloadModel()
            } catch (_: Throwable) {}
        }
    }

    fun isLoaded(): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeIsLoaded()
            } catch (_: Throwable) {
                false
            }
        } else false
    }

    fun stopGeneration() {
        if (isLibraryLoaded) {
            try {
                nativeStopGeneration()
            } catch (_: Throwable) {}
        }
    }

    fun tokenize(text: String): Int {
        return if (isLibraryLoaded) {
            try {
                nativeTokenize(text)
            } catch (_: Throwable) {
                (text.length / 4).coerceAtLeast(1)
            }
        } else (text.length / 4).coerceAtLeast(1)
    }

    fun generateStream(
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        callback: TokenCallback
    ) {
        if (isLibraryLoaded) {
            try {
                nativeGenerateStream(prompt, temperature, maxTokens, callback)
            } catch (_: Throwable) {}
        }
    }

    // JNI external functions
    private external fun nativeInit(): Boolean
    private external fun nativeLoadModel(modelPath: String, threads: Int, contextLength: Int): Boolean
    private external fun nativeUnloadModel()
    private external fun nativeIsLoaded(): Boolean
    private external fun nativeStopGeneration()
    private external fun nativeTokenize(text: String): Int
    private external fun nativeGenerateStream(
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        callback: TokenCallback
    )
}
