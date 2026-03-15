package com.example.snapbadgers.ai.sensor

import kotlin.math.sqrt

class SensorEncoder {

    fun encode(sample: SensorSample): FloatArray {
        val accelMagnitude = sqrt(
            sample.accelX * sample.accelX +
                    sample.accelY * sample.accelY +
                    sample.accelZ * sample.accelZ
        )

        return floatArrayOf(
            accelMagnitude,
            sample.light
        )
    }
}