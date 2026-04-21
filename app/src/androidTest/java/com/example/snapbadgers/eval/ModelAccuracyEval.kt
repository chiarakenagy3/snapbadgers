package com.example.snapbadgers.eval

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import com.example.snapbadgers.ai.text.StubTextEncoder
import com.example.snapbadgers.ai.text.TextEncoderFactory
import com.example.snapbadgers.ai.vision.VisionEncoder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
@LargeTest
class ModelAccuracyEval {

    companion object {
        private const val TAG = "EVAL"
    }

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    private var visionEncoder: VisionEncoder? = null

    @Before
    fun setUp() {
        System.gc()
        Thread.sleep(200)
    }

    @After
    fun tearDown() {
        visionEncoder?.close()
    }

    @Test
    fun textEncoderKnownInputs() = runBlocking {
        Log.i(TAG, "=== Text Encoder Accuracy Eval ===")

        val encoder = try {
            TextEncoderFactory.create(context)
        } catch (e: Throwable) {
            Log.i(TAG, "Model text encoder unavailable, using stub: ${e.message}")
            StubTextEncoder("Accuracy eval stub")
        }

        Log.i(TAG, "text_encoder: ${encoder.label}")

        val calmEmbedding = encoder.encode("calm relaxing music")
        assertEquals("Embedding dimension", EMBEDDING_DIMENSION, calmEmbedding.size)
        assertTrue("Non-blank input should produce non-zero embedding", calmEmbedding.any { abs(it) > 1e-6f })
        Log.i(TAG, "text_calm: dim=${calmEmbedding.size} norm=${"%.4f".format(sqrt(calmEmbedding.sumOf { (it * it).toDouble() }).toFloat())} nonzero=${calmEmbedding.count { abs(it) > 1e-6f }}")

        val blankEmbedding = encoder.encode("")
        assertTrue("Blank input should produce zero embedding", blankEmbedding.all { abs(it) < 1e-6f })
        Log.i(TAG, "text_blank: all_zero=true")

        val similarity = VectorUtils.cosineSimilarity(calmEmbedding, encoder.encode("intense workout running"))
        Log.i(TAG, "text_mood_separation: calm_vs_workout_similarity=$similarity")
        assertTrue("Different moods should produce different embeddings", similarity < 0.99f)
    }

    @Test
    fun textEncoderCrossRunConsistency() = runBlocking {
        Log.i(TAG, "=== Text Encoder Cross-Run Consistency ===")

        val encoder = try {
            TextEncoderFactory.create(context)
        } catch (e: Throwable) {
            StubTextEncoder("Consistency eval stub")
        }

        val testInput = "calm relaxing study music"
        val reference = encoder.encode(testInput)
        var identical = 0
        var different = 0

        repeat(10) {
            val current = encoder.encode(testInput)
            if (reference.contentEquals(current)) {
                identical++
            } else {
                different++
                Log.i(TAG, "text_consistency: run=$it max_diff=${reference.zip(current.toList()).maxOfOrNull { (a, b) -> abs(a - b) } ?: 0f}")
            }
        }

        Log.i(TAG, "text_consistency_10x: identical=$identical different=$different")
        assertTrue("At least 9/10 runs should be identical", identical >= 9)
    }

