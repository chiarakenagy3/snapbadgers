package com.example.snapbadgers.ml

import android.content.Context
import com.example.snapbadgers.domain.TextEncoder
import com.example.snapbadgers.domain.Tokenizer
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

    private val inputMaxLen = 128
    private val outputDim = 128

    private fun initializeInterpreter() {
        if (interpreter != null) return
        
        val options = Interpreter.Options()
        // For Galaxy S25 / Android, NNAPI is the standard way to leverage the NPU via delegates
        nnApiDelegate = NnApiDelegate()
        options.addDelegate(nnApiDelegate)
        
        val modelBuffer = ModelLoader.loadMappedFile(context, modelPath)
        interpreter = Interpreter(modelBuffer, options)
    }

    override suspend fun encode(query: String): FloatArray = withContext(Dispatchers.Default) {
        if (query.isBlank()) {
            return@withContext FloatArray(outputDim) { 0f }
        }

        try {
            initializeInterpreter()
            val tokens = tokenizer.tokenize(query)
            
            // Prepare input buffer (padded to inputMaxLen)
            val inputBuffer = ByteBuffer.allocateDirect(inputMaxLen * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            for (i in 0 until inputMaxLen) {
                if (i < tokens.size) {
                    inputBuffer.putInt(tokens[i])
                } else {
                    inputBuffer.putInt(0) // Padding token
                }
            }
            inputBuffer.rewind()

            // Prepare output buffer
            val outputBuffer = Array(1) { FloatArray(outputDim) }
            
            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)
            
            return@withContext outputBuffer[0]
            
        } catch (e: Exception) {
            e.printStackTrace()
            FloatArray(outputDim) { 0f }
        }
    }

    override fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }
}
