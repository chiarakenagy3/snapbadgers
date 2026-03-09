package com.example.snapbadgers

object SongRecommenderDemo {
    private val songLibrary = listOf(
        Song("1", "Walking on Sunshine", "Katrina and the Waves", listOf(0.9f, 0.8f, 0.1f)),
        Song("2", "Happy", "Pharrell Williams", listOf(0.85f, 0.9f, 0.05f)),
        Song("3", "Someone Like You", "Adele", listOf(0.1f, 0.1f, 0.9f)),
        Song("4", "Fix You", "Coldplay", listOf(0.2f, 0.2f, 0.85f)),
        Song("5", "Thunderstruck", "AC/DC", listOf(0.95f, 0.7f, 0.2f))
    )

    private val service = RecommendationService(songLibrary)

    /**
     * Recommends songs based on both current context (text input) and user preference.
     */
    fun getCombinedRecommendations(contextText: String): List<String> {
        // 1. Get embedding for the current context (e.g., "I'm feeling happy")
        val contextEmbedding = mockEmbedText(contextText)
        
        // 2. Mock User Embedding (e.g., this user usually likes energetic music)
        // In a real app, this would be retrieved from a database or profile service
        val userEmbedding = listOf(0.8f, 0.5f, 0.2f) 
        
        // 3. Get recommendations using both (50/50 split)
        val recommendedSongs = service.recommendSongs(
            contextEmbedding = contextEmbedding,
            userEmbedding = userEmbedding,
            contextWeight = 0.6f // Give slightly more weight to the current mood
        )
        
        return recommendedSongs.map { "${it.title} by ${it.artist}" }
    }

    private fun mockEmbedText(text: String): List<Float> {
        return when {
            text.contains("happy", ignoreCase = true) || text.contains("energetic", ignoreCase = true) -> 
                listOf(0.9f, 0.8f, 0.1f)
            text.contains("sad", ignoreCase = true) || text.contains("mellow", ignoreCase = true) -> 
                listOf(0.1f, 0.2f, 0.8f)
            else -> listOf(0.5f, 0.5f, 0.5f)
        }
    }
}
