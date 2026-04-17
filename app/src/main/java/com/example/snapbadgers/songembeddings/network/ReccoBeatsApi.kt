package com.example.snapbadgers.songembeddings.network

import com.example.snapbadgers.songembeddings.model.AudioFeatures
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ReccoResponse<T>(val content: List<T>)

data class ReccoArtist(val id: String, val name: String)

data class ReccoAlbum(
    val id: String,
    val name: String,
    val artists: List<ReccoArtist>?
)

data class ReccoTrack(
    val id: String,
    @SerializedName("trackTitle") val trackTitle: String,
    val isrc: String?,
    val artists: List<ReccoArtist>?
)

interface ReccoBeatsApi {

    @GET("v1/artist/search")
    suspend fun searchArtist(
        @Query("searchText") searchText: String
    ): ReccoResponse<ReccoArtist>

    @GET("v1/artist/{artistId}/album")
    suspend fun getArtistAlbums(
        @Path("artistId") artistId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): ReccoResponse<ReccoAlbum>

    @GET("v1/album/{albumId}/track")
    suspend fun getAlbumTracks(
        @Path("albumId") albumId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): ReccoResponse<ReccoTrack>

    @GET("v1/track/{trackId}/audio-features")
    suspend fun getTrackFeatures(
        @Path("trackId") trackId: String
    ): AudioFeatures

    @GET("v1/track/search")
    suspend fun searchTracks(
        @Query("searchText") searchText: String
    ): ReccoResponse<ReccoTrack>

    @GET("v1/album/search")
    suspend fun searchAlbums(
        @Query("searchText") searchText: String
    ): ReccoResponse<ReccoAlbum>
}
