package com.example.snapbadgers

import com.example.snapbadgers.embedding.getEmbedding
import com.example.snapbadgers.model.AudioFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingTest {

    @Test
    fun testGetEmbedding() {
        // 1. Create mock audio features
        val features = AudioFeatures(
            danceability = 0.5f,
            energy = 0.8f,
            speechiness = 0.05f,
            acousticness = 0.1f,
            instrumentalness = 0.2f,
            liveness = 0.15f,
            valence = 0.6f,
            tempo = 120f,
            loudness = -5f,
            duration_ms = 210000f
        )

        // 2. Run the embedding pipeline
        val embedding = getEmbedding(features)

        // 3. Assertions
        // Check size (should be 128 as defined in projectTo128)
        assertEquals(128, embedding.size)

        // Check normalization: sum of squares should be approximately 1.0
        var sumSquares = 0f
        for (v in embedding) sumSquares += v * v
        assertTrue("Vector should be normalized", sumSquares > 0.99f && sumSquares < 1.01f)

        println("Embedding test passed!")
        println("First 5 values: ${embedding.take(5)}")
    }
}