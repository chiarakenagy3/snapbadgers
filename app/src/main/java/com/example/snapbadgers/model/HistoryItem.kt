package com.example.snapbadgers.model

import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val result: RecommendationResult
)
