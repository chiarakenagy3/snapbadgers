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

class SensorFeatureExtractorEvalTest {

    private lateinit var mlp: SensorEncoderMLP

    @Before
    fun setUp() {
        mlp = SensorEncoderMLP()
    }

    @Test
    fun `acceleration produces features in valid range`() {
        listOf(
            "walking" to SensorData(
                accelWindow = listOf(
                    floatArrayOf(0.5f, 9.8f, 0.3f), floatArrayOf(1.0f, 10.5f, -0.5f),
                    floatArrayOf(-0.3f, 9.5f, 0.8f), floatArrayOf(0.8f, 10.2f, 0.1f),
                    floatArrayOf(-0.5f, 9.9f, -0.2f)
                ),
                lightLux = 500f, hourOfDay = 14, latitude = 37.7749, longitude = -122.4194
            ),
            "intense" to SensorData(
                accelWindow = (0 until 20).map {
                    floatArrayOf(
                        (it % 5 - 2).toFloat() * 4f,
                        9.8f + (it % 3 - 1).toFloat() * 5f,
                        (it % 4 - 2).toFloat() * 3f
                    )
                },
                lightLux = 100f, hourOfDay = 8
            )
        ).forEach { (label, data) ->
            val features = SensorFeatureExtractor.extract(data)
            assertEquals("Feature vector should be 14-d", 14, features.size)
            features.forEachIndexed { i, v ->
                assertTrue("$label feature $i should be in [-1, 1]: $v", v >= -1.0f && v <= 1.0f)
            }
            if (label == "walking") {
                println("EVAL walking_features:")
                SensorFeatureExtractor.featureLabels().forEachIndexed { i, l -> println("  [$i] $l: ${features[i]}") }
            } else {
                println("EVAL intense_activity: accelMean=${features[0]} accelPeak=${features[3]}")
            }
        }
    }

    @Test
    fun `max accelerometer values +- 20 m per s squared`() {
        val features = SensorFeatureExtractor.extract(SensorData(
            accelWindow = listOf(
                floatArrayOf(20f, 0f, 0f), floatArrayOf(-20f, 0f, 0f),
                floatArrayOf(0f, 20f, 0f), floatArrayOf(0f, -20f, 0f)
            ),
            lightLux = 0f, hourOfDay = 0
        ))

        assertTrue("accelMean should be <= 1.0", features[0] <= 1.0f)
        assertTrue("accelPeak should be <= 1.0", features[3] <= 1.0f)
        println("EVAL max_accel: mean=${features[0]} peak=${features[3]}")
    }

    @Test
    fun `light sensor range and direct sunlight`() {
        println("EVAL light_ranges:")
        listOf(0f, 10f, 200f, 2000f, 10000f, 100000f).forEach { lux ->
            val features = SensorFeatureExtractor.extract(SensorData(lightLux = lux, hourOfDay = 12))
            assertTrue("lightNorm in [0,1] for $lux lux", features[4] in 0f..1f)
            assertTrue("lightCategory in [0,1] for $lux lux", features[5] in 0f..1f)
            println("  lux=$lux -> lightNorm=${features[4]} lightCat=${features[5]}")
        }
        // Direct sunlight max
        val sunlight = SensorFeatureExtractor.extract(SensorData(lightLux = 100000f, hourOfDay = 12))
        assertEquals(1.0f, sunlight[5], 1e-6f)
        println("EVAL sunlight: lightNorm=${sunlight[4]} lightCat=${sunlight[5]}")
    }

