package com.example.snapbadgers.ai.vision.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.vision.domain.VisionEncoder
import com.example.snapbadgers.ai.text.ml.ModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

class QualcommVisionEncoder(
    private val context: Context,
    private val imageProcessor: ImageProcessor = AndroidImageProcessor(),
    private val modelPath: String = "efficientnet_b0_128d_int8.tflite"
) : VisionEncoder, AutoCloseable {

    companion object {
        private const val INPUT_SIZE = 224
        private const val OUTPUT_DIM = 128
        
        // ImageNet normalization constants
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null

    private fun initializeInterpreter() {
        if (interpreter != null) return
        
        try {
            val options = Interpreter.Options()
            // NNAPI delegate for NPU acceleration on Snapdragon
            nnApiDelegate = NnApiDelegate()
            options.addDelegate(nnApiDelegate)
            
            val modelBuffer = ModelLoader.loadMappedFile(context, modelPath)
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Throwable) {
            // Catch LinkageError/UnsatisfiedLinkError on JVM
            e.printStackTrace()
        }
    }

    override suspend fun encode(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        try {
            initializeInterpreter()

            val resizedBitmap = imageProcessor.scale(bitmap, INPUT_SIZE, INPUT_SIZE)
            val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Preprocess image (Resize -> Normalize -> ByteBuffer)
            for (y in 0 until INPUT_SIZE) {
                for (x in 0 until INPUT_SIZE) {
                    val pixel = resizedBitmap.getPixel(x, y)

                    // Extract RGB components using bitwise operations for JVM compatibility
                    // Assuming ARGB_8888 format (standard for Bitmap.getPixel)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    
                    // Normalize to [0, 1] then apply ImageNet stats
                    inputBuffer.putFloat(((r / 255f) - MEAN[0]) / STD[0])
                    inputBuffer.putFloat(((g / 255f) - MEAN[1]) / STD[1])
                    inputBuffer.putFloat(((b / 255f) - MEAN[2]) / STD[2])
                }
            }
            inputBuffer.rewind()

            val outputBuffer = Array(1) { FloatArray(OUTPUT_DIM) }
            val currentInterpreter = interpreter ?: throw IllegalStateException("Interpreter not initialized")
            currentInterpreter.run(inputBuffer, outputBuffer)

            // Align to the same unit hypersphere
            return@withContext VectorUtils.normalize(outputBuffer[0])
        } catch (e: Throwable) {
            e.printStackTrace()
            // Fallback to zero vector on error
            FloatArray(OUTPUT_DIM) { 0f }
        }
    }

    override fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }
}
