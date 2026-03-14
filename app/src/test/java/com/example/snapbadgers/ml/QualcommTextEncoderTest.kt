package com.example.snapbadgers.ml

import android.content.Context
import com.example.snapbadgers.ai.text.domain.Tokenizer
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class QualcommTextEncoderTest {

    private val mockContext = mock(Context::class.`java`)
    private val mockTokenizer = mock(Tokenizer::class.java)

    @Test
    fun `encode with empty string returns 128-d zero vector`() = runBlocking {
        // We skip initialization of the real interpreter in tests by providing a dummy path or mocking
        // For now, these tests verify the fallback logic which doesn't require the interpreter
        val encoder = QualcommTextEncoder(mockContext, mockTokenizer)
        
        val result = encoder.encode("")
        
        assertEquals("Fallback array should be exactly 128 dimensions", 128, result.size)
        
        val expected = FloatArray(128) { 0f }
        assertArrayEquals("Fallback array should contain only zeros", expected, result, 0.0001f)
    }

    @Test
    fun `encode with blank string returns 128-d zero vector`() = runBlocking {
        val encoder = QualcommTextEncoder(mockContext, mockTokenizer)
        
        val result = encoder.encode("   \n  \t  ")
        
        assertEquals("Fallback array should be exactly 128 dimensions", 128, result.size)
        
        val expected = FloatArray(128) { 0f }
        assertArrayEquals("Fallback array should contain only zeros", expected, result, 0.0001f)
    }
}
