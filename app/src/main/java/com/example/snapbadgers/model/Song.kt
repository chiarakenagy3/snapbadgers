package com.example.snapbadgers.model

data class Song(
    val title: String,
    val artist: String,
    val similarity: Float = 0f,
    val inferenceTimeMs: Long = 0L,
    val embedding: FloatArray = floatArrayOf()
)