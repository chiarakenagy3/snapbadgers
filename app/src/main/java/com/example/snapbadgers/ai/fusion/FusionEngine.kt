package com.example.snapbadgers.ai.fusion

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils

class FusionEngine {

    fun fuse(
        textEmbedding: FloatArray,
        visionEmbedding: FloatArray? = null,
        sensorEmbedding: FloatArray? = null
    ): FloatArray {
        val fused = FloatArray(EMBEDDING_DIMENSION)
        var totalWeight = 0f

        totalWeight += accumulate(fused, textEmbedding, weight = 0.75f, salt = 11)
        visionEmbedding?.let {
            totalWeight += accumulate(fused, it, weight = 0.15f, salt = 19)
        }
        sensorEmbedding?.let {
            totalWeight += accumulate(fused, it, weight = 0.10f, salt = 23)
        }

        if (totalWeight <= 0f) return fused

        for (index in fused.indices) {
            fused[index] /= totalWeight
        }

        return VectorUtils.normalize(fused)
    }

    private fun accumulate(target: FloatArray, source: FloatArray, weight: Float, salt: Int): Float {
        val aligned = VectorUtils.alignToEmbeddingDimension(source, salt = salt)
        for (index in target.indices) {
            target[index] += aligned[index] * weight
        }
        return weight
    }
}
