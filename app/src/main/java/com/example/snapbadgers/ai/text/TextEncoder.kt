package com.example.snapbadgers.ai.text

import android.content.Context
import android.util.Log
import com.example.snapbadgers.ai.text.ml.BertTokenizer
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder

enum class TextEncoderMode {
    STUB,
    MODEL
}

interface TextEncoder {
    val mode: TextEncoderMode
    val label: String
    suspend fun encode(text: String): FloatArray
}

object TextEncoderFactory {
    fun create(context: Context): TextEncoder {
        if (!hasAsset(context, MODEL_ASSET) || !hasAsset(context, VOCAB_ASSET)) {
            return StubTextEncoder("Stub heuristic encoder (model assets missing)")
        }

        return runCatching<TextEncoder> {
            val appContext = context.applicationContext
            val tokenizer = BertTokenizer.load(appContext, VOCAB_ASSET)
            QualcommTextEncoder(
                context = appContext,
                tokenizer = tokenizer,
                modelPath = MODEL_ASSET
            )
        }.onFailure {
            Log.w(TAG, "Falling back to StubTextEncoder", it)
        }.getOrElse {
            StubTextEncoder("Stub heuristic encoder (model init failed)")
        }
    }

    private fun hasAsset(context: Context, assetName: String): Boolean {
        return runCatching {
            context.assets.open(assetName).close()
            true
        }.getOrDefault(false)
    }

    private const val TAG = "TextEncoderFactory"
    private const val MODEL_ASSET = "mobile_bert.tflite"
    private const val VOCAB_ASSET = "vocab.txt"
}
