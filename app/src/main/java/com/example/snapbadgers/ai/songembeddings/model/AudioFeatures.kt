package com.example.snapbadgers.ai.songembeddings.model

import com.google.gson.annotations.SerializedName

data class AudioFeatures(
    val id: String,
    val danceability: Float,
    val energy: Float,
    val speechiness: Float,
    val acousticness: Float,
    val instrumentalness: Float,
    val liveness: Float,
    val valence: Float,
    val tempo: Float,
    val loudness: Float,
    val key: Int,
    val mode: Int,
    @SerializedName("duration_ms") val duration_ms: Float = 240000f
)