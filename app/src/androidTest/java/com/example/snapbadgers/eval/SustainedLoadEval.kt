package com.example.snapbadgers.eval

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sustained-load eval: runs the full pipeline continuously and reports
 * latency drift between early and late iterations as a proxy for thermal
 * throttling and any accumulated slowdown (allocator fragmentation,
 * native buffer leaks, TFLite cache invalidation).
 *
 * Complements the existing 4 baseline evals; no other eval exercises
 * a long continuous load with drift comparison.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SustainedLoadEval {

    companion object {
        private const val TAG = "EVAL"
        private const val WARMUP_ITERATIONS = 5
        private const val TOTAL_ITERATIONS = 200
        private const val EARLY_WINDOW = 20
        private const val LATE_WINDOW = 20
        private const val MAX_DRIFT_RATIO = 3.0
    }

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    private lateinit var pipeline: RecommendationPipeline
    private lateinit var testBitmap: Bitmap

    @Before
    fun setUp() {
        pipeline = RecommendationPipeline(context)
        testBitmap = solidColorBitmap(224, 224, Color.rgb(120, 140, 160))
        System.gc()
        Thread.sleep(200)
    }

    @Test
    fun sustainedPipelineLatencyDrift() = runBlocking {
        Log.i(TAG, "=== Sustained Pipeline Load (${TOTAL_ITERATIONS} iters) ===")

        repeat(WARMUP_ITERATIONS) {
            pipeline.runPipeline("warmup calm study music", imageBitmap = testBitmap, onStepUpdate = {})
        }

        val queries = listOf(
            "calm relaxing study music piano",
            "energetic workout playlist for running",
            "happy pop dance weekend vibes",
            "rainy day coffee shop acoustic"
        )

        val latenciesMs = LongArray(TOTAL_ITERATIONS)
        repeat(TOTAL_ITERATIONS) { i ->
            val query = queries[i % queries.size]
            val startNs = System.nanoTime()
            pipeline.runPipeline(query, imageBitmap = testBitmap, onStepUpdate = {})
            latenciesMs[i] = (System.nanoTime() - startNs) / 1_000_000
        }

        val earlyAvg = latenciesMs.take(EARLY_WINDOW).average()
        val lateAvg = latenciesMs.takeLast(LATE_WINDOW).average()
        val driftRatio = if (earlyAvg > 0) lateAvg / earlyAvg else Double.NaN
        val sorted = latenciesMs.sortedArray()
        val p50 = sorted[sorted.size / 2]
        val p95 = sorted[(sorted.size * 0.95).toInt()]
        val p99 = sorted[(sorted.size * 0.99).toInt()]

        Log.i(TAG, "sustained_total_iterations: ${TOTAL_ITERATIONS}")
        Log.i(TAG, "sustained_early_avg_ms: ${"%.2f".format(earlyAvg)}")
        Log.i(TAG, "sustained_late_avg_ms: ${"%.2f".format(lateAvg)}")
        Log.i(TAG, "sustained_drift_ratio: ${"%.3f".format(driftRatio)}")
        Log.i(TAG, "sustained_p50_ms: $p50 p95_ms: $p95 p99_ms: $p99")
        Log.i(TAG, "sustained_min_ms: ${sorted.first()} max_ms: ${sorted.last()}")

        val windowSize = TOTAL_ITERATIONS / 10
        val windowAverages = (0 until 10).map { idx ->
            latenciesMs.drop(idx * windowSize).take(windowSize).average()
        }
        Log.i(
            TAG,
            "sustained_window_avgs_ms: " + windowAverages.joinToString { "%.1f".format(it) }
        )

        assertTrue(
            "Late-window avg (${lateAvg}ms) must not exceed ${MAX_DRIFT_RATIO}× early-window avg (${earlyAvg}ms)",
            driftRatio.isNaN() || driftRatio <= MAX_DRIFT_RATIO
        )
        assertTrue("All iterations must complete in under 10s", sorted.last() < 10_000)
    }

    private fun solidColorBitmap(width: Int, height: Int, color: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    setPixel(x, y, color)
                }
            }
        }
    }
}
