package com.example.snapbadgers.ai.common.ml

import kotlin.math.sqrt

const val EMBEDDING_DIMENSION = 128

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

    fun alignToEmbeddingDimension(
        vector: FloatArray,
        salt: Int = 0,
        dimension: Int = EMBEDDING_DIMENSION
    ): FloatArray {
        if (dimension <= 0) return FloatArray(0)
        if (vector.isEmpty()) return FloatArray(dimension) { 0f }
        if (vector.size == dimension) return normalize(vector.copyOf())

        val projected = FloatArray(dimension)
        for (index in vector.indices) {
            val value = vector[index]
            val primary = positiveModulo(index * 31 + salt * 17, dimension)
            val secondary = positiveModulo(index * 17 + salt * 31 + 7, dimension)
            val tertiary = positiveModulo(primary + secondary + salt, dimension)

            projected[primary] += value
            projected[secondary] += value * 0.5f
            projected[tertiary] += value * 0.25f
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
