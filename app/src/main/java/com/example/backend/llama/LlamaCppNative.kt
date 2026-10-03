package com.example.backend.llama

import android.util.Log

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
        private const val TAG = "LlamaCppNative"
        private val isLibraryLoaded: Boolean

        init {
            var loaded = false
            try {
                // Load OpenMP runtime first if available
                try {
                    System.loadLibrary("omp")
                    Log.i(TAG, "libomp.so loaded successfully.")
                } catch (e: Throwable) {
                    Log.w(TAG, "libomp.so direct load note: ${e.message}")
                }

                // Load native llama-android bridge
                System.loadLibrary("llama-android")
                Log.i(TAG, "libllama-android.so loaded successfully.")
                loaded = true
            } catch (t: Throwable) {
                Log.e(TAG, "FATAL: Failed to load native library libllama-android.so", t)
                loaded = false
            }
            isLibraryLoaded = loaded
        }

        fun isAvailable(): Boolean = isLibraryLoaded
    }

    fun init(): Boolean {
        Log.i(TAG, "init() called. isLibraryLoaded=$isLibraryLoaded")
        return if (isLibraryLoaded) {
            try {
                val res = nativeInit()
                Log.i(TAG, "nativeInit returned: $res")
                res
            } catch (t: Throwable) {
                Log.e(TAG, "nativeInit threw exception", t)
                false
            }
        } else false
    }

    fun loadModel(modelPath: String, threads: Int, contextLength: Int): Boolean {
        Log.i(TAG, "loadModel(path=$modelPath, threads=$threads, ctx=$contextLength) called.")
        if (!isLibraryLoaded) {
            Log.e(TAG, "loadModel aborted: native library is not loaded.")
            return false
        }
        return try {
            val res = nativeLoadModel(modelPath, threads, contextLength)
            Log.i(TAG, "nativeLoadModel returned: $res")
            res
        } catch (t: Throwable) {
            Log.e(TAG, "nativeLoadModel crashed", t)
            false
        }
    }

    fun unloadModel() {
        Log.i(TAG, "unloadModel() called.")
        if (isLibraryLoaded) {
            try {
                nativeUnloadModel()
            } catch (t: Throwable) {
                Log.e(TAG, "nativeUnloadModel failed", t)
            }
        }
    }

    fun isLoaded(): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeIsLoaded()
            } catch (t: Throwable) {
                Log.e(TAG, "nativeIsLoaded failed", t)
                false
            }
        } else false
    }

    fun stopGeneration() {
        Log.i(TAG, "stopGeneration() called.")
        if (isLibraryLoaded) {
            try {
                nativeStopGeneration()
            } catch (t: Throwable) {
                Log.e(TAG, "nativeStopGeneration failed", t)
            }
        }
    }

    fun tokenize(text: String): Int {
        return if (isLibraryLoaded) {
            try {
                nativeTokenize(text)
            } catch (t: Throwable) {
                Log.e(TAG, "nativeTokenize failed", t)
                (text.length / 4).coerceAtLeast(1)
            }
        } else (text.length / 4).coerceAtLeast(1)
    }

    fun generateStream(
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        enableThinking: Boolean = false,
        callback: TokenCallback
    ) {
        Log.i(TAG, "generateStream called. isLibraryLoaded=$isLibraryLoaded, enableThinking=$enableThinking, prompt length=${prompt.length}")
        if (isLibraryLoaded) {
            try {
                nativeGenerateStream(prompt, temperature, maxTokens, enableThinking, callback)
                Log.i(TAG, "nativeGenerateStream completed.")
            } catch (t: Throwable) {
                Log.e(TAG, "nativeGenerateStream crashed", t)
            }
        } else {
            Log.e(TAG, "generateStream cannot execute: native library not loaded.")
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
        enableThinking: Boolean,
        callback: TokenCallback
    )
}
