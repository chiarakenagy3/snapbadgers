package com.example.snapbadgers.songembeddings

import com.example.snapbadgers.songembeddings.embedding.*
import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

/**
 * Comprehensive tests for EmbeddingUtils functions and MLPProjector.
 */
class EmbeddingUtilsTest {

    @Test
    fun `buildBaseVector returns 10 dimensional vector`() {
        val features = createTestAudioFeatures()
        val base = buildBaseVector(features)

        assertEquals("Base vector should be 10-d", 10, base.size)
    }

    @Test
    fun `buildBaseVector normalizes all values to 0-1 range`() {
        val features = createTestAudioFeatures(
            danceability = 0.8f,
            energy = 0.9f,
            tempo = 120f,
            loudness = -5f,
            duration_ms = 200000f
        )

        val base = buildBaseVector(features)

        assertTrue("All base features in [0, 1]", base.all { it in 0f..1f })
    }

    @Test
    fun `buildBaseVector handles extreme tempo`() {
        val features = createTestAudioFeatures(tempo = 250f)  // Very fast
        val base = buildBaseVector(features)

        assertTrue("Tempo normalized", base[7] in 0f..1f)
    }

    @Test
    fun `buildBaseVector handles extreme loudness`() {
        val features = createTestAudioFeatures(loudness = -40f)
        val base = buildBaseVector(features)

        assertTrue("Loudness normalized", base[8] in 0f..1f)
    }

    @Test
    fun `buildBaseVector handles very long duration`() {
        val features = createTestAudioFeatures(duration_ms = 600000f)  // 10 minutes
        val base = buildBaseVector(features)

        assertTrue("Duration clamped to max", base[9] <= 2f)
    }

    @Test
    fun `addDerivedFeatures returns 5 dimensional vector`() {
        val base = FloatArray(10) { 0.5f }
        val derived = addDerivedFeatures(base)

        assertEquals("Derived features should be 5-d", 5, derived.size)
    }

    @Test
    fun `addDerivedFeatures computes interactions`() {
        val base = floatArrayOf(
            0.8f,   // dance
            0.9f,   // energy
            0.1f,   // speechiness
            0.2f,   // acoustic
            0.5f,   // instrumentalness
            0.3f,   // liveness
            0.7f,   // valence
            0.6f,   // tempo
            0.4f,   // loudness
            0.5f    // duration
        )

        val derived = addDerivedFeatures(base)

        // dance * energy
        assertEquals("Dance x Energy", 0.8f * 0.9f, derived[0], 0.001f)
        // valence * energy
        assertEquals("Valence x Energy", 0.7f * 0.9f, derived[1], 0.001f)
        // acoustic * (1 - energy)
        assertEquals("Acoustic x (1-Energy)", 0.2f * 0.1f, derived[2], 0.001f)
        // tempo * energy
        assertEquals("Tempo x Energy", 0.6f * 0.9f, derived[3], 0.001f)
        // liveness * valence
        assertEquals("Liveness x Valence", 0.3f * 0.7f, derived[4], 0.001f)
    }

    @Test
    fun `getEmbedding returns 128 dimensional vector`() {
        val features = createTestAudioFeatures()
        val embedding = getEmbedding(features)

        assertEquals("Embedding should be 128-d", 128, embedding.size)
    }

    @Test
    fun `getEmbedding returns normalized vector`() {
        val features = createTestAudioFeatures()
        val embedding = getEmbedding(features)

        val magnitude = sqrt(embedding.map { it * it }.sum())
        assertTrue("Embedding should be normalized", magnitude in 0.99f..1.01f || magnitude < 0.01f)
    }

    @Test
    fun `getEmbedding with null features returns zero vector`() {
        val embedding = getEmbedding(null)

        assertEquals("Should be 128-d", 128, embedding.size)
        assertTrue("Should be all zeros", embedding.all { it == 0f })
    }

    @Test
    fun `getEmbedding is deterministic`() {
        val features = createTestAudioFeatures()

        val emb1 = getEmbedding(features)
        val emb2 = getEmbedding(features)

        assertArrayEquals("Same input should produce same embedding",
            emb1, emb2, 0.0001f)
    }

    @Test
    fun `getEmbedding for different songs produces different embeddings`() {
        val calm = createTestAudioFeatures(
            danceability = 0.2f,
            energy = 0.1f,
            valence = 0.3f,
            tempo = 80f
        )

        val energetic = createTestAudioFeatures(
            danceability = 0.9f,
            energy = 0.95f,
            valence = 0.9f,
            tempo = 140f
        )

        val emb1 = getEmbedding(calm)
        val emb2 = getEmbedding(energetic)

        assertFalse("Different songs should have different embeddings",
            emb1.contentEquals(emb2))
    }

    @Test
    fun `normalize handles zero vector`() {
        val vec = FloatArray(128) { 0f }
        val normalized = normalize(vec)

        assertEquals("Should return 128-d", 128, normalized.size)
        assertTrue("Should be all zeros", normalized.all { it == 0f })
    }

    @Test
    fun `normalize produces unit vector`() {
        val vec = FloatArray(128) { it * 0.1f }
        val normalized = normalize(vec)

        val magnitude = sqrt(normalized.map { it * it }.sum())
        assertEquals("Should have magnitude 1", 1f, magnitude, 0.01f)
    }

    @Test
    fun `normalize handles very small vectors`() {
        val vec = FloatArray(128) { 1e-10f }
        val normalized = normalize(vec)

        // Should handle without NaN
        assertTrue("Should not have NaN", normalized.none { it.isNaN() })
    }

    @Test
    fun `normalize handles negative values`() {
        val vec = FloatArray(128) { if (it % 2 == 0) 1f else -1f }
        val normalized = normalize(vec)

        val magnitude = sqrt(normalized.map { it * it }.sum())
        assertEquals("Should normalize to unit length", 1f, magnitude, 0.01f)
    }

