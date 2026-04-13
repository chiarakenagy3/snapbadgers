package com.example.snapbadgers.ai.text.ml

import android.content.Context
import android.util.Log
import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.text.TextEncoder
import com.example.snapbadgers.ai.text.TextEncoderMode
import com.example.snapbadgers.ai.text.Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Text encoder backed by MobileBERT, running on-device via TFLite.
 *
 * Delegate strategy: attempts NNAPI first for NPU/DSP acceleration, falls back to
 * CPU-only XNNPack if NNAPI is unavailable or fails.
 *
 * NOTE: [NnApiDelegate] is deprecated starting Android 15 (SDK 35). The recommended
 * migration path is QNN delegate → GPU delegate → XNNPack. QNN requires the
 * Qualcomm AI Engine Direct SDK which is not yet stable for general use.
 * TODO: Replace NnApiDelegate with GpuDelegate when tensorflow-lite-gpu is added.
 */
class QualcommTextEncoder(
    private val context: Context,
    private val tokenizer: Tokenizer,
    private val modelPath: String = "mobile_bert.tflite"
) : TextEncoder, AutoCloseable {

    override val mode: TextEncoderMode = TextEncoderMode.MODEL
    override val label: String = "Qualcomm MobileBERT (NNAPI)"

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null

    private val inputMaxLength = 128
    private val outputDimension = EMBEDDING_DIMENSION

    private val inputBuffer = ByteBuffer.allocateDirect(inputMaxLength * 4).apply {
        order(ByteOrder.nativeOrder())
    }

    private fun initializeInterpreter() {
        if (interpreter != null) return

        val modelBuffer = ModelLoader.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()

        try {
            nnApiDelegate = NnApiDelegate()
            options.addDelegate(nnApiDelegate)
        } catch (e: Throwable) {
            logWarning("NNAPI delegate unavailable, falling back to CPU", e)
            nnApiDelegate?.close()
            nnApiDelegate = null
        }

        val currentInterpreter = Interpreter(modelBuffer, options)
        require(currentInterpreter.inputTensorCount == EXPECTED_INPUT_TENSOR_COUNT) {
            "Unsupported text model signature: expected $EXPECTED_INPUT_TENSOR_COUNT input tensor, found ${currentInterpreter.inputTensorCount}"
        }
        require(currentInterpreter.outputTensorCount >= 1) {
            "Unsupported text model signature: no output tensors found"
        }

        val outputShape = currentInterpreter.getOutputTensor(0).shape()
        require(outputShape.isNotEmpty() && outputShape.last() == outputDimension) {
            "Unsupported text model output shape: ${outputShape.joinToString(prefix = "[", postfix = "]")}"
        }

        interpreter = currentInterpreter
    }

    override suspend fun encode(text: String): FloatArray = withContext(Dispatchers.Default) {
        if (text.isBlank()) {
            return@withContext FloatArray(outputDimension) { 0f }
        }

        try {
            initializeInterpreter()
            val tokens = tokenizer.tokenize(text)

            inputBuffer.clear()

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
        } catch (exception: Exception) {
            logWarning("Text model inference failed", exception)
            throw exception
        }
    }

    override fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }

    private fun logWarning(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable == null) {
                Log.w(TAG, message)
            } else {
                Log.w(TAG, message, throwable)
            }
        }.getOrElse {
            val suffix = throwable?.let { ": ${it.message}" }.orEmpty()
            System.err.println("$TAG: $message$suffix")
        }
    }

    private companion object {
        const val TAG = "QualcommTextEncoder"
        const val EXPECTED_INPUT_TENSOR_COUNT = 1
    }
}
