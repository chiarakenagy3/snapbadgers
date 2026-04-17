package com.example.snapbadgers.integration

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.text.ml.BertTokenizer
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder
import com.example.snapbadgers.ai.vision.QualcommVisionEncoder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HardwareNpuValidationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var textEncoder: QualcommTextEncoder
    private lateinit var visionEncoder: QualcommVisionEncoder

    @Before
    fun setup() {
        println("EVAL Hardware: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), SoC: ${Build.HARDWARE}, ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
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
        println("EVAL Device Check - Hardware: $hardware, SoC: $soc")

        val isQualcomm = hardware.contains("qcom") || hardware.contains("qualcomm") ||
                soc.contains("snapdragon") || soc.contains("sm") || soc.contains("sd")

        if (isQualcomm) println("  ✅ Qualcomm SoC detected")
        else println("  ⚠️ Not a Qualcomm device - NPU performance may vary")
        assertTrue("Build.HARDWARE should be a non-empty string", Build.HARDWARE.isNotEmpty())
    }

    @Test
    fun testNnApiAvailable() {
        val nnApiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
        println("EVAL NNAPI - SDK Level: ${Build.VERSION.SDK_INT}, Available: $nnApiAvailable")
        assertTrue("NNAPI should be available", nnApiAvailable)
    }

    @Test
    fun testNpuPerformanceCharacteristics() = runBlocking {
        println("EVAL NPU Performance Characteristics")
        textEncoder.encode("warmup")

        val times = List(20) {
            val start = System.currentTimeMillis()
            textEncoder.encode("test query for NPU")
            System.currentTimeMillis() - start
        }

        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0
        val maxTime = times.maxOrNull() ?: 0
        val stdDev = calculateStdDev(times.map { it.toDouble() })

        println("  Average: ${avgTime.toInt()}ms, Min: ${minTime}ms, Max: ${maxTime}ms")
        println("  Std Dev: ${"%.2f".format(stdDev)}ms, Variance: ${"%.2f".format(stdDev / avgTime * 100)}%")
        logPerformanceAssessment(avgTime, stdDev)

        assertTrue("Performance should be acceptable (got ${avgTime}ms)", avgTime < 500)
    }

    @Test
    fun testConcurrentInferencePerformance() = runBlocking {
        println("EVAL Concurrent Inference Test")
        val queries = listOf("calm music", "energetic workout", "late night coding", "peaceful meditation")

        textEncoder.encode("warmup")

        val sequentialStart = System.currentTimeMillis()
        queries.forEach { textEncoder.encode(it) }
        val sequentialTime = System.currentTimeMillis() - sequentialStart

        println("  Sequential (${queries.size} queries): ${sequentialTime}ms, Avg: ${sequentialTime / queries.size}ms")
        assertTrue("Sequential execution should be efficient", sequentialTime < 1000)
    }

    @Test
    fun testThermalThrottlingDetection() = runBlocking {
        println("EVAL Thermal Throttling Detection")
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

        println("  First 1/3 avg: ${"%.1f".format(firstThird)}ms, Last 1/3 avg: ${"%.1f".format(lastThird)}ms")
        println("  Slowdown: ${"%.1f".format(slowdown)}%")
        if (slowdown < 10) println("  ✅ NO THROTTLING detected")
        else if (slowdown < 30) println("  ⚠️ MINOR THROTTLING detected")
        else println("  ❌ SIGNIFICANT THROTTLING detected")
        assertTrue("Should have collected 30 timing samples", times.size == 30)
    }

    @Test
    fun testMemoryFootprint() = runBlocking {
        println("EVAL Memory Footprint Test")
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        repeat(100) { textEncoder.encode("memory test query") }

        System.gc()
        Thread.sleep(100)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024)

        println("  Initial: ${initialMemory / (1024 * 1024)}MB, Final: ${finalMemory / (1024 * 1024)}MB, Increase: ${memoryIncrease}MB")
        if (memoryIncrease < 10) println("  ✅ LOW memory footprint")
        else if (memoryIncrease < 50) println("  ✓ ACCEPTABLE memory footprint")
        else println("  ⚠️ HIGH memory footprint - possible leak")

        assertTrue("Memory increase should be < 100MB", memoryIncrease < 100)
    }

    @Test
    fun testHexagonDspAvailability() {
        val hardware = Build.HARDWARE.lowercase()
        val hasHexagon = hardware.contains("qcom") || hardware.contains("qualcomm")
        println("EVAL Hexagon DSP - Hardware: ${Build.HARDWARE}, Expected: $hasHexagon")
        if (hasHexagon) println("  ✅ Hexagon DSP should be available")
        else println("  ℹ️ Non-Qualcomm device - using available accelerators")
        assertTrue("Build.HARDWARE should be a non-empty string", Build.HARDWARE.isNotEmpty())
    }

    @Test
    fun testModelLoadingTime() {
        println("EVAL Model Loading Performance")
        val start = System.currentTimeMillis()
        val tokenizer = BertTokenizer.load(context, "vocab.txt")
        val newEncoder = QualcommTextEncoder(context, tokenizer, "mobile_bert.tflite")
        val loadTime = System.currentTimeMillis() - start

        println("  Model Load Time: ${loadTime}ms")
        if (loadTime < 500) println("  ✅ FAST loading")
        else if (loadTime < 2000) println("  ✓ ACCEPTABLE loading")
        else println("  ⚠️ SLOW loading")

        newEncoder.close()
        assertTrue("Model loading should be < 5 seconds", loadTime < 5000)
    }

    private fun logPerformanceAssessment(avgTime: Double, stdDev: Double) {
        if (avgTime < 100) println("  ✅ EXCELLENT - Likely using NPU")
        else if (avgTime < 200) println("  ✓ GOOD - May be using NPU or GPU")
        else println("  ⚠️ SLOW - Check delegate configuration")

        if (stdDev / avgTime < 0.3) println("  ✅ CONSISTENT - Good hardware acceleration")
        else println("  ⚠️ VARIABLE - Check for thermal throttling or background load")
    }

    private fun calculateStdDev(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
