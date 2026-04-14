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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class InferenceLatencyEval {

    companion object {
        private const val TAG = "EVAL"
        private const val WARMUP_ITERATIONS = 10
        private const val BENCHMARK_ITERATIONS = 100
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

    private fun benchmarkLatency(label: String, warmup: Int = WARMUP_ITERATIONS, iterations: Int = BENCHMARK_ITERATIONS, block: () -> Unit) {
        repeat(warmup) { block() }

        val latencies = LongArray(iterations)
        repeat(iterations) { i ->
            val startNs = System.nanoTime()
            block()
            latencies[i] = System.nanoTime() - startNs
        }

        reportLatencies(label, latencies)
    }

    @Test
    fun textEncoderLatency() = runBlocking {
        Log.i(TAG, "=== Text Encoder Latency Eval ===")

        val encoder = try {
            TextEncoderFactory.create(context)
        } catch (e: Throwable) {
            Log.i(TAG, "Model text encoder unavailable, using StubTextEncoder: ${e.message}")
            StubTextEncoder("Benchmark stub")
        }

        Log.i(TAG, "text_encoder_type: ${encoder.label} mode=${encoder.mode}")

        benchmarkLatency("text_encoder") { encoder.encode("calm relaxing study music with piano") }
    }

    @Test
    fun visionEncoderLatency() = runBlocking {
        Log.i(TAG, "=== Vision Encoder Latency Eval ===")

        val encoder = VisionEncoder(context).also { visionEncoder = it }
        val testBitmap = createTestBitmap(224, 224, Color.rgb(128, 100, 200))

        benchmarkLatency("vision_encoder") { encoder.encode(testBitmap) }
    }

    @Test
    fun heuristicTextEmbeddingLatency() {
        Log.i(TAG, "=== Heuristic Text Embedding Latency Eval ===")

        benchmarkLatency("heuristic_text") { HeuristicTextEmbedding.encode("happy energetic workout playlist for morning run") }
    }

    @Test
    fun fullPipelineLatency() = runBlocking {
        Log.i(TAG, "=== Full Pipeline Latency Eval ===")

        val textEncoder = try {
            TextEncoderFactory.create(context)
        } catch (e: Throwable) {
            StubTextEncoder("Benchmark stub")
        }
        val encoder = VisionEncoder(context).also { visionEncoder = it }
        val fusionEngine = FusionEngine()
        val projectionNetwork = ProjectionNetwork()
        val testBitmap = createTestBitmap(224, 224, Color.rgb(100, 150, 200))
        val testInput = "energetic workout music"
        val sensorEmbedding = VectorUtils.normalize(FloatArray(128) { (it * 7 % 23).toFloat() })

        val sampleSongs = listOf(
            Song("Song A", "Artist A", embedding = HeuristicTextEmbedding.encode("happy dance pop")),
            Song("Song B", "Artist B", embedding = HeuristicTextEmbedding.encode("calm study ambient")),
            Song("Song C", "Artist C", embedding = HeuristicTextEmbedding.encode("workout energy rock"))
        )

        repeat(WARMUP_ITERATIONS) {
            val text = textEncoder.encode(testInput)
            val vision = encoder.encode(testBitmap)
            val fused = fusionEngine.fuse(text, vision, sensorEmbedding)
            val projected = projectionNetwork.project(fused)
            val query = VectorUtils.alignToEmbeddingDimension(projected, salt = 101)
            sampleSongs.map { VectorUtils.cosineSimilarity(query, it.embedding) }
        }

        val latencies = LongArray(BENCHMARK_ITERATIONS)
        repeat(BENCHMARK_ITERATIONS) { i ->
            val startNs = System.nanoTime()

            val text = textEncoder.encode(testInput)
            val vision = encoder.encode(testBitmap)
            val fused = fusionEngine.fuse(text, vision, sensorEmbedding)
            val projected = projectionNetwork.project(fused)
            val query = VectorUtils.alignToEmbeddingDimension(projected, salt = 101)
            sampleSongs
                .map { it.copy(similarity = VectorUtils.cosineSimilarity(query, it.embedding)) }
                .sortedByDescending { it.similarity }

            latencies[i] = System.nanoTime() - startNs
        }

        reportLatencies("full_pipeline", latencies)

        Log.i(TAG, "--- Stage breakdown (single pass) ---")
        measureStage("text_encode") { textEncoder.encode(testInput) }
        measureStage("vision_encode") { encoder.encode(testBitmap) }

        val textEmb = textEncoder.encode(testInput)
        val visionEmb = encoder.encode(testBitmap)
        measureStage("fusion") { fusionEngine.fuse(textEmb, visionEmb, sensorEmbedding) }

        val fused = fusionEngine.fuse(textEmb, visionEmb, sensorEmbedding)
        measureStage("projection") { projectionNetwork.project(fused) }

        val projected = projectionNetwork.project(fused)
        val query = VectorUtils.alignToEmbeddingDimension(projected, salt = 101)
        measureStage("ranking") {
            sampleSongs
                .map { it.copy(similarity = VectorUtils.cosineSimilarity(query, it.embedding)) }
                .sortedByDescending { it.similarity }
        }
    }

    @Test
    fun nnapiDelegateStatus() {
        Log.i(TAG, "=== NNAPI Delegate Status ===")

        try {
            val delegate = org.tensorflow.lite.nnapi.NnApiDelegate()
            Log.i(TAG, "nnapi_delegate: created_successfully=true")
            delegate.close()
        } catch (e: Throwable) {
            Log.i(TAG, "nnapi_delegate: created_successfully=false error=${e.message}")
        }

        try {
            Log.i(TAG, "device_info: board=${android.os.Build.BOARD} hardware=${android.os.Build.HARDWARE} soc=${android.os.Build.SOC_MODEL}")
            Log.i(TAG, "device_info: sdk=${android.os.Build.VERSION.SDK_INT} model=${android.os.Build.MODEL}")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not read device info: ${e.message}")
        }
    }

    private fun reportLatencies(label: String, latencies: LongArray) {
        val sorted = latencies.sorted()
        val p50 = sorted[sorted.size / 2]
        val p95 = sorted[(sorted.size * 0.95).toInt()]
        val p99 = sorted[(sorted.size * 0.99).toInt()]

        Log.i(TAG, "$label latency (${latencies.size} iterations):")
        Log.i(TAG, "  p50=${"%.3f".format(p50 / 1e6)}ms p95=${"%.3f".format(p95 / 1e6)}ms p99=${"%.3f".format(p99 / 1e6)}ms")
        Log.i(TAG, "  min=${"%.3f".format(sorted.first() / 1e6)}ms max=${"%.3f".format(sorted.last() / 1e6)}ms avg=${"%.3f".format(sorted.average() / 1e6)}ms")

        assertTrue("$label p99 should be under 10 seconds", p99 < 10_000_000_000L)
    }

    private suspend fun measureStage(label: String, block: suspend () -> Any) {
        val startNs = System.nanoTime()
        block()
        Log.i(TAG, "  stage_$label: ${"%.3f".format((System.nanoTime() - startNs) / 1e6)}ms")
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
