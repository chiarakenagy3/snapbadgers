package com.example.snapbadgers.songembeddings.repository

import com.example.snapbadgers.songembeddings.model.AudioFeatures
import com.example.snapbadgers.songembeddings.network.SpotifyApi
import com.example.snapbadgers.songembeddings.network.TopTracksResponse
import com.example.snapbadgers.songembeddings.network.SearchResponse

class SpotifyRepository(private val api: SpotifyApi) {

    suspend fun fetchFeatures(trackId: String, token: String): AudioFeatures {
        return api.getAudioFeatures("Bearer $token", trackId)
    }

    suspend fun fetchTopTracks(
        token: String, 
        timeRange: String = "long_term", 
        limit: Int = 10
    ): TopTracksResponse {
        return api.getTopTracks("Bearer $token", timeRange, limit)
    }

    suspend fun searchTracks(
        token: String,
        query: String,
        type: String = "track",
        limit: Int = 10
    ): SearchResponse {
        return api.searchTracks("Bearer $token", query, type, limit)
    }
}