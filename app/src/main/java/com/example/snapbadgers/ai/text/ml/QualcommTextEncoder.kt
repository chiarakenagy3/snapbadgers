package com.example.snapbadgers.ai.text.ml

import android.content.Context
import android.util.Log
import com.example.snapbadgers.ai.common.EncoderUtils
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

    // Resolved at init time by inspecting input tensor names, so we correctly route
    // ids/mask/segment buffers regardless of export order.
    private var inputIdsIndex = 0
    private var inputMaskIndex = 1
    private var segmentIdsIndex = 2

    private val inputMaxLength = 128
    private val outputDimension = EMBEDDING_DIMENSION

    private val inputIdsBuffer = ByteBuffer.allocateDirect(inputMaxLength * 4)
        .apply { order(ByteOrder.nativeOrder()) }
    private val inputMaskBuffer = ByteBuffer.allocateDirect(inputMaxLength * 4)
        .apply { order(ByteOrder.nativeOrder()) }
    private val segmentIdsBuffer = ByteBuffer.allocateDirect(inputMaxLength * 4)
        .apply { order(ByteOrder.nativeOrder()) }

    private fun initializeInterpreter() {
        if (interpreter != null) return

        val modelBuffer = ModelLoader.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()

        try {
            nnApiDelegate = NnApiDelegate()
            options.addDelegate(nnApiDelegate)
        } catch (e: Throwable) {
            EncoderUtils.logWarning(TAG, "NNAPI delegate unavailable, falling back to CPU", e)
            nnApiDelegate?.close()
            nnApiDelegate = null
        }

        val currentInterpreter = Interpreter(modelBuffer, options)
        require(currentInterpreter.inputTensorCount == EXPECTED_INPUT_TENSOR_COUNT) {
            "Unsupported text model signature: expected $EXPECTED_INPUT_TENSOR_COUNT input tensors, found ${currentInterpreter.inputTensorCount}"
        }
        require(currentInterpreter.outputTensorCount >= 1) {
            "Unsupported text model signature: no output tensors found"
        }

        resolveInputOrder(currentInterpreter)

        val outputShape = currentInterpreter.getOutputTensor(0).shape()
        require(outputShape.isNotEmpty() && outputShape.last() == outputDimension) {
            "Unsupported text model output shape: ${outputShape.joinToString(prefix = "[", postfix = "]")}"
        }

        Log.i(
            TAG,
            "MobileBERT ready: inputs=[ids=$inputIdsIndex mask=$inputMaskIndex segment=$segmentIdsIndex] " +
                "output_shape=${outputShape.joinToString(prefix = "[", postfix = "]")}"
        )

        interpreter = currentInterpreter
    }

    // Different TFLite BERT exports use different input tensor orderings. Identify by
    // keyword in the tensor name (input_ids / input_mask or attention_mask / segment_ids
    // or token_type_ids). Falls back to conventional 0/1/2 order if names are opaque.
    private fun resolveInputOrder(currentInterpreter: Interpreter) {
        var idsIdx = -1
        var maskIdx = -1
        var segIdx = -1
        for (i in 0 until currentInterpreter.inputTensorCount) {
            val name = currentInterpreter.getInputTensor(i).name().lowercase()
            when {
                idsIdx < 0 && "ids" in name && "segment" !in name && "type" !in name -> idsIdx = i
                maskIdx < 0 && ("mask" in name || "attention" in name) -> maskIdx = i
                segIdx < 0 && ("segment" in name || "type" in name) -> segIdx = i
            }
        }
        if (idsIdx >= 0 && maskIdx >= 0 && segIdx >= 0) {
            inputIdsIndex = idsIdx
            inputMaskIndex = maskIdx
            segmentIdsIndex = segIdx
        } // otherwise leave default 0/1/2
    }

    override suspend fun encode(text: String): FloatArray = withContext(Dispatchers.Default) {
        if (text.isBlank()) {
            return@withContext FloatArray(outputDimension) { 0f }
        }

        try {
            initializeInterpreter()
            val rawTokens = tokenizer.tokenize(text)

            packBertInputs(rawTokens, inputIdsBuffer, inputMaskBuffer, segmentIdsBuffer, inputMaxLength)

            val currentInterpreter = interpreter!!
            val inputs = arrayOfNulls<Any>(currentInterpreter.inputTensorCount)
            inputs[inputIdsIndex] = inputIdsBuffer
            inputs[inputMaskIndex] = inputMaskBuffer
            inputs[segmentIdsIndex] = segmentIdsBuffer

            val outputShape = currentInterpreter.getOutputTensor(0).shape()
            val outputArray: Any = when (outputShape.size) {
                2 -> Array(outputShape[0]) { FloatArray(outputShape[1]) }
                3 -> Array(outputShape[0]) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }
                else -> error("Unexpected text model output rank: ${outputShape.size}")
            }

            val outputs = hashMapOf<Int, Any>(0 to outputArray)
            @Suppress("UNCHECKED_CAST")
            currentInterpreter.runForMultipleInputsOutputs(inputs as Array<Any>, outputs)

            @Suppress("UNCHECKED_CAST")
            val embedding = when (outputShape.size) {
                2 -> (outputArray as Array<FloatArray>)[0]
                // 3D output [batch, seq, hidden]: take the [CLS] position (index 0)
                3 -> (outputArray as Array<Array<FloatArray>>)[0][0]
                else -> error("unreachable")
            }

            return@withContext VectorUtils.normalize(embedding)
        } catch (exception: Exception) {
            EncoderUtils.logWarning(TAG, "Text model inference failed", exception)
            throw exception
        }
    }

    override fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }

    internal companion object {
        const val TAG = "QualcommTextEncoder"
        const val EXPECTED_INPUT_TENSOR_COUNT = 3
        // Standard BERT uncased vocab conventions (verified against app/src/main/assets/vocab.txt)
        const val CLS_TOKEN_ID = 101
        const val SEP_TOKEN_ID = 102
        const val PAD_TOKEN_ID = 0

        /**
         * Fills three int32 ByteBuffers with the standard BERT three-tensor input layout:
         * `input_ids` = [CLS] body… [SEP] [PAD]…, `input_mask` = 1 for real tokens / 0 for pad,
         * `segment_ids` = all 0 (single-sentence task).
         *
         * Body tokens are truncated to `maxLength - 2` to leave room for the CLS/SEP wrappers.
         * All three buffers are `rewind()`ed before return so they are ready to feed into
         * `Interpreter.runForMultipleInputsOutputs`.
         *
         * Extracted from `encode()` so the intricate packing logic can be unit tested without
         * loading a TFLite interpreter.
         */
        fun packBertInputs(
            rawTokens: IntArray,
            inputIdsBuffer: ByteBuffer,
            inputMaskBuffer: ByteBuffer,
            segmentIdsBuffer: ByteBuffer,
            maxLength: Int
        ) {
            val bodyMaxLen = maxLength - 2
            val bodyLen = minOf(rawTokens.size, bodyMaxLen)
            val realLen = bodyLen + 2

            inputIdsBuffer.clear()
            inputMaskBuffer.clear()
            segmentIdsBuffer.clear()

            inputIdsBuffer.putInt(CLS_TOKEN_ID)
            for (i in 0 until bodyLen) inputIdsBuffer.putInt(rawTokens[i])
            inputIdsBuffer.putInt(SEP_TOKEN_ID)
            repeat(maxLength - realLen) { inputIdsBuffer.putInt(PAD_TOKEN_ID) }

            repeat(realLen) { inputMaskBuffer.putInt(1) }
            repeat(maxLength - realLen) { inputMaskBuffer.putInt(0) }

            repeat(maxLength) { segmentIdsBuffer.putInt(0) }

            inputIdsBuffer.rewind()
            inputMaskBuffer.rewind()
            segmentIdsBuffer.rewind()
        }
    }
}
