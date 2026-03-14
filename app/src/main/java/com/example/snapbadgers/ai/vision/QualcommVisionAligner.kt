package com.example.snapbadgers.ai.vision

import android.content.Context
import com.example.snapbadgers.ai.common.ml.EmbeddingAligner
import com.example.snapbadgers.ai.common.ml.VectorUtils
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

class QualcommVisionAligner(
    private val context: Context,
    private val modelAssetName: String = "vision_aligner.tflite",
    private val embeddingDim: Int = 128
) : EmbeddingAligner {

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null

    private fun initializeInterpreter() {
        if (interpreter != null) return

        val modelBuffer = context.assets.open(modelAssetName).use { input ->
            val bytes = input.readBytes()
            ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
        }

        nnApiDelegate = NnApiDelegate()
        val options = Interpreter.Options().apply {
            addDelegate(nnApiDelegate)
            setNumThreads(2)
        }

        interpreter = Interpreter(modelBuffer, options)
    }

    override fun align(input: FloatArray): FloatArray {
        if (input.size != embeddingDim) {
            return FloatArray(embeddingDim) { 0f }
        }

        return try {
            initializeInterpreter()

            val normalizedInput = VectorUtils.normalize(input)

            val inputBuffer = ByteBuffer.allocateDirect(embeddingDim * 4)
                .order(ByteOrder.nativeOrder())
            for (v in normalizedInput) {
                inputBuffer.putFloat(v)
            }
            inputBuffer.rewind()

            val outputBuffer = Array(1) { FloatArray(embeddingDim) }

            interpreter?.run(inputBuffer, outputBuffer)

            VectorUtils.normalize(outputBuffer[0])
        } catch (_: Exception) {
            FloatArray(embeddingDim) { 0f }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        nnApiDelegate?.close()
        nnApiDelegate = null
    }
}