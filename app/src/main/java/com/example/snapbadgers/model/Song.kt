package com.example.snapbadgers.model

// NOTE: This data class contains a FloatArray field, which means the auto-generated
// equals() and hashCode() use reference equality for `embedding` (not content equality).
// This is intentional for performance — Song identity is determined by title+artist in
// UI contexts, and embedding comparison uses VectorUtils.cosineSimilarity() explicitly.
data class Song(
    val title: String,
    val artist: String,
    val similarity: Float = 0f,
    val inferenceTimeMs: Long = 0L,
    val embedding: FloatArray = floatArrayOf()
)