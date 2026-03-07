package com.example.snapbadgers.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.snapbadgers.domain.VisionEncoder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class QualcommVisionEncoderTest {

    private val mockContext = mock<Context>()
    private val mockBitmap = mock<Bitmap>()
    private val mockImageProcessor = mock<ImageProcessor>()

    @Test
    fun `vision encoder initialization check`() = runBlocking {
        val encoder = QualcommVisionEncoder(mockContext, mockImageProcessor, "dummy.tflite")
        assertEquals(true, encoder is VisionEncoder)
    }

    @Test
    fun `encode with null or failing initialization returns zero vector`() = runBlocking {
        // Mock scale to return the same bitmap or a mock without calling native methods
        whenever(mockImageProcessor.scale(any(), any(), any())).thenReturn(mockBitmap)
        
        // Mock getPixel to avoid native calls during the loop
        whenever(mockBitmap.getPixel(any(), any())).thenReturn(0xFF0000) // Red pixel
        
        val encoder = QualcommVisionEncoder(mockContext, mockImageProcessor, "non_existent.tflite")
        val result = encoder.encode(mockBitmap)
        
        assertEquals("Fallback array should be exactly 128 dimensions", 128, result.size)
        // Since initialization fails (non_existent.tflite), it should return all zeros
        result.forEach { assertEquals(0f, it) }
    }
}
