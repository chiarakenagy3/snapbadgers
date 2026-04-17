package com.example.snapbadgers.songembeddings.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Header("Authorization") authHeader: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String
    ): TokenResponse
}

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("token_type") val tokenType: String
)