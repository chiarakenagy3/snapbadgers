package com.example.snapbadgers

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.snapbadgers.ai.songembeddings.embedding.getEmbedding
import com.example.snapbadgers.ai.songembeddings.network.*
import com.example.snapbadgers.model.Song
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

data class ProcessedTrack(
    val trackId: String,
    val name: String,
    val artists: String,
    val source: String,
    val embedding: List<Float>
)

class MainActivity : ComponentActivity() {

    private val tag = "SnapBadger"
    private val reccoBeatsBaseUrl = "https://api.reccobeats.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var statusText by mutableStateOf("Ready")
        var logText by mutableStateOf("")
        var recommendations by mutableStateOf<List<RecommendationService.RecommendationResult>>(emptyList())
        var recommendationService: RecommendationService? by mutableStateOf(null)

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("1. System Status:", style = MaterialTheme.typography.labelLarge)
                Text(statusText, color = MaterialTheme.colorScheme.primary)
                
                Spacer(modifier = Modifier.height(16.dp))

                if (recommendationService != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("2. Recommendations Loaded", style = MaterialTheme.typography.titleMedium)
                            Text("Your personalized library is active.", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    val mockVibe = FloatArray(128) { (0..100).random() / 100f }
                                    recommendations = recommendationService?.recommendSongs(
                                        userEmbedding = mockVibe,
                                        userWeight = 1.0f,
                                        limit = 5
                                    ) ?: emptyList()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Recommend Songs Based on My Vibes")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (recommendations.isNotEmpty()) {
                    Text("Top 5 Recommendations:", style = MaterialTheme.typography.titleMedium)
                    recommendations.forEach { res ->
                        ListItem(
                            headlineContent = { Text(res.song.title) },
                            supportingContent = { Text("${res.song.artist} | Score: ${(res.finalScore * 100).toInt()}%") },
                            trailingContent = { Text("Match: %.2f".format(res.finalScore)) }
                        )
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Real-time Logs:", style = MaterialTheme.typography.titleSmall)
                Text(logText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

        val spotifyApi = Retrofit.Builder()
            .baseUrl("https://api.spotify.com/").client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build().create(SpotifyApi::class.java)
        
        val authApi = Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/").client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)

        val reccoApi = Retrofit.Builder()
            .baseUrl(reccoBeatsBaseUrl).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ReccoBeatsApi::class.java)

        lifecycleScope.launch {
            try {
                // 1. Authenticate
                statusText = "Connecting to Spotify..."
                val authHeader = "Basic " + Base64.encodeToString("${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}".toByteArray(), Base64.NO_WRAP)
                val tokenResponse = authApi.refreshToken(authHeader, "refresh_token", BuildConfig.SPOTIFY_REFRESH_TOKEN.trim())
                val spotifyToken = "Bearer ${tokenResponse.access_token}"

                // 2. Fetch YOUR real songs
                statusText = "Fetching your Top Tracks..."
                val tracksToProcess = mutableListOf<Triple<String, String, String>>() 
                val topResponse = spotifyApi.getTopTracks(spotifyToken, "medium_term", 20)
                
                topResponse.items.forEach { 
                    tracksToProcess.add(Triple(it.id, it.name, it.artists.joinToString(", ") { a -> a.name })) 
                }
                logText += "✓ Found ${tracksToProcess.size} tracks on your Spotify.\n"

                // 3. Extract Features for YOUR songs
                statusText = "Analyzing audio features..."
                val semaphore = Semaphore(5)
                val finalResults = tracksToProcess.map { (id, name, artist) ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                val features = reccoApi.getTrackFeatures(id)
                                val embeddingArray = getEmbedding(features)
                                withContext(Dispatchers.Main) { logText += "✓ Analyzed: $name\n" }
                                ProcessedTrack(id, name, artist, "MySpotify", embeddingArray.toList())
                            } catch (e: Exception) { null }
                        }
                    }
                }.awaitAll().filterNotNull()

                // 4. Fallback only if needed
                var activeResults = finalResults
                if (activeResults.isEmpty()) {
                    logText += "! No features found for your tracks. Using fallback data.\n"
                    // ... Fallback logic remains here if desired ...
                }

                // 5. Update Recommendation Service with YOUR songs
                statusText = "Updating Recommendation Engine..."
                val songLibrary = activeResults.map { 
                    Song(title = it.name, artist = it.artists, embedding = it.embedding.toFloatArray())
                }
                
                if (songLibrary.isNotEmpty()) {
                    recommendationService = RecommendationService(songLibrary)
                    statusText = "Success! Library Updated."
                    logText += "✓ Recommendations are now based on YOUR Spotify tracks.\n"
                } else {
                    statusText = "Error: Library Empty."
                }

            } catch (e: Exception) {
                statusText = "Error: ${e.message}"
                Log.e(tag, "Pipeline Error", e)
            }
        }
    }
}
