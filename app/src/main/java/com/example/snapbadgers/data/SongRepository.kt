package com.example.snapbadgers.data

import com.example.snapbadgers.model.Song
import kotlin.math.min

class SongRepository {

    private val songs = listOf(
        Song(
            title = "Blinding Lights",
            artist = "The Weeknd",
            embedding = floatArrayOf(18f, 3f, 0f, 0f, 9.8f, 20f)
        ),
        Song(
            title = "Sunflower",
            artist = "Post Malone",
            embedding = floatArrayOf(12f, 2f, 0f, 0f, 8.5f, 50f)
        ),
        Song(
            title = "Weightless",
            artist = "Marconi Union",
            embedding = floatArrayOf(10f, 2f, 1f, 0f, 9.7f, 5f)
        )
    )

    fun getAllSongs(): List<Song> = songs

    fun findTopSong(queryEmbedding: FloatArray): Song {
        val bestSong = songs.maxByOrNull { song ->
            similarity(queryEmbedding, song.embedding)
        } ?: songs.first()

        val score = similarity(queryEmbedding, bestSong.embedding)

        return bestSong.copy(similarity = score)
    }

    private fun similarity(a: FloatArray, b: FloatArray): Float {
        val size = min(a.size, b.size)
        if (size == 0) return 0f

        var score = 0f
        for (i in 0 until size) {
            score += 1f / (1f + kotlin.math.abs(a[i] - b[i]))
        }
        return score / size
    }
}