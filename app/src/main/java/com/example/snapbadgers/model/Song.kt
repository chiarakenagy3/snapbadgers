package com.example.snapbadgers.model

/**
 * A song with an optional embedding vector for similarity search.
 *
 * [embedding] uses reference equality in auto-generated [equals]/[hashCode] for performance.
 * Song identity is determined by [title]+[artist] in UI contexts; embedding comparison
 * uses [com.example.snapbadgers.ai.common.ml.VectorUtils.cosineSimilarity] explicitly.
 */
data class Song(
    val title: String,
    val artist: String,
    val similarity: Float = 0f,
    val inferenceTimeMs: Long = 0L,
    val embedding: FloatArray = floatArrayOf()
)