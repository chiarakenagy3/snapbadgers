package com.example.snapbadgers.ai.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.snapbadgers.ai.text.ml.ModelLoader
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vision encoder backed by EfficientNet-B0, quantized to int8 for on-device inference.
 *
 * Delegate strategy: attempts NNAPI first for NPU/DSP acceleration, falls back to
 * CPU-only XNNPack if NNAPI is unavailable or fails.
 *
 * NOTE: [NnApiDelegate] is deprecated starting Android 15 (SDK 35). The recommended
 * migration path is QNN delegate → GPU delegate → XNNPack. QNN requires the
 * Qualcomm AI Engine Direct SDK which is not yet stable for general use.
 */
class QualcommVisionEncoder(
    private val context: Context,
    private val imageProcessor: ImageProcessor = AndroidImageProcessor(),
    private val modelPath: String = "efficientnet_b0_128d_int8.tflite"
) : AutoCloseable {

    companion object {
        private const val TAG = "QualcommVisionEncoder"
        private const val INPUT_SIZE = 224
        private const val OUTPUT_DIM = 128

        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null

    private val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }

    private fun initializeInterpreter() {
        if (interpreter != null) return

        val modelBuffer = ModelLoader.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()

        try {
            nnApiDelegate = NnApiDelegate()
            options.addDelegate(nnApiDelegate)
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "Interpreter initialized with NNAPI delegate")
        } catch (e: Throwable) {
            Log.w(TAG, "NNAPI delegate unavailable, falling back to CPU", e)
            nnApiDelegate?.close()
            nnApiDelegate = null
            interpreter = Interpreter(modelBuffer, Interpreter.Options())
        }
    }

    fun encode(bitmap: Bitmap): FloatArray {
        try {
            initializeInterpreter()

            val resizedBitmap = imageProcessor.scale(bitmap, INPUT_SIZE, INPUT_SIZE)
            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            resizedBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

            inputBuffer.clear()

            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                inputBuffer.putFloat(((r / 255f) - MEAN[0]) / STD[0])
                inputBuffer.putFloat(((g / 255f) - MEAN[1]) / STD[1])
                inputBuffer.putFloat(((b / 255f) - MEAN[2]) / STD[2])
            }
            inputBuffer.rewind()

            val outputBuffer = Array(1) { FloatArray(OUTPUT_DIM) }
            val currentInterpreter = interpreter ?: throw IllegalStateException("Interpreter not initialized")
            currentInterpreter.run(inputBuffer, outputBuffer)

            return outputBuffer[0]
        } catch (e: Throwable) {
            Log.e(TAG, "Vision encoding failed", e)
            return FloatArray(OUTPUT_DIM) { 0f }
        }
    }

    override fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }
}
