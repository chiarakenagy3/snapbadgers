package com.example.snapbadgers

import kotlin.math.sqrt

class RecommendationService(private val songLibrary: List<Song>) {

    /**
     * Finds the top N songs that are most similar to the provided input embedding.
     */
    fun recommendSongs(inputEmbedding: List<Float>, limit: Int = 5): List<Song> {
        return songLibrary
            .map { song -> song to cosineSimilarity(inputEmbedding, song.embedding) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Calculates cosine similarity between two vectors.
     */
    private fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Double {
        if (v1.size != v2.size) return 0.0
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += Math.pow(v1[i].toDouble(), 2.0)
            normB += Math.pow(v2[i].toDouble(), 2.0)
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }
}
