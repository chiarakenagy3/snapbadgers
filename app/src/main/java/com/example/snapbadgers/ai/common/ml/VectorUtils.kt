package com.example.snapbadgers.ai.common.ml

import kotlin.math.sqrt

const val EMBEDDING_DIMENSION = 128

/**
 * Standard Semantic Indices for the VibeCheck project.
 * These are aligned with the Spotify/ReccoBeats Audio Features 
 * used to generate the song catalog embeddings.
 */
const val IDX_DANCEABILITY = 0
const val IDX_ENERGY = 1
const val IDX_SPEECHINESS = 2
const val IDX_ACOUSTICNESS = 3
const val IDX_INSTRUMENTALNESS = 4
// const val IDX_LIVENESS = 5 // Not currently used in heuristic mapping
const val IDX_VALENCE = 6 // Higher = Happier
const val IDX_TEMPO = 7
// const val IDX_LOUDNESS = 8 // Not currently used in heuristic mapping
// const val IDX_DURATION = 9 // Not currently used in heuristic mapping

object VectorUtils {

    fun normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (value in vector) {
            sumSquares += value * value
        }

        val norm = sqrt(sumSquares)
        if (norm < 1e-8f) {
            return FloatArray(vector.size) { 0f }
        }

        return FloatArray(vector.size) { index -> vector[index] / norm }
    }

    /**
     * Aligns a feature vector to the target embedding dimension.
     * 
     * IMPORTANT: To prevent "shuffling" that hurts similarity, this now uses 
     * a deterministic mapping. The 'salt' is kept for backward compatibility 
     * but its impact is minimized for already-aligned vectors.
     */
    fun alignToEmbeddingDimension(
        vector: FloatArray,
        salt: Int = 0,
        dimension: Int = EMBEDDING_DIMENSION
    ): FloatArray {
        if (dimension <= 0) return FloatArray(0)
        if (vector.isEmpty()) return FloatArray(dimension) { 0f }
        
        // If the vector is already the target dimension, just normalize it.
        // This ensures that model-generated embeddings (128-d) are not 
        // randomly re-shuffled by different salts in the pipeline.
        if (vector.size == dimension) return normalize(vector.copyOf())

        val projected = FloatArray(dimension)
        for (index in vector.indices) {
            val value = vector[index]
            
            // deterministic projection: feature X always lands in bucket Y
            // We use a fixed base to ensure text, vision, and sensors 
            // share the same coordinate space.
            val primary = positiveModulo(index * 31 + salt, dimension)
            val secondary = positiveModulo(index * 17 + salt + 7, dimension)

            projected[primary] += value
            projected[secondary] += value * 0.5f
        }

        return normalize(projected)
    }

    fun cosineSimilarity(left: FloatArray, right: FloatArray): Float {
        if (left.isEmpty() || right.isEmpty() || left.size != right.size) return 0f

        var dotProduct = 0f
        var leftNorm = 0f
        var rightNorm = 0f
        for (index in left.indices) {
            dotProduct += left[index] * right[index]
            leftNorm += left[index] * left[index]
            rightNorm += right[index] * right[index]
        }

        val denominator = sqrt(leftNorm) * sqrt(rightNorm)
        return if (denominator < 1e-8f) 0f else dotProduct / denominator
    }

    fun positiveModulo(value: Int, modulus: Int): Int {
        if (modulus == 0) return 0
        val result = value % modulus
        return if (result >= 0) result else result + modulus
    }
}
