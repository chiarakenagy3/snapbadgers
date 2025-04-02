package com.example.snapbadgers.ai.projection

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class ProjectionNetworkTest {

    private val network = ProjectionNetwork()

    private fun l2Norm(v: FloatArray): Float {
        var sum = 0f
        for (x in v) sum += x * x
        return sqrt(sum)
    }

    @Test
    fun `project produces 128-d output from 128-d input`() {
        val input = FloatArray(EMBEDDING_DIMENSION) { it.toFloat() * 0.01f }
        val output = network.project(input)
        assertEquals(EMBEDDING_DIMENSION, output.size)
    }

    @Test
    fun `project output is L2 normalized`() {
        val input = FloatArray(EMBEDDING_DIMENSION) { it.toFloat() * 0.01f }
        val output = network.project(input)
        val norm = l2Norm(output)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test
    fun `He initialization is deterministic with seed 42`() {
        val net1 = ProjectionNetwork()
        val net2 = ProjectionNetwork()

        val input = FloatArray(EMBEDDING_DIMENSION) { (it + 1).toFloat() * 0.05f }
        val out1 = net1.project(input)
        val out2 = net2.project(input)

        assertArrayEquals("Same seed should produce identical weights and outputs", out1, out2, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `project rejects wrong input dimension`() {
        val wrongSize = FloatArray(64) { 1f }
        network.project(wrongSize)
    }

    @Test
    fun `project with zero input returns zero or normalized vector`() {
        val zero = FloatArray(EMBEDDING_DIMENSION)
        val output = network.project(zero)
        assertEquals(EMBEDDING_DIMENSION, output.size)
        // Biases are 0 in He init, and ReLU of 0 is 0, so output should be zero
        assertTrue("Zero input with zero bias should produce zero output", output.all { it == 0f })
    }

    @Test
    fun `project handles very small input values`() {
        val tiny = FloatArray(EMBEDDING_DIMENSION) { 1e-7f }
        val output = network.project(tiny)
        assertEquals(EMBEDDING_DIMENSION, output.size)
        // Should not produce NaN or Inf
        assertTrue(output.none { it.isNaN() || it.isInfinite() })
    }

    @Test
    fun `loadWeights does not crash with valid sizes`() {
        val net = ProjectionNetwork()
        val w1 = FloatArray(EMBEDDING_DIMENSION * EMBEDDING_DIMENSION) { 0.01f }
        val b1 = FloatArray(EMBEDDING_DIMENSION) { 0f }
        val w2 = FloatArray(EMBEDDING_DIMENSION * EMBEDDING_DIMENSION) { 0.01f }
        val b2 = FloatArray(EMBEDDING_DIMENSION) { 0f }

        net.loadWeights(w1, b1, w2, b2)

        // Verify the loaded weights are used
        val input = FloatArray(EMBEDDING_DIMENSION) { 1f }
        val output = net.project(input)
        assertEquals(EMBEDDING_DIMENSION, output.size)
        val norm = l2Norm(output)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }
}
