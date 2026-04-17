package com.example.snapbadgers.ai.text

import android.content.Context
import com.example.snapbadgers.ai.common.EncoderUtils
import com.example.snapbadgers.ai.text.ml.BertTokenizer
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class TextEncoderMode {
    STUB,
    MODEL
}

data class TextEncoderDescriptor(
    val mode: TextEncoderMode,
    val label: String
)

interface TextEncoder {
    val mode: TextEncoderMode
    val label: String
    suspend fun encode(text: String): FloatArray
}

object TextEncoderFactory {
    fun describe(context: Context): TextEncoderDescriptor {
        return if (EncoderUtils.hasAsset(context, MODEL_ASSET) && EncoderUtils.hasAsset(context, VOCAB_ASSET)) {
            TextEncoderDescriptor(
                mode = TextEncoderMode.MODEL,
                label = "Qualcomm MobileBERT (NNAPI)"
            )
        } else {
            TextEncoderDescriptor(
                mode = TextEncoderMode.STUB,
                label = "Stub heuristic encoder (model assets missing)"
            )
        }
    }

    fun create(context: Context): TextEncoder {
        if (describe(context).mode == TextEncoderMode.STUB) {
            return StubTextEncoder("Stub heuristic encoder (model assets missing)")
        }

        return runCatching<TextEncoder> {
            val appContext = context.applicationContext
            val tokenizer = BertTokenizer.load(appContext, VOCAB_ASSET)
            FallbackTextEncoder(
                primary = QualcommTextEncoder(
                    context = appContext,
                    tokenizer = tokenizer,
                    modelPath = MODEL_ASSET
                ),
                fallback = StubTextEncoder("Stub heuristic encoder (model runtime fallback)")
            )
        }.onFailure {
            EncoderUtils.logWarning(TAG, "Falling back to StubTextEncoder", it)
        }.getOrElse {
            StubTextEncoder("Stub heuristic encoder (model init failed)")
        }
    }

    suspend fun createAsync(context: Context): TextEncoder = withContext(Dispatchers.Default) {
        create(context)
    }

    private const val TAG = "TextEncoderFactory"
    private const val MODEL_ASSET = "mobile_bert.tflite"
    private const val VOCAB_ASSET = "vocab.txt"
}
