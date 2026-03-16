package com.example.snapbadgers.ai.sensor

import com.example.snapbadgers.ai.common.ml.VectorUtils
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

class SensorEncoder {

    fun encode(sample: SensorSample): FloatArray {
        val accelMagnitude = sqrt(
            sample.accelX * sample.accelX +
                    sample.accelY * sample.accelY +
                    sample.accelZ * sample.accelZ
        )
        val accelMagnitudeNorm = (accelMagnitude / 20f).coerceIn(0f, 1f)
        val lightNorm = (ln(sample.light + 1f) / ln(10_001f)).coerceIn(0f, 1f)
        val rawFeatures = floatArrayOf(
            accelMagnitudeNorm,
            (sample.accelX / 20f).coerceIn(-1f, 1f),
            (sample.accelY / 20f).coerceIn(-1f, 1f),
            (sample.accelZ / 20f).coerceIn(-1f, 1f),
            lightNorm,
            accelMagnitudeNorm * lightNorm,
            (abs(sample.accelX - sample.accelY) / 20f).coerceIn(0f, 1f),
            (abs(sample.accelY - sample.accelZ) / 20f).coerceIn(0f, 1f)
        )

        return VectorUtils.alignToEmbeddingDimension(rawFeatures, salt = 23)
    }
}
