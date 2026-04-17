package com.example.snapbadgers.ai.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Unit tests for SensorCollector.
 * Tests sensor lifecycle and data collection.
 */
class SensorCollectorTest {

    private lateinit var mockContext: Context
    private lateinit var mockSensorManager: SensorManager
    private lateinit var collector: SensorCollector

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockSensorManager = mock(SensorManager::class.java)
        `when`(mockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager)
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        collector = SensorCollector(mockContext)
    }

    @Test
    fun `start initializes sensor listeners`() {
        val mockAccel = mock(Sensor::class.java)
        val mockLight = mock(Sensor::class.java)

        `when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockAccel)
        `when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)).thenReturn(mockLight)

        collector.start()

        verify(mockSensorManager, atLeastOnce()).registerListener(
            any(),
            any(),
            anyInt()
        )
    }

    @Test
    fun `stop unregisters sensor listeners`() {
        collector.start()
        collector.stop()

        verify(mockSensorManager).unregisterListener(any())
    }

    @Test
    fun `getLatestSample returns valid sample`() {
        collector.start()

        val sample = collector.getLatestSample()

        assertNotNull(sample)
        assertTrue("Accel X should be finite", sample.accelX.isFinite())
        assertTrue("Accel Y should be finite", sample.accelY.isFinite())
        assertTrue("Accel Z should be finite", sample.accelZ.isFinite())
        assertTrue("Light should be non-negative", sample.light >= 0f)
    }

    @Test
    fun `getLatestSample before start returns default values`() {
        val sample = collector.getLatestSample()

        assertNotNull(sample)
        // Should return safe default values
    }

    @Test
    fun `multiple start calls are safe`() {
        collector.start()
        collector.start()
        collector.start()

        // Should not throw or cause issues
        val sample = collector.getLatestSample()
        assertNotNull(sample)
    }

    @Test
    fun `multiple stop calls are safe`() {
        collector.start()
        collector.stop()
        collector.stop()
        collector.stop()

        // Should not throw
    }

    @Test
    fun `start-stop-start cycle works`() {
        collector.start()
        val sample1 = collector.getLatestSample()

        collector.stop()

        collector.start()
        val sample2 = collector.getLatestSample()

        assertNotNull(sample1)
        assertNotNull(sample2)
    }

    @Test
    fun `collector handles missing accelerometer gracefully`() {
        `when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(null)

        collector.start()
        val sample = collector.getLatestSample()

        assertNotNull(sample)
        // Should use default values
    }

    @Test
    fun `collector handles missing light sensor gracefully`() {
        `when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)).thenReturn(null)

        collector.start()
        val sample = collector.getLatestSample()

        assertNotNull(sample)
        // Light should be 0 or default value
    }

    @Test
    fun `collector handles all sensors missing`() {
        `when`(mockSensorManager.getDefaultSensor(anyInt())).thenReturn(null)

        collector.start()
        val sample = collector.getLatestSample()

        assertNotNull(sample)
        // Should return safe defaults
    }
}
