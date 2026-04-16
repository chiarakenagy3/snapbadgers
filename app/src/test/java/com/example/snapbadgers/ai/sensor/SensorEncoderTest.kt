package com.example.snapbadgers.ai.sensor

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SensorEncoderTest {

    private val encoder = SensorEncoder()

    private fun l2Norm(v: FloatArray): Float {
        var sum = 0f
        for (x in v) sum += x * x
        return sqrt(sum)
    }

    @Test
    fun `encode produces 128-d output`() {
        val sample = SensorSample(accelX = 1f, accelY = 2f, accelZ = 9.8f, light = 500f)
        val result = encoder.encode(sample)
        assertEquals(EMBEDDING_DIMENSION, result.size)
    }

    @Test
    fun `encode output is L2 normalized`() {
        val sample = SensorSample(accelX = 0.5f, accelY = -3f, accelZ = 9.8f, light = 1000f)
        val result = encoder.encode(sample)
        val norm = l2Norm(result)
        assertTrue("Expected unit length, got $norm", norm in 0.999f..1.001f)
    }

    @Test
    fun `encode with zero sensor values`() {
        val sample = SensorSample(accelX = 0f, accelY = 0f, accelZ = 0f, light = 0f)
        val result = encoder.encode(sample)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        // Even with 0 lux, IDX_ACOUSTICNESS is set to (1.0 - 0.0) = 1.0
        // So the vector should NOT be all zeros.
        val norm = l2Norm(result)
        assertTrue("Vector should be normalized even with zero inputs", norm in 0.999f..1.001f)
    }

    @Test
    fun `encode with extreme sensor values does not produce NaN`() {
        val sample = SensorSample(accelX = 100f, accelY = -100f, accelZ = 100f, light = 100_000f)
        val result = encoder.encode(sample)
        assertTrue(result.none { it.isNaN() || it.isInfinite() })
    }

    @Test
    fun `encode is deterministic`() {
        val sample = SensorSample(accelX = 1.5f, accelY = -0.3f, accelZ = 9.8f, light = 250f)
        val first = encoder.encode(sample)
        val second = encoder.encode(sample)
        assertArrayEquals(first, second, 0f)
    }
}
