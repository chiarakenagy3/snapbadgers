package com.example.snapbadgers.ai.common

import android.content.Context
import android.util.Log
import kotlin.math.abs

object EncoderUtils {

    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable == null) {
                Log.w(tag, message)
            } else {
                Log.w(tag, message, throwable)
            }
        }.getOrElse {
            val suffix = throwable?.let { t -> ": ${t.message}" }.orEmpty()
            System.err.println("$tag: $message$suffix")
        }
    }

    fun isZeroVector(vector: FloatArray, threshold: Float = 1e-6f): Boolean {
        return vector.none { abs(it) > threshold }
    }

    fun hasAsset(context: Context, assetName: String): Boolean {
        return runCatching {
            context.assets.open(assetName).use { }
            true
        }.getOrDefault(false)
    }
}
