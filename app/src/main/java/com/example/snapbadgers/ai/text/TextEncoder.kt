package com.example.snapbadgers.ai.text

import android.content.Context

class TextEncoder(private val context: Context) {

    fun encode(text: String): FloatArray {
        // TODO: Replace with real tokenizer + TFLite model inference
        val length = text.length.toFloat()
        val wordCount = text.trim().split("\\s+".toRegex()).size.toFloat()

        return floatArrayOf(
            length,
            wordCount,
            if (text.contains("calm", ignoreCase = true)) 1f else 0f,
            if (text.contains("study", ignoreCase = true)) 1f else 0f
        )
    }
}