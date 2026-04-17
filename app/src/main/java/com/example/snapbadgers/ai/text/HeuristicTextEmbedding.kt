package com.example.snapbadgers.ai.text

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils

object HeuristicTextEmbedding {

    fun encode(text: String): FloatArray {
        val embedding = FloatArray(EMBEDDING_DIMENSION)
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) {
            return embedding
        }

        val length = normalizedText.length.toFloat()
        val tokens = normalizedText.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        val wordCount = tokens.size.toFloat()

        embedding[0] = (length / 160f).coerceIn(0f, 1f)
        embedding[1] = (wordCount / 24f).coerceIn(0f, 1f)
        embedding[2] = if (normalizedText.contains("calm", ignoreCase = true)) 1f else 0f
        embedding[3] = if (normalizedText.contains("study", ignoreCase = true)) 1f else 0f
        embedding[4] = if (normalizedText.contains("happy", ignoreCase = true)) 1f else 0f
        embedding[5] = if (normalizedText.contains("sad", ignoreCase = true)) 1f else 0f
        embedding[6] = if (normalizedText.contains("workout", ignoreCase = true)) 1f else 0f
        embedding[7] = if (normalizedText.contains("night", ignoreCase = true)) 1f else 0f

        for ((index, token) in tokens.withIndex()) {
            val bucket = 8 + VectorUtils.positiveModulo(
                token.hashCode() + index * 31,
                EMBEDDING_DIMENSION - 8
            )
            embedding[bucket] += 1f
        }

        return VectorUtils.normalize(embedding)
    }
}
