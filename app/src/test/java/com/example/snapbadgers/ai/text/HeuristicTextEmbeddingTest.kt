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

        // Before normalization these slots would be 1.0; after normalization they
        // should still be the largest contributor in their respective dimension
        assertTrue("calm keyword should activate index 2", calm[2] > 0f)
        assertTrue("study keyword should activate index 3", study[3] > 0f)
        assertTrue("happy keyword should activate index 4", happy[4] > 0f)
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
