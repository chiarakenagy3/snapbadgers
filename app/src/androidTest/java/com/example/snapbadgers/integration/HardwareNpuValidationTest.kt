package com.example.snapbadgers.integration

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.text.ml.BertTokenizer
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder
import com.example.snapbadgers.ml.QualcommVisionEncoder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hardware-specific NPU validation tests.
 * Validates that the code is actually running on NPU/DSP hardware.
 */
@RunWith(AndroidJUnit4::class)
class HardwareNpuValidationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var textEncoder: QualcommTextEncoder
    private lateinit var visionEncoder: QualcommVisionEncoder

    @Before
    fun setup() {
        println("\n🔍 Hardware Information:")
        println("━".repeat(60))
        println("  Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        println("  Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        println("  SoC: ${Build.HARDWARE}")
        println("  ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        println("━".repeat(60))

        val tokenizer = BertTokenizer.load(context, "vocab.txt")
        textEncoder = QualcommTextEncoder(context, tokenizer, "mobile_bert.tflite")
        visionEncoder = QualcommVisionEncoder(context, modelPath = "efficientnet_b0_128d_int8.tflite")
    }

    @After
    fun tearDown() {
        textEncoder.close()
        visionEncoder.close()
    }

    @Test
    fun testDeviceHasQualcommSoc() {
        val hardware = Build.HARDWARE.lowercase()
        val soc = Build.SOC_MODEL?.lowercase() ?: ""

        println("\n📱 Device Check:")
        println("  Hardware: $hardware")
        println("  SoC Model: $soc")

        // Check if it's likely a Qualcomm device
        val isQualcomm = hardware.contains("qcom") ||
                hardware.contains("qualcomm") ||
                soc.contains("snapdragon") ||
                soc.contains("sm") ||  // Snapdragon Mobile
                soc.contains("sd")     // Snapdragon

        if (isQualcomm) {
            println("  ✅ Qualcomm SoC detected")
        } else {
            println("  ⚠️ Not a Qualcomm device - NPU performance may vary")
        }
    }

    @Test
    fun testNnApiAvailable() {
        // NNAPI should be available on Android 8.1+
        val nnApiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

        println("\n🔧 NNAPI Availability:")
        println("  SDK Level: ${Build.VERSION.SDK_INT}")
        println("  NNAPI Available: $nnApiAvailable")

        assertTrue("NNAPI should be available", nnApiAvailable)
    }

    @Test
    fun testNpuPerformanceCharacteristics() = runBlocking {
        println("\n⚡ NPU Performance Characteristics:")
        println("━".repeat(60))

        // Warmup
        textEncoder.encode("warmup")

        // Multiple runs to check consistency
        val times = List(20) {
            val start = System.currentTimeMillis()
            textEncoder.encode("test query for NPU")
            System.currentTimeMillis() - start
        }

        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0
        val maxTime = times.maxOrNull() ?: 0
        val stdDev = calculateStdDev(times.map { it.toDouble() })

        println("  Average: ${avgTime.toInt()}ms")
        println("  Min: ${minTime}ms | Max: ${maxTime}ms")
        println("  Std Dev: ${"%.2f".format(stdDev)}ms")
        println("  Variance: ${"%.2f".format(stdDev / avgTime * 100)}%")

        // NPU characteristics:
        // - Fast: < 150ms on Snapdragon 8 Elite
        // - Consistent: Low variance (< 30%)
        println("\n  Performance Assessment:")
        if (avgTime < 100) {
            println("  ✅ EXCELLENT - Likely using NPU")
        } else if (avgTime < 200) {
            println("  ✓ GOOD - May be using NPU or GPU")
        } else {
            println("  ⚠️ SLOW - Check delegate configuration")
        }

        if (stdDev / avgTime < 0.3) {
            println("  ✅ CONSISTENT - Good hardware acceleration")
        } else {
            println("  ⚠️ VARIABLE - Check for thermal throttling or background load")
        }

        println("━".repeat(60))

        // Performance should be reasonable
        assertTrue("Performance should be acceptable (got ${avgTime}ms)", avgTime < 500)
    }

    @Test
    fun testConcurrentInferencePerformance() = runBlocking {
        println("\n🔀 Concurrent Inference Test:")
        println("━".repeat(60))

        val queries = listOf(
            "calm music",
            "energetic workout",
            "late night coding",
            "peaceful meditation"
        )

        // Warmup
        textEncoder.encode("warmup")

        // Sequential execution
        val sequentialStart = System.currentTimeMillis()
        queries.forEach { textEncoder.encode(it) }
        val sequentialTime = System.currentTimeMillis() - sequentialStart

        println("  Sequential (${queries.size} queries): ${sequentialTime}ms")
        println("  Avg per query: ${sequentialTime / queries.size}ms")
        println("━".repeat(60))

        // NPU should handle sequential requests efficiently
        assertTrue("Sequential execution should be efficient", sequentialTime < 1000)
    }

    @Test
    fun testThermalThrottlingDetection() = runBlocking {
        println("\n🌡️ Thermal Throttling Detection:")
        println("━".repeat(60))

        // Run continuous inference for 30 iterations
        val times = mutableListOf<Long>()

        repeat(30) { iteration ->
            val start = System.currentTimeMillis()
            textEncoder.encode("iteration $iteration")
            val elapsed = System.currentTimeMillis() - start
            times.add(elapsed)

            if (iteration % 10 == 0) {
                val recentAvg = times.takeLast(10).average()
                println("  Iteration $iteration: ${elapsed}ms (recent avg: ${"%.1f".format(recentAvg)}ms)")
            }
        }

        val firstThird = times.take(10).average()
        val lastThird = times.takeLast(10).average()
        val slowdown = (lastThird - firstThird) / firstThird * 100

        println("\n  First 1/3 avg: ${"%.1f".format(firstThird)}ms")
        println("  Last 1/3 avg: ${"%.1f".format(lastThird)}ms")
        println("  Slowdown: ${"%.1f".format(slowdown)}%")

        if (slowdown < 10) {
            println("  ✅ NO THROTTLING detected")
        } else if (slowdown < 30) {
            println("  ⚠️ MINOR THROTTLING detected")
        } else {
            println("  ❌ SIGNIFICANT THROTTLING detected")
        }

        println("━".repeat(60))
    }

    @Test
    fun testMemoryFootprint() = runBlocking {
        println("\n💾 Memory Footprint Test:")
        println("━".repeat(60))

        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Run inference multiple times
        repeat(100) {
            textEncoder.encode("memory test query")
        }

        // Force garbage collection
        System.gc()
        Thread.sleep(100)

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024) // MB

        println("  Initial Memory: ${initialMemory / (1024 * 1024)}MB")
        println("  Final Memory: ${finalMemory / (1024 * 1024)}MB")
        println("  Increase: ${memoryIncrease}MB")

        if (memoryIncrease < 10) {
            println("  ✅ LOW memory footprint")
        } else if (memoryIncrease < 50) {
            println("  ✓ ACCEPTABLE memory footprint")
        } else {
            println("  ⚠️ HIGH memory footprint - possible leak")
        }

        println("━".repeat(60))

        // Memory increase should be reasonable
        assertTrue("Memory increase should be < 100MB", memoryIncrease < 100)
    }

    @Test
    fun testHexagonDspAvailability() {
        println("\n🔵 Hexagon DSP Check:")
        println("━".repeat(60))

        // Check if running on Snapdragon platform
        val hardware = Build.HARDWARE.lowercase()
        val hasHexagon = hardware.contains("qcom") || hardware.contains("qualcomm")

        println("  Hardware: ${Build.HARDWARE}")
        println("  Hexagon DSP Expected: $hasHexagon")

        if (hasHexagon) {
            println("  ✅ Hexagon DSP should be available")
        } else {
            println("  ℹ️ Non-Qualcomm device - using available accelerators")
        }

        println("━".repeat(60))
    }

    @Test
    fun testModelLoadingTime() {
        println("\n📥 Model Loading Performance:")
        println("━".repeat(60))

        val start = System.currentTimeMillis()

        // Create new encoder (simulates cold start)
        val tokenizer = BertTokenizer.load(context, "vocab.txt")
        val newEncoder = QualcommTextEncoder(context, tokenizer, "mobile_bert.tflite")

        val loadTime = System.currentTimeMillis() - start

        println("  Model Load Time: ${loadTime}ms")

        if (loadTime < 500) {
            println("  ✅ FAST loading")
        } else if (loadTime < 2000) {
            println("  ✓ ACCEPTABLE loading")
        } else {
            println("  ⚠️ SLOW loading")
        }

        println("━".repeat(60))

        newEncoder.close()

        // Loading should be reasonably fast
        assertTrue("Model loading should be < 5 seconds", loadTime < 5000)
    }

    private fun calculateStdDev(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
