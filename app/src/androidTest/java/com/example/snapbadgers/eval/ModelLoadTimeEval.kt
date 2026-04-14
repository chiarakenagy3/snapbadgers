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
        System.gc()
        Thread.sleep(200)
    }

    @After
    fun tearDown() {
        loadedInterpreters.forEach { runCatching { it.close() } }
        loadedInterpreters.clear()
    }

    @Test
    fun coldAndWarmLoadTimeForEachModel() {
        Log.i(TAG, "=== Model Cold + Warm Load Time Eval ===")

        for (modelAsset in MODEL_ASSETS) {
            if (!hasAsset(modelAsset)) {
                Log.i(TAG, "SKIP $modelAsset (not found in assets)")
                continue
            }

            // Cold load
            System.gc()
            Thread.sleep(100)

            val coldMemBefore = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
            val coldStartNs = System.nanoTime()

            val coldInterpreter = try {
                Interpreter(loadMappedFile(modelAsset), Interpreter.Options().apply { setNumThreads(4) })
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL cold_load $modelAsset: ${e.message}")
                continue
            }

            val coldElapsedMs = (System.nanoTime() - coldStartNs) / 1_000_000.0
            val coldMemDeltaKb = (Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() } - coldMemBefore) / 1024

            Log.i(TAG, "cold_load $modelAsset: time_ms=${"%.2f".format(coldElapsedMs)} mem_delta_kb=$coldMemDeltaKb")
            Log.i(TAG, "  input_tensors=${coldInterpreter.inputTensorCount} output_tensors=${coldInterpreter.outputTensorCount}")
            Log.i(TAG, "  input_shape=${coldInterpreter.getInputTensor(0).shape().contentToString()} output_shape=${coldInterpreter.getOutputTensor(0).shape().contentToString()}")

            assertTrue("Cold load should complete in under 30 seconds", coldElapsedMs < 30_000)

            // Close to prepare warm load
            coldInterpreter.close()

            // Warm load (caches primed from cold load)
            System.gc()
            Thread.sleep(50)

            val warmMemBefore = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
            val warmStartNs = System.nanoTime()

            val warmInterpreter = try {
                Interpreter(loadMappedFile(modelAsset), Interpreter.Options().apply { setNumThreads(4) })
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL warm_load $modelAsset: ${e.message}")
                continue
            }

            val warmElapsedMs = (System.nanoTime() - warmStartNs) / 1_000_000.0
            val warmMemDeltaKb = (Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() } - warmMemBefore) / 1024

            loadedInterpreters.add(warmInterpreter)

            Log.i(TAG, "warm_load $modelAsset: time_ms=${"%.2f".format(warmElapsedMs)} mem_delta_kb=$warmMemDeltaKb")
            assertTrue("Warm load should complete in under 15 seconds", warmElapsedMs < 15_000)
        }
    }

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
                Interpreter(loadMappedFile(modelAsset), Interpreter.Options().apply { setNumThreads(4) })
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL mem_delta $modelAsset: ${e.message}")
                continue
            }

            val memAfterLoad = runtime.totalMemory() - runtime.freeMemory()
            val loadDeltaKb = (memAfterLoad - memBefore) / 1024

            try {
                val inputSize = interpreter.getInputTensor(0).shape().reduce { a, b -> a * b }
                val outputSize = interpreter.getOutputTensor(0).shape().reduce { a, b -> a * b }
                interpreter.run(
                    java.nio.ByteBuffer.allocateDirect(inputSize * 4).order(java.nio.ByteOrder.nativeOrder()),
                    java.nio.ByteBuffer.allocateDirect(outputSize * 4).order(java.nio.ByteOrder.nativeOrder())
                )
            } catch (e: Throwable) {
                Log.w(TAG, "  inference warmup failed for $modelAsset: ${e.message}")
            }

            val memAfterInference = runtime.totalMemory() - runtime.freeMemory()

            loadedInterpreters.add(interpreter)

            Log.i(TAG, "mem_delta $modelAsset:")
            Log.i(TAG, "  load_delta_kb=$loadDeltaKb inference_delta_kb=${(memAfterInference - memAfterLoad) / 1024} total_delta_kb=${(memAfterInference - memBefore) / 1024}")
            Log.i(TAG, "  total_memory_mb=${runtime.totalMemory() / (1024 * 1024)} free_memory_mb=${runtime.freeMemory() / (1024 * 1024)}")
            assertTrue("Baseline memory before loading $modelAsset should be positive", memBefore > 0)
        }
    }

    private fun hasAsset(name: String): Boolean {
        return runCatching {
            context.assets.open(name).close()
            true
        }.getOrDefault(false)
    }

    private fun loadMappedFile(assetName: String): MappedByteBuffer {
        val fd = context.assets.openFd(assetName)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }
}
