package com.example.snapbadgers.data

import com.example.snapbadgers.model.Song
import kotlin.math.min

/**
 * SongRepository is now dynamic. It should be populated from the Spotify/ReccoBeats sync results.
 */
class SongRepository(private var songs: List<Song> = emptyList()) {

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
    }

    fun getAllSongs(): List<Song> = songs

    fun findTopSong(queryEmbedding: FloatArray): Song? {
        if (songs.isEmpty()) return null
        
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
