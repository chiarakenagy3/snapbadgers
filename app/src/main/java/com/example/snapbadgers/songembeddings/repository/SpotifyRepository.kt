package com.example.snapbadgers.songembeddings.repository

import com.example.snapbadgers.songembeddings.model.AudioFeatures
import com.example.snapbadgers.songembeddings.network.SpotifyApi
import com.example.snapbadgers.songembeddings.network.TopTracksResponse
import com.example.snapbadgers.songembeddings.network.SearchResponse

class SpotifyRepository(private val api: SpotifyApi) {

    // Fetch audio features from Spotify API
    suspend fun fetchFeatures(trackId: String, token: String): AudioFeatures {
        return api.getAudioFeatures("Bearer $token", trackId)
    }

    // Fetch top tracks from Spotify API with explicit parameters
    suspend fun fetchTopTracks(
        token: String, 
        timeRange: String = "long_term", 
        limit: Int = 10
    ): TopTracksResponse {
        return api.getTopTracks("Bearer $token", timeRange, limit)
    }

    // Search tracks from Spotify API
    suspend fun searchTracks(
        token: String,
        query: String,
        type: String = "track",
        limit: Int = 10
    ): SearchResponse {
        return api.searchTracks("Bearer $token", query, type, limit)
    }
}