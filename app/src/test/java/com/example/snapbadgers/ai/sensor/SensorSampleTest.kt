package com.example.snapbadgers.ai.sensor

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SensorSample data class.
 */
class SensorSampleTest {

    @Test
    fun `SensorSample creation with valid values`() {
        val sample = SensorSample(
            accelX = 1.0f,
            accelY = 2.0f,
            accelZ = 9.8f,
            light = 100.0f
        )

        assertEquals(1.0f, sample.accelX, 0.0001f)
        assertEquals(2.0f, sample.accelY, 0.0001f)
        assertEquals(9.8f, sample.accelZ, 0.0001f)
        assertEquals(100.0f, sample.light, 0.0001f)
    }

    @Test
    fun `SensorSample with zero values`() {
        val sample = SensorSample(0f, 0f, 0f, 0f)

        assertEquals(0f, sample.accelX, 0.0001f)
        assertEquals(0f, sample.accelY, 0.0001f)
        assertEquals(0f, sample.accelZ, 0.0001f)
        assertEquals(0f, sample.light, 0.0001f)
    }

    @Test
    fun `SensorSample with negative acceleration`() {
        val sample = SensorSample(-5f, -3f, -9.8f, 50f)

        assertEquals(-5f, sample.accelX, 0.0001f)
        assertEquals(-3f, sample.accelY, 0.0001f)
        assertEquals(-9.8f, sample.accelZ, 0.0001f)
    }

    @Test
    fun `SensorSample with extreme values`() {
        val sample = SensorSample(
            accelX = Float.MAX_VALUE,
            accelY = Float.MIN_VALUE,
            accelZ = 0f,
            light = Float.MAX_VALUE
        )

        assertTrue("accelX should be finite", sample.accelX.isFinite())
        assertTrue("light should be finite", sample.light.isFinite())
    }

    @Test
    fun `SensorSample equality check`() {
        val sample1 = SensorSample(1f, 2f, 3f, 4f)
        val sample2 = SensorSample(1f, 2f, 3f, 4f)

        assertEquals(sample1, sample2)
    }

    @Test
    fun `SensorSample copy works correctly`() {
        val original = SensorSample(1f, 2f, 3f, 4f)
        val copy = original.copy(accelX = 10f)

        assertEquals(10f, copy.accelX, 0.0001f)
        assertEquals(2f, copy.accelY, 0.0001f)
        assertEquals(3f, copy.accelZ, 0.0001f)
        assertEquals(4f, copy.light, 0.0001f)
    }

    @Test
    fun `SensorSample toString contains values`() {
        val sample = SensorSample(1f, 2f, 3f, 4f)
        val string = sample.toString()

        assertTrue(string.contains("1"))
        assertTrue(string.contains("2"))
        assertTrue(string.contains("3"))
        assertTrue(string.contains("4"))
    }
}
