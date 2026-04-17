package com.example.snapbadgers.model

import org.junit.Assert.*
import org.junit.Test

class UiStateTest {

    @Test
    fun `UiState singletons are equal`() {
        assertEquals(UiState.Idle, UiState.Idle)
        assertEquals(UiState.Loading, UiState.Loading)
    }

    @Test
    fun `UiState Success contains result`() {
        val result = RecommendationResult(
            recommendations = listOf(Song(title = "Title", artist = "Artist", embedding = FloatArray(128))),
            inferenceTimeMs = 100L,
            usedVisionInput = false
        )
        assertEquals(result, UiState.Success(result).result)
    }

    @Test
    fun `UiState Error contains message`() {
        assertEquals("Test error", UiState.Error("Test error").message)
    }

    @Test
    fun `UiState type checking and when expression cover all cases`() {
        val states = listOf(
            UiState.Idle,
            UiState.Loading,
            UiState.Success(RecommendationResult(emptyList(), 0L, false)),
            UiState.Error("error")
        )
        states.forEach { state ->
            val label = when (state) {
                is UiState.Idle -> "idle"
                is UiState.Loading -> "loading"
                is UiState.Success -> "success"
                is UiState.Error -> "error"
            }
            assertNotNull(label)
        }
        assertTrue(states[0] is UiState.Idle)
        assertTrue(states[1] is UiState.Loading)
        assertTrue(states[2] is UiState.Success)
        assertTrue(states[3] is UiState.Error)
    }
}
