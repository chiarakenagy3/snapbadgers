package com.example.snapbadgers.songembeddings.embedding

import android.content.Context
import android.util.Log
import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

object MLPProjector {
    private var interpreter: Interpreter? = null

    private const val INPUT_DIM = 15
    private const val HIDDEN_DIM = 64
    private const val OUTPUT_DIM = 128
    private const val SEED = 42L

    private val weights1 = FloatArray(INPUT_DIM * HIDDEN_DIM)
    private val bias1 = FloatArray(HIDDEN_DIM)
    private val weights2 = FloatArray(HIDDEN_DIM * OUTPUT_DIM)
    private val bias2 = FloatArray(OUTPUT_DIM)

    init {
        val random = Random(SEED)
        val scale1 = sqrt(2.0f / (INPUT_DIM + HIDDEN_DIM))
        for (i in weights1.indices) weights1[i] = (random.nextFloat() * 2 - 1) * scale1
        val scale2 = sqrt(2.0f / (HIDDEN_DIM + OUTPUT_DIM))
        for (i in weights2.indices) weights2[i] = (random.nextFloat() * 2 - 1) * scale2
    }

    fun init(context: Context, modelPath: String = "mlp_quantized.tflite") {
        try {
            Log.d("MLP", "init() called")
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val modelBuffer: MappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }

            interpreter = Interpreter(modelBuffer, options)
            debugModel(interpreter!!)
        } catch (e: Exception) {
            Log.e("MLP", "Model load failed", e)
        }
    }

    fun debugModel(interpreter: Interpreter) {
        val input = interpreter.getInputTensor(0)
        val output = interpreter.getOutputTensor(0)

        Log.d("MLP", "Input type: ${input.dataType()}")
        Log.d("MLP", "Input shape: ${input.shape().contentToString()}")
        val iq = input.quantizationParams()
        Log.d("MLP", "Input scale: ${iq.scale}, zeroPoint: ${iq.zeroPoint}")

        Log.d("MLP", "Output type: ${output.dataType()}")
        Log.d("MLP", "Output shape: ${output.shape().contentToString()}")
        val oq = output.quantizationParams()
        Log.d("MLP", "Output scale: ${oq.scale}, zeroPoint: ${oq.zeroPoint}")
    }

    fun project(input: FloatArray): FloatArray {
        require(input.size == INPUT_DIM) {
            "Expected input size $INPUT_DIM, got ${input.size}"
        }

        val interp = interpreter ?: return manualProject(input)

        return try {
            runQuantizedOrFloat(interp, input)
        } catch (e: Exception) {
            Log.e("MLP", "TFLite inference failed, fallback to manualProject()", e)
            manualProject(input)
        }
    }

    private fun runQuantizedOrFloat(interpreter: Interpreter, input: FloatArray): FloatArray {
        val inTensor = interpreter.getInputTensor(0)
        val outTensor = interpreter.getOutputTensor(0)

        val inShape = inTensor.shape()
        val outShape = outTensor.shape()

        require(inShape.isNotEmpty()) { "Input tensor shape is empty" }
        require(outShape.isNotEmpty()) { "Output tensor shape is empty" }

        val expectedInputElems = inShape.reduce { a, b -> a * b }
        require(expectedInputElems == input.size) {
            "Model expects $expectedInputElems input values, but got ${input.size}. shape=${inShape.contentToString()}"
        }

        val inputBuffer = when (inTensor.dataType()) {
            DataType.INT8 -> quantizeInt8Input(
                input,
                inTensor.quantizationParams().scale,
                inTensor.quantizationParams().zeroPoint
            )
            DataType.UINT8 -> quantizeUInt8Input(
                input,
                inTensor.quantizationParams().scale,
                inTensor.quantizationParams().zeroPoint
            )
            DataType.FLOAT32 -> floatInputBuffer(input)
            else -> error("Unsupported input tensor type: ${inTensor.dataType()}")
        }

        val outElems = outShape.reduce { a, b -> a * b }
        val outputBuffer = ByteBuffer.allocateDirect(
            when (outTensor.dataType()) {
                DataType.INT8, DataType.UINT8 -> outElems
                DataType.FLOAT32 -> outElems * 4
                else -> error("Unsupported output tensor type: ${outTensor.dataType()}")
            }
        ).order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        return when (outTensor.dataType()) {
            DataType.INT8 -> dequantizeInt8Output(
                outputBuffer,
                outElems,
                outTensor.quantizationParams().scale,
                outTensor.quantizationParams().zeroPoint
            )
            DataType.UINT8 -> dequantizeUInt8Output(
                outputBuffer,
                outElems,
                outTensor.quantizationParams().scale,
                outTensor.quantizationParams().zeroPoint
            )
            DataType.FLOAT32 -> {
                FloatArray(outElems) { outputBuffer.float }
            }
            else -> error("Unsupported output tensor type: ${outTensor.dataType()}")
        }
    }

    private fun floatInputBuffer(src: FloatArray): ByteBuffer {
        val bb = ByteBuffer.allocateDirect(src.size * 4).order(ByteOrder.nativeOrder())
        for (v in src) bb.putFloat(v)
        bb.rewind()
        return bb
    }

    private fun quantizeInt8Input(src: FloatArray, scale: Float, zeroPoint: Int): ByteBuffer {
        require(scale != 0f) { "Input tensor scale is 0; this tensor may not be quantized." }
        val bb = ByteBuffer.allocateDirect(src.size).order(ByteOrder.nativeOrder())

        for (x in src) {
            val q = (x / scale).roundToInt() + zeroPoint
            bb.put(q.coerceIn(-128, 127).toByte())
        }

        bb.rewind()
        return bb
    }

    private fun quantizeUInt8Input(src: FloatArray, scale: Float, zeroPoint: Int): ByteBuffer {
        require(scale != 0f) { "Input tensor scale is 0; this tensor may not be quantized." }
        val bb = ByteBuffer.allocateDirect(src.size).order(ByteOrder.nativeOrder())

        for (x in src) {
            val q = (x / scale).roundToInt() + zeroPoint
            bb.put((q.coerceIn(0, 255) and 0xFF).toByte())
        }

        bb.rewind()
        return bb
    }

    private fun dequantizeInt8Output(
        buffer: ByteBuffer,
        size: Int,
        scale: Float,
        zeroPoint: Int
    ): FloatArray {
        require(scale != 0f) { "Output tensor scale is 0; this tensor may not be quantized." }
        val out = FloatArray(size)

        for (i in 0 until size) {
            val q = buffer.get().toInt() // signed int8
            out[i] = scale * (q - zeroPoint)
        }

        return out
    }

    private fun dequantizeUInt8Output(
        buffer: ByteBuffer,
        size: Int,
        scale: Float,
        zeroPoint: Int
    ): FloatArray {
        require(scale != 0f) { "Output tensor scale is 0; this tensor may not be quantized." }
        val out = FloatArray(size)

        for (i in 0 until size) {
            val q = buffer.get().toInt() and 0xFF
            out[i] = scale * (q - zeroPoint)
        }

        return out
    }

    private fun manualProject(input: FloatArray): FloatArray {
        val hidden = FloatArray(HIDDEN_DIM)
        for (j in 0 until HIDDEN_DIM) {
            var sum = bias1[j]
            for (i in 0 until INPUT_DIM) {
                sum += input[i] * weights1[i * HIDDEN_DIM + j]
            }
            hidden[j] = if (sum > 0) sum else 0f
        }

        val output = FloatArray(OUTPUT_DIM)
        for (k in 0 until OUTPUT_DIM) {
            var sum = bias2[k]
            for (j in 0 until HIDDEN_DIM) {
                sum += hidden[j] * weights2[j * OUTPUT_DIM + k]
            }
            output[k] = sum
        }
        return output
    }
}

