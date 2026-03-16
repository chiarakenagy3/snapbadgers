package com.example.snapbadgers.ai.text

import android.util.Log
import kotlin.math.abs

class FallbackTextEncoder(
    private val primary: TextEncoder,
    private val fallback: TextEncoder,
    private val tag: String = "FallbackTextEncoder"
) : TextEncoder, AutoCloseable {

    private var activeEncoder: TextEncoder = primary

    override val mode: TextEncoderMode
        get() = activeEncoder.mode

    override val label: String
        get() = activeEncoder.label

    override suspend fun encode(text: String): FloatArray {
        val currentEncoder = activeEncoder
        if (currentEncoder === fallback) {
            return fallback.encode(text)
        }

        return try {
            val result = currentEncoder.encode(text)
            if (text.isNotBlank() && isZeroVector(result)) {
                logWarning("Model-backed text encoder produced a zero vector for non-blank input. Falling back to stub.")
                switchToFallback()
                fallback.encode(text)
            } else {
                result
            }
        } catch (throwable: Throwable) {
            logWarning("Model-backed text encoder failed during encode. Falling back to stub.", throwable)
            switchToFallback()
            fallback.encode(text)
        }
    }

    override fun close() {
        (activeEncoder as? AutoCloseable)?.close()
        if (activeEncoder !== fallback) {
            (fallback as? AutoCloseable)?.close()
        }
    }

    private fun switchToFallback() {
        if (activeEncoder === fallback) return
        (activeEncoder as? AutoCloseable)?.close()
        activeEncoder = fallback
    }

    private fun isZeroVector(vector: FloatArray): Boolean {
        return vector.none { abs(it) > ZERO_THRESHOLD }
    }

    private fun logWarning(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable == null) {
                Log.w(tag, message)
            } else {
                Log.w(tag, message, throwable)
            }
        }.getOrElse {
            val suffix = throwable?.let { ": ${it.message}" }.orEmpty()
            System.err.println("$tag: $message$suffix")
        }
    }

    private companion object {
        const val ZERO_THRESHOLD = 1e-6f
    }
}
