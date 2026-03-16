package com.example.snapbadgers.sensor

import kotlin.math.sqrt

/**
 * SensorFeatureExtractor
 *
 * Converts raw SensorData into a normalized float array suitable for MLP input.
 *
 * Output feature vector (14 dimensions):
 *  [0]  accelMean         – mean magnitude of accelerometer window
 *  [1]  accelVariance     – variance of magnitude (motion consistency)
 *  [2]  accelRMS          – root mean square of magnitude (motion energy)
 *  [3]  accelPeak         – peak magnitude in window (burst detection)
 *  [4]  lightNorm         – normalized lux (0–1, clamped at 10,000 lux)
 *  [5]  lightCategory     – 0=dark, 0.33=dim, 0.67=bright, 1=very bright
 *  [6]  timeNormSin       – sin encoding of hour (captures cyclical nature)
 *  [7]  timeNormCos       – cos encoding of hour
 *  [8]  timeCategory      – 0=night, 0.33=morning, 0.67=afternoon, 1=evening
 *  [9]  latNorm           – latitude normalized to [-1, 1]
 *  [10] lonNorm           – longitude normalized to [-1, 1]
 *  [11] hemisphereNorth   – 1 if northern hemisphere, 0 if southern
 *  [12] hemisphereEast    – 1 if eastern hemisphere, 0 if western
 *  [13] urbanProxy        – rough proxy for urban density via coordinate rounding variance
 */
object SensorFeatureExtractor {

    private const val MAX_LUX = 10_000f
    // Max expected acceleration magnitude (m/s²) for normalization
    // ~20 covers intense activity; gravity alone is ~9.8
    private const val MAX_ACCEL = 20f

    fun extract(data: SensorData): FloatArray {
        val features = FloatArray(14)

        // --- Accelerometer features (stats over rolling window of magnitudes) ---
        val magnitudes = data.accelWindow.map { sample ->
            sqrt(sample[0] * sample[0] + sample[1] * sample[1] + sample[2] * sample[2])
        }
        if (magnitudes.isEmpty()) {
            // No accelerometer data — default to gravity-level still reading
            val gravityNorm = (9.8f / MAX_ACCEL).coerceIn(0f, 1f)
            features[0] = gravityNorm  // mean
            features[1] = 0f           // variance
            features[2] = gravityNorm  // rms
            features[3] = gravityNorm  // peak
        } else {
            val mean = magnitudes.average().toFloat()
            val variance = magnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
            val rms = sqrt(magnitudes.map { it * it }.average().toFloat())
            val peak = magnitudes.max()

            features[0] = (mean     / MAX_ACCEL).coerceIn(0f, 1f)
            features[1] = (variance / (MAX_ACCEL * MAX_ACCEL)).coerceIn(0f, 1f)
            features[2] = (rms      / MAX_ACCEL).coerceIn(0f, 1f)
            features[3] = (peak     / MAX_ACCEL).coerceIn(0f, 1f)
        }

        // --- Light features ---
        val lightNorm = (data.lightLux / MAX_LUX).coerceIn(0f, 1f)
        features[4] = lightNorm
        features[5] = when {
            data.lightLux < 10f   -> 0.0f   // dark
            data.lightLux < 200f  -> 0.33f  // dim
            data.lightLux < 2000f -> 0.67f  // bright
            else                  -> 1.0f   // very bright (outdoors)
        }

        // --- Time features (cyclic encoding to avoid 23→0 discontinuity) ---
        val angleRad = (2.0 * Math.PI * data.hourOfDay / 24.0)
        features[6] = Math.sin(angleRad).toFloat()
        features[7] = Math.cos(angleRad).toFloat()
        features[8] = when (data.hourOfDay) {
            in 0..5   -> 0.0f   // night
            in 6..11  -> 0.33f  // morning
            in 12..17 -> 0.67f  // afternoon
            else      -> 1.0f   // evening
        }

        // --- GPS / Location features ---
        features[9]  = (data.latitude  / 90.0).toFloat().coerceIn(-1f, 1f)
        features[10] = (data.longitude / 180.0).toFloat().coerceIn(-1f, 1f)
        features[11] = if (data.latitude  >= 0) 1f else 0f
        features[12] = if (data.longitude >= 0) 1f else 0f

        // Urban proxy: fractional part variance of lat/lon as very rough density hint
        val latFrac = (data.latitude  - data.latitude.toInt()).toFloat()
        val lonFrac = (data.longitude - data.longitude.toInt()).toFloat()
        features[13] = sqrt((latFrac * latFrac + lonFrac * lonFrac) / 2f).coerceIn(0f, 1f)

        return features
    }

    /** Returns human-readable labels for each feature dimension (for debugging) */
    fun featureLabels(): List<String> = listOf(
        "accelMean", "accelVariance", "accelRMS", "accelPeak",
        "lightNorm", "lightCategory",
        "timeNormSin", "timeNormCos", "timeCategory",
        "latNorm", "lonNorm", "hemisphereNorth", "hemisphereEast",
        "urbanProxy"
    )
}
