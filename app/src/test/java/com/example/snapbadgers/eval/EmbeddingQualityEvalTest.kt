package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * EmbeddingQualityEvalTest
 *
 * Evaluates the HeuristicTextEmbedding encoder for semantic separation,
 * output correctness, and determinism.
 *
 * These tests run on JVM without a device.
 */
class EmbeddingQualityEvalTest {

    // ------------------------------------------------------------------
    // Semantic separation
    // ------------------------------------------------------------------

    @Test
    fun `different moods map to different embedding regions`() {
        val calm = HeuristicTextEmbedding.encode("calm piano relaxing ambient")
        val workout = HeuristicTextEmbedding.encode("intense workout high energy")
        val party = HeuristicTextEmbedding.encode("happy party dance upbeat")

        val calmVsWorkout = VectorUtils.cosineSimilarity(calm, workout)
        val calmVsParty = VectorUtils.cosineSimilarity(calm, party)
        val workoutVsParty = VectorUtils.cosineSimilarity(workout, party)

        println("EVAL semantic_separation:")
        println("  calm_vs_workout: $calmVsWorkout")
        println("  calm_vs_party:   $calmVsParty")
        println("  workout_vs_party: $workoutVsParty")

        // Different moods should have measurable cosine distance (similarity < 1)
        assertTrue("calm vs workout should be distinguishable", calmVsWorkout < 0.95f)
        assertTrue("calm vs party should be distinguishable", calmVsParty < 0.95f)
        assertTrue("workout vs party should be distinguishable", workoutVsParty < 0.95f)
    }

    @Test
    fun `similar moods map closer than dissimilar moods`() {
        val calm1 = HeuristicTextEmbedding.encode("calm relaxing peaceful music")
        val calm2 = HeuristicTextEmbedding.encode("calm study ambient lo-fi")
        val energy = HeuristicTextEmbedding.encode("intense workout running fast")

        val calmPairSimilarity = VectorUtils.cosineSimilarity(calm1, calm2)
        val calmVsEnergy = VectorUtils.cosineSimilarity(calm1, energy)

        println("EVAL semantic_clustering:")
        println("  calm_pair_similarity: $calmPairSimilarity")
        println("  calm_vs_energy:       $calmVsEnergy")

        assertTrue(
            "Two calm queries should be more similar than calm vs energy",
            calmPairSimilarity > calmVsEnergy
        )
    }

    @Test
    fun `mood keyword activation is reflected in embedding`() {
        val withCalm = HeuristicTextEmbedding.encode("calm music")
        val withoutCalm = HeuristicTextEmbedding.encode("loud music")

        // HeuristicTextEmbedding sets embedding[2] = 1f for "calm"
        // After normalization this should still be a positive signal
        println("EVAL mood_keyword_calm:")
        println("  with_calm[2]:    ${withCalm[2]}")
        println("  without_calm[2]: ${withoutCalm[2]}")
        assertTrue("'calm' keyword should activate dimension 2", withCalm[2] > withoutCalm[2])
    }

    @Test
    fun `workout keyword activation is reflected in embedding`() {
        val withWorkout = HeuristicTextEmbedding.encode("workout music")
        val withoutWorkout = HeuristicTextEmbedding.encode("sleep music")

        println("EVAL mood_keyword_workout:")
        println("  with_workout[6]:    ${withWorkout[6]}")
        println("  without_workout[6]: ${withoutWorkout[6]}")
        assertTrue("'workout' keyword should activate dimension 6", withWorkout[6] > withoutWorkout[6])
    }

    @Test
    fun `happy keyword activation is reflected in embedding`() {
        val withHappy = HeuristicTextEmbedding.encode("happy songs")
        val withoutHappy = HeuristicTextEmbedding.encode("dark songs")

        println("EVAL mood_keyword_happy:")
        println("  with_happy[4]:    ${withHappy[4]}")
        println("  without_happy[4]: ${withoutHappy[4]}")
        assertTrue("'happy' keyword should activate dimension 4", withHappy[4] > withoutHappy[4])
    }

