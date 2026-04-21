package com.example.snapbadgers.integration

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndRecommendationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var pipeline: RecommendationPipeline

    @Before
    fun setup() {
        pipeline = RecommendationPipeline(context)
        runBlocking { pipeline.warmUp() }
    }

    @Test
    fun testTextOnlyScenarios() = runBlocking {
        mapOf(
            "calm peaceful night music for studying and focusing" to "Calm Night",
            "high energy workout pump up beats fast tempo" to "Workout",
            "gentle morning coffee jazz acoustic" to "Morning Coffee",
            "upbeat party dance happy celebration" to "Party Mood",
            "meditation mindfulness calm peaceful zen" to "Meditation",
            "driving commute road trip upbeat" to "Commute Driving"
        ).forEach { (query, label) ->
            println("EVAL Scenario: $label")
            val result = pipeline.runPipeline(input = query, onStepUpdate = {})
            println("  Inference time: ${result.inferenceTimeMs}ms")
            result.recommendations.forEach { song ->
                println("    - ${song.title} by ${song.artist} (${"%.3f".format(song.similarity)})")
            }
            assertTrue("$label should return recommendations", result.recommendations.isNotEmpty())
            assertTrue("$label should complete in reasonable time", result.inferenceTimeMs < 5000)
        }
    }

    @Test
    fun testLateNightCodingScenario() = runBlocking {
        println("EVAL Scenario: Late Night Coding")
        val bitmap = createDarkAmbientSceneBitmap()
        val result = pipeline.runPipeline(
            input = "late night coding focus ambient electronic",
            imageBitmap = bitmap,
            onStepUpdate = {}
        )
        println("  Used vision: ${result.usedVisionInput}")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        assertTrue(result.usedVisionInput)
        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testRainyDayScenario() = runBlocking {
        println("EVAL Scenario: Rainy Day Indoors")
        val rainySceneBitmap = createRainySceneBitmap()
        val result = pipeline.runPipeline(
            input = "rainy day cozy indoor relaxing",
            imageBitmap = rainySceneBitmap,
            onStepUpdate = {}
        )
        println("  Used vision: ${result.usedVisionInput}")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testMinimalInputScenario() = runBlocking {
        println("EVAL Scenario: Minimal Input")
        val result = pipeline.runPipeline(input = "calm", onStepUpdate = {})
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        assertTrue("Should handle single word", result.recommendations.isNotEmpty())
    }

    @Test
    fun testDetailedInputScenario() = runBlocking {
        println("EVAL Scenario: Detailed Description")
        val detailedInput = """
            I'm looking for calm, peaceful ambient music with a slow tempo
            that would be perfect for late night studying. Something electronic
            or instrumental without vocals that helps with focus and concentration.
        """.trimIndent()

        val result = pipeline.runPipeline(input = detailedInput, onStepUpdate = {})
        println("  Query length: ${detailedInput.length} chars")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        assertTrue("Should handle detailed input", result.recommendations.isNotEmpty())
    }

    @Test
    fun testMultipleModalitiesScenario() = runBlocking {
        println("EVAL Scenario: Full Multimodal Input")
        val sceneBitmap = createColorfulSceneBitmap()
        val result = pipeline.runPipeline(
            input = "energetic colorful upbeat happy",
            imageBitmap = sceneBitmap,
            onStepUpdate = {}
        )
        println("  Used vision: ${result.usedVisionInput}")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist} (${"%.3f".format(song.similarity)})")
        }
        assertTrue(result.usedVisionInput)
        assertTrue(result.recommendations.isNotEmpty())
        assertEquals(3, result.recommendations.size)
    }

    @Test
    fun testRecommendationQualityMetrics() = runBlocking {
        println("EVAL Recommendation Quality Metrics")
        val queries = listOf("calm music", "energetic workout", "peaceful meditation", "upbeat party")

        queries.forEach { query ->
            val result = pipeline.runPipeline(query, onStepUpdate = {})
            println("  Query: '$query' -> ${result.recommendations.size} recs, ${result.inferenceTimeMs}ms")
            println("    Top similarity: ${"%.3f".format(result.recommendations.firstOrNull()?.similarity ?: 0f)}")

            assertTrue("Should have recommendations", result.recommendations.isNotEmpty())
            assertTrue("Should complete quickly", result.inferenceTimeMs < 3000)
            result.recommendations.forEach { song ->
                assertTrue("Similarity should be valid", song.similarity >= 0f && song.similarity <= 1f)
                assertTrue("Title should not be empty", song.title.isNotBlank())
                assertTrue("Artist should not be empty", song.artist.isNotBlank())
            }
        }
    }

    private fun createDarkAmbientSceneBitmap(): Bitmap {
        return Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 224) {
                for (x in 0 until 224) {
                    val blue = ((y * 128 / 224) + 64).coerceIn(0, 255)
                    setPixel(x, y, (0xFF000000 or blue.toLong()).toInt())
                }
            }
        }
    }

    private fun createRainySceneBitmap(): Bitmap {
        return Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 224) {
                for (x in 0 until 224) {
                    val gray = 100 + (x + y) % 50
                    setPixel(x, y, (0xFF000000 or (gray.toLong() shl 16) or (gray.toLong() shl 8) or (gray + 20).toLong()).toInt())
                }
            }
        }
    }

    private fun createColorfulSceneBitmap(): Bitmap {
        return Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 224) {
                for (x in 0 until 224) {
                    val r = (x * 255 / 224).coerceIn(0, 255)
                    val g = (y * 255 / 224).coerceIn(0, 255)
                    val b = ((x + y) * 255 / 448).coerceIn(0, 255)
                    setPixel(x, y, (0xFF000000 or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()).toInt())
                }
            }
        }
    }
}