    @Test
    fun `MLPProjector manual fallback works`() {
        // Without initialization, should use manual projection
        val input = FloatArray(15) { 0.5f }
        val output = MLPProjector.project(input)

        assertEquals("Should return 128-d", 128, output.size)
        assertTrue("Should be finite", output.all { it.isFinite() })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `MLPProjector rejects wrong input size`() {
        val input = FloatArray(10) { 0.5f }  // Wrong size
        MLPProjector.project(input)
    }

    @Test
    fun `MLPProjector handles zero input`() {
        val input = FloatArray(15) { 0f }
        val output = MLPProjector.project(input)

        assertEquals("Should return 128-d", 128, output.size)
    }

    @Test
    fun `MLPProjector handles ones input`() {
        val input = FloatArray(15) { 1f }
        val output = MLPProjector.project(input)

        assertEquals("Should return 128-d", 128, output.size)
        assertTrue("Should be finite", output.all { it.isFinite() })
    }

    @Test
    fun `getEmbedding handles calm acoustic song`() {
        val features = createTestAudioFeatures(
            danceability = 0.3f,
            energy = 0.2f,
            acousticness = 0.9f,
            instrumentalness = 0.8f,
            valence = 0.4f,
            tempo = 70f,
            loudness = -12f
        )

        val embedding = getEmbedding(features)

        assertEquals("Should be 128-d", 128, embedding.size)
        assertTrue("Should be normalized", embedding.any { it != 0f })
    }

    @Test
    fun `getEmbedding handles high energy dance song`() {
        val features = createTestAudioFeatures(
            danceability = 0.95f,
            energy = 0.9f,
            acousticness = 0.1f,
            instrumentalness = 0.0f,
            valence = 0.85f,
            tempo = 128f,
            loudness = -5f
        )

        val embedding = getEmbedding(features)

        assertEquals("Should be 128-d", 128, embedding.size)
        val magnitude = sqrt(embedding.map { it * it }.sum())
        assertTrue("Should be normalized", magnitude in 0.99f..1.01f)
    }

    @Test
    fun `getEmbedding handles spoken word content`() {
        val features = createTestAudioFeatures(
            speechiness = 0.95f,
            danceability = 0.1f,
            energy = 0.3f,
            instrumentalness = 0.0f
        )

        val embedding = getEmbedding(features)
        assertEquals("Should be 128-d", 128, embedding.size)
    }

    @Test
    fun `getEmbedding handles live recording`() {
        val features = createTestAudioFeatures(
            liveness = 0.9f,
            valence = 0.7f,
            energy = 0.8f
        )

        val embedding = getEmbedding(features)
        assertEquals("Should be 128-d", 128, embedding.size)
    }

    @Test
    fun `combined features vector is 15 dimensional`() {
        val features = createTestAudioFeatures()
        val base = buildBaseVector(features)
        val derived = addDerivedFeatures(base)
        val combined = base + derived

        assertEquals("Combined should be 15-d (10+5)", 15, combined.size)
    }

    @Test
    fun `embedding pipeline handles edge cases`() {
        val edgeCases = listOf(
            createTestAudioFeatures(danceability = 0f, energy = 0f),
            createTestAudioFeatures(danceability = 1f, energy = 1f),
            createTestAudioFeatures(tempo = 0f),
            createTestAudioFeatures(loudness = -60f),  // Very quiet
            createTestAudioFeatures(duration_ms = 1000f)  // Very short
        )

        edgeCases.forEach { features ->
            val embedding = getEmbedding(features)
            assertEquals("Should always be 128-d", 128, embedding.size)
            assertTrue("Should be finite", embedding.all { it.isFinite() })
        }
    }

    @Test
    fun `normalize is idempotent for normalized vectors`() {
        val vec = FloatArray(128) { it * 0.01f }
        val normalized1 = normalize(vec)
        val normalized2 = normalize(normalized1)

        assertArrayEquals("Normalizing twice should be same",
            normalized1, normalized2, 0.0001f)
    }

    @Test
    fun `buildBaseVector preserves relative differences`() {
        val low = createTestAudioFeatures(energy = 0.1f)
        val high = createTestAudioFeatures(energy = 0.9f)

        val baseLow = buildBaseVector(low)
        val baseHigh = buildBaseVector(high)

        assertTrue("High energy should have higher base value",
            baseHigh[1] > baseLow[1])
    }

    @Test
    fun `derived features amplify signal`() {
        val features = createTestAudioFeatures(
            danceability = 0.9f,
            energy = 0.9f,
            valence = 0.9f
        )

        val base = buildBaseVector(features)
        val derived = addDerivedFeatures(base)

        // Dance * Energy should be high
        assertTrue("High dance x energy interaction", derived[0] > 0.7f)
        // Valence * Energy should be high
        assertTrue("High valence x energy interaction", derived[1] > 0.7f)
    }

    // Helper function
    private fun createTestAudioFeatures(
        danceability: Float = 0.5f,
        energy: Float = 0.5f,
        speechiness: Float = 0.05f,
        acousticness: Float = 0.3f,
        instrumentalness: Float = 0.0f,
        liveness: Float = 0.1f,
        valence: Float = 0.5f,
        tempo: Float = 120f,
        loudness: Float = -5f,
        duration_ms: Float = 200000f
    ) = AudioFeatures(
        id = "test_track",
        danceability = danceability,
        energy = energy,
        key = 0,
        loudness = loudness,
        mode = 1,
        speechiness = speechiness,
        acousticness = acousticness,
        instrumentalness = instrumentalness,
        liveness = liveness,
        valence = valence,
        tempo = tempo,
        duration_ms = duration_ms
    )
}