    @Test
    fun visionEncoderKnownInputs(): Unit = runBlocking {
        Log.i(TAG, "=== Vision Encoder Accuracy Eval ===")

        val encoder = VisionEncoder(context).also { visionEncoder = it }

        val colors = mapOf(
            "red" to Color.RED,
            "blue" to Color.BLUE,
            "green" to Color.GREEN,
            "white" to Color.WHITE,
            "black" to Color.BLACK
        )

        val embeddings = mutableMapOf<String, FloatArray>()

        for ((name, color) in colors) {
            val embedding = encoder.encode(createSolidBitmap(224, 224, color))

            assertEquals("Vision embedding should be ${EMBEDDING_DIMENSION}-d", EMBEDDING_DIMENSION, embedding.size)
            assertTrue("No NaN in vision embedding for $name", embedding.none { it.isNaN() })
            assertTrue("No Inf in vision embedding for $name", embedding.none { it.isInfinite() })

            Log.i(TAG, "vision_$name: dim=${embedding.size} norm=${"%.4f".format(sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat())} nonzero=${embedding.count { abs(it) > 1e-6f }}")

            embeddings[name] = embedding
        }

        Log.i(TAG, "vision_color_similarity:")
        Log.i(TAG, "  red_vs_blue: ${VectorUtils.cosineSimilarity(embeddings["red"]!!, embeddings["blue"]!!)}")
        Log.i(TAG, "  red_vs_green: ${VectorUtils.cosineSimilarity(embeddings["red"]!!, embeddings["green"]!!)}")
        Log.i(TAG, "  black_vs_white: ${VectorUtils.cosineSimilarity(embeddings["black"]!!, embeddings["white"]!!)}")
    }

    @Test
    fun visionEncoderStability() = runBlocking {
        Log.i(TAG, "=== Vision Encoder Stability ===")

        val encoder = VisionEncoder(context).also { visionEncoder = it }
        val bitmap = createSolidBitmap(224, 224, Color.rgb(128, 128, 128))
        val reference = encoder.encode(bitmap)
        var identical = 0
        var maxDrift = 0f

        repeat(10) {
            val current = encoder.encode(bitmap)
            if (reference.contentEquals(current)) {
                identical++
            } else {
                val diff = reference.zip(current.toList()).maxOfOrNull { (a, b) -> abs(a - b) } ?: 0f
                if (diff > maxDrift) maxDrift = diff
            }
        }

        Log.i(TAG, "vision_stability_10x: identical=$identical max_drift=$maxDrift")
        assertTrue("Vision encoder should be stable (identical or near-identical)", identical >= 8 || maxDrift < 0.01f)
    }

    @Test
    fun visionEncoderCrossRunConsistency() = runBlocking {
        Log.i(TAG, "=== Vision Encoder Cross-Run Consistency ===")

        val encoder = VisionEncoder(context).also { visionEncoder = it }
        val bitmap = createSolidBitmap(128, 128, Color.rgb(200, 100, 50))
        val reference = encoder.encode(bitmap)
        var allBitIdentical = true

        repeat(10) {
            if (!reference.contentEquals(encoder.encode(bitmap))) {
                allBitIdentical = false
            }
        }

        Log.i(TAG, "vision_cross_run_10x: bit_identical=$allBitIdentical")
        assertTrue("Vision encoder should be deterministic across 10 runs", allBitIdentical)
    }

    @Test
    fun heuristicEmbeddingAccuracy() {
        Log.i(TAG, "=== Heuristic Embedding Accuracy ===")

        assertTrue("'calm' should activate dimension 2", HeuristicTextEmbedding.encode("calm music")[2] > 0f)
        assertTrue("'study' should activate dimension 3", HeuristicTextEmbedding.encode("study focus")[3] > 0f)
        assertTrue("'happy' should activate dimension 4", HeuristicTextEmbedding.encode("happy vibes")[4] > 0f)
        assertTrue("'sad' should activate dimension 5", HeuristicTextEmbedding.encode("sad lonely")[5] > 0f)
        assertTrue("'workout' should activate dimension 6", HeuristicTextEmbedding.encode("workout energy")[6] > 0f)
        assertTrue("'night' should activate dimension 7", HeuristicTextEmbedding.encode("night drive")[7] > 0f)

        Log.i(TAG, "heuristic_keyword_accuracy: all 6 keywords correctly detected")

        listOf("calm" to "workout", "happy" to "sad", "study" to "night").forEach { (a, b) ->
            Log.i(TAG, "heuristic_separation: $a vs $b = ${VectorUtils.cosineSimilarity(HeuristicTextEmbedding.encode("$a music"), HeuristicTextEmbedding.encode("$b music"))}")
        }
    }

    private fun createSolidBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
