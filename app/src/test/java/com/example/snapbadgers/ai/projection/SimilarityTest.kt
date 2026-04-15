package com.example.snapbadgers.ai.projection

import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SimilarityTest
 *
 * Verifies that cosine similarity scores are meaningful after the fix —
 * similar queries should score higher against matching songs than mismatched ones.
 *
 * JVM-only (no Android context, no .tflite). Tests the heuristic path only.
 *
 * Run with: ./gradlew test --tests "*.SimilarityTest"
 */
class SimilarityTest {

    private val fusionEngine = FusionEngine()

    @Test
    fun `workout query scores higher against workout song than sleep song`() {
        val workoutQuery = fuse("workout energetic running upbeat night")
        val workoutSong  = fuse("Blinding Lights energetic pop workout night drive upbeat")
        val sleepSong    = fuse("Weightless calm study relax sleep ambient")

        val scoreVsWorkout = VectorUtils.cosineSimilarity(workoutQuery, workoutSong)
        val scoreVsSleep   = VectorUtils.cosineSimilarity(workoutQuery, sleepSong)

        println("workout query vs workout song: $scoreVsWorkout")
        println("workout query vs sleep song:   $scoreVsSleep")

        assertTrue(
            "workout query should score higher against workout song (got $scoreVsWorkout) than sleep song (got $scoreVsSleep)",
            scoreVsWorkout > scoreVsSleep
        )
    }

    @Test
    fun `sleep query scores higher against sleep song than workout song`() {
        val sleepQuery  = fuse("calm sleep relax ambient study")
        val sleepSong   = fuse("Weightless calm study relax sleep ambient")
        val workoutSong = fuse("Blinding Lights energetic pop workout night drive upbeat")

        val scoreVsSleep   = VectorUtils.cosineSimilarity(sleepQuery, sleepSong)
        val scoreVsWorkout = VectorUtils.cosineSimilarity(sleepQuery, workoutSong)

        println("sleep query vs sleep song:   $scoreVsSleep")
        println("sleep query vs workout song: $scoreVsWorkout")

        assertTrue(
            "sleep query should score higher against sleep song (got $scoreVsSleep) than workout song (got $scoreVsWorkout)",
            scoreVsSleep > scoreVsWorkout
        )
    }

    @Test
    fun `happy query scores higher against happy song than sleep song`() {
        val happyQuery = fuse("happy chill daytime pop easy")
        val happySong  = fuse("Sunflower happy chill pop easy listening daytime")
        val sleepSong  = fuse("Weightless calm study relax sleep ambient")

        val scoreVsHappy = VectorUtils.cosineSimilarity(happyQuery, happySong)
        val scoreVsSleep = VectorUtils.cosineSimilarity(happyQuery, sleepSong)

        println("happy query vs happy song: $scoreVsHappy")
        println("happy query vs sleep song: $scoreVsSleep")

        assertTrue(
            "happy query should score higher against happy song (got $scoreVsHappy) than sleep song (got $scoreVsSleep)",
            scoreVsHappy > scoreVsSleep
        )
    }

    @Test
    fun `similarity scores are not near zero for matching descriptions`() {
        val query = fuse("happy chill daytime pop")
        val song  = fuse("Sunflower happy chill pop easy listening daytime")
        val score = VectorUtils.cosineSimilarity(query, song)

        println("matching score: $score")

        assertTrue("score should be above 0.1 for matching descriptions, got $score", score > 0.1f)
    }

    @Test
    fun `identical embeddings produce similarity of 1`() {
        val embedding = fuse("calm study relax ambient")
        val score = VectorUtils.cosineSimilarity(embedding, embedding)

        println("self-similarity: $score")

        assertTrue("identical vectors should have similarity ~1.0, got $score", score > 0.99f)
    }

    private fun fuse(description: String): FloatArray {
        return fusionEngine.fuse(textEmbedding = HeuristicTextEmbedding.encode(description))
    }
}
