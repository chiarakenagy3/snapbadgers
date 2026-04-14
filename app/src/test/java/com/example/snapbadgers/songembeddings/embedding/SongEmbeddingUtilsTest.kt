package com.example.snapbadgers.songembeddings.embedding

import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SongEmbeddingUtilsTest {

    private fun sampleFeatures(
        tempo: Float = 128f, loudness: Float = -8f, durationMs: Float = 180_000f
    ) = AudioFeatures(
        id = "track-1", danceability = 0.7f, energy = 0.6f, speechiness = 0.04f,
        acousticness = 0.2f, instrumentalness = 0.1f, liveness = 0.3f, valence = 0.8f,
        tempo = tempo, loudness = loudness, key = 5, mode = 1, duration_ms = durationMs
    )

    @Test
    fun `buildBaseVector should scale and clamp values correctly`() {
        val vector = buildBaseVector(sampleFeatures(tempo = 220f, loudness = -70f, durationMs = 700_000f))
        assertEquals(10, vector.size)
        assertEquals(0.7f, vector[0], 1e-6f)
        assertEquals(0.6f, vector[1], 1e-6f)
        assertEquals(1f, vector[7], 1e-6f)  // tempo clamped
        assertEquals(0f, vector[8], 1e-6f)  // loudness clamped
        assertEquals(2f, vector[9], 1e-6f)  // duration clamped
    }

    @Test
    fun `addDerivedFeatures should compute interaction features`() {
        val base = floatArrayOf(0.5f, 0.8f, 0.1f, 0.2f, 0.0f, 0.4f, 0.6f, 0.7f, 0.5f, 1.0f)
        assertArrayEquals(
            floatArrayOf(0.4f, 0.48f, 0.04f, 0.56f, 0.24f),
            addDerivedFeatures(base), 1e-6f
        )
    }

    @Test
    fun `getEmbedding with null should return zero 128d vector`() {
        val embedding = getEmbedding(null)
        assertEquals(128, embedding.size)
        assertTrue(embedding.all { it == 0f })
    }

    @Test
    fun `getEmbedding should be deterministic and normalized`() {
        val first = getEmbedding(sampleFeatures())
        val second = getEmbedding(sampleFeatures())
        assertEquals(128, first.size)
        assertArrayEquals(first, second, 1e-6f)
        val norm = sqrt(first.sumOf { (it * it).toDouble() }).toFloat()
        assertTrue("Embedding should be L2-normalized", norm in 0.999f..1.001f)
    }

    @Test
    fun `normalize should return zero vector for near-zero input`() {
        assertTrue(VectorUtils.normalize(FloatArray(8) { 0f }).all { it == 0f })
    }
}
