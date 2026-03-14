package com.example.snapbadgers.data

import android.content.Context
import android.util.Log
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.model.EmbeddedTrack
import com.example.snapbadgers.model.Song
import com.google.gson.Gson
import java.io.File

class SongRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val embeddedSongs: List<Song> by lazy { loadEmbeddedSongs() }
    private val fallbackSongs = listOf(
        Song(
            title = "Blinding Lights",
            artist = "The Weeknd",
            embedding = buildFallbackEmbedding(
                raw = floatArrayOf(18f, 3f, 0f, 0f, 9.8f, 20f),
                salt = "Blinding Lights".hashCode()
            )
        ),
        Song(
            title = "Sunflower",
            artist = "Post Malone",
            embedding = buildFallbackEmbedding(
                raw = floatArrayOf(12f, 2f, 0f, 0f, 8.5f, 50f),
                salt = "Sunflower".hashCode()
            )
        ),
        Song(
            title = "Weightless",
            artist = "Marconi Union",
            embedding = buildFallbackEmbedding(
                raw = floatArrayOf(10f, 2f, 1f, 0f, 9.7f, 5f),
                salt = "Weightless".hashCode()
            )
        )
    )

    fun getAllSongs(): List<Song> = embeddedSongs.ifEmpty { fallbackSongs }

    fun findTopSong(queryEmbedding: FloatArray): Song {
        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(queryEmbedding, salt = QUERY_SALT)
        val songs = candidateSongs()
        val bestSong = songs.maxByOrNull { song ->
            similarity(normalizedQuery, song.embedding)
        } ?: songs.first()

        val score = similarity(normalizedQuery, bestSong.embedding)
        return bestSong.copy(similarity = score)
    }

    private fun candidateSongs(): List<Song> {
        return embeddedSongs.ifEmpty { fallbackSongs }
    }

    private fun loadEmbeddedSongs(): List<Song> {
        val embeddedTracks = loadEmbeddedTracksFromFile()
            ?: loadEmbeddedTracksFromAssets()
            ?: return emptyList()

        return embeddedTracks
            .filter { it.embedding.isNotEmpty() }
            .map { track ->
                Song(
                    title = track.name,
                    artist = track.artists,
                    embedding = VectorUtils.alignToEmbeddingDimension(
                        track.embedding.toFloatArray(),
                        salt = track.trackId.hashCode()
                    )
                )
            }
    }

    private fun loadEmbeddedTracksFromFile(): List<EmbeddedTrack>? {
        val file = File(appContext.filesDir, TRACKS_FEATURES_FILE)
        if (!file.exists()) return null

        return runCatching { parseEmbeddedTracks(file.readText()) }
            .onFailure { Log.w(TAG, "Failed to read ${file.absolutePath}", it) }
            .getOrNull()
    }

    private fun loadEmbeddedTracksFromAssets(): List<EmbeddedTrack>? {
        return runCatching {
            appContext.assets.open(TRACKS_FEATURES_FILE).bufferedReader().use { reader ->
                parseEmbeddedTracks(reader.readText())
            }
        }.onFailure {
            Log.w(TAG, "Failed to read asset $TRACKS_FEATURES_FILE", it)
        }.getOrNull()
    }

    private fun parseEmbeddedTracks(json: String): List<EmbeddedTrack> {
        return gson.fromJson(json, Array<EmbeddedTrack>::class.java)?.toList().orEmpty()
    }

    private fun similarity(a: FloatArray, b: FloatArray): Float {
        return VectorUtils.cosineSimilarity(a, b)
    }

    private fun buildFallbackEmbedding(raw: FloatArray, salt: Int): FloatArray {
        return VectorUtils.alignToEmbeddingDimension(raw, salt = salt)
    }

    private companion object {
        const val TAG = "SongRepository"
        const val TRACKS_FEATURES_FILE = "tracks_features.json"
        const val QUERY_SALT = 101
    }
}
