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
    
    // Accelerometer — rolling window of recent readings (each entry is [x, y, z])
    // Window size: ~20 samples at SENSOR_DELAY_NORMAL (~5 seconds of motion history)
    
    val accelWindow: List<FloatArray> = emptyList(),

    // Light sensor (lux)
    
    val lightLux: Float = 0f,

    // Time of day (0–23 hour)
    
    val hourOfDay: Int = 0,

    // GPS
    
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
