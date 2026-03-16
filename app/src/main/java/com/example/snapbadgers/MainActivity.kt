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
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import com.example.snapbadgers.ai.songembeddings.embedding.getEmbedding
import com.example.snapbadgers.ai.songembeddings.network.*
import com.example.snapbadgers.model.InferenceSteps
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
        
        // Recommendation States
        var vibeInput by mutableStateOf("")
        var isProcessing by mutableStateOf(false)
        var recommendedSong by mutableStateOf<Song?>(null)
        var stepsProgress by mutableStateOf(InferenceSteps())
        
        // AI Pipeline instance
        val pipeline = RecommendationPipeline(this)

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

                // AI Vibe Search Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val modelExists = assets.list("")?.contains("mobile_bert.tflite") == true
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. AI Recommendation Engine", style = MaterialTheme.typography.titleMedium)
                        
                        if (!modelExists) {
                            Text(
                                "⚠ Warning: mobile_bert.tflite missing!", 
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = vibeInput,
                            onValueChange = { vibeInput = it },
                            label = { Text("What is your vibe?") },
                            placeholder = { Text("e.g. Chill jazz night, Intense gym") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    isProcessing = true
                                    recommendedSong = pipeline.runPipeline(vibeInput) { 
                                        stepsProgress = it 
                                    }
                                    isProcessing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = vibeInput.isNotEmpty() && !isProcessing && modelExists
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing...")
                            } else {
                                Text("Find My Vibe Song")
                            }
                        }
                    }
                }

                if (recommendedSong != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Top Recommendation:", style = MaterialTheme.typography.titleMedium)
                    ListItem(
                        headlineContent = { Text(recommendedSong!!.title) },
                        supportingContent = { 
                            Text("${recommendedSong!!.artist} | Latency: ${recommendedSong!!.inferenceTimeMs}ms") 
                        },
                        trailingContent = { 
                            if (recommendedSong!!.similarity > 0) {
                                Text("Score: %.2f".format(recommendedSong!!.similarity))
                            }
                        }
                    )
                    
                    // Simple progress bar for AI steps
                    val progressValue = when {
                        stepsProgress.ranked -> 1f
                        stepsProgress.fused -> 0.75f
                        stepsProgress.textEncoded -> 0.4f
                        else -> 0.1f
                    }
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Execution Log:", style = MaterialTheme.typography.titleSmall)
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

        val reccoBeatsApi = Retrofit.Builder()
            .baseUrl(reccoBeatsBaseUrl).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ReccoBeatsApi::class.java)

        // Library Sync logic
        lifecycleScope.launch {
            try {
                statusText = "Syncing with Spotify..."
                val authHeader = "Basic " + Base64.encodeToString("${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}".toByteArray(), Base64.NO_WRAP)
                val tokenResponse = authApi.refreshToken(authHeader, "refresh_token", BuildConfig.SPOTIFY_REFRESH_TOKEN.trim())
                val spotifyToken = "Bearer ${tokenResponse.access_token}"

                val tracksToProcess = mutableListOf<Triple<String, String, String>>() 
                val topResponse = spotifyApi.getTopTracks(spotifyToken, "medium_term", 20)
                topResponse.items.forEach { tracksToProcess.add(Triple(it.id, it.name, it.artists.joinToString(", ") { a -> a.name })) }

                statusText = "Analyzing audio features..."
                val semaphore = Semaphore(5)
                val finalResults = tracksToProcess.map { (id, name, artist) ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                val features = reccoBeatsApi.getTrackFeatures(id)
                                val embeddingArray = getEmbedding(features)
                                withContext(Dispatchers.Main) { logText += "✓ Analyzed: $name\n" }
                                ProcessedTrack(id, name, artist, "Spotify", embeddingArray.toList())
                            } catch (e: Exception) { null }
                        }
                    }
                }.awaitAll().filterNotNull()

                if (finalResults.isNotEmpty()) {
                    val json = Gson().toJson(finalResults)
                    val file = File(filesDir, "tracks_features.json")
                    withContext(Dispatchers.IO) { file.writeText(json) }
                    statusText = "Ready! Library Loaded (${finalResults.size} songs)"
                    logText += "✓ Recommendations are now based on YOUR Spotify tracks.\n"
                }

            } catch (e: Exception) {
                statusText = "Error: ${e.message}"
                Log.e(tag, "Pipeline Error", e)
            }
        }
    }
}
