package com.example.snapbadgers.ai.vision

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import kotlinx.coroutines.runBlocking
import kotlin.math.sqrt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisionEncoderFallbackTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var encoder: VisionEncoder? = null

    @After
    fun tearDown() {
        encoder?.close()
        encoder = null
    }

    @Test
    fun missingAssetFallsBackToStubEncoder() = runBlocking {
        encoder = VisionEncoder(context, modelAsset = "nonexistent_model_asset.tflite")
        val bitmap = solidColorBitmap(Color.rgb(200, 50, 50))

        val embedding = encoder!!.encode(bitmap)

        assertEquals(EMBEDDING_DIMENSION, embedding.size)
        assertFalse("Stub embedding should not be all zeros for a colored bitmap", embedding.all { it == 0f })
        assertTrue("All values must be finite", embedding.all { it.isFinite() })
        assertL2Normalized(embedding)
    }

    @Test
    fun stubOutputIsDeterministicForSameBitmap() = runBlocking {
        encoder = VisionEncoder(context, modelAsset = "nonexistent_model_asset.tflite")
        val bitmap = solidColorBitmap(Color.rgb(30, 180, 90))

        val first = encoder!!.encode(bitmap)
        val second = encoder!!.encode(bitmap)

        assertEquals(first.size, second.size)
        for (i in first.indices) {
            assertEquals("index $i must match across calls", first[i], second[i], 0f)
        }
    }

    @Test
    fun differentBitmapsProduceDifferentEmbeddings() = runBlocking {
        encoder = VisionEncoder(context, modelAsset = "nonexistent_model_asset.tflite")
        val red = solidColorBitmap(Color.rgb(220, 20, 20))
        val blue = solidColorBitmap(Color.rgb(20, 20, 220))

        val redEmbedding = encoder!!.encode(red)
        val blueEmbedding = encoder!!.encode(blue)

        val diff = redEmbedding.indices.sumOf { i ->
            val d = (redEmbedding[i] - blueEmbedding[i]).toDouble()
            d * d
        }
        assertTrue("Red and blue bitmaps should yield distinct stub embeddings (l2² diff=$diff)", diff > 1e-4)
    }

    @Test
    fun degenerateBitmapDimensionsAreHandled() = runBlocking {
        encoder = VisionEncoder(context, modelAsset = "nonexistent_model_asset.tflite")
        val tiny = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.rgb(128, 128, 128))
        }

        val embedding = encoder!!.encode(tiny)

        assertEquals(EMBEDDING_DIMENSION, embedding.size)
        assertTrue("All values must be finite for a 1x1 bitmap", embedding.all { it.isFinite() })
        assertL2Normalized(embedding)
    }

    private fun assertL2Normalized(embedding: FloatArray, tolerance: Float = 1e-4f) {
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("Embedding must be L2-normalized", 1f, norm, tolerance)
    }

    private fun solidColorBitmap(color: Int, size: Int = 64): Bitmap {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    setPixel(x, y, color)
                }
            }
        }
    }
}
