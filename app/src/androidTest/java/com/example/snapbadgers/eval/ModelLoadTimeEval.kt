package com.example.snapbadgers.eval

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * ModelLoadTimeEval
 *
 * Measures model loading time (cold and warm) and memory impact
 * for each TFLite model in the app.
 *
 * Requires a device or emulator. Written for the Samsung Galaxy S25
 * (Snapdragon 8 Elite) but runs on any Android device/emulator.
 *
 * Results are logged to Log.i("EVAL", ...) for ADB capture:
 *   adb logcat -s EVAL
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ModelLoadTimeEval {

    companion object {
        private const val TAG = "EVAL"
        private val MODEL_ASSETS = listOf(
            "mobile_bert.tflite",
            "efficientnet_b0_128d_int8.tflite",
            "mlp_quantized.tflite"
        )
    }

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    private val loadedInterpreters = mutableListOf<Interpreter>()

    @Before
    fun setUp() {
        // Force GC before measurements
        System.gc()
        Thread.sleep(200)
    }

    @After
    fun tearDown() {
        loadedInterpreters.forEach { runCatching { it.close() } }
        loadedInterpreters.clear()
    }

    // ------------------------------------------------------------------
    // Cold load time
    // ------------------------------------------------------------------

    @Test
    fun coldLoadTimeForEachModel() {
        Log.i(TAG, "=== Model Cold Load Time Eval ===")

        for (modelAsset in MODEL_ASSETS) {
            if (!hasAsset(modelAsset)) {
                Log.i(TAG, "SKIP $modelAsset (not found in assets)")
                continue
            }

            System.gc()
            Thread.sleep(100)

            val memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val startNs = System.nanoTime()

            val interpreter = try {
                val buffer = loadMappedFile(modelAsset)
                Interpreter(buffer, Interpreter.Options().apply { setNumThreads(4) })
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL cold_load $modelAsset: ${e.message}")
                continue
            }

            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
            val memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memDeltaKb = (memAfter - memBefore) / 1024

            loadedInterpreters.add(interpreter)

            Log.i(TAG, "cold_load $modelAsset: time_ms=${"%.2f".format(elapsedMs)} mem_delta_kb=$memDeltaKb")
            Log.i(TAG, "  input_tensors=${interpreter.inputTensorCount} output_tensors=${interpreter.outputTensorCount}")

            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()
            Log.i(TAG, "  input_shape=${inputShape.contentToString()} output_shape=${outputShape.contentToString()}")

            assertTrue("Cold load should complete in under 30 seconds", elapsedMs < 30_000)
        }
    }

    // ------------------------------------------------------------------
    // Warm load (close and reopen)
    // ------------------------------------------------------------------

    @Test
    fun warmLoadTimeForEachModel() {
        Log.i(TAG, "=== Model Warm Load Time Eval ===")

        for (modelAsset in MODEL_ASSETS) {
            if (!hasAsset(modelAsset)) {
                Log.i(TAG, "SKIP $modelAsset (not found in assets)")
                continue
            }

            // First load to warm caches
            val buffer = try {
                loadMappedFile(modelAsset)
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL warm_load $modelAsset buffer: ${e.message}")
                continue
            }

            val warmUpInterpreter = try {
                Interpreter(buffer, Interpreter.Options().apply { setNumThreads(4) })
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL warm_load $modelAsset first: ${e.message}")
                continue
            }
            warmUpInterpreter.close()

            // Measure warm reload
            System.gc()
            Thread.sleep(50)

            val memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val startNs = System.nanoTime()

            val interpreter = try {
                val reloadBuffer = loadMappedFile(modelAsset)
                Interpreter(reloadBuffer, Interpreter.Options().apply { setNumThreads(4) })
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL warm_load $modelAsset reload: ${e.message}")
                continue
            }

            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
            val memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memDeltaKb = (memAfter - memBefore) / 1024

            loadedInterpreters.add(interpreter)

            Log.i(TAG, "warm_load $modelAsset: time_ms=${"%.2f".format(elapsedMs)} mem_delta_kb=$memDeltaKb")
            assertTrue("Warm load should complete in under 15 seconds", elapsedMs < 15_000)
        }
    }

    // ------------------------------------------------------------------
    // Memory delta per model
    // ------------------------------------------------------------------

    @Test
    fun memoryDeltaPerModel() {
        Log.i(TAG, "=== Memory Delta Per Model ===")

        for (modelAsset in MODEL_ASSETS) {
            if (!hasAsset(modelAsset)) {
                Log.i(TAG, "SKIP $modelAsset (not found in assets)")
                continue
            }

            System.gc()
            Thread.sleep(200)

            val runtime = Runtime.getRuntime()
            val memBefore = runtime.totalMemory() - runtime.freeMemory()

            val interpreter = try {
                val buffer = loadMappedFile(modelAsset)
                Interpreter(buffer, Interpreter.Options().apply { setNumThreads(4) })
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL mem_delta $modelAsset: ${e.message}")
                continue
            }

            val memAfterLoad = runtime.totalMemory() - runtime.freeMemory()
            val loadDeltaKb = (memAfterLoad - memBefore) / 1024

            // Run one inference to trigger any lazy allocation
            try {
                val inputTensor = interpreter.getInputTensor(0)
                val inputSize = inputTensor.shape().reduce { a, b -> a * b }
                val dummyInput = java.nio.ByteBuffer.allocateDirect(inputSize * 4)
                    .order(java.nio.ByteOrder.nativeOrder())
                val outputTensor = interpreter.getOutputTensor(0)
                val outputSize = outputTensor.shape().reduce { a, b -> a * b }
                val dummyOutput = java.nio.ByteBuffer.allocateDirect(outputSize * 4)
                    .order(java.nio.ByteOrder.nativeOrder())
                interpreter.run(dummyInput, dummyOutput)
            } catch (e: Throwable) {
                Log.w(TAG, "  inference warmup failed for $modelAsset: ${e.message}")
            }

            val memAfterInference = runtime.totalMemory() - runtime.freeMemory()
            val inferenceDeltaKb = (memAfterInference - memAfterLoad) / 1024
            val totalDeltaKb = (memAfterInference - memBefore) / 1024

            loadedInterpreters.add(interpreter)

            Log.i(TAG, "mem_delta $modelAsset:")
            Log.i(TAG, "  load_delta_kb=$loadDeltaKb inference_delta_kb=$inferenceDeltaKb total_delta_kb=$totalDeltaKb")
            Log.i(TAG, "  total_memory_mb=${runtime.totalMemory() / (1024 * 1024)} free_memory_mb=${runtime.freeMemory() / (1024 * 1024)}")
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun hasAsset(name: String): Boolean {
        return runCatching {
            context.assets.open(name).close()
            true
        }.getOrDefault(false)
    }

    private fun loadMappedFile(assetName: String): MappedByteBuffer {
        val fd = context.assets.openFd(assetName)
        val inputStream = FileInputStream(fd.fileDescriptor)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }
}
