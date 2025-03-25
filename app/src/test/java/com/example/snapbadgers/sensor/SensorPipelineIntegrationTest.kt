package com.example.snapbadgers.sensor

import com.example.snapbadgers.testing.BaseUnitTest
import com.example.snapbadgers.testing.TestFixtures
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration tests for the sensor processing pipeline.
 *
 * Tests the integration between SensorFeatureExtractor and SensorEncoderMLP,
 * consolidating the sensor/ package functionality.
 *
 * This addresses the architectural gap where sensor/ package (advanced MLP)
 * and ai/sensor/ package (simple stub) are separate.
 */
class SensorPipelineIntegrationTest : BaseUnitTest() {

    @Test
    fun `full sensor pipeline from raw data to embedding`() {
        val sensorData = TestFixtures.createStationarySensorData()

        // Step 1: Extract features
        val features = SensorFeatureExtractor.extract(sensorData)
        assertEquals("Should extract 14 features", 14, features.size)
        assertInRange(features, -1f, 1f)

        // Step 2: Encode to embedding
        val mlp = SensorEncoderMLP()
        val embedding = mlp.encode(features)
        assertEquals("Should produce 32-d embedding", 32, embedding.size)
        assertNormalized(embedding)
    }

    @Test
    fun `sensor pipeline handles workout scenario end-to-end`() {
        val workoutData = TestFixtures.createWorkoutSensorData()

        val features = SensorFeatureExtractor.extract(workoutData)
        val mlp = SensorEncoderMLP()
        val embedding = mlp.encode(features)

        // High activity should produce different embedding than stationary
        val stationaryData = TestFixtures.createStationarySensorData()
        val stationaryFeatures = SensorFeatureExtractor.extract(stationaryData)
        val stationaryEmbedding = mlp.encode(stationaryFeatures)

        assertFalse("Workout and stationary should produce different embeddings",
            embedding.contentEquals(stationaryEmbedding))
    }

    @Test
    fun `sensor pipeline preserves information through transformation`() {
        val morningCommute = SensorData(
            accelWindow = List(10) { floatArrayOf(2f, 1.5f, 9.8f) },
            lightLux = 300f,
            hourOfDay = 8,
            latitude = 37.7749,
            longitude = -122.4194
        )

        val eveningCommute = morningCommute.copy(
            hourOfDay = 18,
            lightLux = 50f  // Darker in evening
        )

        val mlp = SensorEncoderMLP()

        val morningFeatures = SensorFeatureExtractor.extract(morningCommute)
        val eveningFeatures = SensorFeatureExtractor.extract(eveningCommute)

        val morningEmb = mlp.encode(morningFeatures)
        val eveningEmb = mlp.encode(eveningFeatures)

        // Time difference should be captured
        assertFalse("Morning and evening should differ",
            morningEmb.contentEquals(eveningEmb))
    }

    @Test
    fun `sensor pipeline handles edge cases gracefully`() {
        val edgeCases = listOf(
            SensorData(emptyList(), 0f, 0, 0.0, 0.0),  // Minimal data
            SensorData(emptyList(), 10000f, 23, 90.0, 180.0),  // Max values
            SensorData(
                List(50) { floatArrayOf(20f, 20f, 20f) },  // Extreme movement
                5000f,
                12,
                -90.0,
                -180.0
            )
        )

        val mlp = SensorEncoderMLP()

        edgeCases.forEach { sensorData ->
            val features = SensorFeatureExtractor.extract(sensorData)
            val embedding = mlp.encode(features)

            assertEquals("Should always produce 32-d", 32, embedding.size)
            assertAllFinite(embedding)
            assertNormalized(embedding)
        }
    }

    @Test
    fun `sensor feature labels match extraction output`() {
        val labels = SensorFeatureExtractor.featureLabels()
        val sensorData = TestFixtures.createStationarySensorData()
        val features = SensorFeatureExtractor.extract(sensorData)

        assertEquals("Labels should match feature count", features.size, labels.size)
        assertEquals("First label should be accelMean", "accelMean", labels[0])
        assertEquals("Last label should be urbanProxy", "urbanProxy", labels[13])
    }

    @Test
    fun `sensor pipeline is deterministic`() {
        val sensorData = TestFixtures.createMovingSensorData()
        val mlp = SensorEncoderMLP()

        val features1 = SensorFeatureExtractor.extract(sensorData)
        val features2 = SensorFeatureExtractor.extract(sensorData)

        assertArrayEquals("Feature extraction is deterministic",
            features1, features2, 0.0001f)

        val emb1 = mlp.encode(features1)
        val emb2 = mlp.encode(features1)

        assertArrayEquals("MLP encoding is deterministic",
            emb1, emb2, 0.0001f)
    }

    @Test
    fun `sensor pipeline handles geographic diversity`() {
        val locations = listOf(
            Pair(37.7749, -122.4194),   // San Francisco
            Pair(51.5074, -0.1278),      // London
            Pair(-33.8688, 151.2093),    // Sydney
            Pair(35.6762, 139.6503),     // Tokyo
            Pair(0.0, 0.0)                // Null Island
        )

        val mlp = SensorEncoderMLP()
        val embeddings = mutableListOf<FloatArray>()

        locations.forEach { (lat, lon) ->
            val data = TestFixtures.createStationarySensorData(
                latitude = lat,
                longitude = lon
            )
            val features = SensorFeatureExtractor.extract(data)
            val embedding = mlp.encode(features)

            embeddings.add(embedding)
            assertNormalized(embedding)
        }

        // All locations should produce valid but potentially different embeddings
        assertTrue("Should have 5 embeddings", embeddings.size == 5)
    }

    @Test
    fun `sensor pipeline handles time of day variations`() {
        val times = listOf(2, 8, 12, 18, 23)  // Night, Morning, Noon, Evening, Late
        val mlp = SensorEncoderMLP()

        times.forEach { hour ->
            val data = TestFixtures.createStationarySensorData(hourOfDay = hour)
            val features = SensorFeatureExtractor.extract(data)
            val embedding = mlp.encode(features)

            assertEquals("Should always be 32-d", 32, embedding.size)
            assertNormalized(embedding)
        }
    }

    @Test
    fun `sensor pipeline captures activity intensity differences`() {
        val intensities = listOf(0f, 1f, 3f, 5f, 10f)
        val mlp = SensorEncoderMLP()
        val embeddings = mutableListOf<FloatArray>()

        intensities.forEach { intensity ->
            val data = TestFixtures.createMovingSensorData(intensity = intensity)
            val features = SensorFeatureExtractor.extract(data)
            val embedding = mlp.encode(features)

            embeddings.add(embedding)
            assertNormalized(embedding)
        }

        // Different intensities should produce different embeddings
        val uniqueEmbeddings = embeddings.distinct()
        assertTrue("Different intensities should produce varied embeddings",
            uniqueEmbeddings.size > 1)
    }
}
