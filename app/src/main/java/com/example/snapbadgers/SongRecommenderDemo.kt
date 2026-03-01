package com.example.snapbadgers

object SongRecommenderDemo {
    // Mock data: In a real app, these embeddings would come from a model like BERT or Word2Vec
    private val songLibrary = listOf(
        Song("1", "Walking on Sunshine", "Katrina and the Waves", listOf(0.9f, 0.8f, 0.1f)),
        Song("2", "Happy", "Pharrell Williams", listOf(0.85f, 0.9f, 0.05f)),
        Song("3", "Someone Like You", "Adele", listOf(0.1f, 0.1f, 0.9f)),
        Song("4", "Fix You", "Coldplay", listOf(0.2f, 0.2f, 0.85f)),
        Song("5", "Thunderstruck", "AC/DC", listOf(0.95f, 0.7f, 0.2f))
    )

    private val service = RecommendationService(songLibrary)

    /**
     * Simulates embedding text and getting recommendations.
     * For demo purposes, we map certain moods to hardcoded vectors.
     */
    fun getRecommendationsForText(text: String): List<String> {
        val inputEmbedding = mockEmbedText(text)
        val recommendedSongs = service.recommendSongs(inputEmbedding)
        return recommendedSongs.map { "${it.title} by ${it.artist}" }
    }

    private fun mockEmbedText(text: String): List<Float> {
        // This is a placeholder for a real embedding model (like MediaPipe or TensorFlow Lite)
        return when {
            text.contains("happy", ignoreCase = true) || text.contains("energetic", ignoreCase = true) -> 
                listOf(0.9f, 0.8f, 0.1f) // High energy, high happiness
            text.contains("sad", ignoreCase = true) || text.contains("mellow", ignoreCase = true) -> 
                listOf(0.1f, 0.2f, 0.8f) // Low energy, low happiness
            else -> listOf(0.5f, 0.5f, 0.5f) // Neutral
        }
    }
}
