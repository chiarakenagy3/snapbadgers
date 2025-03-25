package com.example.snapbadgers.ai.text

import android.content.Context
import android.content.res.AssetManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Tests for TextEncoderFactory logic and model detection.
 */
class TextEncoderFactoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockAssets: AssetManager

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockAssets = mock(AssetManager::class.java)
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.assets).thenReturn(mockAssets)
    }

    @Test
    fun `describe returns MODEL mode when assets exist`() {
        // Mock assets exist
        `when`(mockAssets.open("mobile_bert.tflite")).thenReturn(mock())
        `when`(mockAssets.open("vocab.txt")).thenReturn(mock())

        val descriptor = TextEncoderFactory.describe(mockContext)

        assertEquals(TextEncoderMode.MODEL, descriptor.mode)
        assertTrue(descriptor.label.contains("Qualcomm", ignoreCase = true))
    }

    @Test
    fun `describe returns STUB mode when model missing`() {
        // Mock assets don't exist
        `when`(mockAssets.open(anyString())).thenThrow(RuntimeException("File not found"))

        val descriptor = TextEncoderFactory.describe(mockContext)

        assertEquals(TextEncoderMode.STUB, descriptor.mode)
        assertTrue(descriptor.label.contains("stub", ignoreCase = true))
    }

    @Test
    fun `describe returns STUB mode when vocab missing`() {
        // Mock only model exists
        `when`(mockAssets.open("mobile_bert.tflite")).thenReturn(mock())
        `when`(mockAssets.open("vocab.txt")).thenThrow(RuntimeException("File not found"))

        val descriptor = TextEncoderFactory.describe(mockContext)

        assertEquals(TextEncoderMode.STUB, descriptor.mode)
    }

    @Test
    fun `create returns StubTextEncoder when assets missing`() {
        // Mock assets don't exist
        `when`(mockAssets.open(anyString())).thenThrow(RuntimeException("File not found"))

        val encoder = TextEncoderFactory.create(mockContext)

        assertEquals(TextEncoderMode.STUB, encoder.mode)
        assertTrue(encoder is StubTextEncoder)
    }

    @Test
    fun `createAsync returns encoder asynchronously`() = runBlocking {
        `when`(mockAssets.open(anyString())).thenThrow(RuntimeException("File not found"))

        val encoder = TextEncoderFactory.createAsync(mockContext)

        assertNotNull(encoder)
        assertEquals(TextEncoderMode.STUB, encoder.mode)
    }

    @Test
    fun `create returns FallbackTextEncoder when model init fails`() {
        // Mock assets exist but model init will fail
        `when`(mockAssets.open("mobile_bert.tflite")).thenReturn(mock())
        `when`(mockAssets.open("vocab.txt")).thenReturn(mock())
        // But context is not properly set up for model loading

        val encoder = TextEncoderFactory.create(mockContext)

        // Should fall back to StubTextEncoder on failure
        assertEquals(TextEncoderMode.STUB, encoder.mode)
    }
}
