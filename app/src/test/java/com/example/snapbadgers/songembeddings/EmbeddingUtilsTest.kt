package com.example.snapbadgers.songembeddings

import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.songembeddings.embedding.*
import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class EmbeddingUtilsTest {

    private fun af(
        danceability: Float = 0.5f, energy: Float = 0.5f, speechiness: Float = 0.05f,
        acousticness: Float = 0.3f, instrumentalness: Float = 0.0f, liveness: Float = 0.1f,
        valence: Float = 0.5f, tempo: Float = 120f, loudness: Float = -5f,
        durationMs: Int = 200000
    ) = AudioFeatures(
        id = "test_track", danceability = danceability, energy = energy, key = 0,
        loudness = loudness, mode = 1, speechiness = speechiness, acousticness = acousticness,
        instrumentalness = instrumentalness, liveness = liveness, valence = valence,
        tempo = tempo, durationMs = durationMs.toFloat()
    )

    @Test
    fun `buildBaseVector returns 10 dimensional vector`() {
        assertEquals(10, buildBaseVector(af()).size)
    }

    @Test
    fun `buildBaseVector normalizes all values to 0-1 range`() {
        val base = buildBaseVector(af(danceability = 0.8f, energy = 0.9f, tempo = 120f, loudness = -5f, durationMs = 200000))
        assertTrue("All base features in [0, 1]", base.all { it in 0f..1f })
    }

    @Test
    fun `buildBaseVector handles extreme inputs`() {
        listOf(
            af(tempo = 250f) to 7,          // extreme tempo
            af(loudness = -40f) to 8,       // extreme loudness
        ).forEach { (features, idx) ->
            assertTrue("Index $idx normalized", buildBaseVector(features)[idx] in 0f..1f)
        }
        assertTrue("Duration clamped to max", buildBaseVector(af(durationMs = 600000))[9] <= 2f)
    }

    @Test
    fun `addDerivedFeatures returns 5 dimensional vector`() {
        assertEquals(5, addDerivedFeatures(FloatArray(10) { 0.5f }).size)
    }

    @Test
    fun `addDerivedFeatures computes interactions`() {
        val base = floatArrayOf(0.8f, 0.9f, 0.1f, 0.2f, 0.5f, 0.3f, 0.7f, 0.6f, 0.4f, 0.5f)
        val derived = addDerivedFeatures(base)
        assertEquals(0.8f * 0.9f, derived[0], 0.001f)   // dance * energy
        assertEquals(0.7f * 0.9f, derived[1], 0.001f)   // valence * energy
        assertEquals(0.2f * 0.1f, derived[2], 0.001f)   // acoustic * (1-energy)
        assertEquals(0.6f * 0.9f, derived[3], 0.001f)   // tempo * energy
        assertEquals(0.3f * 0.7f, derived[4], 0.001f)   // liveness * valence
    }

    @Test
    fun `getEmbedding returns 128-d normalized vector`() {
        val embedding = getEmbedding(af())
        assertEquals(128, embedding.size)
        val magnitude = sqrt(embedding.map { it * it }.sum())
        assertTrue("Embedding should be normalized", magnitude in 0.99f..1.01f || magnitude < 0.01f)
    }

    @Test
    fun `getEmbedding with null features returns zero vector`() {
        val embedding = getEmbedding(null)
        assertEquals(128, embedding.size)
        assertTrue(embedding.all { it == 0f })
    }

    @Test
    fun `getEmbedding is deterministic`() {
        assertArrayEquals(getEmbedding(af()), getEmbedding(af()), 0.0001f)
    }

    @Test
    fun `getEmbedding for different songs produces different embeddings`() {
        val calm = af(danceability = 0.2f, energy = 0.1f, valence = 0.3f, tempo = 80f)
        val energetic = af(danceability = 0.9f, energy = 0.95f, valence = 0.9f, tempo = 140f)
        assertFalse(getEmbedding(calm).contentEquals(getEmbedding(energetic)))
    }

    @Test
    fun `normalize handles various inputs`() {
        listOf(
            FloatArray(128) { 0f } to "zero",
            FloatArray(128) { it * 0.1f } to "ramp",
            FloatArray(128) { 1e-10f } to "small",
            FloatArray(128) { if (it % 2 == 0) 1f else -1f } to "negative",
        ).forEach { (vec, label) ->
            val normalized = VectorUtils.normalize(vec)
            assertEquals("$label: size preserved", 128, normalized.size)
            assertTrue("$label: no NaN", normalized.none { it.isNaN() })
            val mag = sqrt(normalized.map { it * it }.sum())
            assertTrue("$label: unit or zero", mag in 0.99f..1.01f || mag < 0.01f)
        }
    }

    @Test
    fun `normalize is idempotent`() {
        val vec = FloatArray(128) { it * 0.01f }
        val n1 = VectorUtils.normalize(vec)
        assertArrayEquals(n1, VectorUtils.normalize(n1), 0.0001f)
    }

    @Test
    fun `MLPProjector manual fallback works`() {
        listOf(
            FloatArray(15) { 0.5f } to "half",
            FloatArray(15) { 0f } to "zero",
            FloatArray(15) { 1f } to "ones",
        ).forEach { (input, label) ->
            val output = MLPProjector.project(input)
            assertEquals("$label: 128-d", 128, output.size)
            assertTrue("$label: finite", output.all { it.isFinite() })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `MLPProjector rejects wrong input size`() {
        MLPProjector.project(FloatArray(10) { 0.5f })
    }

    @Test
    fun `getEmbedding handles genre-specific audio profiles`() {
        listOf(
            af(danceability = 0.3f, energy = 0.2f, acousticness = 0.9f, instrumentalness = 0.8f, valence = 0.4f, tempo = 70f, loudness = -12f),
            af(danceability = 0.95f, energy = 0.9f, acousticness = 0.1f, instrumentalness = 0.0f, valence = 0.85f, tempo = 128f, loudness = -5f),
            af(speechiness = 0.95f, danceability = 0.1f, energy = 0.3f, instrumentalness = 0.0f),
            af(liveness = 0.9f, valence = 0.7f, energy = 0.8f),
        ).forEach { features ->
            val embedding = getEmbedding(features)
            assertEquals(128, embedding.size)
            assertTrue(embedding.all { it.isFinite() })
        }
    }

    @Test
    fun `combined features vector is 15 dimensional`() {
        val base = buildBaseVector(af())
        assertEquals(15, (base + addDerivedFeatures(base)).size)
    }

    @Test
    fun `embedding pipeline handles edge cases`() {
        listOf(
            af(danceability = 0f, energy = 0f),
            af(danceability = 1f, energy = 1f),
            af(tempo = 0f),
            af(loudness = -60f),
            af(durationMs = 1000),
        ).forEach { features ->
            val embedding = getEmbedding(features)
            assertEquals(128, embedding.size)
            assertTrue(embedding.all { it.isFinite() })
        }
    }

    @Test
    fun `buildBaseVector preserves relative differences`() {
        assertTrue(buildBaseVector(af(energy = 0.9f))[1] > buildBaseVector(af(energy = 0.1f))[1])
    }

    @Test
    fun `derived features amplify signal`() {
        val base = buildBaseVector(af(danceability = 0.9f, energy = 0.9f, valence = 0.9f))
        val derived = addDerivedFeatures(base)
        assertTrue("High dance x energy interaction", derived[0] > 0.7f)
        assertTrue("High valence x energy interaction", derived[1] > 0.7f)
    }
}
