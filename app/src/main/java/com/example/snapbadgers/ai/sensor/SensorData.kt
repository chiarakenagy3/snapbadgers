package com.example.snapbadgers.ai.sensor

/**
 * Data container that holds raw and derived sensor inputs for the Sensor Encoder.
 *
 * Sensors used:
 *  - Accelerometer → rolling window of x, y, z readings
 *  - Light sensor  → lux reading
 *  - Time of day   → hour of day (0–23)
 *  - GPS/Location  → latitude, longitude (used to derive context bucket)
 */
data class SensorData(
    val accelWindow: List<FloatArray> = emptyList(),
    val lightLux: Float = 0f,
    val hourOfDay: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
