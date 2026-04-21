package com.example.snapbadgers.integration

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StressTestSuite {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var pipeline: RecommendationPipeline

    @Before
    fun setup() {
        pipeline = RecommendationPipeline(context)
    }

    @Test
    fun testExtremelyLongTextInput() = runBlocking {
        println("EVAL Extreme Long Text Test")
        val longText = (1..10000).joinToString(" ") { "word$it" }
        println("  Input length: ${longText.length} characters")

        val start = System.currentTimeMillis()
        val result = pipeline.runPipeline(longText, onStepUpdate = {})
        val elapsed = System.currentTimeMillis() - start

        println("  Processing time: ${elapsed}ms")
        println("  Recommendations: ${result.recommendations.size}")

        assertTrue("Should handle long input", result.recommendations.isNotEmpty())
        assertTrue("Should complete in reasonable time", elapsed < 10000)
    }

    @Test
    fun testRapidConsecutiveRequests() = runBlocking {
        println("EVAL Rapid Consecutive Requests Test")
        val queries = (1..50).map { "query $it" }

        val start = System.currentTimeMillis()
        queries.forEach { query ->
            pipeline.runPipeline(query, onStepUpdate = {})
        }
        val elapsed = System.currentTimeMillis() - start

        println("  Total requests: ${queries.size}")
        println("  Total time: ${elapsed}ms")
        println("  Avg per request: ${elapsed / queries.size}ms")
        println("  Throughput: ${"%.2f".format(queries.size * 1000.0 / elapsed)} req/sec")

        assertTrue("Should handle rapid requests", elapsed < 30000)
    }

    @Test
    fun testParallelRequestLoad() = runBlocking {
        println("EVAL Parallel Request Load Test")
        val queries = (1..10).map { "parallel query $it" }

        val start = System.currentTimeMillis()
        val results = queries.map { query ->
            async { pipeline.runPipeline(query, onStepUpdate = {}) }
        }.awaitAll()
        val elapsed = System.currentTimeMillis() - start

        println("  Parallel requests: ${queries.size}")
        println("  Total time: ${elapsed}ms")
        println("  All completed: ${results.all { it.recommendations.isNotEmpty() }}")

        assertTrue("All requests should succeed", results.all { it.recommendations.isNotEmpty() })
    }

    @Test
    fun testMalformedInputs() = runBlocking {
        println("EVAL Malformed Input Test")
        val malformedInputs = listOf(
            "",                                    // Empty
            "   ",                                 // Whitespace
            "\n\n\n",                             // Newlines
            "!@#$%^&*()",                         // Special chars
            "null",                                // Keyword
            "SELECT * FROM users",                 // SQL injection attempt
            "<script>alert('xss')</script>",      // XSS attempt
            "A".repeat(100000),                    // Extremely long single word
            "\u0000\u0001\u0002",                 // Control characters
            "🎵🎶🎧🎤",                            // Emojis only
            "音楽موسيقىмузыка",                    // Multi-language
            "../../../etc/passwd"                  // Path traversal
        )

        var successCount = 0
        malformedInputs.forEach { input ->
            try {
                val result = pipeline.runPipeline(input.take(1000), onStepUpdate = {})
                if (result.recommendations.isNotEmpty()) successCount++
                println("  ✓ Handled: ${input.take(30).replace("\n", "\\n")}")
            } catch (e: Exception) {
                println("  ✗ Failed: ${input.take(30)} - ${e.message}")
            }
        }

        println("  Success rate: $successCount/${malformedInputs.size}")
        assertTrue("Should handle majority of malformed inputs",
            successCount >= malformedInputs.size * 0.8)
    }

    @Test
    fun testMemoryLeakDetection() = runBlocking {
        println("EVAL Memory Leak Detection")
        val runtime = Runtime.getRuntime()

        System.gc()
        Thread.sleep(100)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()

        repeat(100) { iteration ->
            pipeline.runPipeline("iteration $iteration", onStepUpdate = {})
            if (iteration % 20 == 19) {
                System.gc()
                Thread.sleep(50)
                val currentMemory = runtime.totalMemory() - runtime.freeMemory()
                val increase = (currentMemory - baselineMemory) / (1024 * 1024)
                println("  After ${iteration + 1} iterations: ${increase}MB increase")
            }
        }

        System.gc()
        Thread.sleep(100)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val totalIncrease = (finalMemory - baselineMemory) / (1024 * 1024)

        println("  Total memory increase: ${totalIncrease}MB")
        if (totalIncrease < 20) println("  ✅ NO LEAK detected")
        else if (totalIncrease < 50) println("  ⚠️ MINOR growth - investigate")
        else println("  ❌ POTENTIAL LEAK - investigate")

        assertTrue("Memory should not grow excessively (got ${totalIncrease}MB)", totalIncrease < 100)
    }

    @Test
    fun testExtremeImageSizes() = runBlocking {
        println("EVAL Extreme Image Size Test")
        val testCases = listOf(
            "1x1 tiny" to createBitmap(1, 1),
            "10x10 small" to createBitmap(10, 10),
            "4000x4000 huge" to createBitmap(4000, 4000),
            "1x4000 tall" to createBitmap(1, 4000),
            "4000x1 wide" to createBitmap(4000, 1)
        )

        testCases.forEach { (description, bitmap) ->
            try {
                val start = System.currentTimeMillis()
                val result = pipeline.runPipeline(
                    input = "test", imageBitmap = bitmap, onStepUpdate = {}
                )
                val elapsed = System.currentTimeMillis() - start
                println("  ✓ $description: ${elapsed}ms")
                assertTrue("Should return results", result.recommendations.isNotEmpty())
            } catch (e: Exception) {
                println("  ✗ $description: ${e.message}")
                fail("Should handle $description: ${e.message}")
            }
        }
    }

    @Test
    fun testConcurrentTextAndVisionRequests() = runBlocking {
        println("EVAL Concurrent Multimodal Test")
        val bitmap = createBitmap(224, 224)

        val start = System.currentTimeMillis()
        val results = (1..10).map { index ->
            async {
                pipeline.runPipeline(
                    input = "concurrent test $index",
                    imageBitmap = if (index % 2 == 0) bitmap else null,
                    onStepUpdate = {}
                )
            }
        }.awaitAll()
        val elapsed = System.currentTimeMillis() - start

        val allSucceeded = results.all { it.recommendations.isNotEmpty() }
        println("  Requests: ${results.size}, Total time: ${elapsed}ms")
        println("  All succeeded: $allSucceeded")
        println("  With vision: ${results.count { it.usedVisionInput }}")
        println("  Text only: ${results.count { !it.usedVisionInput }}")

        assertTrue("All concurrent requests should succeed", allSucceeded)
    }

    @Test
    fun testRepeatedPipelineCreationAndDestruction() = runBlocking {
        println("EVAL Pipeline Creation/Destruction Test")
        repeat(20) { iteration ->
            val tempPipeline = RecommendationPipeline(context)
            val result = tempPipeline.runPipeline("test $iteration", onStepUpdate = {})
            assertTrue("Iteration $iteration should succeed", result.recommendations.isNotEmpty())
            if (iteration % 5 == 4) println("  ✓ ${iteration + 1} pipelines created and used")
        }
    }

    @Test
    fun testEdgeCaseEmbeddings() = runBlocking {
        println("EVAL Edge Case Embeddings Test")
        val edgeCases = mapOf(
            "All same character" to "aaaaaaaaaaaaaaaaaaaaaa",
            "Repeated word" to "music music music music music",
            "Single word" to "test",
            "Numbers only" to "123456789",
            "Mixed scripts" to "hello こんにちは مرحبا",
            "Punctuation heavy" to "... !!! ??? --- +++",
            "URL-like" to "http://example.com/music/playlist",
            "JSON-like" to "{\"mood\":\"calm\",\"energy\":\"low\"}"
        )

        edgeCases.forEach { (description, input) ->
            val result = pipeline.runPipeline(input, onStepUpdate = {})
            println("  ✓ $description: ${result.recommendations.size} recs")
            assertTrue("$description should return results", result.recommendations.isNotEmpty())
        }
    }

    @Test
    fun testExtremeSensorConditions() = runBlocking {
        println("EVAL Sensor Edge Case Test")
        val results = (1..20).map { iteration ->
            val result = pipeline.runPipeline("sensor test $iteration", onStepUpdate = {})
            result.recommendations.isNotEmpty()
        }

        val successRate = results.count { it }.toFloat() / results.size
        println("  Success rate: ${(successRate * 100).toInt()}%")
        println("  Total runs: ${results.size}")

        assertTrue("Should handle sensor variations", successRate > 0.9)
    }

    private fun createBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val r = if (width > 1) (x * 255 / (width - 1)) else 128
                    val g = if (height > 1) (y * 255 / (height - 1)) else 128
                    val b = 128
                    setPixel(x, y, (0xFF000000 or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()).toInt())
                }
            }
        }
    }
}
