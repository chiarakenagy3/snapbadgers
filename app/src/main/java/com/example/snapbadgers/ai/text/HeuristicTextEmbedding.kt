package com.example.snapbadgers.ai.text

import com.example.snapbadgers.ai.common.ml.*

/**
 * HeuristicTextEmbedding
 *
 * Refined to provide more distinct profiles for "sad" vs "workout".
 */
object HeuristicTextEmbedding {

    fun encode(text: String): FloatArray {
        val embedding = FloatArray(EMBEDDING_DIMENSION)
        val normalizedText = text.trim().lowercase()
        
        if (normalizedText.isEmpty()) return embedding

        val tokens = normalizedText.split(Regex("\\s+")).filter { it.isNotBlank() }

        // --- Refined Semantic Mapping ---

        // Happiness / Vibe (Valence)
        if (containsAny(normalizedText, "happy", "joy", "cheerful", "bright", "sunny", "vibey")) {
            embedding[IDX_VALENCE] = 0.9f
            embedding[IDX_ENERGY] = 0.6f // Happy usually has some energy
        } else if (containsAny(normalizedText, "sad", "gloomy", "melancholy", "depressing", "dark", "lonely")) {
            embedding[IDX_VALENCE] = 0.1f
            embedding[IDX_ENERGY] = 0.2f       // Sad should be LOW energy
            embedding[IDX_ACOUSTICNESS] = 0.8f // Sad is often acoustic
            embedding[IDX_INSTRUMENTALNESS] = 0.3f
        }

        // Energy / Intensity
        if (containsAny(normalizedText, "workout", "energetic", "fast", "aggressive", "hype", "pumped", "gym")) {
            embedding[IDX_ENERGY] = 0.95f
            embedding[IDX_TEMPO] = 0.9f
            embedding[IDX_DANCEABILITY] = 0.8f
            embedding[IDX_VALENCE] = 0.6f // Workout is positive/neutral
        } else if (containsAny(normalizedText, "calm", "relax", "chill", "peaceful", "sleep", "ambient", "mellow")) {
            embedding[IDX_ENERGY] = 0.15f
            embedding[IDX_ACOUSTICNESS] = 0.9f
            embedding[IDX_DANCEABILITY] = 0.1f
            embedding[IDX_VALENCE] = 0.5f
        }

        // Study / Focus
        if (containsAny(normalizedText, "study", "focus", "reading", "work")) {
            embedding[IDX_INSTRUMENTALNESS] = 0.9f
            embedding[IDX_ENERGY] = 0.3f
            embedding[IDX_SPEECHINESS] = 0.05f // Focus usually means no lyrics
        }

        // --- Keyword Bucketing ---
        // Reduced weight (0.1f) so random words don't drown out the semantic vibe
        val reservedIndices = 10
        for ((index, token) in tokens.withIndex()) {
            val bucket = reservedIndices + VectorUtils.positiveModulo(
                token.hashCode() + index * 31,
                EMBEDDING_DIMENSION - reservedIndices
            )
            embedding[bucket] += 0.1f
        }

        return VectorUtils.normalize(embedding)
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
