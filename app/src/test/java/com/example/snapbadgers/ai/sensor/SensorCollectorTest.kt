package com.example.snapbadgers.ai.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
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

        // Mock default sensors before creating the collector so it can initialize its fields
        val mockAccel = mock(Sensor::class.java)
        val mockLight = mock(Sensor::class.java)
        `when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockAccel)
        `when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)).thenReturn(mockLight)

        collector = SensorCollector(mockContext)
    }

    @Test
    fun `start initializes sensor listeners`() {
        // Mocking moved to setup() to ensure they are present during collector initialization
        collector.start()

        verify(mockSensorManager, atLeastOnce()).registerListener(
            any(SensorEventListener::class.java),
            any(Sensor::class.java),
            anyInt()
        )
    }

    @Test
    fun `stop unregisters sensor listeners`() {
        collector.start()
        collector.stop()

        verify(mockSensorManager).unregisterListener(any(SensorEventListener::class.java))
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
        // Re-create collector with missing sensor
        val localMockContext = mock(Context::class.java)
        val localMockSensorManager = mock(SensorManager::class.java)
        `when`(localMockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(localMockSensorManager)
        `when`(localMockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(null)
        
        val localCollector = SensorCollector(localMockContext)
        localCollector.start()
        val sample = localCollector.getLatestSample()

        assertNotNull(sample)
    }

    @Test
    fun `collector handles missing light sensor gracefully`() {
        // Re-create collector with missing sensor
        val localMockContext = mock(Context::class.java)
        val localMockSensorManager = mock(SensorManager::class.java)
        `when`(localMockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(localMockSensorManager)
        `when`(localMockSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)).thenReturn(null)

        val localCollector = SensorCollector(localMockContext)
        localCollector.start()
        val sample = localCollector.getLatestSample()

        assertNotNull(sample)
    }

    @Test
    fun `collector handles all sensors missing`() {
        // Re-create collector with no sensors
        val localMockContext = mock(Context::class.java)
        val localMockSensorManager = mock(SensorManager::class.java)
        `when`(localMockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(localMockSensorManager)
        `when`(localMockSensorManager.getDefaultSensor(anyInt())).thenReturn(null)

        val localCollector = SensorCollector(localMockContext)
        localCollector.start()
        val sample = localCollector.getLatestSample()

        assertNotNull(sample)
    }
}
