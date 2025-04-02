package com.example.snapbadgers.ui

import com.example.snapbadgers.domain.TextEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Comprehensive tests for MainViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var textEncoder: TextEncoder
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        textEncoder = mock()
        viewModel = MainViewModel(textEncoder)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial embedding state is null`() = runTest {
        val state = viewModel.embeddingState.value

        assertNull("Initial state should be null", state)
    }

    @Test
    fun `processQuery updates embedding state`() = runTest {
        val expectedEmbedding = FloatArray(128) { it * 0.01f }
        whenever(textEncoder.encode("test query")).thenReturn(expectedEmbedding)

        viewModel.processQuery("test query")
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("State should be updated", state)
        assertArrayEquals("Should contain encoded embedding", expectedEmbedding, state, 0.0001f)
    }

    @Test
    fun `processQuery with empty string`() = runTest {
        val embedding = FloatArray(128) { 0f }
        whenever(textEncoder.encode("")).thenReturn(embedding)

        viewModel.processQuery("")
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("Should still update state", state)
    }

    @Test
    fun `processQuery with long text`() = runTest {
        val longText = "word ".repeat(1000)
        val embedding = FloatArray(128) { 0.5f }
        whenever(textEncoder.encode(longText)).thenReturn(embedding)

        viewModel.processQuery(longText)
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("Should handle long text", state)
    }

    @Test
    fun `processQuery multiple times updates state`() = runTest {
        val emb1 = FloatArray(128) { 0.1f }
        val emb2 = FloatArray(128) { 0.9f }

        whenever(textEncoder.encode("first")).thenReturn(emb1)
        whenever(textEncoder.encode("second")).thenReturn(emb2)

        viewModel.processQuery("first")
        advanceUntilIdle()
        val state1 = viewModel.embeddingState.value

        viewModel.processQuery("second")
        advanceUntilIdle()
        val state2 = viewModel.embeddingState.value

        assertArrayEquals("First query result", emb1, state1, 0.0001f)
        assertArrayEquals("Second query result", emb2, state2, 0.0001f)
        assertFalse("States should differ", state1.contentEquals(state2))
    }

    @Test
    fun `embeddingState flow emits updates`() = runTest {
        val embedding = FloatArray(128) { 0.5f }
        whenever(textEncoder.encode("test")).thenReturn(embedding)

        val initialState = viewModel.embeddingState.value
        assertNull("Initial state null", initialState)

        viewModel.processQuery("test")
        advanceUntilIdle()

        val updatedState = viewModel.embeddingState.first()
        assertNotNull("State should be updated", updatedState)
    }

    @Test
    fun `processQuery handles special characters`() = runTest {
        val query = "test !@#$%^&*() query"
        val embedding = FloatArray(128) { 0.5f }
        whenever(textEncoder.encode(query)).thenReturn(embedding)

        viewModel.processQuery(query)
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("Should handle special chars", state)
    }

    @Test
    fun `processQuery handles unicode text`() = runTest {
        val query = "音楽 música موسيقى 🎵"
        val embedding = FloatArray(128) { 0.5f }
        whenever(textEncoder.encode(query)).thenReturn(embedding)

        viewModel.processQuery(query)
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("Should handle unicode", state)
    }

    @Test
    fun `processQuery runs in viewModelScope`() = runTest {
        val embedding = FloatArray(128) { 0.5f }
        whenever(textEncoder.encode("test")).thenReturn(embedding)

        viewModel.processQuery("test")
        // Should not block - runs in coroutine scope
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("Should complete asynchronously", state)
    }

    @Test
    fun `multiple rapid queries update state correctly`() = runTest {
        val queries = listOf("query1", "query2", "query3", "query4", "query5")
        val embeddings = queries.mapIndexed { i, _ ->
            FloatArray(128) { i * 0.1f }
        }

        queries.forEachIndexed { i, query ->
            whenever(textEncoder.encode(query)).thenReturn(embeddings[i])
        }

        queries.forEach { query ->
            viewModel.processQuery(query)
        }
        advanceUntilIdle()

        val finalState = viewModel.embeddingState.value
        assertNotNull("Final state should be set", finalState)
        // Last query wins
        assertArrayEquals("Should have last embedding", embeddings.last(), finalState, 0.0001f)
    }

    @Test
    fun `processQuery with whitespace only`() = runTest {
        val query = "   \n\t   "
        val embedding = FloatArray(128) { 0f }
        whenever(textEncoder.encode(query)).thenReturn(embedding)

        viewModel.processQuery(query)
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("Should handle whitespace", state)
    }

    @Test
    fun `processQuery handles newlines`() = runTest {
        val query = "line1\nline2\nline3"
        val embedding = FloatArray(128) { 0.5f }
        whenever(textEncoder.encode(query)).thenReturn(embedding)

        viewModel.processQuery(query)
        advanceUntilIdle()

        val state = viewModel.embeddingState.value
        assertNotNull("Should handle newlines", state)
    }
}
