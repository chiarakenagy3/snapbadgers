package com.example.snapbadgers

import com.example.snapbadgers.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationServiceTest {

    @Test
    fun testRecommendSongs() {
        // 1. Create a mock song library
        // Song A: High values in first few dimensions
        val songAEmb = FloatArray(128) { i -> if (i < 10) 1.0f else 0.0f }
        // Song B: High values in middle dimensions
        val songBEmb = FloatArray(128) { i -> if (i in 50..60) 1.0f else 0.0f }
        
        val songLibrary = listOf(
            Song("Song A", "Artist A", embedding = songAEmb),
            Song("Song B", "Artist B", embedding = songBEmb)
        )

        val service = RecommendationService(songLibrary)

        // 2. Scenario: Text search matching Song A
        val textQuery = FloatArray(128) { i -> if (i < 10) 0.5f else 0.0f }
        
        val results = service.recommendSongs(
            textEmbedding = textQuery,
            textWeight = 1.0f,
            visionWeight = 0.0f,
            userWeight = 0.0f
        )

        // Assertions
        assertTrue(results.isNotEmpty())
        assertEquals("Song A", results[0].song.title)
        assertTrue("Score should be high for match", results[0].finalScore > 0.9f)
        
        // 3. Scenario: Vision search matching Song B
        val visionQuery = FloatArray(128) { i -> if (i in 50..60) 0.8f else 0.0f }
        val resultsVision = service.recommendSongs(
            visionEmbedding = visionQuery,
            textWeight = 0.0f,
            visionWeight = 1.0f
        )
        
        assertEquals("Song B", resultsVision[0].song.title)

        // 4. Scenario: Weighted Multi-modal
        // Query that is 50% Song A and 50% Song B
        val mixedText = FloatArray(128) { i -> if (i < 10) 1.0f else 0.0f } // Matches A
        val mixedVision = FloatArray(128) { i -> if (i in 50..60) 1.0f else 0.0f } // Matches B
        
        val mixedResults = service.recommendSongs(
            textEmbedding = mixedText,
            visionEmbedding = mixedVision,
            textWeight = 0.7f,  // Heavily weight text (Song A)
            visionWeight = 0.3f
        )
        
        assertEquals("Song A should win due to higher weight", "Song A", mixedResults[0].song.title)
        
        println("RecommendationService logic verified!")
    }
}
