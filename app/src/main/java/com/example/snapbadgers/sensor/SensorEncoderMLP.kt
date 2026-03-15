package com.example.snapbadgers.sensor

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * SensorEncoderMLP
 *
 * A tiny 2-layer MLP running entirely on-device (no framework needed).
 *
 * Architecture:
 *   Input:   14-d feature vector  (from SensorFeatureExtractor)
 *            [accelMean, accelVariance, accelRMS, accelPeak,
 *             lightNorm, lightCategory, timeNormSin, timeNormCos, timeCategory,
 *             latNorm, lonNorm, hemisphereNorth, hemisphereEast, urbanProxy]
 *   Hidden:  32 units, ReLU activation
 *   Output:  32-d embedding, L2-normalized
 *
 * Weights are initialized with He initialization for demonstration.
 * In production, replace initWeights() with weights loaded from a
 * trained .bin / .json asset file.
 */
class SensorEncoderMLP(
    private val inputDim: Int = 14,
    private val hiddenDim: Int = 32,
    private val outputDim: Int = 32
) {
    // Layer 1: [hiddenDim x inputDim] weights + [hiddenDim] bias
    private var w1: Array<FloatArray> = Array(hiddenDim) { FloatArray(inputDim) }
    private var b1: FloatArray = FloatArray(hiddenDim)

    // Layer 2: [outputDim x hiddenDim] weights + [outputDim] bias
    private var w2: Array<FloatArray> = Array(outputDim) { FloatArray(hiddenDim) }
    private var b2: FloatArray = FloatArray(outputDim)

    init {
        initWeights()
    }

    // ------------------------------------------------------------------
    // Forward pass
    // ------------------------------------------------------------------

    /**
     * Encodes sensor features into a 32-d L2-normalized embedding.
     * @param features Output of SensorFeatureExtractor.extract() — size 10
     * @return 32-d FloatArray embedding
     */
    fun encode(features: FloatArray): FloatArray {
        require(features.size == inputDim) {
            "Expected $inputDim features, got ${features.size}"
        }

        // Layer 1: linear + ReLU
        val hidden = FloatArray(hiddenDim)
        for (i in 0 until hiddenDim) {
            var sum = b1[i]
            for (j in 0 until inputDim) sum += w1[i][j] * features[j]
            hidden[i] = relu(sum)
        }

        // Layer 2: linear
        val output = FloatArray(outputDim)
        for (i in 0 until outputDim) {
            var sum = b2[i]
            for (j in 0 until hiddenDim) sum += w2[i][j] * hidden[j]
            output[i] = sum
        }

        return l2Normalize(output)
    }

    // ------------------------------------------------------------------
    // Weight loading
    // ------------------------------------------------------------------

    /**
     * Load weights from flat float arrays (e.g., deserialized from a .bin asset).
     *
     * Expected sizes:
     *   w1Flat : hiddenDim * inputDim  = 320
     *   b1Flat : hiddenDim             = 32
     *   w2Flat : outputDim * hiddenDim = 1024
     *   b2Flat : outputDim             = 32
     */
    fun loadWeights(
        w1Flat: FloatArray,
        b1Flat: FloatArray,
        w2Flat: FloatArray,
        b2Flat: FloatArray
    ) {
        for (i in 0 until hiddenDim)
            for (j in 0 until inputDim)
                w1[i][j] = w1Flat[i * inputDim + j]
        b1 = b1Flat.copyOf()

        for (i in 0 until outputDim)
            for (j in 0 until hiddenDim)
                w2[i][j] = w2Flat[i * hiddenDim + j]
        b2 = b2Flat.copyOf()
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun relu(x: Float) = if (x > 0f) x else 0f

    private fun l2Normalize(v: FloatArray): FloatArray {
        var norm = 0f
        for (x in v) norm += x * x
        norm = sqrt(norm)
        if (norm == 0f) return v
        return FloatArray(v.size) { v[it] / norm }
    }

    /**
     * He initialization — good default for ReLU networks.
     * Replace with loadWeights() once you have trained weights.
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