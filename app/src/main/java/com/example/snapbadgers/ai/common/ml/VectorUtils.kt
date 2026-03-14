package com.example.snapbadgers.ai.common.ml

import kotlin.math.sqrt

object VectorUtils {
    fun normalize(vec: FloatArray): FloatArray {
        var sum = 0f
        for (v in vec) sum += v * v
        val norm = sqrt(sum)
        if (norm < 1e-8f) return FloatArray(vec.size) { 0f }
        return FloatArray(vec.size) { i -> vec[i] / norm }
    }
}
