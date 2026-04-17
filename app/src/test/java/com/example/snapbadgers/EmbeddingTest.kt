package com.example.snapbadgers

import com.example.snapbadgers.songembeddings.embedding.getEmbedding
import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingTest {

    @Test
    fun testGetEmbedding() {
        val features = AudioFeatures(
            id = "test-track",
            danceability = 0.5f,
            energy = 0.8f,
            speechiness = 0.05f,
            acousticness = 0.1f,
            instrumentalness = 0.2f,
            liveness = 0.15f,
            valence = 0.6f,
            tempo = 120f,
            loudness = -5f,
            key = 0,
            mode = 1,
            durationMs = 210000f
        )

        val embedding = getEmbedding(features)

        assertEquals(128, embedding.size)

        var sumSquares = 0f
        for (v in embedding) sumSquares += v * v
        assertTrue("Vector should be normalized", sumSquares > 0.99f && sumSquares < 1.01f)
    }

    @Test
    fun testDifferentMoods() {
        val upbeatFeatures = AudioFeatures(
            id = "upbeat", danceability = 0.8f, energy = 0.9f, speechiness = 0.1f,
            acousticness = 0.05f, instrumentalness = 0.0f, liveness = 0.2f,
            valence = 0.9f, tempo = 128f, loudness = -4f, key = 5, mode = 1
        )

        val acousticFeatures = AudioFeatures(
            id = "acoustic", danceability = 0.3f, energy = 0.2f, speechiness = 0.03f,
            acousticness = 0.9f, instrumentalness = 0.1f, liveness = 0.1f,
            valence = 0.2f, tempo = 75f, loudness = -15f, key = 2, mode = 0
        )

        val upbeatEmbedding = getEmbedding(upbeatFeatures)
        val acousticEmbedding = getEmbedding(acousticFeatures)

        val similarity = cosineSimilarity(upbeatEmbedding, acousticEmbedding)
        assertTrue("Different moods should not be too similar", similarity < 0.8f)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return dotProduct / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble())).toFloat()
    }
}
