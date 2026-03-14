package com.example.snapbadgers.ai.common.ml

import kotlin.math.sqrt

object VectorUtils {

    fun normalize(vec: FloatArray): FloatArray {
        var sum = 0f
        for (v in vec) {
            sum += v * v
        }

        val norm = sqrt(sum)
        if (norm < 1e-8f) {
            return FloatArray(vec.size) { 0f }
        }

        return FloatArray(vec.size) { i -> vec[i] / norm }
    }

    fun dot(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var sum = 0f
        for (i in a.indices) {
            sum += a[i] * b[i]
        }
        return sum
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denom = sqrt(normA) * sqrt(normB)
        return if (denom < 1e-8f) 0f else dot / denom
    }

    fun weightedSum(
        vectors: List<Pair<FloatArray, Float>>
    ): FloatArray {
        if (vectors.isEmpty()) return FloatArray(0)

        val dim = vectors.first().first.size
        val out = FloatArray(dim)
        var totalWeight = 0f

        for ((vec, weight) in vectors) {
            if (vec.size != dim || weight <= 0f) continue
            totalWeight += weight
            for (i in 0 until dim) {
                out[i] += vec[i] * weight
            }
        }

        if (totalWeight < 1e-8f) return FloatArray(dim) { 0f }

        for (i in out.indices) {
            out[i] /= totalWeight
        }

        return out
    }
}