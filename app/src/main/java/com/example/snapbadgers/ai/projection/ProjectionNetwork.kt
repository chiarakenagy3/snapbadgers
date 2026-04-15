package com.example.snapbadgers.ai.projection

import android.content.Context
import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * ProjectionNetwork
 *
 * Maps the 128-d fused context embedding (output of FusionEngine) into the
 * song embedding space so that cosine similarity against pre-computed song
 * embeddings is meaningful.
 *
 * Architecture (baked into projection_net.tflite):
 *   Input:  128-d fused context embedding
 *   Hidden: 128 units, ReLU activation
 *   Output: 128-d projected embedding, L2-normalized
 *
 * Inference runs on the Snapdragon NPU via the NNAPI delegate with CPU
 * fallback. The compiled .tflite asset is produced by:
 *   1. Training the MLP in PyTorch with cosine similarity loss
 *   2. Exporting to ONNX (opset 17)
 *   3. Compiling via Qualcomm AI Hub → downloads projection_net.tflite
 *   4. Placing projection_net.tflite in app/src/main/assets/
 *
 * Usage:
 *   val projectionNetwork = ProjectionNetwork(context)
 *   val projected = projectionNetwork.project(fusedEmbedding) // 128-d
 *   // ...
 *   projectionNetwork.close()
 */
class ProjectionNetwork(
    context: Context,
    private val inputDim: Int = EMBEDDING_DIMENSION,   // 128
    private val outputDim: Int = EMBEDDING_DIMENSION,  // 128
    assetFileName: String = "projection_net.tflite"
) {
    private val interpreter: Interpreter

    init {
        val opts = Interpreter.Options().apply {
            useNNAPI = true                      // route to Snapdragon NPU
            setAllowFp16PrecisionForFp32(true)   // fp16 where safe for speed
        }
        interpreter = Interpreter(loadModelFile(context, assetFileName), opts)
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Projects a fused context embedding into the song embedding space.
     *
     * L2 normalization is baked into the .tflite model as the final op,
     * so the returned vector is already a unit vector — do not normalize again.
     *
     * @param fusedEmbedding 128-d output from FusionEngine
     * @return 128-d L2-normalized projected embedding, ready for
     *         SongRepository.findTopSongs()
     */
    fun project(fusedEmbedding: FloatArray): FloatArray {
        require(fusedEmbedding.size == inputDim) {
            "Expected $inputDim-d input, got ${fusedEmbedding.size}-d. " +
            "Make sure FusionEngine output is passed directly."
        }
        val input  = Array(1) { fusedEmbedding }
        val output = Array(1) { FloatArray(outputDim) }
        interpreter.run(input, output)
        return output[0]
    }

    /**
     * Release the interpreter and NPU resources.
     * Call this when the enclosing ViewModel or component is destroyed.
     */
    fun close() = interpreter.close()

    // ------------------------------------------------------------------
    // Sanity check (call once during init/debug to verify the asset)
    // ------------------------------------------------------------------

    /**
     * Verifies that the loaded model produces a correctly shaped,
     * L2-normalized output. Logs a warning if normalization is off.
     *
     * Safe to call in a debug build or during ViewModel init:
     *   projectionNetwork.runSanityCheck()
     */
    fun runSanityCheck() {
        val fakeInput = FloatArray(inputDim) { 0.5f }
        val result = project(fakeInput)

        check(result.size == outputDim) {
            "Sanity check FAILED: output size ${result.size}, expected $outputDim"
        }

        val norm = Math.sqrt(result.map { it * it.toDouble() }.sum())
        if (norm !in 0.99..1.01) {
            android.util.Log.w(
                "ProjectionNetwork",
                "Output vector is not unit-normalized (norm=$norm). " +
                "If L2 norm is not baked into the .tflite, wrap output[0] " +
                "with VectorUtils.normalize() inside project()."
            )
        } else {
            android.util.Log.d("ProjectionNetwork", "Sanity check passed. Norm=$norm")
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun loadModelFile(context: Context, assetName: String): MappedByteBuffer {
        val afd = context.assets.openFd(assetName)
        return FileInputStream(afd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }
}