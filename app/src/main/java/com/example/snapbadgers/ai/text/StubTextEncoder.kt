package com.example.snapbadgers.ai.text

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils

class StubTextEncoder(
    override val label: String = "Stub heuristic encoder"
) : TextEncoder {

    override val mode: TextEncoderMode = TextEncoderMode.STUB

    override suspend fun encode(text: String): FloatArray {
        // TODO: Replace with real tokenizer + TFLite model inference
        val embedding = FloatArray(EMBEDDING_DIMENSION)
        val length = text.length.toFloat()
        val wordCount = text.trim().split("\\s+".toRegex()).size.toFloat()
        val tokens = text.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        embedding[0] = (length / 160f).coerceIn(0f, 1f)
        embedding[1] = (wordCount / 24f).coerceIn(0f, 1f)
        embedding[2] = if (text.contains("calm", ignoreCase = true)) 1f else 0f
        embedding[3] = if (text.contains("study", ignoreCase = true)) 1f else 0f
        embedding[4] = if (text.contains("happy", ignoreCase = true)) 1f else 0f
        embedding[5] = if (text.contains("sad", ignoreCase = true)) 1f else 0f
        embedding[6] = if (text.contains("workout", ignoreCase = true)) 1f else 0f
        embedding[7] = if (text.contains("night", ignoreCase = true)) 1f else 0f

        for ((index, token) in tokens.withIndex()) {
            val bucket = 8 + VectorUtils.positiveModulo(
                token.hashCode() + index * 31,
                EMBEDDING_DIMENSION - 8
            )
            embedding[bucket] += 1f
        }

        return VectorUtils.normalize(embedding)
    }
}
