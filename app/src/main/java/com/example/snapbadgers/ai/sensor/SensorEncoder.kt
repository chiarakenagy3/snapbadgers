package com.example.snapbadgers.ai.sensor

import com.example.snapbadgers.ai.common.ml.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * SensorEncoder
 *
 * Maps physical sensor data (accelerometer, light) to the standard 
 * 128-d embedding space. This allows environmental context to 
 * influence the song recommendations.
 */
class SensorEncoder {

    fun encode(sample: SensorSample): FloatArray {
        val embedding = FloatArray(EMBEDDING_DIMENSION)

        // Calculate magnitude (standard gravity is ~9.8)
        val accelMagnitude = sqrt(
            sample.accelX * sample.accelX +
            sample.accelY * sample.accelY +
            sample.accelZ * sample.accelZ
        )
        
        // Normalize: 0 to 20 m/s^2 mapped to 0 to 1.0
        val accelMagnitudeNorm = (accelMagnitude / 20f).coerceIn(0f, 1f)
        
        // Log-scale light normalization (Lux can range from 0 to 100,000)
        val lightNorm = (ln(sample.light + 1f) / ln(10_001f)).coerceIn(0f, 1f)

        // --- Standard Feature Mapping ---

        // Motion -> Energy & Danceability
        // High movement suggests energetic or danceable music.
        embedding[IDX_ENERGY] = accelMagnitudeNorm
        embedding[IDX_DANCEABILITY] = (accelMagnitudeNorm * 1.2f).coerceIn(0f, 1f)
        embedding[IDX_TEMPO] = accelMagnitudeNorm

        // Light -> Valence & Acousticness
        // Bright light correlates with "sunny/happy" (high valence).
        // Low light (darkness) correlates with "acoustic/mellow" vibes.
        embedding[IDX_VALENCE] = lightNorm
        embedding[IDX_ACOUSTICNESS] = (1.0f - lightNorm).coerceIn(0f, 1f)

        // Stillness -> Instrumentalness
        // If the phone is very still (< 0.2G deviation from resting), 
        // it might imply a focused/study context.
        if (accelMagnitudeNorm < 0.15f) {
            embedding[IDX_INSTRUMENTALNESS] = 0.6f
        }

        // --- Raw Feature Backup (indices 10+) ---
        // We preserve raw component values in higher indices just in case the model
        // needs to distinguish between horizontal and vertical orientation.
        embedding[10] = (sample.accelX / 20f).coerceIn(-1f, 1f)
        embedding[11] = (sample.accelY / 20f).coerceIn(-1f, 1f)
        embedding[12] = (sample.accelZ / 20f).coerceIn(-1f, 1f)
        embedding[13] = abs(sample.accelX - sample.accelY) / 20f

        return VectorUtils.normalize(embedding)
    }
}
