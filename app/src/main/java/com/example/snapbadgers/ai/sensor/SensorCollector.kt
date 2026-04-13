package com.example.snapbadgers.ai.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

data class SensorSample(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val light: Float = 0f
)

class SensorCollector(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    @Volatile private var latestAccelX = 0f
    @Volatile private var latestAccelY = 0f
    @Volatile private var latestAccelZ = 0f
    @Volatile private var latestLight = 0f
    @Volatile private var isRegistered = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    latestAccelX = event.values[0]
                    latestAccelY = event.values[1]
                    latestAccelZ = event.values[2]
                }
                Sensor.TYPE_LIGHT -> {
                    latestLight = event.values[0]
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        if (isRegistered) return
        accelerometer?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        lightSensor?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        sensorManager.unregisterListener(listener)
        isRegistered = false
    }

    fun getLatestSample(): SensorSample {
        return SensorSample(
            accelX = latestAccelX,
            accelY = latestAccelY,
            accelZ = latestAccelZ,
            light = latestLight
        )
    }
}