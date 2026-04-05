package com.example.snapbadgers.ai.fusion

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils

/**
 * FusionEngine
 *
 * Merges scene (vision) + text + sensor embeddings into a single
 * 128-d fused context embedding using weighted averaging + L2 normalization.
 *
 * Modality weights rationale for VibeCheck:
 *   vision = 0.60 — scene is the primary signal (what you see drives the vibe)
 *   text   = 0.25 — user query refines the scene context
 *   sensor = 0.15 — motion/light/time adds supplementary context
 *
 * All three encoders now output proper 128-d vectors so alignToEmbeddingDimension
 * is effectively a no-op (passes through with normalization only).
 */
class FusionEngine(
    private val visionWeight: Float = 0.60f,
    private val textWeight:   Float = 0.25f,
    private val sensorWeight: Float = 0.15f
) {

    fun fuse(
        textEmbedding:   FloatArray,
        visionEmbedding: FloatArray? = null,
        sensorEmbedding: FloatArray? = null
    ): FloatArray {
        val fused = FloatArray(EMBEDDING_DIMENSION)
        var totalWeight = 0f

        totalWeight += accumulate(fused, textEmbedding, weight = textWeight, salt = 11)
        visionEmbedding?.let {
            totalWeight += accumulate(fused, it, weight = visionWeight, salt = 19)
        }
        sensorEmbedding?.let {
            totalWeight += accumulate(fused, it, weight = sensorWeight, salt = 23)
        }

        if (totalWeight <= 0f) return fused

        for (index in fused.indices) {
            fused[index] /= totalWeight
        }

        return VectorUtils.normalize(fused)
    }

    private fun accumulate(
        target: FloatArray,
        source: FloatArray,
        weight: Float,
        salt: Int
    ): Float {
        val aligned = VectorUtils.alignToEmbeddingDimension(source, salt = salt)
        for (index in target.indices) {
            target[index] += aligned[index] * weight
        }
        return weight
    }
}