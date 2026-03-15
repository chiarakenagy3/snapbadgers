package com.example.snapbadgers.ai.vision

import android.graphics.Bitmap

class VisionEncoder {

    fun encode(bitmap: Bitmap): FloatArray {
        // TODO: Replace with real vision model inference
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        return floatArrayOf(
            width,
            height,
            width / (height.takeIf { it != 0f } ?: 1f)
        )
    }
}