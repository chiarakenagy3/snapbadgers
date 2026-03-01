package com.example.snapbadgers

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val embedding: List<Float>
)
