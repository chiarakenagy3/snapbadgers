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

class EmbeddingQualityEvalTest {

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

        assertTrue(calmVsWorkout < 0.95f)
        assertTrue(calmVsParty < 0.95f)
        assertTrue(workoutVsParty < 0.95f)
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

        assertTrue(calmPairSimilarity > calmVsEnergy)
    }

    @Test
    fun `mood keyword activation is reflected in embedding`() {
        listOf(
            Triple("calm", "loud", 2),
            Triple("workout", "sleep", 6),
            Triple("happy", "dark", 4)
        ).forEach { (keyword, opposite, dim) ->
            val withKeyword = HeuristicTextEmbedding.encode("$keyword music")
            val withoutKeyword = HeuristicTextEmbedding.encode("$opposite music")

            println("EVAL mood_keyword_$keyword:")
            println("  with_${keyword}[$dim]:    ${withKeyword[dim]}")
            println("  without_${keyword}[$dim]: ${withoutKeyword[dim]}")
            assertTrue("'$keyword' keyword should activate dimension $dim", withKeyword[dim] > withoutKeyword[dim])
        }
    }

    @Test
    fun `output is 128-d`() {
        assertEquals(EMBEDDING_DIMENSION, HeuristicTextEmbedding.encode("test input").size)
        println("EVAL output_dimension: $EMBEDDING_DIMENSION")
    }

    @Test
    fun `output is L2 normalized`() {
        listOf(
            "calm piano",
            "intense workout high energy beats",
            "happy party dance upbeat positive vibes",
            "sad lonely night rain",
            "study focus concentration deep work"
        ).forEach { input ->
            val embedding = HeuristicTextEmbedding.encode(input)
            val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
            assertEquals("Embedding for '$input' should be L2-normalized", 1.0f, norm, 1e-5f)
            println("EVAL l2_norm for '$input': $norm")
        }
    }

    @Test
    fun `empty or whitespace input returns zero vector`() {
        listOf("", "   \t\n  ").forEach { input ->
            val embedding = HeuristicTextEmbedding.encode(input)
            assertEquals(EMBEDDING_DIMENSION, embedding.size)
            assertTrue("Input '$input' should produce zero vector", embedding.all { it == 0f })
        }
        println("EVAL empty_whitespace_input: all_zeros=true")
    }

    @Test
    fun `determinism across 100 invocations`() {
        val input = "happy dance party summer vibes"
        val reference = HeuristicTextEmbedding.encode(input)

        repeat(100) {
            assertArrayEquals("Invocation $it must match reference", reference, HeuristicTextEmbedding.encode(input), 0f)
        }
        println("EVAL determinism_100x: all_identical=true")
    }

    @Test
    fun `diverse inputs use multiple embedding dimensions`() {
        val inputs = listOf(
            "calm piano", "intense workout", "happy party", "sad night rain",
            "study focus", "dance electronic", "jazz smooth", "rock guitar heavy"
        )

        val activeDimensions = mutableSetOf<Int>()
        for (input in inputs) {
            HeuristicTextEmbedding.encode(input).forEachIndexed { index, value ->
                if (abs(value) > 1e-6f) activeDimensions.add(index)
            }
        }

        println("EVAL embedding_space_coverage: active_dims=${activeDimensions.size}/${EMBEDDING_DIMENSION}")
        assertTrue("Should activate multiple dimensions, got ${activeDimensions.size}", activeDimensions.size > 1)
    }

    @Test
    fun `longer text activates more dimensions`() {
        val shortActive = HeuristicTextEmbedding.encode("calm").count { abs(it) > 1e-6f }
        val longActive = HeuristicTextEmbedding.encode("calm relaxing piano ambient chill lo-fi study background music").count { abs(it) > 1e-6f }

        println("EVAL dimension_activation: short=$shortActive long=$longActive")
        assertTrue(longActive >= shortActive)
    }
}
