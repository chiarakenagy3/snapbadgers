package com.example.snapbadgers.ai.text.domain

interface TextEncoder {
    suspend fun encode(query: String): FloatArray
}
