package com.example.snapbadgers.ai.text.ml

import android.content.Context
import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.text.TextEncoder
import com.example.snapbadgers.ai.text.Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

class QualcommTextEncoder(
    private val context: Context,
    private val tokenizer: Tokenizer,
    private val modelPath: String = "mobile_bert.tflite"
) : TextEncoder, AutoCloseable {

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null

    private val inputMaxLength = 128
    private val outputDimension = EMBEDDING_DIMENSION

    private fun initializeInterpreter() {
        if (interpreter != null) return

        val options = Interpreter.Options()
        nnApiDelegate = NnApiDelegate()
        options.addDelegate(nnApiDelegate)

        val modelBuffer = ModelLoader.loadMappedFile(context, modelPath)
        interpreter = Interpreter(modelBuffer, options)
    }

    override suspend fun encode(text: String): FloatArray = withContext(Dispatchers.Default) {
        if (text.isBlank()) {
            return@withContext FloatArray(outputDimension) { 0f }
        }

        try {
            initializeInterpreter()
            val tokens = tokenizer.tokenize(text)

            val inputBuffer = ByteBuffer.allocateDirect(inputMaxLength * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            for (index in 0 until inputMaxLength) {
                if (index < tokens.size) {
                    inputBuffer.putInt(tokens[index])
                } else {
                    inputBuffer.putInt(0)
                }
            }
            inputBuffer.rewind()

            val outputBuffer = Array(1) { FloatArray(outputDimension) }
            interpreter?.run(inputBuffer, outputBuffer)
            return@withContext VectorUtils.normalize(outputBuffer[0])
        } catch (_: Exception) {
            FloatArray(outputDimension) { 0f }
        }
    }

    override fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }
}
