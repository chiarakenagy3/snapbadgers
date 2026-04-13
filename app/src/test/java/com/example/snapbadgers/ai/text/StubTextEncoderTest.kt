package com.example.snapbadgers.ai.text

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StubTextEncoderTest {

    private val encoder = StubTextEncoder("Test stub encoder")

    @Test
    fun `mode is STUB`() {
        assertEquals(TextEncoderMode.STUB, encoder.mode)
    }

    @Test
    fun `encode produces 128-d output`() = runBlocking {
        val result = encoder.encode("chill vibes")
        assertEquals(EMBEDDING_DIMENSION, result.size)
    }

    @Test
    fun `label matches constructor argument`() {
        assertEquals("Test stub encoder", encoder.label)
    }
}
