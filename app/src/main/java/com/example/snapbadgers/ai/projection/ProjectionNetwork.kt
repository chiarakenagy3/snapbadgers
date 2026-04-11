package com.example.snapbadgers.ai.projection

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import kotlin.math.sqrt

/**
 * ProjectionNetwork
 *
 * Maps the 128-d fused context embedding into the song embedding space
 * so that cosine similarity against song embeddings is meaningful.
 *
 * Architecture:
 *   Input:  128-d fused context embedding (output of FusionEngine)
 *   Hidden: 128 units, ReLU activation
 *   Output: 128-d projected embedding, L2-normalized
 *
 * The output feeds directly into SongRepository.findTopSongs() which
 * applies alignToEmbeddingDimension (salt=101) then cosine similarity
 * against pre-computed 128-d song embeddings.
 *
 * Weights are He-initialized for demonstration. In production, replace
 * initWeights() with loadWeights() using weights trained on
 * (fused embedding → target song embedding) pairs with a similarity loss.
 *
 * Usage:
 *   val projectionNetwork = ProjectionNetwork()
 *   val projected = projectionNetwork.project(fusedEmbedding) // 128-d
 */
class ProjectionNetwork(
    private val inputDim: Int = EMBEDDING_DIMENSION,   // 128
    private val hiddenDim: Int = EMBEDDING_DIMENSION,  // 128
    private val outputDim: Int = EMBEDDING_DIMENSION   // 128
) {

    // Layer 1: [hiddenDim x inputDim]
    private val w1: Array<FloatArray> = Array(hiddenDim) { FloatArray(inputDim) }
    private val b1: FloatArray = FloatArray(hiddenDim)

    // Layer 2: [outputDim x hiddenDim]
    private val w2: Array<FloatArray> = Array(outputDim) { FloatArray(hiddenDim) }
    private val b2: FloatArray = FloatArray(outputDim)

    init {
        initWeights()
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Projects a fused context embedding into the song embedding space.
     *
     * @param fusedEmbedding 128-d output from FusionEngine
     * @return 128-d L2-normalized projected embedding ready for song ranking
     */
    fun project(fusedEmbedding: FloatArray): FloatArray {
        require(fusedEmbedding.size == inputDim) {
            "Expected $inputDim-d input, got ${fusedEmbedding.size}-d. " +
            "Make sure FusionEngine output is passed directly."
        }
        return mlpForward(fusedEmbedding)
    }

    // ------------------------------------------------------------------
    // Weight loading
    // ------------------------------------------------------------------

    /**
     * Load trained projection weights from flat float arrays.
     *
     * Expected sizes:
     *   w1Flat: hiddenDim * inputDim  = 128 * 128 = 16,384
     *   b1Flat: hiddenDim             = 128
     *   w2Flat: outputDim * hiddenDim = 128 * 128 = 16,384
     *   b2Flat: outputDim             = 128
     *
     * In production: load these from a trained .bin asset file.
     * Train using (fusedEmbedding, targetSongEmbedding) pairs
     * with cosine similarity loss.
     */
    fun loadWeights(
        w1Flat: FloatArray,
        b1Flat: FloatArray,
        w2Flat: FloatArray,
        b2Flat: FloatArray
    ) {
        require(w1Flat.size == hiddenDim * inputDim) { "w1 size mismatch" }
        require(b1Flat.size == hiddenDim)            { "b1 size mismatch" }
        require(w2Flat.size == outputDim * hiddenDim) { "w2 size mismatch" }
        require(b2Flat.size == outputDim)            { "b2 size mismatch" }

        for (i in 0 until hiddenDim)
            for (j in 0 until inputDim)
                w1[i][j] = w1Flat[i * inputDim + j]
        b1Flat.copyInto(b1)

        for (i in 0 until outputDim)
            for (j in 0 until hiddenDim)
                w2[i][j] = w2Flat[i * hiddenDim + j]
        b2Flat.copyInto(b2)
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** 2-layer MLP: Linear → ReLU → Linear → L2Norm */
    private fun mlpForward(input: FloatArray): FloatArray {
        // Layer 1: linear + ReLU
        val hidden = FloatArray(hiddenDim)
        for (i in 0 until hiddenDim) {
            var sum = b1[i]
            for (j in 0 until inputDim) sum += w1[i][j] * input[j]
            hidden[i] = relu(sum)
        }

        // Layer 2: linear
        val output = FloatArray(outputDim)
        for (i in 0 until outputDim) {
            var sum = b2[i]
            for (j in 0 until hiddenDim) sum += w2[i][j] * hidden[j]
            output[i] = sum
        }

        return VectorUtils.normalize(output)
    }

    private fun relu(x: Float) = if (x > 0f) x else 0f

    /**
     * He initialization — good default for ReLU networks.
     * Replace with loadWeights() once trained weights are available.
     */
    private fun initWeights() {
        val rng = java.util.Random(42)
        val scale1 = sqrt(2.0 / inputDim).toFloat()
        val scale2 = sqrt(2.0 / hiddenDim).toFloat()

        for (i in 0 until hiddenDim) {
            b1[i] = 0f
            for (j in 0 until inputDim)
                w1[i][j] = rng.nextGaussian().toFloat() * scale1
        }
        for (i in 0 until outputDim) {
            b2[i] = 0f
            for (j in 0 until hiddenDim)
                w2[i][j] = rng.nextGaussian().toFloat() * scale2
        }
    }
}