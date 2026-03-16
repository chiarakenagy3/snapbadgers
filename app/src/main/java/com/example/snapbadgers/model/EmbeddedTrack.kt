package com.example.snapbadgers.model

data class EmbeddedTrack(
    val trackId: String,
    val name: String,
    val artists: String,
    val source: String = "",
    val embedding: List<Float>
)
