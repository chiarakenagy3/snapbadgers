package com.example.snapbadgers.ai.sensor

import com.example.snapbadgers.ai.common.ml.TwoLayerMLP

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
 * Weights are He-initialized (Gaussian) for demonstration.
 * In production, replace initWeights() with weights loaded from a
 * trained .bin / .json asset file via loadWeights().
 */
class SensorEncoderMLP(
    inputDim: Int = 14,
    hiddenDim: Int = 32,
    outputDim: Int = 32
) : TwoLayerMLP(inputDim, hiddenDim, outputDim) {

    fun encode(features: FloatArray): FloatArray {
        require(features.size == inputDim) {
            "Expected $inputDim features, got ${features.size}"
        }
        return forward(features)
    }

    override fun initWeights() {
        val rng = java.util.Random(42)
        val scale1 = kotlin.math.sqrt(2.0 / inputDim).toFloat()
        val scale2 = kotlin.math.sqrt(2.0 / hiddenDim).toFloat()

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