fun buildBaseVector(f: AudioFeatures): FloatArray {
    return floatArrayOf(
        f.danceability,
        f.energy,
        f.speechiness,
        f.acousticness,
        f.instrumentalness,
        f.liveness,
        f.valence,
        (f.tempo / 200f).coerceIn(0f, 1f),
        (f.loudness + 60f).coerceIn(0f, 60f) / 60f,
        (f.duration_ms / 300000f).coerceIn(0f, 2f)
    )
}

fun addDerivedFeatures(base: FloatArray): FloatArray {
    val dance = base[0]
    val energy = base[1]
    val acoustic = base[3]
    val live = base[5]
    val valence = base[6]
    val tempo = base[7]

    return floatArrayOf(
        dance * energy,
        valence * energy,
        acoustic * (1f - energy),
        tempo * energy,
        live * valence
    )
}

fun getEmbedding(features: AudioFeatures?): FloatArray {
    if (features == null) return FloatArray(128) { 0f }

    val base = buildBaseVector(features)
    val derived = addDerivedFeatures(base)
    val combined = base + derived

    var embedding = MLPProjector.project(combined)
    embedding = normalize(embedding)

    return embedding
}

fun normalize(vec: FloatArray): FloatArray {
    var sum = 0f
    for (v in vec) sum += v * v
    val norm = sqrt(sum)
    if (norm < 1e-8f) return FloatArray(vec.size) { 0f }
    return FloatArray(vec.size) { i -> vec[i] / norm }
}
