package com.example.snapbadgers.ai.common.ml

interface EmbeddingAligner {
    fun align(input: FloatArray): FloatArray
}