package com.example.snapbadgers

import kotlin.math.sqrt

class RecommendationService(private val songLibrary: List<Song>) {

    /**
     * Recommends songs by combining context and user embeddings.
     * 
     * @param contextEmbedding The embedding representing the current situation/mood.
     * @param userEmbedding The embedding representing the user's long-term preferences.
     * @param contextWeight How much to weight context vs user preferences (0.0 to 1.0).
     */
    fun recommendSongs(
        contextEmbedding: List<Float>,
        userEmbedding: List<Float>,
        contextWeight: Float = 0.5f,
        limit: Int = 5
    ): List<Song> {
        val combinedQuery = combineEmbeddings(contextEmbedding, userEmbedding, contextWeight)
        
        return songLibrary
            .map { song -> song to cosineSimilarity(combinedQuery, song.embedding) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Computes a weighted average of two embeddings to create a single query vector.
     */
    private fun combineEmbeddings(
        v1: List<Float>, 
        v2: List<Float>, 
        weightV1: Float
    ): List<Float> {
        if (v1.size != v2.size) return v1 // Fallback or throw error
        
        val weightV2 = 1.0f - weightV1
        return v1.indices.map { i ->
            (v1[i] * weightV1) + (v2[i] * weightV2)
        }
    }

    /**
     * Calculates cosine similarity between two vectors.
     */
    fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Double {
        if (v1.size != v2.size || v1.isEmpty()) return 0.0
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }
}
