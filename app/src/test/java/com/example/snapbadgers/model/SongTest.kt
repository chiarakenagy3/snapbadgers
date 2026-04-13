package com.example.snapbadgers.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SongTest {

    @Test
    fun `default values are correct`() {
        val song = Song(title = "Test Song", artist = "Test Artist")
        assertEquals("Test Song", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals(0f, song.similarity, 0f)
        assertEquals(0L, song.inferenceTimeMs)
        assertEquals(0, song.embedding.size)
    }

    @Test
    fun `embedding is retained`() {
        val emb = floatArrayOf(0.1f, 0.2f, 0.3f)
        val song = Song(title = "Song", artist = "Artist", embedding = emb)
        assertArrayEquals(emb, song.embedding, 0f)
    }
}
