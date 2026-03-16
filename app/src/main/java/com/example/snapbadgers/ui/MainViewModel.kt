package com.example.snapbadgers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapbadgers.domain.TextEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val textEncoder: TextEncoder) : ViewModel() {

    private val _embeddingState = MutableStateFlow<FloatArray?>(null)
    val embeddingState: StateFlow<FloatArray?> = _embeddingState.asStateFlow()

    fun processQuery(query: String) {
        viewModelScope.launch {
            val embedding = textEncoder.encode(query)
            _embeddingState.value = embedding
        }
    }
}
