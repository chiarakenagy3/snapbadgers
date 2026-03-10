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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.snapbadgers.embedding.getEmbedding
import com.example.snapbadgers.network.*
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

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.White) {
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

        val spotifyApi = Retrofit.Builder().baseUrl("https://api.spotify.com/").client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build().create(SpotifyApi::class.java)
        val authApi = Retrofit.Builder().baseUrl("https://accounts.spotify.com/").client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
        val reccoApi = Retrofit.Builder().baseUrl(RECCOBEATS_BASE_URL).client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build().create(ReccoBeatsApi::class.java)

        lifecycleScope.launch {
            try {
                statusText = "Authenticating..."
                val authHeader = "Basic " + Base64.encodeToString("${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}".toByteArray(), Base64.NO_WRAP)
                val tokenResponse = authApi.refreshToken(authHeader, "refresh_token", BuildConfig.SPOTIFY_REFRESH_TOKEN.trim())
                val spotifyToken = "Bearer ${tokenResponse.access_token}"

                statusText = "Fetching Top Tracks..."
                val topTracks = spotifyApi.getTopTracks(spotifyToken, "medium_term", 10).items
                updateLog("✓ Found ${topTracks.size} tracks on Spotify")

                val semaphore = Semaphore(2)
                val results = topTracks.map { spotifyTrack ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            // 1. Primary Search
                            var foundTrack = withTimeoutOrNull(20000) {
                                searchWithStrategy(reccoApi, spotifyTrack.name, spotifyTrack.artists.firstOrNull()?.name)
                            }

                            var label = "Matched"

                            // 2. Fallback to Blank Space if null
                            if (foundTrack == null) {
                                withContext(Dispatchers.Main) { updateLog("⚠ Falling back for: ${spotifyTrack.name}") }
                                foundTrack = withTimeoutOrNull(10000) {
                                    searchWithStrategy(reccoApi, "Blank Space", "Taylor Swift")
                                }
                                label = "Fallback"
                            }

                            if (foundTrack != null) {
                                try {
                                    val features = reccoApi.getTrackFeatures(foundTrack.id)
                                    val embedding = getEmbedding(features).toList()
                                    withContext(Dispatchers.Main) { updateLog("✓ [$label] ${foundTrack!!.trackTitle}") }
                                    if (label == "Matched") {
                                        val prettyFeatures = Gson().newBuilder()
                                            .setPrettyPrinting() // 格式化输出，让它带缩进
                                            .create()
                                            .toJson(features)

                                        updateLog("--- Audio Features ---")
                                        updateLog(prettyFeatures)
                                        updateLog("----------------------")
                                    }
                                    val artistName = foundTrack.artists?.firstOrNull()?.name ?: "Unknown"

                                    ProcessedTrack(
                                        foundTrack.id,
                                        foundTrack.trackTitle,
                                        artistName,
                                        label,
                                        embedding
                                    )
                                } catch (e: Exception) { null }
                            } else null
                        }
                    }
                }.awaitAll().filterNotNull()

                if (results.isNotEmpty()) {
                    val gson = Gson().newBuilder()
                        .setPrettyPrinting()
                        .create()

                    var json = gson.toJson(results)

                    json = json.replace(Regex("\"embedding\": \\[([^\\]]+)]")) { match ->
                        val content = match.groupValues[1]
                            .replace("\n", "")
                            .replace(" ", "")
                        "\"embedding\": [$content]"
                    }

                    withContext(Dispatchers.IO) { File(filesDir, "tracks_features.json").writeText(json) }
                    statusText = "Done! Saved ${results.size}"
                }

            } catch (e: Exception) {
                statusText = "Error"
                updateLog("Fatal Error: ${e.localizedMessage}")
            }
        }
    }

    // --- Helper Functions ---

    private fun updateLog(msg: String) {
        logBuilder.append(msg).append("\n")
        logText = logBuilder.toString().takeLast(3000)
    }

    private suspend fun searchWithStrategy(reccoApi: ReccoBeatsApi, trackName: String, artistName: String?): ReccoTrack? {
        val simple = try {
            reccoApi.searchTracks(trackName).content.find { it.trackTitle.contains(trackName, true) }
        } catch (e: Exception) { null }

        if (simple != null) return simple

        return if (artistName != null) findTrackDeeply(reccoApi, trackName, artistName) else null
    }

    private suspend fun findTrackDeeply(reccoApi: ReccoBeatsApi, trackName: String, artistName: String): ReccoTrack? {
        return withContext(Dispatchers.IO) {
            try {
                val artist = reccoApi.searchArtist(artistName.split(",")[0]).content.firstOrNull() ?: return@withContext null
                val albums = artistAlbumCache.getOrPut(artist.id) { reccoApi.getArtistAlbums(artist.id).content }

                for (album in albums.take(15)) { // Limit to 5 albums for speed
                    val tracks = albumTrackCache.getOrPut(album.id) { reccoApi.getAlbumTracks(album.id).content }
                    val match = tracks.find { it.trackTitle.contains(trackName, true) }
                    if (match != null) return@withContext match
                }
            } catch (e: Exception) { }
            null
        }
    }
}