    // ------------------------------------------------------------------
    // Output correctness
    // ------------------------------------------------------------------

    @Test
    fun `output is 128-d`() {
        val embedding = HeuristicTextEmbedding.encode("test input")
        assertEquals("Output should be 128 dimensions", EMBEDDING_DIMENSION, embedding.size)
        println("EVAL output_dimension: ${embedding.size}")
    }

    @Test
    fun `output is L2 normalized`() {
        val inputs = listOf(
            "calm piano",
            "intense workout high energy beats",
            "happy party dance upbeat positive vibes",
            "sad lonely night rain",
            "study focus concentration deep work"
        )

        for (input in inputs) {
            val embedding = HeuristicTextEmbedding.encode(input)
            val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
            assertEquals("Embedding for '$input' should be L2-normalized", 1.0f, norm, 1e-5f)
            println("EVAL l2_norm for '$input': $norm")
        }
    }

    @Test
    fun `empty input returns zero vector`() {
        val embedding = HeuristicTextEmbedding.encode("")
        assertEquals(EMBEDDING_DIMENSION, embedding.size)
        assertTrue("Empty input should produce zero vector", embedding.all { it == 0f })
        println("EVAL empty_input: all_zeros=true")
    }

    @Test
    fun `whitespace-only input returns zero vector`() {
        val embedding = HeuristicTextEmbedding.encode("   \t\n  ")
        assertEquals(EMBEDDING_DIMENSION, embedding.size)
        assertTrue("Whitespace-only input should produce zero vector", embedding.all { it == 0f })
        println("EVAL whitespace_input: all_zeros=true")
    }

    // ------------------------------------------------------------------
    // Determinism
    // ------------------------------------------------------------------

    @Test
    fun `same input produces same embedding`() {
        val input = "calm relaxing piano study music"
        val first = HeuristicTextEmbedding.encode(input)
        val second = HeuristicTextEmbedding.encode(input)

        assertArrayEquals("Identical inputs must produce identical embeddings", first, second, 0f)
        println("EVAL determinism: bit_identical=true")
    }

    @Test
    fun `determinism across 100 invocations`() {
        val input = "happy dance party summer vibes"
        val reference = HeuristicTextEmbedding.encode(input)

        var allIdentical = true
        repeat(100) {
            val current = HeuristicTextEmbedding.encode(input)
            if (!current.contentEquals(reference)) {
                allIdentical = false
            }
        }
        assertTrue("100 invocations should all produce identical output", allIdentical)
        println("EVAL determinism_100x: all_identical=$allIdentical")
    }

    // ------------------------------------------------------------------
    // Embedding space coverage
    // ------------------------------------------------------------------

    @Test
    fun `diverse inputs use multiple embedding dimensions`() {
        val inputs = listOf(
            "calm piano",
            "intense workout",
            "happy party",
            "sad night rain",
            "study focus",
            "dance electronic",
            "jazz smooth",
            "rock guitar heavy"
        )

        val activeDimensions = mutableSetOf<Int>()
        for (input in inputs) {
            val embedding = HeuristicTextEmbedding.encode(input)
            embedding.forEachIndexed { index, value ->
                if (abs(value) > 1e-6f) {
                    activeDimensions.add(index)
                }
            }
        }

        println("EVAL embedding_space_coverage: active_dims=${activeDimensions.size}/${EMBEDDING_DIMENSION}")
        assertTrue(
            "Diverse inputs should activate at least 20% of dimensions",
            activeDimensions.size >= EMBEDDING_DIMENSION * 0.2
        )
    }

    @Test
    fun `longer text activates more dimensions`() {
        val short = HeuristicTextEmbedding.encode("calm")
        val long = HeuristicTextEmbedding.encode("calm relaxing piano ambient chill lo-fi study background music")

        val shortActive = short.count { abs(it) > 1e-6f }
        val longActive = long.count { abs(it) > 1e-6f }

        println("EVAL dimension_activation: short=$shortActive long=$longActive")
        assertTrue("Longer text should activate more dimensions", longActive >= shortActive)
    }
}
