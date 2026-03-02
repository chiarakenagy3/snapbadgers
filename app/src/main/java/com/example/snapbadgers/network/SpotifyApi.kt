package com.example.snapbadgers.network

import com.example.snapbadgers.model.AudioFeatures
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

data class Artist(val name: String)
data class Track(
    val id: String, 
    val name: String, 
    val artists: List<Artist>,
    val preview_url: String?
)
data class TopTracksResponse(val items: List<Track>)
data class SearchResponse(val tracks: SearchTracksContainer)
data class SearchTracksContainer(val items: List<Track>)

interface SpotifyApi {

    @GET("v1/audio-features/{id}")
    suspend fun getAudioFeatures(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): AudioFeatures

    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Header("Authorization") token: String,
        @Query("time_range") timeRange: String,
        @Query("limit") limit: Int
    ): TopTracksResponse

    @GET("v1/search")
    suspend fun searchTracks(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("limit") limit: Int
    ): SearchResponse
}