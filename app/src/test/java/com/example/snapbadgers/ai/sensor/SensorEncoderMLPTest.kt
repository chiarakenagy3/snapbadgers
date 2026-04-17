package com.example.snapbadgers.ai.sensor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SensorEncoderMLPTest {

    private val mlp = SensorEncoderMLP()

    private fun l2Norm(v: FloatArray): Float {
        var sum = 0f
        for (x in v) sum += x * x
        return sqrt(sum)
    }

    @Test
    fun `encode produces 32-d output`() {
        val features = FloatArray(14) { 0.5f }
        val result = mlp.encode(features)
        assertEquals(32, result.size)
    }

    @Test
    fun `encode output is L2 normalized`() {
        val features = FloatArray(14) { (it + 1).toFloat() * 0.05f }
        val result = mlp.encode(features)
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encode rejects wrong input dimension`() {
        mlp.encode(FloatArray(10) { 1f })
    }

    @Test
    fun `encode is deterministic with seed 42`() {
        val mlp1 = SensorEncoderMLP()
        val mlp2 = SensorEncoderMLP()
        val features = FloatArray(14) { it.toFloat() * 0.1f }

        val out1 = mlp1.encode(features)
        val out2 = mlp2.encode(features)

        assertArrayEquals("Same seed should produce identical outputs", out1, out2, 0f)
    }

    @Test
    fun `loadWeights does not crash with valid sizes`() {
        val mlpInst = SensorEncoderMLP()
        val w1 = FloatArray(32 * 14) { 0.01f }
        val b1 = FloatArray(32) { 0f }
        val w2 = FloatArray(32 * 32) { 0.01f }
        val b2 = FloatArray(32) { 0f }

        mlpInst.loadWeights(w1, b1, w2, b2)

        val features = FloatArray(14) { 1f }
        val result = mlpInst.encode(features)
        assertEquals(32, result.size)
    }
}
