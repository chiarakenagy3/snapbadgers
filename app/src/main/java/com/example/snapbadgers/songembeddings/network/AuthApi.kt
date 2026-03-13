package com.example.snapbadgers.songembeddings.network

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
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)