    @Test
    fun `edge cases - freefall, empty window, and time features`() {
        // Freefall
        val freefall = SensorFeatureExtractor.extract(SensorData(
            accelWindow = listOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f)),
            lightLux = 0f, hourOfDay = 0
        ))
        assertEquals(0f, freefall[0], 1e-6f)
        assertEquals(0f, freefall[1], 1e-6f)
        assertEquals(0f, freefall[2], 1e-6f)
        assertEquals(0f, freefall[3], 1e-6f)
        println("EVAL freefall: mean=${freefall[0]} var=${freefall[1]} rms=${freefall[2]} peak=${freefall[3]}")

        // Empty window defaults to gravity
        val empty = SensorFeatureExtractor.extract(SensorData(accelWindow = emptyList(), lightLux = 500f, hourOfDay = 12))
        assertEquals((9.8f / 20f).coerceIn(0f, 1f), empty[0], 1e-5f)
        assertEquals(0f, empty[1], 1e-6f)
        println("EVAL empty_accel: mean=${empty[0]} var=${empty[1]}")

        // Time features 24h cycle
        println("EVAL time_features_24h:")
        for (hour in 0..23) {
            val features = SensorFeatureExtractor.extract(SensorData(hourOfDay = hour, lightLux = 0f))
            assertTrue("timeNormSin in [-1,1]", features[6] >= -1f && features[6] <= 1f)
            assertTrue("timeNormCos in [-1,1]", features[7] >= -1f && features[7] <= 1f)
            assertTrue("timeCategory in [0,1]", features[8] in 0f..1f)
            println("  hour=$hour -> sin=${"%.3f".format(features[6])} cos=${"%.3f".format(features[7])} cat=${features[8]}")
        }
    }

    @Test
    fun `GPS features cover extremes`() {
        println("EVAL gps_features:")
        listOf(
            Pair(90.0, 180.0), Pair(-90.0, -180.0), Pair(0.0, 0.0), Pair(37.7749, -122.4194)
        ).forEach { (lat, lon) ->
            val features = SensorFeatureExtractor.extract(SensorData(latitude = lat, longitude = lon, lightLux = 0f, hourOfDay = 0))
            assertTrue("latNorm in [-1,1]", features[9] >= -1f && features[9] <= 1f)
            assertTrue("lonNorm in [-1,1]", features[10] >= -1f && features[10] <= 1f)
            println("  lat=$lat lon=$lon -> latNorm=${features[9]} lonNorm=${features[10]} hemiN=${features[11]} hemiE=${features[12]}")
        }
    }

    @Test
    fun `feature vector is 14-d and labels match`() {
        val features = SensorFeatureExtractor.extract(SensorData(lightLux = 100f, hourOfDay = 12))
        val labels = SensorFeatureExtractor.featureLabels()
        assertEquals(14, features.size)
        assertEquals(14, labels.size)
        println("EVAL feature_count: ${features.size}")
        println("EVAL feature_labels: ${labels.joinToString()}")
    }

    @Test
    fun `SensorEncoderMLP produces 32-d L2-normalized output`() {
        listOf(
            SensorData(accelWindow = listOf(floatArrayOf(0f, 9.8f, 0f)), lightLux = 500f, hourOfDay = 14, latitude = 37.7749, longitude = -122.4194),
            SensorData(accelWindow = listOf(floatArrayOf(1f, 9.8f, -0.5f)), lightLux = 300f, hourOfDay = 10, latitude = 40.7128, longitude = -74.006)
        ).forEach { data ->
            val embedding = mlp.encode(SensorFeatureExtractor.extract(data))
            assertEquals(32, embedding.size)
            val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
            assertEquals(1.0f, norm, 1e-4f)
            println("EVAL mlp_output: dim=${embedding.size} norm=$norm")
        }
    }

    @Test
    fun `SensorEncoderMLP is deterministic`() {
        val features = FloatArray(14) { (it + 1).toFloat() / 14f }
        assertTrue(mlp.encode(features).contentEquals(mlp.encode(features)))
        println("EVAL mlp_determinism: identical=true")
    }

    @Test
    fun `SensorEncoderMLP stability across diverse inputs`() {
        val rng = java.util.Random(77)
        var nanCount = 0
        var infCount = 0

        repeat(100) {
            val embedding = mlp.encode(FloatArray(14) { rng.nextFloat() * 2f - 1f })
            if (embedding.any { it.isNaN() }) nanCount++
            if (embedding.any { it.isInfinite() }) infCount++
        }

        assertEquals(0, nanCount)
        assertEquals(0, infCount)
        println("EVAL mlp_stability_100: nan=$nanCount inf=$infCount")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SensorEncoderMLP rejects wrong input dimension`() {
        mlp.encode(FloatArray(10) { 0f })
    }
}
