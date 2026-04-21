package com.example.snapbadgers.songembeddings.repository

import com.example.snapbadgers.songembeddings.model.AudioFeatures
import com.example.snapbadgers.songembeddings.network.SearchResponse
import com.example.snapbadgers.songembeddings.network.SpotifyApi
import com.example.snapbadgers.songembeddings.network.TopTracksResponse
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

class SpotifyAuthException(message: String) : Exception(message)

class SpotifyRepository(
    private val api: SpotifyApi,
    private val maxAttempts: Int = 3,
    private val initialBackoffMillis: Long = 1_000L,
) {

    suspend fun fetchFeatures(trackId: String, token: String): AudioFeatures =
        withRetry { api.getAudioFeatures("Bearer $token", trackId) }

    suspend fun fetchTopTracks(
        token: String,
        timeRange: String = "long_term",
        limit: Int = 10,
    ): TopTracksResponse =
        withRetry { api.getTopTracks("Bearer $token", timeRange, limit) }

    suspend fun searchTracks(
        token: String,
        query: String,
        type: String = "track",
        limit: Int = 10,
    ): SearchResponse =
        withRetry { api.searchTracks("Bearer $token", query, type, limit) }

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var backoff = initialBackoffMillis
        repeat(maxAttempts - 1) {
            try {
                return block()
            } catch (e: HttpException) {
                when (e.code()) {
                    401 -> throw SpotifyAuthException("Spotify auth failed (401)")
                    429 -> {
                        delay(backoff)
                        backoff *= 2
                    }
                    else -> throw e
                }
            } catch (e: IOException) {
                delay(backoff)
                backoff *= 2
            }
        }
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) throw SpotifyAuthException("Spotify auth failed (401)") else throw e
        }
    }
}
