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
    fun `normalize handles various inputs`() {
        listOf(
            Triple(floatArrayOf(3f, 4f), true, "unit vector 3-4-5"),
            Triple(FloatArray(128), false, "zero vector"),
            Triple(FloatArray(64) { it.toFloat() + 1f }, true, "preserves size"),
        ).forEach { (input, expectUnit, label) ->
            val result = VectorUtils.normalize(input)
            assertEquals("$label: size", input.size, result.size)
            if (expectUnit) {
                val norm = l2Norm(result)
                assertTrue("$label: unit length, got $norm", norm in 0.999f..1.001f)
            } else {
                assertTrue("$label: all zeros", result.all { it == 0f })
            }
        }
        // Verify exact values for 3-4-5 triangle
        val r = VectorUtils.normalize(floatArrayOf(3f, 4f))
        assertEquals(3f / 5f, r[0], 1e-6f)
        assertEquals(4f / 5f, r[1], 1e-6f)
    }

    // ---- alignToEmbeddingDimension ----

    @Test
    fun `align produces correct output and normalization`() {
        listOf(
            Triple(floatArrayOf(1f, 2f, 3f), 0, "short input"),
            Triple(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() + 1f }, 0, "matching dim"),
            Triple(floatArrayOf(5f, 10f, 15f, 20f), 7, "with salt"),
        ).forEach { (input, salt, label) ->
            val result = VectorUtils.alignToEmbeddingDimension(input, salt = salt)
            assertEquals("$label: output dim", EMBEDDING_DIMENSION, result.size)
            val norm = l2Norm(result)
            assertTrue("$label: unit length, got $norm", norm in 0.999f..1.001f)
        }
    }

    @Test
    fun `align with different salts produces different outputs`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f)
        val sim = VectorUtils.cosineSimilarity(
            VectorUtils.alignToEmbeddingDimension(input, salt = 11),
            VectorUtils.alignToEmbeddingDimension(input, salt = 19)
        )
        assertTrue("Different salts should differ, sim=$sim", sim < 0.9999f)
    }

    @Test
    fun `align with empty vector returns zero vector`() {
        val result = VectorUtils.alignToEmbeddingDimension(floatArrayOf(), salt = 0)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `align with zero dimension returns empty array`() {
        assertEquals(0, VectorUtils.alignToEmbeddingDimension(floatArrayOf(1f, 2f), salt = 0, dimension = 0).size)
    }

    // ---- cosineSimilarity ----

    @Test
    fun `cosine similarity known cases`() {
        listOf(
            Triple(floatArrayOf(1f, 2f, 3f), floatArrayOf(1f, 2f, 3f), 1f),       // identical
            Triple(floatArrayOf(1f, 2f, 3f), floatArrayOf(-1f, -2f, -3f), -1f),    // opposite
            Triple(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), 0f),                 // orthogonal
            Triple(floatArrayOf(), floatArrayOf(), 0f),                              // empty
            Triple(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f, 3f), 0f),            // mismatched
        ).forEach { (a, b, expected) ->
            assertEquals("${a.toList()} vs ${b.toList()}", expected, VectorUtils.cosineSimilarity(a, b), 1e-5f)
        }
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
