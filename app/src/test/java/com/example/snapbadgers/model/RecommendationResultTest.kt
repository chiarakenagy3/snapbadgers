package com.example.snapbadgers.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecommendationResultTest {

    @Test
    fun `topRecommendation returns first song`() {
        val songs = listOf(
            Song(title = "Top", artist = "A", similarity = 0.9f),
            Song(title = "Second", artist = "B", similarity = 0.7f)
        )
        val result = RecommendationResult(
            recommendations = songs,
            inferenceTimeMs = 100L,
            usedVisionInput = true
        )
        assertEquals("Top", result.topRecommendation?.title)
    }

    @Test
    fun `topRecommendation returns null for empty list`() {
        val result = RecommendationResult(
            recommendations = emptyList(),
            inferenceTimeMs = 50L,
            usedVisionInput = false
        )
        assertNull(result.topRecommendation)
    }
}
