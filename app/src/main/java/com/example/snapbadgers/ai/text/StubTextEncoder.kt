package com.example.snapbadgers.ai.text

class StubTextEncoder(
    override val label: String = "Stub heuristic encoder"
) : TextEncoder {

    override val mode: TextEncoderMode = TextEncoderMode.STUB

    override suspend fun encode(text: String): FloatArray {
        return HeuristicTextEmbedding.encode(text)
    }
}
