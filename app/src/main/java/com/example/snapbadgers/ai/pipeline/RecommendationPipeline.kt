package com.example.snapbadgers.ai.pipeline

import android.content.Context
import android.util.Log
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.sensor.SensorCollector
import com.example.snapbadgers.ai.sensor.SensorEncoder
import com.example.snapbadgers.ai.text.domain.TextEncoder
import com.example.snapbadgers.ai.text.ml.BertTokenizer
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder
import com.example.snapbadgers.data.SongRepository
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.ProcessedTrack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.io.File

class RecommendationPipeline(private val context: Context) {

    private val tokenizer = BertTokenizer.load(context, "vocab.txt")
    private val textEncoder: TextEncoder = QualcommTextEncoder(context, tokenizer)
    private val sensorCollector = SensorCollector(context)
    private val sensorEncoder = SensorEncoder()
    private val fusionEngine = FusionEngine()
    private var songRepository = SongRepository()

    init {
        loadLibrary()
    }

    /**
     * Load the synced Spotify library from internal storage
     */
    private fun loadLibrary() {
        try {
            val file = File(context.filesDir, "tracks_features.json")
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<List<ProcessedTrack>>() {}.type
                val processedTracks: List<ProcessedTrack> = Gson().fromJson(json, type)
                
                val songs = processedTracks.map {
                    Song(
                        title = it.name,
                        artist = it.artists,
                        embedding = it.embedding.toFloatArray()
                    )
                }
                songRepository.updateSongs(songs)
                Log.d("Pipeline", "Library loaded with ${songs.size} songs from JSON")
            } else {
                Log.w("Pipeline", "tracks_features.json not found. Run Spotify sync first.")
            }
        } catch (e: Exception) {
            Log.e("Pipeline", "Failed to load library: ${e.message}")
        }
    }

    suspend fun runPipeline(
        input: String,
        onStepUpdate: (InferenceSteps) -> Unit
    ): Song {
        var steps = InferenceSteps()
        val startMs = System.currentTimeMillis()

        try {
            // Refresh library just in case new sync happened
            loadLibrary()

            sensorCollector.start()

            delay(120)
            val textEmbedding = textEncoder.encode(input)
            steps = steps.copy(textEncoded = true)
            onStepUpdate(steps)

            delay(120)
            val sensorSample = sensorCollector.getLatestSample()
            val sensorEmbedding = sensorEncoder.encode(sensorSample)

            delay(120)
            val fusedEmbedding = fusionEngine.fuse(
                textEmbedding = textEmbedding,
                sensorEmbedding = sensorEmbedding
            )
            steps = steps.copy(fused = true)
            onStepUpdate(steps)

            delay(120)
            steps = steps.copy(projected = true)
            onStepUpdate(steps)

            delay(160)
            val song = songRepository.findTopSong(fusedEmbedding)
            steps = steps.copy(ranked = true)
            onStepUpdate(steps)

            val inferenceMs = System.currentTimeMillis() - startMs
            
            // If library is still empty after load attempt
            return song?.copy(inferenceTimeMs = inferenceMs)
                ?: Song(
                    title = "Library Sync Required",
                    artist = "Please run Spotify sync in Main screen first",
                    inferenceTimeMs = inferenceMs
                )

        } finally {
            sensorCollector.stop()
        }
    }
}
