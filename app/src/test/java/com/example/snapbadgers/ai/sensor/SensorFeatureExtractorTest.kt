package com.example.snapbadgers.ai.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorFeatureExtractorTest {

    @Test
    fun `extract produces 14-d feature vector`() {
        val data = SensorData(
            accelWindow = listOf(floatArrayOf(0f, 0f, 9.8f)),
            lightLux = 500f,
            hourOfDay = 14,
            latitude = 40.7,
            longitude = -74.0
        )
        val features = SensorFeatureExtractor.extract(data)
        assertEquals(14, features.size)
    }

    @Test
    fun `extract with empty accel window uses gravity defaults`() {
        val data = SensorData(lightLux = 100f, hourOfDay = 12)
        val features = SensorFeatureExtractor.extract(data)

        val expectedMean = (9.8f / 20f).coerceIn(0f, 1f)
        assertEquals(expectedMean, features[0], 1e-5f) // accelMean
        assertEquals(0f, features[1], 1e-5f)            // accelVariance
        assertEquals(expectedMean, features[2], 1e-5f) // accelRMS
        assertEquals(expectedMean, features[3], 1e-5f) // accelPeak
    }

    @Test
    fun `light categories are correct`() {
        val dark = SensorFeatureExtractor.extract(SensorData(lightLux = 5f))
        assertEquals(0.0f, dark[5], 1e-5f)

        val dim = SensorFeatureExtractor.extract(SensorData(lightLux = 100f))
        assertEquals(0.33f, dim[5], 1e-5f)

        val bright = SensorFeatureExtractor.extract(SensorData(lightLux = 1000f))
        assertEquals(0.67f, bright[5], 1e-5f)

        val veryBright = SensorFeatureExtractor.extract(SensorData(lightLux = 5000f))
        assertEquals(1.0f, veryBright[5], 1e-5f)
    }

    @Test
    fun `time categories are correct`() {
        val night = SensorFeatureExtractor.extract(SensorData(hourOfDay = 2))
        assertEquals(0.0f, night[8], 1e-5f)

        val morning = SensorFeatureExtractor.extract(SensorData(hourOfDay = 9))
        assertEquals(0.33f, morning[8], 1e-5f)

        val afternoon = SensorFeatureExtractor.extract(SensorData(hourOfDay = 14))
        assertEquals(0.67f, afternoon[8], 1e-5f)

        val evening = SensorFeatureExtractor.extract(SensorData(hourOfDay = 20))
        assertEquals(1.0f, evening[8], 1e-5f)
    }

    @Test
    fun `hemisphere flags are correct`() {
        val northEast = SensorFeatureExtractor.extract(
            SensorData(latitude = 40.7, longitude = 74.0)
        )
        assertEquals(1f, northEast[11], 0f) // hemisphereNorth
        assertEquals(1f, northEast[12], 0f) // hemisphereEast

        val southWest = SensorFeatureExtractor.extract(
            SensorData(latitude = -33.9, longitude = -74.0)
        )
        assertEquals(0f, southWest[11], 0f) // hemisphereNorth
        assertEquals(0f, southWest[12], 0f) // hemisphereEast
    }
}
