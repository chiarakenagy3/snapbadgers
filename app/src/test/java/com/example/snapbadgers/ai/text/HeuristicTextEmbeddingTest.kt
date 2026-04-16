package com.example.snapbadgers.ai.text

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class HeuristicTextEmbeddingTest {

    private fun l2Norm(v: FloatArray): Float {
        var sum = 0f
        for (x in v) sum += x * x
        return sqrt(sum)
    }

    @Test
    fun `encode produces 128-d output`() {
        val result = HeuristicTextEmbedding.encode("relaxing music for studying")
        assertEquals(EMBEDDING_DIMENSION, result.size)
    }

    @Test
    fun `encode output is L2 normalized`() {
        val result = HeuristicTextEmbedding.encode("upbeat workout playlist")
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test
    fun `encode detects mood keywords`() {
        val calm = HeuristicTextEmbedding.encode("calm evening")
        val study = HeuristicTextEmbedding.encode("study session")
        val happy = HeuristicTextEmbedding.encode("happy vibes")

        // Indices are defined in VectorUtils.kt: 
        // IDX_ACOUSTICNESS = 3
        // IDX_INSTRUMENTALNESS = 4
        // IDX_VALENCE = 6
        assertTrue("calm keyword should activate acousticness", calm[3] > 0f)
        assertTrue("study keyword should activate instrumentalness", study[4] > 0f)
        assertTrue("happy keyword should activate valence", happy[6] > 0f)
    }

    @Test
    fun `encode empty string returns zero vector`() {
        val result = HeuristicTextEmbedding.encode("")
        assertEquals(EMBEDDING_DIMENSION, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `encode whitespace-only string returns zero vector`() {
        val result = HeuristicTextEmbedding.encode("   \t  ")
        assertEquals(EMBEDDING_DIMENSION, result.size)
        assertTrue(result.all { it == 0f })
    }
}
