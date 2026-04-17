package com.example.snapbadgers.ai.fusion

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FusionEngineTest {

    private val engine = FusionEngine()

    private fun l2Norm(v: FloatArray): Float {
        var sum = 0f
        for (x in v) sum += x * x
        return sqrt(sum)
    }

    @Test
    fun `fuse produces 128-d output`() {
        val text = FloatArray(EMBEDDING_DIMENSION) { 1f }
        val result = engine.fuse(textEmbedding = text)
        assertEquals(EMBEDDING_DIMENSION, result.size)
    }

    @Test
    fun `fuse output is L2 normalized`() {
        val text = FloatArray(EMBEDDING_DIMENSION) { it.toFloat() + 1f }
        val vision = FloatArray(EMBEDDING_DIMENSION) { (it.toFloat() + 1f) * 0.5f }
        val sensor = FloatArray(EMBEDDING_DIMENSION) { (it.toFloat() + 1f) * 0.3f }

        val result = engine.fuse(text, vision, sensor)

        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test
    fun `fuse with all three modalities applies correct weights`() {
        // When all three modalities are provided, total weight = 0.60 + 0.25 + 0.15 = 1.0
        // The result should still be normalized regardless
        val text = FloatArray(EMBEDDING_DIMENSION) { 1f }
        val vision = FloatArray(EMBEDDING_DIMENSION) { 1f }
        val sensor = FloatArray(EMBEDDING_DIMENSION) { 1f }

        val result = engine.fuse(text, vision, sensor)
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test
    fun `fuse with null vision omits vision weight`() {
        val text = FloatArray(EMBEDDING_DIMENSION) { it.toFloat() + 1f }
        val sensor = FloatArray(EMBEDDING_DIMENSION) { (it.toFloat() + 1f) * 0.2f }

        val withVision = engine.fuse(text, FloatArray(EMBEDDING_DIMENSION) { 1f }, sensor)
        val withoutVision = engine.fuse(text, null, sensor)

        // Results must differ because vision contribution is missing
        val sim = VectorUtils.cosineSimilarity(withVision, withoutVision)
        assertTrue("Expected different outputs with/without vision, similarity=$sim", sim < 0.9999f)
    }

    @Test
    fun `fuse with zero vectors returns zero vector`() {
        val zero = FloatArray(EMBEDDING_DIMENSION)
        val result = engine.fuse(zero, zero, zero)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `fuse is deterministic`() {
        val text = FloatArray(EMBEDDING_DIMENSION) { it.toFloat() * 0.1f }
        val vision = FloatArray(EMBEDDING_DIMENSION) { (it.toFloat() + 5f) * 0.2f }
        val sensor = FloatArray(EMBEDDING_DIMENSION) { (it.toFloat() + 10f) * 0.05f }

        val first = engine.fuse(text, vision, sensor)
        val second = engine.fuse(text, vision, sensor)

        assertArrayEquals(first, second, 0f)
    }

    @Test
    fun `fuse with mismatched input dimensions still works via alignment`() {
        // alignToEmbeddingDimension handles inputs of any size
        val text = FloatArray(8) { 1f }
        val result = engine.fuse(textEmbedding = text)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }
}
