package com.example.snapbadgers.ai.text.ml

import android.content.Context
import android.util.Log
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.text.domain.TextEncoder
import com.example.snapbadgers.ai.text.domain.Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate

class QualcommTextEncoder(
    private val context: Context,
    private val tokenizer: Tokenizer,
    private val modelPath: String = "mobilebert_embedder_128_drq.tflite"
) : TextEncoder, AutoCloseable {

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null

    private val seqLen = 384
    private val outputDim = 128

    private fun initializeInterpreter() {
        if (interpreter != null) return

        val modelBuffer = ModelLoader.loadMappedFile(context, modelPath)

        try {
            val options = Interpreter.Options()
            nnApiDelegate = NnApiDelegate()
            options.addDelegate(nnApiDelegate)

            interpreter = Interpreter(modelBuffer, options)
            Log.d("TextEncoder", "Model loaded with NNAPI")

        } catch (e: Exception) {

            Log.w("TextEncoder", "NNAPI failed, fallback CPU", e)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            interpreter = Interpreter(modelBuffer, options)
        }

        val t = interpreter!!.getOutputTensor(0)
        Log.d(
            "TextEncoder",
            "Output tensor name=${t.name()} shape=${t.shape().contentToString()} type=${t.dataType()}"
        )
    }

    override suspend fun encode(query: String): FloatArray = withContext(Dispatchers.Default) {

        if (query.isBlank()) {
            return@withContext FloatArray(outputDim)
        }

        try {

            initializeInterpreter()

            // --------------------------------------------------
            // 1. TOKENIZE
            // --------------------------------------------------

            val tokens = tokenizer.tokenize(query)

            Log.d(
                "TextEncoder",
                "Input: '$query' -> tokens(${tokens.size})=${tokens.take(16)}"
            )

            // --------------------------------------------------
            // 2. BUILD INPUT TENSORS
            // --------------------------------------------------

            val ids = Array(1) { IntArray(seqLen) }
            val mask = Array(1) { IntArray(seqLen) }
            val segmentIds = Array(1) { IntArray(seqLen) }

            for (i in 0 until seqLen) {

                if (i < tokens.size) {
                    ids[0][i] = tokens[i]
                    mask[0][i] = 1
                } else {
                    ids[0][i] = 0
                    mask[0][i] = 0
                }

                segmentIds[0][i] = 0
            }

            Log.d(
                "TextEncoder",
                "ids head=${ids[0].take(12)}"
            )

            Log.d(
                "TextEncoder",
                "mask head=${mask[0].take(12)}"
            )

            // --------------------------------------------------
            // 3. RUN INFERENCE
            // --------------------------------------------------

            val output = Array(1) { FloatArray(outputDim) }

            val inputs = arrayOf(ids, segmentIds, mask)
            val outputs = mutableMapOf<Int, Any>(0 to output)

            interpreter!!.runForMultipleInputsOutputs(inputs, outputs)

            val raw = output[0]

            // --------------------------------------------------
            // 4. RAW VECTOR STATS
            // --------------------------------------------------

            val mean = raw.average().toFloat()
            val min = raw.minOrNull() ?: 0f
            val max = raw.maxOrNull() ?: 0f

            val variance =
                raw.map { (it - mean) * (it - mean) }.average().toFloat()

            val norm =
                kotlin.math.sqrt(raw.sumOf { (it * it).toDouble() }).toFloat()

            Log.d(
                "TextEncoder",
                "RAW mean=$mean min=$min max=$max var=$variance norm=$norm"
            )

            Log.d(
                "TextEncoder",
                "RAW head=${raw.take(12)}"
            )

            // --------------------------------------------------
            // 5. CHECK ZERO VECTOR
            // --------------------------------------------------

            if (norm < 1e-6f) {

                Log.e(
                    "TextEncoder",
                    "⚠️ RAW VECTOR IS ZERO"
                )
            }

            // --------------------------------------------------
            // 6. NORMALIZE
            // --------------------------------------------------

            val normed = VectorUtils.normalize(raw.clone())

            val nMean = normed.average().toFloat()

            val nVar =
                normed.map { (it - nMean) * (it - nMean) }.average().toFloat()

            val nNorm =
                kotlin.math.sqrt(normed.sumOf { (it * it).toDouble() }).toFloat()

            Log.d(
                "TextEncoder",
                "NORM mean=$nMean var=$nVar norm=$nNorm"
            )

            Log.d(
                "TextEncoder",
                "NORM head=${normed.take(12)}"
            )

            return@withContext normed

        } catch (e: Exception) {

            Log.e("TextEncoder", "Inference error", e)

            return@withContext FloatArray(outputDim)
        }
    }

    override fun close() {

        interpreter?.close()
        nnApiDelegate?.close()

        interpreter = null
        nnApiDelegate = null
    }
}