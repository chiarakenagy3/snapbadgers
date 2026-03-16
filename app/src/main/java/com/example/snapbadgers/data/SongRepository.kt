package com.example.snapbadgers.data

import android.content.Context
import android.util.Log
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import com.example.snapbadgers.model.EmbeddedTrack
import com.example.snapbadgers.model.Song
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

class SongRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val embeddedSongs: List<Song> by lazy { loadEmbeddedSongs() }
    private val sampleSongs: List<Song> by lazy { loadSampleSongs() }
    private val fallbackSongs = listOf(
        Song(
            title = "Blinding Lights",
            artist = "The Weeknd",
            embedding = buildFallbackEmbedding(
                description = "Blinding Lights by The Weeknd energetic pop workout night drive upbeat"
            )
        ),
        Song(
            title = "Sunflower",
            artist = "Post Malone",
            embedding = buildFallbackEmbedding(
                description = "Sunflower by Post Malone happy chill pop easy listening daytime"
            )
        ),
        Song(
            title = "Weightless",
            artist = "Marconi Union",
            embedding = buildFallbackEmbedding(
                description = "Weightless by Marconi Union calm study relax sleep ambient"
            )
        )
    )

    fun getAllSongs(): List<Song> = candidateSongs()

    val hasEmbeddedCatalog: Boolean
        get() = embeddedSongs.isNotEmpty()

    fun findTopSongs(queryEmbedding: FloatArray, limit: Int = DEFAULT_RECOMMENDATION_LIMIT): List<Song> {
        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(queryEmbedding, salt = QUERY_SALT)
        return candidateSongs()
            .map { song ->
                val score = similarity(normalizedQuery, song.embedding)
                song.copy(similarity = score)
            }
            .sortedByDescending { it.similarity }
            .take(limit.coerceAtLeast(1))
    }

    fun findTopSong(queryEmbedding: FloatArray): Song {
        return findTopSongs(queryEmbedding, limit = 1).first()
    }

    private fun candidateSongs(): List<Song> {
        return embeddedSongs
            .ifEmpty { sampleSongs }
            .ifEmpty { fallbackSongs }
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

    private fun loadSampleSongs(): List<Song> {
        val catalog = runCatching {
            appContext.assets.open(SAMPLE_SONGS_FILE).bufferedReader().use { reader ->
                gson.fromJson(reader.readText(), SampleSongCatalog::class.java)
            }
        }.onFailure {
            Log.w(TAG, "Failed to read asset $SAMPLE_SONGS_FILE", it)
        }.getOrNull() ?: return emptyList()

        return catalog.songs.map { sample ->
            Song(
                title = sample.title,
                artist = sample.artist,
                embedding = buildFallbackEmbedding(buildSampleSongDescription(sample))
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

    private fun buildFallbackEmbedding(description: String): FloatArray {
        return HeuristicTextEmbedding.encode(description)
    }

    private fun buildSampleSongDescription(sample: SampleSongAsset): String {
        return buildString {
            append(sample.title)
            append(' ')
            append(sample.artist)

            if (sample.genre.isNotEmpty()) {
                append(' ')
                append(sample.genre.joinToString(" "))
            }

            if (sample.mood.isNotEmpty()) {
                append(' ')
                append(sample.mood.joinToString(" "))
            }

            if (sample.contextTags.isNotEmpty()) {
                append(' ')
                append(sample.contextTags.joinToString(" "))
            }

            append(" tempo ")
            append(sample.tempoBpm)
            append(" energy ")
            append((sample.energy * 100).toInt())
            append(" valence ")
            append((sample.valence * 100).toInt())
            append(" danceability ")
            append((sample.danceability * 100).toInt())
            append(' ')
            append(sample.metadataText)
        }
    }

    private companion object {
        const val TAG = "SongRepository"
        const val TRACKS_FEATURES_FILE = "tracks_features.json"
        const val SAMPLE_SONGS_FILE = "sample_songs.json"
        const val QUERY_SALT = 101
        const val DEFAULT_RECOMMENDATION_LIMIT = 3
    }

    private data class SampleSongCatalog(
        val songs: List<SampleSongAsset> = emptyList()
    )

    private data class SampleSongAsset(
        val id: String,
        val title: String,
        val artist: String,
        val genre: List<String> = emptyList(),
        val mood: List<String> = emptyList(),
        @SerializedName("tempo_bpm")
        val tempoBpm: Int = 0,
        val energy: Float = 0f,
        val valence: Float = 0f,
        val danceability: Float = 0f,
        @SerializedName("context_tags")
        val contextTags: List<String> = emptyList(),
        @SerializedName("metadata_text")
        val metadataText: String = ""
    )
}
