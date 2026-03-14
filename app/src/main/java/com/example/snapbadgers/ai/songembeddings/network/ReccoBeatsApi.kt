package com.example.snapbadgers.ai.songembeddings.network

import com.example.snapbadgers.ai.songembeddings.model.AudioFeatures
import com.google.gson.annotations.SerializedName // 必须导入这个才能用注解
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// 基础的分页返回包装类
data class ReccoResponse<T>(val content: List<T>)

data class ReccoArtist(val id: String, val name: String)

data class ReccoAlbum(
    val id: String,
    val name: String,
    val artists: List<ReccoArtist>? // 补回：主界面 Fallback 会用到
)

data class ReccoTrack(
    val id: String,
    // 关键修改：脚本返回的是 trackTitle 而不是 name
    @SerializedName("trackTitle") val trackTitle: String,
    val isrc: String?,
    val artists: List<ReccoArtist>? // 补回：主界面需要用它来拼接歌手名
)

interface ReccoBeatsApi {

    // Artist
    @GET("v1/artist/search")
    suspend fun searchArtist(
        @Query("searchText") searchText: String
    ): ReccoResponse<ReccoArtist>

    // Album
    @GET("v1/artist/{artistId}/album")
    suspend fun getArtistAlbums(
        @Path("artistId") artistId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): ReccoResponse<ReccoAlbum>

    // Track
    @GET("v1/album/{albumId}/track")
    suspend fun getAlbumTracks(
        @Path("albumId") albumId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): ReccoResponse<ReccoTrack>

    // Feature
    @GET("v1/track/{trackId}/audio-features")
    suspend fun getTrackFeatures(
        @Path("trackId") trackId: String
    ): AudioFeatures

    // Search Track
    @GET("v1/track/search")
    suspend fun searchTracks(
        @Query("searchText") searchText: String
    ): ReccoResponse<ReccoTrack>

    // Search Album
    @GET("v1/album/search")
    suspend fun searchAlbums(
        @Query("searchText") searchText: String
    ): ReccoResponse<ReccoAlbum>
}