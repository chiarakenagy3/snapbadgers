package com.example.snapbadgers.ai.sensor

import com.example.snapbadgers.testing.BaseUnitTest
import com.example.snapbadgers.testing.TestFixtures
import org.junit.Assert.*
import org.junit.Test

class SensorPipelineIntegrationTest : BaseUnitTest() {

    @Test
    fun `full sensor pipeline from raw data to embedding`() {
        val sensorData = TestFixtures.createStationarySensorData()

        val features = SensorFeatureExtractor.extract(sensorData)
        assertEquals("Should extract 14 features", 14, features.size)
        assertInRange(features, -1f, 1f)

        val embedding = SensorEncoderMLP().encode(features)
        assertEquals("Should produce 32-d embedding", 32, embedding.size)
        assertNormalized(embedding)
    }

    @Test
    fun `sensor pipeline differentiates scenarios`() {
        val mlp = SensorEncoderMLP()

        // Workout vs stationary
        val workoutEmb = mlp.encode(SensorFeatureExtractor.extract(TestFixtures.createWorkoutSensorData()))
        val stationaryEmb = mlp.encode(SensorFeatureExtractor.extract(TestFixtures.createStationarySensorData()))
        assertFalse("Workout and stationary should differ", workoutEmb.contentEquals(stationaryEmb))

        // Morning vs evening commute
        val morningCommute = SensorData(
            accelWindow = List(10) { floatArrayOf(2f, 1.5f, 9.8f) },
            lightLux = 300f, hourOfDay = 8, latitude = 37.7749, longitude = -122.4194
        )
        val eveningCommute = morningCommute.copy(hourOfDay = 18, lightLux = 50f)
        val morningEmb = mlp.encode(SensorFeatureExtractor.extract(morningCommute))
        val eveningEmb = mlp.encode(SensorFeatureExtractor.extract(eveningCommute))
        assertFalse("Morning and evening should differ", morningEmb.contentEquals(eveningEmb))
    }

    @Test
    fun `sensor pipeline handles edge cases gracefully`() {
        val edgeCases = listOf(
            SensorData(emptyList(), 0f, 0, 0.0, 0.0),
            SensorData(emptyList(), 10000f, 23, 90.0, 180.0),
            SensorData(List(50) { floatArrayOf(20f, 20f, 20f) }, 5000f, 12, -90.0, -180.0)
        )
        val mlp = SensorEncoderMLP()

        edgeCases.forEach { sensorData ->
            val embedding = mlp.encode(SensorFeatureExtractor.extract(sensorData))
            assertEquals("Should always produce 32-d", 32, embedding.size)
            assertAllFinite(embedding)
            assertNormalized(embedding)
        }
    }

    @Test
    fun `sensor feature labels match extraction output`() {
        val labels = SensorFeatureExtractor.featureLabels()
        val features = SensorFeatureExtractor.extract(TestFixtures.createStationarySensorData())
        assertEquals("Labels should match feature count", features.size, labels.size)
        assertEquals("accelMean", labels[0])
        assertEquals("urbanProxy", labels[13])
    }

    @Test
    fun `sensor pipeline is deterministic`() {
        val sensorData = TestFixtures.createMovingSensorData()
        val mlp = SensorEncoderMLP()

        val features1 = SensorFeatureExtractor.extract(sensorData)
        val features2 = SensorFeatureExtractor.extract(sensorData)
        assertArrayEquals("Feature extraction is deterministic", features1, features2, 0.0001f)

        assertArrayEquals("MLP encoding is deterministic",
            mlp.encode(features1), mlp.encode(features1), 0.0001f)
    }

    @Test
    fun `sensor pipeline handles geographic and temporal diversity`() {
        val mlp = SensorEncoderMLP()

        // Geographic diversity
        val locations = listOf(
            Pair(37.7749, -122.4194), Pair(51.5074, -0.1278),
            Pair(-33.8688, 151.2093), Pair(35.6762, 139.6503), Pair(0.0, 0.0)
        )
        locations.forEach { (lat, lon) ->
            val embedding = mlp.encode(SensorFeatureExtractor.extract(
                TestFixtures.createStationarySensorData(latitude = lat, longitude = lon)
            ))
            assertNormalized(embedding)
        }

        // Temporal diversity
        listOf(2, 8, 12, 18, 23).forEach { hour ->
            val embedding = mlp.encode(SensorFeatureExtractor.extract(
                TestFixtures.createStationarySensorData(hourOfDay = hour)
            ))
            assertEquals(32, embedding.size)
            assertNormalized(embedding)
        }
    }

    @Test
    fun `sensor pipeline captures activity intensity differences`() {
        val mlp = SensorEncoderMLP()
        val embeddings = listOf(0f, 1f, 3f, 5f, 10f).map { intensity ->
            mlp.encode(SensorFeatureExtractor.extract(
                TestFixtures.createMovingSensorData(intensity = intensity)
            )).also { assertNormalized(it) }
        }
        assertTrue("Different intensities should produce varied embeddings",
            embeddings.distinct().size > 1)
    }
}
