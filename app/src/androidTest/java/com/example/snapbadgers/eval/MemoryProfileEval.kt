package com.example.snapbadgers.eval

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.projection.ProjectionNetwork
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import com.example.snapbadgers.ai.text.StubTextEncoder
import com.example.snapbadgers.ai.text.TextEncoderFactory
import com.example.snapbadgers.ai.vision.VisionEncoder
import com.example.snapbadgers.model.Song
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MemoryProfileEval {

    companion object {
        private const val TAG = "EVAL"
        private const val PIPELINE_ITERATIONS = 50
        private const val LEAK_CHECK_ITERATIONS = 30
    }

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    private var visionEncoder: VisionEncoder? = null

    @Before
    fun setUp() {
        forceGc()
    }

    @After
    fun tearDown() {
        visionEncoder?.close()
    }

    @Test
    fun baselineMemory() {
        forceGc()
        val runtime = Runtime.getRuntime()
        val usedKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024
        val totalKb = runtime.totalMemory() / 1024
        val maxKb = runtime.maxMemory() / 1024

        Log.i(TAG, "=== Memory Baseline ===")
        Log.i(TAG, "baseline: used_kb=$usedKb total_kb=$totalKb max_kb=$maxKb")
        Log.i(TAG, "baseline: used_mb=${usedKb / 1024} total_mb=${totalKb / 1024} max_mb=${maxKb / 1024}")
        assertTrue("Used memory should be positive", usedKb > 0)
        assertTrue("Max memory should be positive", maxKb > 0)
    }

    @Test
    fun peakMemoryAfterPipelineIterations() = runBlocking {
        Log.i(TAG, "=== Peak Memory After $PIPELINE_ITERATIONS Pipeline Iterations ===")

        val textEncoder = try {
            TextEncoderFactory.create(context)
        } catch (e: Throwable) {
            StubTextEncoder("Memory eval stub")
        }
        val encoder = VisionEncoder(context).also { visionEncoder = it }
        val fusionEngine = FusionEngine()
        val projectionNetwork = ProjectionNetwork()
        val testBitmap = createTestBitmap(224, 224, Color.rgb(128, 128, 128))
        val sensorEmbedding = VectorUtils.normalize(FloatArray(128) { (it * 7 % 23).toFloat() })

        val sampleSongs = listOf(
            Song("Song A", "Artist A", embedding = HeuristicTextEmbedding.encode("happy dance pop")),
            Song("Song B", "Artist B", embedding = HeuristicTextEmbedding.encode("calm study ambient")),
            Song("Song C", "Artist C", embedding = HeuristicTextEmbedding.encode("workout energy rock"))
        )

        forceGc()
        val baselineKb = usedMemoryKb()
        Log.i(TAG, "pipeline_baseline_kb: $baselineKb")

        val memorySnapshots = mutableListOf<Long>()

        repeat(PIPELINE_ITERATIONS) { iteration ->
            val fused = fusionEngine.fuse(
                textEncoder.encode("energetic workout music"),
                encoder.encode(testBitmap),
                sensorEmbedding
            )
            val query = VectorUtils.alignToEmbeddingDimension(projectionNetwork.project(fused), salt = 101)
            sampleSongs
                .map { it.copy(similarity = VectorUtils.cosineSimilarity(query, it.embedding)) }
                .sortedByDescending { it.similarity }

            if (iteration % 10 == 0) {
                forceGc()
                val currentKb = usedMemoryKb()
                memorySnapshots.add(currentKb)
                Log.i(TAG, "pipeline_iter_$iteration: used_kb=$currentKb delta_kb=${currentKb - baselineKb}")
            }
        }

        forceGc()
        val finalKb = usedMemoryKb()
        val peakKb = memorySnapshots.maxOrNull() ?: finalKb

        Log.i(TAG, "pipeline_peak_kb: $peakKb")
        Log.i(TAG, "pipeline_final_kb: $finalKb")
        Log.i(TAG, "pipeline_delta_kb: ${finalKb - baselineKb}")
        Log.i(TAG, "pipeline_snapshots: ${memorySnapshots.joinToString()}")
        assertTrue("Peak memory should be positive", peakKb > 0)
        assertTrue("Should have collected memory snapshots", memorySnapshots.isNotEmpty())
    }

    @Test
    fun leakDetectionMonotonicGrowth() = runBlocking {
        Log.i(TAG, "=== Leak Detection: Monotonic Growth Check ===")

        val fusionEngine = FusionEngine()
        val projectionNetwork = ProjectionNetwork()

        forceGc()
        val baselineKb = usedMemoryKb()

        val snapshots = mutableListOf<Long>()

        repeat(LEAK_CHECK_ITERATIONS) { iteration ->
            repeat(10) {
                projectionNetwork.project(
                    fusionEngine.fuse(HeuristicTextEmbedding.encode("test query $iteration $it"), null, null)
                )
            }

            forceGc()
            val currentKb = usedMemoryKb()
            snapshots.add(currentKb)

            if (iteration % 10 == 0) {
                Log.i(TAG, "leak_check_iter_$iteration: used_kb=$currentKb delta_from_baseline=${currentKb - baselineKb}")
            }
        }

        var consecutiveIncreases = 0
        var maxConsecutiveIncreases = 0
        for (i in 1 until snapshots.size) {
            if (snapshots[i] > snapshots[i - 1]) {
                consecutiveIncreases++
                if (consecutiveIncreases > maxConsecutiveIncreases) {
                    maxConsecutiveIncreases = consecutiveIncreases
                }
            } else {
                consecutiveIncreases = 0
            }
        }

        val firstThird = snapshots.take(snapshots.size / 3).average()
        val lastThird = snapshots.takeLast(snapshots.size / 3).average()

        Log.i(TAG, "leak_check_summary:")
        Log.i(TAG, "  iterations: $LEAK_CHECK_ITERATIONS (x10 ops each)")
        Log.i(TAG, "  max_consecutive_increases: $maxConsecutiveIncreases")
        Log.i(TAG, "  first_third_avg_kb: ${"%.0f".format(firstThird)}")
        Log.i(TAG, "  last_third_avg_kb: ${"%.0f".format(lastThird)}")
        Log.i(TAG, "  growth_kb: ${"%.0f".format(lastThird - firstThird)}")
        Log.i(TAG, "  snapshots: ${snapshots.joinToString()}")

        assertTrue(
            "Max consecutive memory increases ($maxConsecutiveIncreases) should be < 20 (leak indicator)",
            maxConsecutiveIncreases < 20
        )
    }

    @Test
    fun visionEncoderMemoryImpact() = runBlocking {
        Log.i(TAG, "=== Vision Encoder Memory Impact ===")

        forceGc()
        val beforeKb = usedMemoryKb()

        val encoder = VisionEncoder(context).also { visionEncoder = it }

        forceGc()
        val afterCreateKb = usedMemoryKb()
        Log.i(TAG, "vision_create_delta_kb: ${afterCreateKb - beforeKb}")

        val bitmap = createTestBitmap(224, 224, Color.rgb(100, 150, 200))
        encoder.encode(bitmap)

        forceGc()
        val afterFirstInferenceKb = usedMemoryKb()
        Log.i(TAG, "vision_first_inference_delta_kb: ${afterFirstInferenceKb - afterCreateKb}")

        repeat(20) { encoder.encode(bitmap) }

        forceGc()
        val afterManyInferencesKb = usedMemoryKb()
        Log.i(TAG, "vision_20_inferences_delta_kb: ${afterManyInferencesKb - afterFirstInferenceKb}")
        Log.i(TAG, "vision_total_delta_kb: ${afterManyInferencesKb - beforeKb}")
        assertTrue("Memory before encoder creation should be positive", beforeKb > 0)
        assertTrue("Memory after encoder creation should be positive", afterCreateKb > 0)
    }

    @Test
    fun textEncoderMemoryImpact() = runBlocking {
        Log.i(TAG, "=== Text Encoder Memory Impact ===")

        forceGc()
        val beforeKb = usedMemoryKb()

        val encoder = try {
            TextEncoderFactory.create(context)
        } catch (e: Throwable) {
            StubTextEncoder("Memory eval stub")
        }

        forceGc()
        val afterCreateKb = usedMemoryKb()
        Log.i(TAG, "text_encoder_type: ${encoder.label}")
        Log.i(TAG, "text_create_delta_kb: ${afterCreateKb - beforeKb}")

        encoder.encode("test input")

        forceGc()
        val afterFirstInferenceKb = usedMemoryKb()
        Log.i(TAG, "text_first_inference_delta_kb: ${afterFirstInferenceKb - afterCreateKb}")

        repeat(20) { encoder.encode("test input iteration $it") }

        forceGc()
        val afterManyInferencesKb = usedMemoryKb()
        Log.i(TAG, "text_20_inferences_delta_kb: ${afterManyInferencesKb - afterFirstInferenceKb}")
        Log.i(TAG, "text_total_delta_kb: ${afterManyInferencesKb - beforeKb}")
        assertTrue("Memory before encoder creation should be positive", beforeKb > 0)
        assertNotNull("Text encoder should be created", encoder)
    }

    private fun forceGc() {
        System.gc()
        System.runFinalization()
        System.gc()
        Thread.sleep(100)
    }

    private fun usedMemoryKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024
    }

    private fun createTestBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
