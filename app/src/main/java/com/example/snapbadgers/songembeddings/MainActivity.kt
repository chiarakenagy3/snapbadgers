package com.example.snapbadgers.songembeddings

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
import com.example.snapbadgers.BuildConfig
import com.example.snapbadgers.songembeddings.embedding.MLPProjector
import com.example.snapbadgers.songembeddings.embedding.getEmbedding
import com.example.snapbadgers.songembeddings.network.AuthApi
import com.example.snapbadgers.songembeddings.network.ReccoAlbum
import com.example.snapbadgers.songembeddings.network.ReccoBeatsApi
import com.example.snapbadgers.songembeddings.network.ReccoTrack
import com.example.snapbadgers.songembeddings.network.SpotifyApi
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ProcessedTrack(
    val trackId: String,
    val name: String,
    val artists: String,
    val source: String,
    val embedding: List<Float>
)

class MainActivity : ComponentActivity() {

    private val TAG = "SnapBadger"
    private val RECCOBEATS_BASE_URL = "https://api.reccobeats.com/"

    // UI State
    private var statusText by mutableStateOf("Ready")
    private var logText by mutableStateOf("")
    private val logBuilder = StringBuilder()

    // Caches
    private val artistAlbumCache = ConcurrentHashMap<String, List<ReccoAlbum>>()
    private val albumTrackCache = ConcurrentHashMap<String, List<ReccoTrack>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Essential: Initialize the MLP Projector
        MLPProjector.init(this)

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("System Status:", style = MaterialTheme.typography.labelLarge)
                    Text(statusText, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Process Log:", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(logText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val spotifyApi = Retrofit.Builder().baseUrl("https://api.spotify.com/").client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build().create(
            SpotifyApi::class.java)
        val authApi = Retrofit.Builder().baseUrl("https://accounts.spotify.com/").client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build().create(
            AuthApi::class.java)
        val reccoApi = Retrofit.Builder().baseUrl(RECCOBEATS_BASE_URL).client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build().create(
            ReccoBeatsApi::class.java)

        lifecycleScope.launch {
            try {
                // 1. Authentication
                statusText = "Authenticating..."
                val authHeader = "Basic " + Base64.encodeToString("${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}".toByteArray(), Base64.NO_WRAP)
                val tokenResponse = try {
                    authApi.refreshToken(authHeader, "refresh_token", BuildConfig.SPOTIFY_REFRESH_TOKEN.trim())
                } catch (e: Exception) {
                    updateLogMain("Auth Error: ${e.message}")
                    throw e
                }
                val spotifyToken = "Bearer ${tokenResponse.access_token}"

                // 2. Fetch Top Tracks
                statusText = "Fetching Top Tracks..."
                val topTracks = try {
                    spotifyApi.getTopTracks(spotifyToken, "medium_term", 10).items
                } catch (e: Exception) {
                    updateLogMain("Spotify API Error: ${e.message}")
                    throw e
                }
                updateLogMain("✓ Found ${topTracks.size} tracks on Spotify")

                // 3. Process Tracks using supervisorScope to prevent one failure from cancelling others
                val semaphore = Semaphore(2)
                val results = supervisorScope {
                    topTracks.map { spotifyTrack ->
                        async(Dispatchers.IO) {
                            withContext(Dispatchers.Main) { statusText = "Processing: ${spotifyTrack.name}" }
                            
                            semaphore.withPermit {
                                // Search with robust error handling
                                var foundTrack: ReccoTrack? = try {
                                    withTimeoutOrNull(25000) {
                                        searchWithStrategy(reccoApi, spotifyTrack.name, spotifyTrack.artists.firstOrNull()?.name)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Search error for ${spotifyTrack.name}: ${e.message}")
                                    null
                                }

                                var label = "Matched"

                                // Fallback if not found
                                if (foundTrack == null) {
                                    updateLogMain("⚠ Not found: ${spotifyTrack.name}. Trying fallback.")
                                    foundTrack = try {
                                        searchWithStrategy(reccoApi, "Blank Space", "Taylor Swift")
                                    } catch (e: Exception) { null }
                                    label = "Fallback"
                                }

                                if (foundTrack != null) {
                                    try {
                                        val features = reccoApi.getTrackFeatures(foundTrack.id)
                                        val embedding = getEmbedding(features).toList()
                                        updateLogMain("✓ [$label] ${foundTrack.trackTitle}")
                                        
                                        val artistName = foundTrack.artists?.firstOrNull()?.name ?: "Unknown"
                                        ProcessedTrack(foundTrack.id, foundTrack.trackTitle, artistName, label, embedding)
                                    } catch (e: Exception) { 
                                        Log.e(TAG, "Feature extraction error for ${foundTrack.trackTitle}: ${e.message}")
                                        null 
                                    }
                                } else null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                // 4. Save Results
                if (results.isNotEmpty()) {
                    statusText = "Saving Results..."
                    val gson = Gson().newBuilder().setPrettyPrinting().create()
                    var json = gson.toJson(results)

                    // Compact embedding arrays
                    json = json.replace(Regex("\"embedding\":\\s*\\[([^\\]]+)\\]")) { match ->
                        val content = match.groupValues[1].replace("\n", "").replace(" ", "")
                        "\"embedding\": [$content]"
                    }

                    withContext(Dispatchers.IO) { File(filesDir, "tracks_features.json").writeText(json) }
                    statusText = "Success! Saved ${results.size} tracks"
                    updateLogMain("✓ Data saved to internal files dir.")
                } else {
                    statusText = "Finished with no matches."
                }

            } catch (e: CancellationException) {
                // Ignore normal cancellation
                throw e
            } catch (e: Exception) {
                statusText = "Pipeline Failed"
                updateLogMain("Fatal Error: ${e.localizedMessage}")
                Log.e(TAG, "Fatal Exception in Main Pipeline", e)
            }
        }
    }

    private fun updateLogMain(msg: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            logBuilder.append(msg).append("\n")
            logText = logBuilder.toString().takeLast(4000)
        }
    }

    private suspend fun searchWithStrategy(reccoApi: ReccoBeatsApi, trackName: String, artistName: String?): ReccoTrack? {
        val simple = try {
            reccoApi.searchTracks(trackName).content.find { it.trackTitle.contains(trackName, true) }
        } catch (e: Exception) {
            Log.w(TAG, "Simple search failed for $trackName: ${e.message}")
            null
        }

        if (simple != null) return simple
        return if (artistName != null) findTrackDeeply(reccoApi, trackName, artistName) else null
    }

    private suspend fun findTrackDeeply(reccoApi: ReccoBeatsApi, trackName: String, artistName: String): ReccoTrack? {
        return withContext(Dispatchers.IO) {
            try {
                val artistSearch = reccoApi.searchArtist(artistName.split(",")[0])
                val artist = artistSearch.content.firstOrNull() ?: return@withContext null
                
                val albums = artistAlbumCache.getOrPut(artist.id) { 
                    try { reccoApi.getArtistAlbums(artist.id).content } catch (e: Exception) { emptyList() }
                }

                for (album in albums.take(5)) {
                    val tracks = try {
                        albumTrackCache.getOrPut(album.id) { reccoApi.getAlbumTracks(album.id).content }
                    } catch (e: Exception) { emptyList() }
                    
                    val match = tracks.find { it.trackTitle.contains(trackName, true) }
                    if (match != null) return@withContext match
                }
            } catch (e: Exception) {
                Log.w(TAG, "Deep search failed for $trackName: ${e.message}")
            }
            null
        }
    }
}
