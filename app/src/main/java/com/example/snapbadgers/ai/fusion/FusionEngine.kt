package com.example.snapbadgers.ai.fusion

class FusionEngine {

    fun fuse(
        textEmbedding: FloatArray,
        visionEmbedding: FloatArray? = null,
        sensorEmbedding: FloatArray? = null
    ): FloatArray {
        val fusedList = mutableListOf<Float>()

        fusedList.addAll(textEmbedding.toList())
        visionEmbedding?.let { fusedList.addAll(it.toList()) }
        sensorEmbedding?.let { fusedList.addAll(it.toList()) }

        return fusedList.toFloatArray()
    }
}