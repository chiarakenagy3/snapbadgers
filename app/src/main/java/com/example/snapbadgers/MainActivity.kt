package com.example.snapbadgers

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.snapbadgers.embedding.YamnetFeatureExtractor
import com.example.snapbadgers.repository.SpotifyRepository
import com.example.snapbadgers.network.SpotifyApi
import com.example.snapbadgers.network.AuthApi
import com.example.snapbadgers.network.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private val tag = "SnapBadger"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var statusText by mutableStateOf("Initializing...")
        var topTracksList by mutableStateOf("")
        var embeddingResult by mutableStateOf("")

        setContent {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Status: $statusText")
                Spacer(modifier = Modifier.height(16.dp))
                if (topTracksList.isNotEmpty()) {
                    Text(text = "Tracks for Analysis:")
                    Text(text = topTracksList)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (embeddingResult.isNotEmpty()) {
                    Text(text = "128-d Song Embedding:")
                    Text(text = embeddingResult)
                }
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val authRetrofit = Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SpotifyApi::class.java)
        val authApi = authRetrofit.create(AuthApi::class.java)
        val repo = SpotifyRepository(api)
        val yamnetExtractor = YamnetFeatureExtractor(this)

        lifecycleScope.launch {
            try {
                // 1. Refresh Token
                statusText = "Refreshing token..."
                val clientId = BuildConfig.SPOTIFY_CLIENT_ID.trim()
                val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET.trim()
                val refreshToken = BuildConfig.SPOTIFY_REFRESH_TOKEN.trim()

                if (clientId.isEmpty() || clientSecret.isEmpty() || refreshToken.isEmpty()) {
                    statusText = "Error: Missing ID/Secret/Refresh Token in local.properties"
                    return@launch
                }

                val authHeader = "Basic " + Base64.encodeToString(
                    "$clientId:$clientSecret".toByteArray(),
                    Base64.NO_WRAP
                )

                val tokenResponse = authApi.refreshToken(
                    authHeader = authHeader,
                    grantType = "refresh_token",
                    refreshToken = refreshToken
                )
                val accessToken = tokenResponse.access_token

                // 2. Fetch Top Tracks - Using a conservative limit of 10
                statusText = "Searching for tracks with audio previews..."

                val topTracksResponse = try {
                    api.getTopTracks(
                        token = "Bearer $accessToken",
                        timeRange = "long_term",
                        limit = 10
                    )
                } catch (e: Exception) {
                    null
                }
                
                var tracksWithAudio = topTracksResponse?.items?.filter { it.preview_url != null } ?: emptyList()

                // 3. Fallback: Search with conservative limit of 10
                if (tracksWithAudio.isEmpty()) {
                    statusText = "No top track previews found. Searching global hits..."
                    val searchResponse = api.searchTracks(
                        token = "Bearer $accessToken",
                        query = "genre:pop",
                        type = "track",
                        limit = 10
                    )
                    tracksWithAudio = searchResponse.tracks.items.filter { it.preview_url != null }
                }

                // 4. Final Fallback Strategy
                val finalAudioUrl: String?
                val targetTrackName: String
                
                if (tracksWithAudio.isNotEmpty()) {
                    val targetTrack = tracksWithAudio.first()
                    finalAudioUrl = targetTrack.preview_url
                    targetTrackName = targetTrack.name
                    topTracksList = tracksWithAudio.take(5).joinToString("\n") { "${it.name} [✓ Audio]" }
                } else {
                    // Ultimate fallback to ensure the demo always works
                    statusText = "Spotify provided no previews. Using Demo Audio for YAMNet..."
                    finalAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                    targetTrackName = "Demo Track (Fallback)"
                    topTracksList = "No Spotify previews found.\nRunning YAMNet on Sample MP3..."
                }

                if (finalAudioUrl != null) {
                    statusText = "Downloading audio for $targetTrackName..."

                    val audioData = withContext(Dispatchers.IO) {
                        yamnetExtractor.downloadAudio(finalAudioUrl)
                    }

                    if (audioData != null) {
                        statusText = "Extracting features using YAMNet..."
                        val vector = withContext(Dispatchers.Default) {
                            yamnetExtractor.extractFeatures(audioData)
                        }
                        embeddingResult = "Vector: " + vector.take(8).joinToString(", ") { "%.4f".format(it) }
                        statusText = "128-d Embedding generated for $targetTrackName"
                    } else {
                        statusText = "Failed to download audio data."
                    }
                }

            } catch (e: Exception) {
                val errorMsg = if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    "HTTP ${e.code()}: $errorBody"
                } else {
                    e.message ?: "Unknown error"
                }
                statusText = "Error: $errorMsg"
            }
        }
    }
}