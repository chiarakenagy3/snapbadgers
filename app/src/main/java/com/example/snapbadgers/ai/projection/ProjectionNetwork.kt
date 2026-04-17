package com.example.snapbadgers.ai.projection

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.TwoLayerMLP

/**
 * Maps the 128-d fused context embedding into the song embedding space
 * so that cosine similarity against song embeddings is meaningful.
 *
 * Architecture:
 *   Input:  128-d fused context embedding (output of FusionEngine)
 *   Hidden: 128 units, ReLU activation
 *   Output: 128-d projected embedding, L2-normalized
 *
 * Weights are He-initialized (Uniform) for demonstration. In production, replace
 * initWeights() with loadWeights() using weights trained on
 * (fused embedding → target song embedding) pairs with a similarity loss.
 */
class ProjectionNetwork(
    inputDim: Int = EMBEDDING_DIMENSION,
    hiddenDim: Int = EMBEDDING_DIMENSION,
    outputDim: Int = EMBEDDING_DIMENSION
) : TwoLayerMLP(inputDim, hiddenDim, outputDim) {

    fun project(fusedEmbedding: FloatArray): FloatArray {
        require(fusedEmbedding.size == inputDim) {
            "Expected $inputDim-d input, got ${fusedEmbedding.size}-d. " +
            "Make sure FusionEngine output is passed directly."
        }
        return forward(fusedEmbedding)
    }

    override fun initWeights() {
        val rng = kotlin.random.Random(42)
        val scale1 = kotlin.math.sqrt(2.0 / inputDim).toFloat()
        val scale2 = kotlin.math.sqrt(2.0 / hiddenDim).toFloat()

        for (i in 0 until hiddenDim) {
            b1[i] = 0f
            for (j in 0 until inputDim)
                w1[i][j] = rng.nextFloat() * 2f * scale1 - scale1
        }
        for (i in 0 until outputDim) {
            b2[i] = 0f
            for (j in 0 until hiddenDim)
                w2[i][j] = rng.nextFloat() * 2f * scale2 - scale2
        }
    }
}
