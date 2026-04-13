package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.sensor.SensorData
import com.example.snapbadgers.ai.sensor.SensorEncoderMLP
import com.example.snapbadgers.ai.sensor.SensorFeatureExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * SensorFeatureExtractorEvalTest
 *
 * Evaluates the SensorFeatureExtractor and SensorEncoderMLP for
 * correct feature ranges, edge case handling, output dimensions,
 * and MLP normalization.
 *
 * These tests run on JVM without a device.
 */
class SensorFeatureExtractorEvalTest {

    private lateinit var mlp: SensorEncoderMLP

    @Before
    fun setUp() {
        mlp = SensorEncoderMLP()
    }

    // ------------------------------------------------------------------
    // Feature ranges: real-world accelerometer values
    // ------------------------------------------------------------------

    @Test
    fun `normal walking acceleration produces features in 0-1 range`() {
        // Walking typically produces ~10-12 m/s^2 magnitude (gravity + motion)
        val walkingSamples = listOf(
            floatArrayOf(0.5f, 9.8f, 0.3f),
            floatArrayOf(1.0f, 10.5f, -0.5f),
            floatArrayOf(-0.3f, 9.5f, 0.8f),
            floatArrayOf(0.8f, 10.2f, 0.1f),
            floatArrayOf(-0.5f, 9.9f, -0.2f)
        )
        val data = SensorData(
            accelWindow = walkingSamples,
            lightLux = 500f,
            hourOfDay = 14,
            latitude = 37.7749,
            longitude = -122.4194
        )

        val features = SensorFeatureExtractor.extract(data)
        println("EVAL walking_features:")
        SensorFeatureExtractor.featureLabels().forEachIndexed { i, label ->
            println("  [$i] $label: ${features[i]}")
        }

        // All features should be in reasonable range [-1, 1]
        features.forEachIndexed { i, v ->
            assertTrue("Feature $i should be in [-1, 1]: $v", v >= -1.0f && v <= 1.0f)
        }
    }

    @Test
    fun `high intensity activity acceleration within range`() {
        // Running/shaking: magnitude up to ~20 m/s^2
        val intenseSamples = (0 until 20).map {
            floatArrayOf(
                (it % 5 - 2).toFloat() * 4f,
                9.8f + (it % 3 - 1).toFloat() * 5f,
                (it % 4 - 2).toFloat() * 3f
            )
        }
        val data = SensorData(
            accelWindow = intenseSamples,
            lightLux = 100f,
            hourOfDay = 8
        )

        val features = SensorFeatureExtractor.extract(data)
        assertEquals("Feature vector should be 14-d", 14, features.size)
        features.forEachIndexed { i, v ->
            assertTrue("Feature $i should be in [-1, 1]: $v", v >= -1.0f && v <= 1.0f)
        }
        println("EVAL intense_activity: accelMean=${features[0]} accelPeak=${features[3]}")
    }

    @Test
    fun `max accelerometer values +- 20 m per s squared`() {
        val maxSamples = listOf(
            floatArrayOf(20f, 0f, 0f),
            floatArrayOf(-20f, 0f, 0f),
            floatArrayOf(0f, 20f, 0f),
            floatArrayOf(0f, -20f, 0f)
        )
        val data = SensorData(accelWindow = maxSamples, lightLux = 0f, hourOfDay = 0)
        val features = SensorFeatureExtractor.extract(data)

        // accelMean should be clamped to [0, 1]
        assertTrue("accelMean should be <= 1.0", features[0] <= 1.0f)
        assertTrue("accelPeak should be <= 1.0", features[3] <= 1.0f)
        println("EVAL max_accel: mean=${features[0]} peak=${features[3]}")
    }

    // ------------------------------------------------------------------
    // Light sensor ranges
    // ------------------------------------------------------------------

    @Test
    fun `light range 0 to 100000 lux`() {
        val testCases = listOf(0f, 10f, 200f, 2000f, 10000f, 100000f)
        println("EVAL light_ranges:")
        for (lux in testCases) {
            val data = SensorData(lightLux = lux, hourOfDay = 12)
            val features = SensorFeatureExtractor.extract(data)
            val lightNorm = features[4]
            val lightCat = features[5]
            assertTrue("lightNorm should be in [0, 1] for $lux lux", lightNorm in 0f..1f)
            assertTrue("lightCategory should be in [0, 1] for $lux lux", lightCat in 0f..1f)
            println("  lux=$lux -> lightNorm=$lightNorm lightCat=$lightCat")
        }
    }

