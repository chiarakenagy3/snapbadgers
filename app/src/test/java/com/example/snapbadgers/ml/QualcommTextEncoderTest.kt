package com.example.snapbadgers.ml

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class QualcommTextEncoderTest {

    @Test
    fun `encode with empty string returns 128-d zero vector`() = runBlocking {
        val encoder = QualcommTextEncoder()
        
        val result = encoder.encode("")
        
        assertEquals("Fallback array should be exactly 128 dimensions", 128, result.size)
        
        val expected = FloatArray(128) { 0f }
        assertArrayEquals("Fallback array should contain only zeros", expected, result, 0.0001f)
    }

    @Test
    fun `encode with blank string returns 128-d zero vector`() = runBlocking {
        val encoder = QualcommTextEncoder()
        
        val result = encoder.encode("   \n  \t  ")
        
        assertEquals("Fallback array should be exactly 128 dimensions", 128, result.size)
        
        val expected = FloatArray(128) { 0f }
        assertArrayEquals("Fallback array should contain only zeros", expected, result, 0.0001f)
    }
}
