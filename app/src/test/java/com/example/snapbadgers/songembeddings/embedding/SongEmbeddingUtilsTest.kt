package com.example.snapbadgers.songembeddings.embedding

import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SongEmbeddingUtilsTest {

    private fun sampleFeatures(
        tempo: Float = 128f,
        loudness: Float = -8f,
        durationMs: Float = 180_000f
    ) = AudioFeatures(
        id = "track-1",
        danceability = 0.7f,
        energy = 0.6f,
        speechiness = 0.04f,
        acousticness = 0.2f,
        instrumentalness = 0.1f,
        liveness = 0.3f,
        valence = 0.8f,
        tempo = tempo,
        loudness = loudness,
        key = 5,
        mode = 1,
        duration_ms = durationMs
    )

    @Test
    fun `buildBaseVector should scale and clamp values correctly`() {
        val vector = buildBaseVector(
            sampleFeatures(tempo = 220f, loudness = -70f, durationMs = 700_000f)
        )

        assertEquals(10, vector.size)
        assertEquals(0.7f, vector[0], 1e-6f)
        assertEquals(0.6f, vector[1], 1e-6f)
        assertEquals(1f, vector[7], 1e-6f) // tempo clamped to 1
        assertEquals(0f, vector[8], 1e-6f) // loudness clamped to 0
        assertEquals(2f, vector[9], 1e-6f) // duration clamped to 2
    }

    @Test
    fun `addDerivedFeatures should compute interaction features`() {
        val base = floatArrayOf(
            0.5f, // dance
            0.8f, // energy
            0.1f, // speechiness
            0.2f, // acoustic
            0.0f, // instrumentalness
            0.4f, // liveness
            0.6f, // valence
            0.7f, // tempo
            0.5f, // loudness norm
            1.0f  // duration norm
        )

        val derived = addDerivedFeatures(base)
        val expected = floatArrayOf(
            0.4f,  // dance * energy
            0.48f, // valence * energy
            0.04f, // acoustic * (1 - energy)
            0.56f, // tempo * energy
            0.24f  // liveness * valence
        )
        assertArrayEquals(expected, derived, 1e-6f)
    }

    @Test
    fun `getEmbedding with null should return zero 128d vector`() {
        val embedding = getEmbedding(null)
        assertEquals(128, embedding.size)
        assertTrue(embedding.all { it == 0f })
    }

    @Test
    fun `getEmbedding should be deterministic and normalized`() {
        val features = sampleFeatures()
        val first = getEmbedding(features)
        val second = getEmbedding(features)

        assertEquals(128, first.size)
        assertArrayEquals(first, second, 1e-6f)

        val norm = sqrt(first.sumOf { (it * it).toDouble() }).toFloat()
        assertTrue("Embedding should be L2-normalized", norm in 0.999f..1.001f)
    }

    @Test
    fun `normalize should return zero vector for near-zero input`() {
        val normalized = normalize(FloatArray(8) { 0f })
        assertTrue(normalized.all { it == 0f })
    }
}
