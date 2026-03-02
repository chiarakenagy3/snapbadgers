package com.example.snapbadgers.model

// Data class for Spotify audio features
data class AudioFeatures(
    val danceability: Float,
    val energy: Float,
    val speechiness: Float,
    val acousticness: Float,
    val instrumentalness: Float,
    val liveness: Float,
    val valence: Float,
    val tempo: Float,
    val loudness: Float,
    val duration_ms: Float
)