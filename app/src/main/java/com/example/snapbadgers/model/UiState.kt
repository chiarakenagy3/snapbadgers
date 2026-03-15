package com.example.snapbadgers.model

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val result: RecommendationResult) : UiState()
    data class Error(val message: String) : UiState()
}
