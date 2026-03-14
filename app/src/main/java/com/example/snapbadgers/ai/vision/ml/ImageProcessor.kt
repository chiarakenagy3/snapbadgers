package com.example.snapbadgers.ai.vision.ml

import android.graphics.Bitmap

interface ImageProcessor {
    fun scale(bitmap: Bitmap, width: Int, height: Int): Bitmap
}

class AndroidImageProcessor : ImageProcessor {
    override fun scale(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
