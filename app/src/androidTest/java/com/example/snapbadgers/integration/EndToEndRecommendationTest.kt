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

/**
 * End-to-end recommendation tests with realistic user scenarios.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndRecommendationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var pipeline: RecommendationPipeline

    @Before
    fun setup() {
        pipeline = RecommendationPipeline(context)
        // Warmup
        runBlocking {
            pipeline.warmUp()
        }
    }

    @Test
    fun testCalmNightScenario() = runBlocking {
        println("\n🌙 Scenario: Calm Night Study Session")
        println("━".repeat(60))

        val result = pipeline.runPipeline(
            input = "calm peaceful night music for studying and focusing",
            onStepUpdate = {}
        )

        println("  Query: 'calm peaceful night...'")
        println("  Inference time: ${result.inferenceTimeMs}ms")
        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist} (${("%.3f".format(song.similarity))})")
        }
        println("━".repeat(60))

        assertTrue("Should return recommendations", result.recommendations.isNotEmpty())
        assertTrue("Should complete in reasonable time", result.inferenceTimeMs < 5000)
    }

    @Test
    fun testEnergeticWorkoutScenario() = runBlocking {
        println("\n💪 Scenario: Energetic Workout Session")
        println("━".repeat(60))

        val result = pipeline.runPipeline(
            input = "high energy workout pump up beats fast tempo",
            onStepUpdate = {}
        )

        println("  Query: 'high energy workout...'")
        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testMorningCoffeeScenario() = runBlocking {
        println("\n☕ Scenario: Morning Coffee Relaxation")
        println("━".repeat(60))

        val result = pipeline.runPipeline(
            input = "gentle morning coffee jazz acoustic",
            onStepUpdate = {}
        )

        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testLateNightCodingScenario() = runBlocking {
        println("\n💻 Scenario: Late Night Coding")
        println("━".repeat(60))

        val bitmap = createDarkAmbientSceneBitmap()

        val result = pipeline.runPipeline(
            input = "late night coding focus ambient electronic",
            imageBitmap = bitmap,
            onStepUpdate = {}
        )

        println("  Query: 'late night coding...' + dark ambient image")
        println("  Used vision: ${result.usedVisionInput}")
        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue(result.usedVisionInput)
        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testPartyMoodScenario() = runBlocking {
        println("\n🎉 Scenario: Party Mood")
        println("━".repeat(60))

        val result = pipeline.runPipeline(
            input = "upbeat party dance happy celebration",
            onStepUpdate = {}
        )

        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testMeditationScenario() = runBlocking {
        println("\n🧘 Scenario: Meditation Session")
        println("━".repeat(60))

        val result = pipeline.runPipeline(
            input = "meditation mindfulness calm peaceful zen",
            onStepUpdate = {}
        )

        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testCommuteDrivingScenario() = runBlocking {
        println("\n🚗 Scenario: Commute Driving")
        println("━".repeat(60))

        val result = pipeline.runPipeline(
            input = "driving commute road trip upbeat",
            onStepUpdate = {}
        )

        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testRainyDayScenario() = runBlocking {
        println("\n🌧️ Scenario: Rainy Day Indoors")
        println("━".repeat(60))

        val rainySceneBitmap = createRainySceneBitmap()

        val result = pipeline.runPipeline(
            input = "rainy day cozy indoor relaxing",
            imageBitmap = rainySceneBitmap,
            onStepUpdate = {}
        )

        println("  Used vision: ${result.usedVisionInput}")
        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun testMinimalInputScenario() = runBlocking {
        println("\n📝 Scenario: Minimal Input")
        println("━".repeat(60))

        val result = pipeline.runPipeline(
            input = "calm",
            onStepUpdate = {}
        )

        println("  Query: 'calm'")
        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue("Should handle single word", result.recommendations.isNotEmpty())
    }

    @Test
    fun testDetailedInputScenario() = runBlocking {
        println("\n📚 Scenario: Detailed Description")
        println("━".repeat(60))

        val detailedInput = """
            I'm looking for calm, peaceful ambient music with a slow tempo
            that would be perfect for late night studying. Something electronic
            or instrumental without vocals that helps with focus and concentration.
        """.trimIndent()

        val result = pipeline.runPipeline(
            input = detailedInput,
            onStepUpdate = {}
        )

        println("  Query length: ${detailedInput.length} chars")
        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist}")
        }
        println("━".repeat(60))

        assertTrue("Should handle detailed input", result.recommendations.isNotEmpty())
    }

    @Test
    fun testMultipleModalitiesScenario() = runBlocking {
        println("\n🎨 Scenario: Full Multimodal Input")
        println("━".repeat(60))

        val sceneBitmap = createColorfulSceneBitmap()

        val result = pipeline.runPipeline(
            input = "energetic colorful upbeat happy",
            imageBitmap = sceneBitmap,
            onStepUpdate = {}
        )

        println("  Text: 'energetic colorful...'")
        println("  Vision: Colorful scene")
        println("  Sensors: Active")
        println("  Used vision: ${result.usedVisionInput}")
        println("  Recommendations:")
        result.recommendations.forEach { song ->
            println("    - ${song.title} by ${song.artist} (${("%.3f".format(song.similarity))})")
        }
        println("━".repeat(60))

        assertTrue(result.usedVisionInput)
        assertTrue(result.recommendations.isNotEmpty())
        assertEquals(3, result.recommendations.size)
    }

    @Test
    fun testRecommendationQualityMetrics() = runBlocking {
        println("\n📊 Recommendation Quality Metrics")
        println("━".repeat(60))

        val queries = listOf(
            "calm music",
            "energetic workout",
            "peaceful meditation",
            "upbeat party"
        )

        queries.forEach { query ->
            val result = pipeline.runPipeline(query, onStepUpdate = {})

            println("  Query: '$query'")
            println("    Recommendations: ${result.recommendations.size}")
            println("    Inference time: ${result.inferenceTimeMs}ms")
            println("    Top similarity: ${"%.3f".format(result.recommendations.firstOrNull()?.similarity ?: 0f)}")

            // Quality checks
            assertTrue("Should have recommendations", result.recommendations.isNotEmpty())
            assertTrue("Should complete quickly", result.inferenceTimeMs < 3000)

            result.recommendations.forEach { song ->
                assertTrue("Similarity should be valid", song.similarity >= 0f && song.similarity <= 1f)
                assertTrue("Title should not be empty", song.title.isNotBlank())
                assertTrue("Artist should not be empty", song.artist.isNotBlank())
            }
        }

        println("━".repeat(60))
    }

    private fun createDarkAmbientSceneBitmap(): Bitmap {
        return Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            // Dark blue/purple gradient
            for (y in 0 until 224) {
                for (x in 0 until 224) {
                    val blue = ((y * 128 / 224) + 64).coerceIn(0, 255)
                    setPixel(x, y, (0xFF000000 or blue).toInt())
                }
            }
        }
    }

    private fun createRainySceneBitmap(): Bitmap {
        return Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            // Gray/blue tones
            for (y in 0 until 224) {
                for (x in 0 until 224) {
                    val gray = 100 + (x + y) % 50
                    setPixel(x, y, (0xFF000000 or (gray shl 16) or (gray shl 8) or (gray + 20)).toInt())
                }
            }
        }
    }

    private fun createColorfulSceneBitmap(): Bitmap {
        return Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            // Vibrant colors
            for (y in 0 until 224) {
                for (x in 0 until 224) {
                    val r = (x * 255 / 224).coerceIn(0, 255)
                    val g = (y * 255 / 224).coerceIn(0, 255)
                    val b = ((x + y) * 255 / 448).coerceIn(0, 255)
                    setPixel(x, y, (0xFF000000 or (r shl 16) or (g shl 8) or b).toInt())
                }
            }
        }
    }
}
