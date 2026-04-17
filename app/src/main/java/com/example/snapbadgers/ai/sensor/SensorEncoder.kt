package com.example.snapbadgers.ai.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import java.util.Calendar
import java.util.LinkedList

/**
 * SensorEncoder
 *
 * Responsibilities:
 *  1. Reads live sensor data from Android (accelerometer, light, GPS, system clock)
 *  2. Maintains a rolling window of accelerometer readings for statistical feature extraction
 *  3. Extracts 14-d feature vector via SensorFeatureExtractor
 *  4. Runs the tiny MLP to produce a 32-d embedding
 *
 * Usage:
 *   val encoder = SensorEncoder(context)
 *   encoder.start()                        // begin listening to sensors
 *   val embedding = encoder.getEmbedding() // call anytime to get latest 32-d vector
 *   encoder.stop()                         // release sensor listeners
 */
class SensorEncoder(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mlp = SensorEncoderMLP()

    // Latest raw readings (updated by listeners)
    private var latestLux: Float = 0f
    private var latestLat: Double = 0.0
    private var latestLon: Double = 0.0

    // Rolling window of accelerometer readings: ~20 samples at SENSOR_DELAY_NORMAL ≈ 5 s
    private val accelWindow = LinkedList<FloatArray>()

    private companion object {
        const val WINDOW_SIZE = 20
    }

    /** Start collecting sensor data. Call in onResume() or when app is active. */
    fun start() {
        registerAccelerometer()
        registerLightSensor()
        requestLocationUpdates()
    }

    /** Stop collecting sensor data. Call in onPause() to save battery. */
    fun stop() {
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(lightListener)
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: SecurityException) {
            // Permission was revoked at runtime
        }
    }

    /**
     * Returns the latest 32-d sensor embedding.
     * Safe to call from any thread — reads last known sensor values.
     */
    fun getEmbedding(): FloatArray {
        val features = SensorFeatureExtractor.extract(buildSensorData())
        return mlp.encode(features)
    }

    /**
     * Returns current SensorData snapshot (useful for debugging / logging).
     */
    fun getCurrentSensorData(): SensorData = buildSensorData()

    /**
     * Returns extracted 14-d feature vector before MLP (useful for debugging).
     */
    fun getCurrentFeatures(): FloatArray =
        SensorFeatureExtractor.extract(buildSensorData())

    private fun buildSensorData(): SensorData = SensorData(
        accelWindow = synchronized(accelWindow) { accelWindow.map { it.copyOf() } },
        lightLux    = latestLux,
        hourOfDay   = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        latitude    = latestLat,
        longitude   = latestLon
    )

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val sample = floatArrayOf(event.values[0], event.values[1], event.values[2])
                synchronized(accelWindow) {
                    accelWindow.addLast(sample)
                    if (accelWindow.size > WINDOW_SIZE) accelWindow.removeFirst()
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun registerAccelerometer() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(
                accelListener,
                accel,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                latestLux = event.values[0]
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun registerLightSensor() {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor != null) {
            sensorManager.registerListener(
                lightListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latestLat = location.latitude
            latestLon = location.longitude
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun requestLocationUpdates() {
        try {
            // Try GPS first, fall back to network
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> null
            }
            provider?.let {
                locationManager.requestLocationUpdates(
                    it,
                    60_000L,  // min time: 60 seconds (battery-friendly)
                    50f,      // min distance: 50 metres
                    locationListener
                )
                // Seed with last known location immediately
                locationManager.getLastKnownLocation(it)?.let { loc ->
                    latestLat = loc.latitude
                    latestLon = loc.longitude
                }
            }
        } catch (e: SecurityException) {
            // ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION not granted
            // Embedding will use (0, 0) for location — still functional
        }
    }
}

