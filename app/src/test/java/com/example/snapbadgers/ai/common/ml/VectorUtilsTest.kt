package com.example.snapbadgers.ai.common.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class VectorUtilsTest {

    private fun l2Norm(v: FloatArray): Float {
        var sum = 0f
        for (x in v) sum += x * x
        return sqrt(sum)
    }

    // ---- normalize ----

    @Test
    fun `normalize produces unit length vector`() {
        val input = floatArrayOf(3f, 4f)
        val result = VectorUtils.normalize(input)
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
        assertEquals(3f / 5f, result[0], 1e-6f)
        assertEquals(4f / 5f, result[1], 1e-6f)
    }

    @Test
    fun `normalize zero vector returns zero vector`() {
        val zero = FloatArray(128)
        val result = VectorUtils.normalize(zero)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `normalize preserves vector size`() {
        val input = FloatArray(64) { it.toFloat() + 1f }
        val result = VectorUtils.normalize(input)
        assertEquals(64, result.size)
    }

    // ---- alignToEmbeddingDimension ----

    @Test
    fun `align produces correct output dimension`() {
        val input = floatArrayOf(1f, 2f, 3f)
        val result = VectorUtils.alignToEmbeddingDimension(input, salt = 0)
        assertEquals(EMBEDDING_DIMENSION, result.size)
    }

    @Test
    fun `align with matching dimension returns normalized copy`() {
        val input = FloatArray(EMBEDDING_DIMENSION) { it.toFloat() + 1f }
        val result = VectorUtils.alignToEmbeddingDimension(input, salt = 0)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test
    fun `align output is L2 normalized`() {
        val input = floatArrayOf(5f, 10f, 15f, 20f)
        val result = VectorUtils.alignToEmbeddingDimension(input, salt = 7)
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test
    fun `align with different salts produces different outputs`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f)
        val result1 = VectorUtils.alignToEmbeddingDimension(input, salt = 11)
        val result2 = VectorUtils.alignToEmbeddingDimension(input, salt = 19)
        val sim = VectorUtils.cosineSimilarity(result1, result2)
        assertTrue("Different salts should produce different projections, sim=$sim", sim < 0.9999f)
    }

    @Test
    fun `align with empty vector returns zero vector of target dimension`() {
        val result = VectorUtils.alignToEmbeddingDimension(floatArrayOf(), salt = 0)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `align with zero dimension returns empty array`() {
        val result = VectorUtils.alignToEmbeddingDimension(floatArrayOf(1f, 2f), salt = 0, dimension = 0)
        assertEquals(0, result.size)
    }

    // ---- cosineSimilarity ----

    @Test
    fun `cosine similarity of identical vectors is 1`() {
        val v = floatArrayOf(1f, 2f, 3f)
        val sim = VectorUtils.cosineSimilarity(v, v)
        assertEquals(1f, sim, 1e-5f)
    }

    @Test
    fun `cosine similarity of opposite vectors is negative 1`() {
        val v = floatArrayOf(1f, 2f, 3f)
        val neg = floatArrayOf(-1f, -2f, -3f)
        val sim = VectorUtils.cosineSimilarity(v, neg)
        assertEquals(-1f, sim, 1e-5f)
    }

    @Test
    fun `cosine similarity of orthogonal vectors is 0`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        val sim = VectorUtils.cosineSimilarity(a, b)
        assertEquals(0f, sim, 1e-5f)
    }

    @Test
    fun `cosine similarity with empty vectors returns 0`() {
        assertEquals(0f, VectorUtils.cosineSimilarity(floatArrayOf(), floatArrayOf()), 0f)
    }

    @Test
    fun `cosine similarity with mismatched sizes returns 0`() {
        val a = floatArrayOf(1f, 2f)
        val b = floatArrayOf(1f, 2f, 3f)
        assertEquals(0f, VectorUtils.cosineSimilarity(a, b), 0f)
    }

    // ---- positiveModulo ----

    @Test
    fun `positiveModulo with positive values`() {
        assertEquals(3, VectorUtils.positiveModulo(13, 5))
    }

    @Test
    fun `positiveModulo with negative value returns positive result`() {
        assertEquals(2, VectorUtils.positiveModulo(-3, 5))
    }

    @Test
    fun `positiveModulo with zero modulus returns 0`() {
        assertEquals(0, VectorUtils.positiveModulo(10, 0))
    }
}
