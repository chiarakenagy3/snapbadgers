package com.example.snapbadgers.domain

import android.graphics.Bitmap

interface VisionEncoder {
    /**
     * Encodes a bitmap image into a 128-dimensional embedding vector.
     * @param bitmap The input image to encode.
     * @return A float array representing the scene embedding.
     */
    suspend fun encode(bitmap: Bitmap): FloatArray
}
