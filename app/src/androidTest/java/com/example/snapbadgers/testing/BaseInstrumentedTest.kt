package com.example.snapbadgers.testing

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

/**
 * Base class for all instrumented tests.
 *
 * Provides common setup, utilities, and helpers for Android instrumented tests.
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseInstrumentedTest {

    protected lateinit var context: Context

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    open fun tearDown() {
        // Cleanup if needed
    }

    /**
     * Create a test bitmap for vision encoding tests.
     */
    protected fun createTestBitmap(
        width: Int = 224,
        height: Int = 224,
        fill: String = "solid"
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        when (fill) {
            "solid" -> {
                // Fill with gray
                val pixels = IntArray(width * height) { 0xFF808080.toInt() }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            }
            "gradient" -> {
                // Create a gradient
                val pixels = IntArray(width * height) { i ->
                    val x = i % width
                    val y = i / width
                    val gray = ((x + y) * 255 / (width + height)).coerceIn(0, 255)
                    (0xFF000000 or (gray shl 16) or (gray shl 8) or gray).toInt()
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            }
            "checkerboard" -> {
                // Create checkerboard pattern
                val pixels = IntArray(width * height) { i ->
                    val x = i % width
                    val y = i / width
                    if ((x / 16 + y / 16) % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }

        return bitmap
    }

    /**
     * Assert that an embedding is properly normalized.
     */
    protected fun assertEmbeddingIsNormalized(embedding: FloatArray, tolerance: Float = 0.01f) {
        TestFixtures.assertEmbeddingIsNormalized(embedding, tolerance)
    }

    /**
     * Check if device has Qualcomm Snapdragon chipset.
     */
    protected fun isQualcommDevice(): Boolean {
        val socManufacturer = android.os.Build.HARDWARE.lowercase()
        return socManufacturer.contains("qualcomm") ||
               socManufacturer.contains("qcom") ||
               android.os.Build.BOARD.lowercase().contains("msm") ||
               android.os.Build.BOARD.lowercase().contains("sm8")
    }

    /**
     * Check if this is likely a Galaxy S25 or similar flagship.
     */
    protected fun isFlagshipDevice(): Boolean {
        val model = android.os.Build.MODEL.lowercase()
        return model.contains("s25") ||
               model.contains("s24") ||
               (android.os.Build.VERSION.SDK_INT >= 35 && isQualcommDevice())
    }

    /**
     * Log test info for debugging.
     */
    protected fun logDeviceInfo() {
        println("Device: ${android.os.Build.MODEL}")
        println("Manufacturer: ${android.os.Build.MANUFACTURER}")
        println("Hardware: ${android.os.Build.HARDWARE}")
        println("Board: ${android.os.Build.BOARD}")
        println("SDK: ${android.os.Build.VERSION.SDK_INT}")
        println("Qualcomm: ${isQualcommDevice()}")
        println("Flagship: ${isFlagshipDevice()}")
    }
}