    @Test
    fun `direct sunlight 100000 lux produces max light features`() {
        val data = SensorData(lightLux = 100000f, hourOfDay = 12)
        val features = SensorFeatureExtractor.extract(data)
        assertEquals("Direct sunlight should produce lightCategory=1.0", 1.0f, features[5], 1e-6f)
        println("EVAL sunlight: lightNorm=${features[4]} lightCat=${features[5]}")
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    fun `zero acceleration freefall`() {
        val freefallSamples = listOf(
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(0f, 0f, 0f)
        )
        val data = SensorData(accelWindow = freefallSamples, lightLux = 0f, hourOfDay = 0)
        val features = SensorFeatureExtractor.extract(data)

        assertEquals("Freefall accelMean should be 0", 0f, features[0], 1e-6f)
        assertEquals("Freefall accelVariance should be 0", 0f, features[1], 1e-6f)
        assertEquals("Freefall accelRMS should be 0", 0f, features[2], 1e-6f)
        assertEquals("Freefall accelPeak should be 0", 0f, features[3], 1e-6f)
        println("EVAL freefall: mean=${features[0]} var=${features[1]} rms=${features[2]} peak=${features[3]}")
    }

    @Test
    fun `empty accelerometer window defaults to gravity`() {
        val data = SensorData(accelWindow = emptyList(), lightLux = 500f, hourOfDay = 12)
        val features = SensorFeatureExtractor.extract(data)

        val expectedGravityNorm = (9.8f / 20f).coerceIn(0f, 1f)
        assertEquals("Empty window should default to gravity mean", expectedGravityNorm, features[0], 1e-5f)
        assertEquals("Empty window should have zero variance", 0f, features[1], 1e-6f)
        println("EVAL empty_accel: mean=${features[0]} var=${features[1]}")
    }

    @Test
    fun `time features cover full 24-hour cycle`() {
        println("EVAL time_features_24h:")
        for (hour in 0..23) {
            val data = SensorData(hourOfDay = hour, lightLux = 0f)
            val features = SensorFeatureExtractor.extract(data)
            val sin = features[6]
            val cos = features[7]
            val cat = features[8]
            assertTrue("timeNormSin should be in [-1, 1]", sin >= -1f && sin <= 1f)
            assertTrue("timeNormCos should be in [-1, 1]", cos >= -1f && cos <= 1f)
            assertTrue("timeCategory should be in [0, 1]", cat in 0f..1f)
            println("  hour=$hour -> sin=${"%.3f".format(sin)} cos=${"%.3f".format(cos)} cat=$cat")
        }
    }

    @Test
    fun `GPS features cover extremes`() {
        val cases = listOf(
            Pair(90.0, 180.0),    // North pole, date line
            Pair(-90.0, -180.0),  // South pole, date line
            Pair(0.0, 0.0),       // Equator/prime meridian
            Pair(37.7749, -122.4194) // San Francisco
        )
        println("EVAL gps_features:")
        for ((lat, lon) in cases) {
            val data = SensorData(latitude = lat, longitude = lon, lightLux = 0f, hourOfDay = 0)
            val features = SensorFeatureExtractor.extract(data)
            assertTrue("latNorm in [-1,1]", features[9] >= -1f && features[9] <= 1f)
            assertTrue("lonNorm in [-1,1]", features[10] >= -1f && features[10] <= 1f)
            println("  lat=$lat lon=$lon -> latNorm=${features[9]} lonNorm=${features[10]} hemiN=${features[11]} hemiE=${features[12]}")
        }
    }

    // ------------------------------------------------------------------
    // Feature count
    // ------------------------------------------------------------------

    @Test
    fun `feature vector is 14-d`() {
        val data = SensorData(lightLux = 100f, hourOfDay = 12)
        val features = SensorFeatureExtractor.extract(data)
        assertEquals("Feature vector should be 14 dimensions", 14, features.size)
        println("EVAL feature_count: ${features.size}")
    }

    @Test
    fun `feature labels match feature count`() {
        val labels = SensorFeatureExtractor.featureLabels()
        assertEquals("Labels count should match feature count (14)", 14, labels.size)
        println("EVAL feature_labels: ${labels.joinToString()}")
    }

    // ------------------------------------------------------------------
    // MLP output
    // ------------------------------------------------------------------

    @Test
    fun `SensorEncoderMLP produces 32-d output`() {
        val data = SensorData(
            accelWindow = listOf(floatArrayOf(0f, 9.8f, 0f)),
            lightLux = 500f,
            hourOfDay = 14,
            latitude = 37.7749,
            longitude = -122.4194
        )
        val features = SensorFeatureExtractor.extract(data)
        val embedding = mlp.encode(features)

        assertEquals("MLP output should be 32-d", 32, embedding.size)
        println("EVAL mlp_output_dim: ${embedding.size}")
    }

    @Test
    fun `SensorEncoderMLP produces L2-normalized output`() {
        val data = SensorData(
            accelWindow = listOf(floatArrayOf(1f, 9.8f, -0.5f)),
            lightLux = 300f,
            hourOfDay = 10,
            latitude = 40.7128,
            longitude = -74.006
        )
        val features = SensorFeatureExtractor.extract(data)
        val embedding = mlp.encode(features)

        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("MLP output should be L2-normalized", 1.0f, norm, 1e-4f)
        println("EVAL mlp_norm: $norm")
    }

    @Test
    fun `SensorEncoderMLP is deterministic`() {
        val features = FloatArray(14) { (it + 1).toFloat() / 14f }
        val first = mlp.encode(features)
        val second = mlp.encode(features)

        assertTrue("MLP should be deterministic", first.contentEquals(second))
        println("EVAL mlp_determinism: identical=true")
    }

    @Test
    fun `SensorEncoderMLP stability across diverse inputs`() {
        val rng = java.util.Random(77)
        var nanCount = 0
        var infCount = 0

        repeat(100) {
            val features = FloatArray(14) { rng.nextFloat() * 2f - 1f }
            val embedding = mlp.encode(features)
            if (embedding.any { it.isNaN() }) nanCount++
            if (embedding.any { it.isInfinite() }) infCount++
        }

        assertEquals("No NaN in MLP outputs", 0, nanCount)
        assertEquals("No Inf in MLP outputs", 0, infCount)
        println("EVAL mlp_stability_100: nan=$nanCount inf=$infCount")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SensorEncoderMLP rejects wrong input dimension`() {
        mlp.encode(FloatArray(10) { 0f })
    }
}
