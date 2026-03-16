package com.example.snapbadgers.ai.fusion

import kotlin.math.min

class FusionEngine {

    fun fuse(
        textEmbedding: FloatArray,
        visionEmbedding: FloatArray? = null,
        sensorEmbedding: FloatArray? = null
    ): FloatArray {
        val result = FloatArray(128)
        
        // Count active modalities to avoid dampening the signal
        var activeCount = 1.0f
        if (visionEmbedding != null) activeCount += 1.0f
        if (sensorEmbedding != null) activeCount += 1.0f
        
        for (i in 0 until 128) {
            val t = textEmbedding.getOrElse(i) { 0f }
            val v = visionEmbedding?.getOrElse(i) { 0f } ?: 0f
            val s = sensorEmbedding?.getOrElse(i) { 0f } ?: 0f
            
            // Average only across available inputs
            result[i] = (t + v + s) / activeCount
        }

        return result
    }
}