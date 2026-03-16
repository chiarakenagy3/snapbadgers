package com.example.snapbadgers.ai.text

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackTextEncoderTest {

    @Test
    fun `falls back to stub when model encoder throws`() = runBlocking {
        val encoder = FallbackTextEncoder(
            primary = object : TextEncoder {
                override val mode: TextEncoderMode = TextEncoderMode.MODEL
                override val label: String = "Failing model"

                override suspend fun encode(text: String): FloatArray {
                    error("boom")
                }
            },
            fallback = FakeStubTextEncoder()
        )

        val result = encoder.encode("calm study session")

        assertEquals(TextEncoderMode.STUB, encoder.mode)
        assertEquals("Fake stub", encoder.label)
        assertTrue(result.any { it != 0f })
    }

    @Test
    fun `falls back to stub when model encoder returns zero vector for non blank input`() = runBlocking {
        val encoder = FallbackTextEncoder(
            primary = object : TextEncoder {
                override val mode: TextEncoderMode = TextEncoderMode.MODEL
                override val label: String = "Zero model"

                override suspend fun encode(text: String): FloatArray {
                    return FloatArray(128)
                }
            },
            fallback = FakeStubTextEncoder()
        )

        val result = encoder.encode("workout night")

        assertEquals(TextEncoderMode.STUB, encoder.mode)
        assertEquals("Fake stub", encoder.label)
        assertTrue(result.any { it != 0f })
    }

    private class FakeStubTextEncoder : TextEncoder {
        override val mode: TextEncoderMode = TextEncoderMode.STUB
        override val label: String = "Fake stub"

        override suspend fun encode(text: String): FloatArray {
            return FloatArray(128) { index ->
                if (index == text.length % 128) 1f else 0f
            }
        }
    }
}
