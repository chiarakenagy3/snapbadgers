package com.example.snapbadgers.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for UiState sealed class hierarchy.
 */
class UiStateTest {

    @Test
    fun `UiState Idle is singleton-like`() {
        val state1 = UiState.Idle
        val state2 = UiState.Idle

        assertEquals(state1, state2)
    }

    @Test
    fun `UiState Loading is singleton-like`() {
        val state1 = UiState.Loading
        val state2 = UiState.Loading

        assertEquals(state1, state2)
    }

    @Test
    fun `UiState Success contains result`() {
        val result = RecommendationResult(
            recommendations = listOf(
                Song("Title", "Artist", 0f, 0L, FloatArray(128))
            ),
            inferenceTimeMs = 100L,
            usedVisionInput = false
        )

        val state = UiState.Success(result)

        assertEquals(result, (state as UiState.Success).result)
    }

    @Test
    fun `UiState Error contains message`() {
        val errorMessage = "Test error message"

        val state = UiState.Error(errorMessage)

        assertEquals(errorMessage, (state as UiState.Error).message)
    }

    @Test
    fun `UiState type checking works`() {
        val idleState: UiState = UiState.Idle
        val loadingState: UiState = UiState.Loading
        val successState: UiState = UiState.Success(
            RecommendationResult(emptyList(), 0L, false)
        )
        val errorState: UiState = UiState.Error("error")

        assertTrue(idleState is UiState.Idle)
        assertTrue(loadingState is UiState.Loading)
        assertTrue(successState is UiState.Success)
        assertTrue(errorState is UiState.Error)
    }

    @Test
    fun `UiState when expression covers all cases`() {
        val states = listOf(
            UiState.Idle,
            UiState.Loading,
            UiState.Success(RecommendationResult(emptyList(), 0L, false)),
            UiState.Error("error")
        )

        states.forEach { state ->
            val result = when (state) {
                is UiState.Idle -> "idle"
                is UiState.Loading -> "loading"
                is UiState.Success -> "success"
                is UiState.Error -> "error"
            }

            assertNotNull(result)
        }
    }
}
