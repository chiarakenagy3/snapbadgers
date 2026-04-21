package com.example.snapbadgers.ai.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class SensorEncoderLifecycleTest {

    companion object {
        private const val SENSOR_EMBEDDING_DIM = 32
        private const val L2_TOLERANCE = 1e-4f
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun getEmbeddingBeforeStartReturnsValidVector() {
        val encoder = SensorEncoder(context)

        val embedding = encoder.getEmbedding()

        assertEquals(
            "Sensor encoder must emit a 32-d embedding even with no sensor samples",
            SENSOR_EMBEDDING_DIM,
            embedding.size
        )
        assertTrue("Embedding must contain only finite values", embedding.all { it.isFinite() })
        assertL2Normalized(embedding)
    }

    @Test
    fun startAndStopAreIdempotent() {
        val encoder = SensorEncoder(context)

        encoder.start()
        encoder.start()
        val embedding = encoder.getEmbedding()
        encoder.stop()
        encoder.stop()

        assertEquals(SENSOR_EMBEDDING_DIM, embedding.size)
        assertL2Normalized(embedding)
    }

    @Test
    fun stopWithoutStartDoesNotThrow() {
        val encoder = SensorEncoder(context)
        encoder.stop()
    }

    @Test
    fun getEmbeddingAfterStopStillReturnsEmbedding() {
        val encoder = SensorEncoder(context)
        encoder.start()
        encoder.stop()

        val embedding = encoder.getEmbedding()

        assertEquals(SENSOR_EMBEDDING_DIM, embedding.size)
        assertTrue(embedding.all { it.isFinite() })
        assertL2Normalized(embedding)
    }

    @Test
    fun restartAfterStopWorks() {
        val encoder = SensorEncoder(context)
        encoder.start()
        encoder.stop()
        encoder.start()

        val embedding = encoder.getEmbedding()
        encoder.stop()

        assertEquals(SENSOR_EMBEDDING_DIM, embedding.size)
    }

    @Test
    fun currentFeaturesHasFourteenDimensions() {
        val encoder = SensorEncoder(context)

        val features = encoder.getCurrentFeatures()

        assertEquals("Feature vector must be 14-d before MLP encoding", 14, features.size)
        assertTrue(features.all { it.isFinite() })
    }

    @Test
    fun currentSensorDataSnapshotHasSafeDefaults() {
        val encoder = SensorEncoder(context)

        val snapshot = encoder.getCurrentSensorData()

        assertTrue("hourOfDay must be 0..23, was ${snapshot.hourOfDay}", snapshot.hourOfDay in 0..23)
        assertTrue("latitude must be finite", snapshot.latitude.isFinite())
        assertTrue("longitude must be finite", snapshot.longitude.isFinite())
        assertTrue("lightLux must be finite", snapshot.lightLux.isFinite())
    }

    @Test
    fun concurrentGetEmbeddingCallsAreSafe() {
        val encoder = SensorEncoder(context)
        encoder.start()
        try {
            val threads = List(8) {
                Thread {
                    repeat(20) { encoder.getEmbedding() }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }

            val embedding = encoder.getEmbedding()
            assertEquals(SENSOR_EMBEDDING_DIM, embedding.size)
        } finally {
            encoder.stop()
        }
    }

    private fun assertL2Normalized(embedding: FloatArray) {
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("Embedding must be L2-normalized", 1f, norm, L2_TOLERANCE)
    }
}
