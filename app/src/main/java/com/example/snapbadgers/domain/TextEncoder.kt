package com.example.snapbadgers.domain

interface TextEncoder {
    suspend fun encode(query: String): FloatArray
}
