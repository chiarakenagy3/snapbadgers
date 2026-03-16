package com.example.snapbadgers.model

data class RecommendationResult(
    val recommendations: List<Song>,
    val inferenceTimeMs: Long,
    val usedVisionInput: Boolean
) {
    val topRecommendation: Song?
        get() = recommendations.firstOrNull()
}
