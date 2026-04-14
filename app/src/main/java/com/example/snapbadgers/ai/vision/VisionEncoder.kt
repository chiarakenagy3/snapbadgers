package com.example.snapbadgers.ai.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.get
import com.example.snapbadgers.ai.common.ml.*
import com.example.snapbadgers.ml.QualcommVisionEncoder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.max

class VisionEncoder(
    context: Context,
    private val modelAsset: String = MODEL_ASSET
) : AutoCloseable {

    private val appContext = context.applicationContext
    private val modelInitMutex = Mutex()

    private var modelEncoder: QualcommVisionEncoder? = null
    private var forceStubFallback = !hasAsset(appContext, modelAsset)

    suspend fun encode(bitmap: Bitmap): FloatArray {
        if (forceStubFallback) {
            return encodeStub(bitmap)
        }

        val activeModelEncoder = getOrCreateModelEncoder() ?: return encodeStub(bitmap)

        return try {
            val embedding = activeModelEncoder.encode(bitmap)
            if (isZeroVector(embedding)) {
                logWarning("Vision model produced a zero vector. Falling back to stub image encoder.")
                switchToStubFallback()
                encodeStub(bitmap)
            } else {
                // Now passes through directly if 128-d thanks to VectorUtils update
                VectorUtils.alignToEmbeddingDimension(embedding, salt = MODEL_OUTPUT_SALT)
            }
        } catch (throwable: Throwable) {
            logWarning("Vision model inference failed. Falling back to stub image encoder.", throwable)
            switchToStubFallback()
            encodeStub(bitmap)
        }
    }

    override fun close() {
        modelEncoder?.close()
        modelEncoder = null
    }

    private suspend fun getOrCreateModelEncoder(): QualcommVisionEncoder? {
        modelEncoder?.let { return it }
        if (forceStubFallback) return null

        return modelInitMutex.withLock {
            // Double-check status after acquiring lock
            val existing = modelEncoder
            if (existing != null) {
                existing
            } else if (forceStubFallback) {
                null
            } else {
                runCatching {
                    QualcommVisionEncoder(context = appContext, modelPath = modelAsset)
                }.onFailure { throwable ->
                    logWarning("Vision model initialization failed. Falling back to stub image encoder.", throwable)
                    forceStubFallback = true
                }.getOrNull()?.also { initializedEncoder ->
                    modelEncoder = initializedEncoder
                }
            }
        }
    }

    private fun switchToStubFallback() {
        forceStubFallback = true
        modelEncoder?.close()
        modelEncoder = null
    }

    private fun encodeStub(bitmap: Bitmap): FloatArray {
        val width = bitmap.width.coerceAtLeast(1)
        val height = bitmap.height.coerceAtLeast(1)
        val stepX = max(1, width / SAMPLE_GRID_SIZE)
        val stepY = max(1, height / SAMPLE_GRID_SIZE)

        var samples = 0
        var redSum = 0f
        var greenSum = 0f
        var blueSum = 0f
        var brightnessSum = 0f

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap[x, y]
                val red = ((pixel shr 16) and 0xFF) / 255f
                val green = ((pixel shr 8) and 0xFF) / 255f
                val blue = (pixel and 0xFF) / 255f
                val brightness = (red + green + blue) / 3f

                redSum += red
                greenSum += green
                blueSum += blue
                brightnessSum += brightness
                samples += 1
            }
        }

        val embedding = FloatArray(EMBEDDING_DIMENSION)
        if (samples == 0) return embedding

        val avgRed = redSum / samples
        val avgGreen = greenSum / samples
        val avgBlue = blueSum / samples
        val avgBrightness = brightnessSum / samples
        
        // --- Standard Feature Mapping ---
        
        // Brightness correlates with Energy and Valence (Happy)
        embedding[IDX_VALENCE] = avgBrightness
        embedding[IDX_ENERGY] = avgBrightness
        
        // High blue content often suggests a "chill" or "mellow" (low energy, high acoustic) vibe
        if (avgBlue > avgRed && avgBlue > avgGreen) {
            embedding[IDX_ENERGY] = (embedding[IDX_ENERGY] * 0.8f).coerceAtLeast(0.1f)
            embedding[IDX_ACOUSTICNESS] = (avgBlue - max(avgRed, avgGreen)).coerceIn(0f, 1f)
        }
        
        // High red content often suggests "intensity" or "passion"
        if (avgRed > avgBlue && avgRed > avgGreen) {
            embedding[IDX_ENERGY] = (embedding[IDX_ENERGY] * 1.2f).coerceIn(0f, 1f)
            embedding[IDX_DANCEABILITY] = avgRed
        }

        // --- Geometric Backup (indices 10+) ---
        embedding[10] = (width / 2000f).coerceIn(0f, 1f)
        embedding[11] = (height / 2000f).coerceIn(0f, 1f)
        embedding[12] = avgRed
        embedding[13] = avgGreen
        embedding[14] = avgBlue

        return VectorUtils.normalize(embedding)
    }

    private fun isZeroVector(vector: FloatArray): Boolean {
        return vector.none { abs(it) > ZERO_THRESHOLD }
    }

    private fun hasAsset(context: Context, assetName: String): Boolean {
        return runCatching {
            context.assets.open(assetName).close()
            true
        }.getOrDefault(false)
    }

    private fun logWarning(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable == null) {
                Log.w(TAG, message)
            } else {
                Log.w(TAG, message, throwable)
            }
        }.getOrElse {
            val suffix = throwable?.let { ": ${it.message}" }.orEmpty()
            System.err.println("$TAG: $message$suffix")
        }
    }

    private companion object {
        const val TAG = "VisionEncoder"
        const val MODEL_ASSET = "efficientnet_b0_128d_int8.tflite"
        const val SAMPLE_GRID_SIZE = 16
        const val ZERO_THRESHOLD = 1e-6f
        const val MODEL_OUTPUT_SALT = 211
    }
}
