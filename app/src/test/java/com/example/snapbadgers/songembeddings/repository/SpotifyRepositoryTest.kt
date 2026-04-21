package com.example.snapbadgers.songembeddings.repository

import com.example.snapbadgers.songembeddings.model.AudioFeatures
import com.example.snapbadgers.songembeddings.network.Artist
import com.example.snapbadgers.songembeddings.network.SearchResponse
import com.example.snapbadgers.songembeddings.network.SearchTracksContainer
import com.example.snapbadgers.songembeddings.network.SpotifyApi
import com.example.snapbadgers.songembeddings.network.TopTracksResponse
import com.example.snapbadgers.songembeddings.network.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * JVM tests for SpotifyRepository — verifies request shape (Bearer token, defaults, query args)
 * and surfaces transport errors cleanly to callers.
 */
class SpotifyRepositoryTest {

    private val features = AudioFeatures(
        id = "abc",
        danceability = 0.5f,
        energy = 0.6f,
        speechiness = 0.05f,
        acousticness = 0.3f,
        instrumentalness = 0f,
        liveness = 0.1f,
        valence = 0.7f,
        tempo = 120f,
        loudness = -5f,
        key = 0,
        mode = 1
    )

    @Test
    fun `fetchFeatures prefixes bearer and forwards track id`() {
        runBlocking {
            val api = mock<SpotifyApi> {
                onBlocking { getAudioFeatures(any(), any()) } doReturn features
            }
            val repo = SpotifyRepository(api)

            val result = repo.fetchFeatures("abc", "RAWTOKEN")

            assertSame(features, result)
            verify(api).getAudioFeatures("Bearer RAWTOKEN", "abc")
        }
    }

    @Test
    fun `fetchTopTracks uses defaults and bearer token`() {
        runBlocking {
            val top = TopTracksResponse(items = listOf(Track("1", "T", listOf(Artist("A")), null)))
            val api = mock<SpotifyApi> {
                onBlocking { getTopTracks(any(), any(), any()) } doReturn top
            }
            val repo = SpotifyRepository(api)

            val result = repo.fetchTopTracks("TOKEN")

            assertEquals(1, result.items.size)
            verify(api).getTopTracks("Bearer TOKEN", "long_term", 10)
        }
    }

    @Test
    fun `fetchTopTracks forwards explicit args`() {
        runBlocking {
            val api = mock<SpotifyApi> {
                onBlocking { getTopTracks(any(), any(), any()) } doReturn TopTracksResponse(emptyList())
            }
            val repo = SpotifyRepository(api)

            repo.fetchTopTracks("TOK", timeRange = "short_term", limit = 50)

            verify(api).getTopTracks("Bearer TOK", "short_term", 50)
        }
    }

    @Test
    fun `searchTracks uses defaults and bearer token`() {
        runBlocking {
            val searchResponse = SearchResponse(SearchTracksContainer(items = emptyList()))
            val api = mock<SpotifyApi> {
                onBlocking { searchTracks(any(), any(), any(), any()) } doReturn searchResponse
            }
            val repo = SpotifyRepository(api)

            val result = repo.searchTracks("TOK", "calm piano")

            assertSame(searchResponse, result)
            verify(api).searchTracks("Bearer TOK", "calm piano", "track", 10)
        }
    }

    @Test
    fun `401 responses surface as SpotifyAuthException`() {
        val api = mock<SpotifyApi> {
            onBlocking { getAudioFeatures(any(), any()) } doThrow HttpException(
                Response.error<Any>(
                    401,
                    "unauthorized".toResponseBody("text/plain".toMediaTypeOrNull())
                )
            )
        }
        val repo = SpotifyRepository(api)

        assertThrows(SpotifyAuthException::class.java) {
            runBlocking { repo.fetchFeatures("abc", "EXPIRED") }
        }
    }

    @Test
    fun `non-auth HTTP failures still surface as HttpException`() {
        val api = mock<SpotifyApi> {
            onBlocking { getAudioFeatures(any(), any()) } doThrow HttpException(
                Response.error<Any>(
                    500,
                    "boom".toResponseBody("text/plain".toMediaTypeOrNull())
                )
            )
        }
        val repo = SpotifyRepository(api)

        assertThrows(HttpException::class.java) {
            runBlocking { repo.fetchFeatures("abc", "TOK") }
        }
    }
}
