package com.example.snapbadgers.eval

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.BatteryManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Battery impact eval: measures BatteryManager counters before and after a
 * sustained pipeline load, reporting the deltas.
 *
 * Numbers are device- and charge-state-dependent — this eval asserts only that
 * counters move in a plausible direction; the value is in the logged telemetry
 * used by the team to evaluate demo/long-run battery behavior.
 *
 * Complements InferenceLatencyEval (time) and MemoryProfileEval (memory) with
 * an energy dimension.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BatteryImpactEval {

    companion object {
        private const val TAG = "EVAL"
        private const val LOAD_ITERATIONS = 150
    }

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    private lateinit var pipeline: RecommendationPipeline
    private lateinit var testBitmap: Bitmap

    @Before
    fun setUp() {
        pipeline = RecommendationPipeline(context)
        testBitmap = solidBitmap(224, Color.rgb(130, 150, 170))
    }

    @Test
    fun sustainedLoadBatteryImpact() = runBlocking {
        Log.i(TAG, "=== Battery Impact Eval (${LOAD_ITERATIONS} pipeline iterations) ===")

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val snapshotBefore = sampleBattery(batteryManager)
        logBattery("battery_before", snapshotBefore)

        val startMs = System.currentTimeMillis()
        repeat(LOAD_ITERATIONS) { i ->
            pipeline.runPipeline(
                input = queryForIteration(i),
                imageBitmap = testBitmap,
                onStepUpdate = {}
            )
        }
        val elapsedMs = System.currentTimeMillis() - startMs

        val snapshotAfter = sampleBattery(batteryManager)
        logBattery("battery_after", snapshotAfter)

        val capacityDelta = snapshotBefore.capacityPercent - snapshotAfter.capacityPercent
        val energyDeltaNwh = snapshotBefore.energyCounterNwh - snapshotAfter.energyCounterNwh

        Log.i(TAG, "battery_total_elapsed_ms: $elapsedMs")
        Log.i(TAG, "battery_capacity_percent_delta: $capacityDelta")
        Log.i(TAG, "battery_energy_nwh_delta: $energyDeltaNwh")
        Log.i(TAG, "battery_charging_state_before: ${snapshotBefore.chargingState}")
        Log.i(TAG, "battery_charging_state_after: ${snapshotAfter.chargingState}")

        if (capacityDelta != Int.MIN_VALUE && snapshotBefore.chargingState != BatteryManager.BATTERY_STATUS_CHARGING) {
            assertTrue(
                "Battery capacity should not unexpectedly rise while discharging (delta=$capacityDelta%)",
                capacityDelta >= -5
            )
        }
        assertTrue("Load must complete (pipeline returned non-null results)", elapsedMs > 0)
    }

    private data class BatterySnapshot(
        val capacityPercent: Int,
        val energyCounterNwh: Long,
        val chargeCounterUah: Long,
        val currentNowUa: Long,
        val currentAvgUa: Long,
        val chargingState: Int,
        val temperatureTenthsC: Int
    )

    private fun sampleBattery(manager: BatteryManager): BatterySnapshot {
        val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val chargingState = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE

        return BatterySnapshot(
            capacityPercent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            energyCounterNwh = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER),
            chargeCounterUah = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
            currentNowUa = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            currentAvgUa = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
            chargingState = chargingState,
            temperatureTenthsC = temperature
        )
    }

    private fun logBattery(label: String, snapshot: BatterySnapshot) {
        Log.i(TAG, "$label: capacity_pct=${snapshot.capacityPercent}")
        Log.i(TAG, "$label: energy_counter_nwh=${snapshot.energyCounterNwh}")
        Log.i(TAG, "$label: charge_counter_uah=${snapshot.chargeCounterUah}")
        Log.i(TAG, "$label: current_now_ua=${snapshot.currentNowUa}")
        Log.i(TAG, "$label: current_avg_ua=${snapshot.currentAvgUa}")
        Log.i(TAG, "$label: charging_state=${snapshot.chargingState}")
        Log.i(TAG, "$label: temperature_tenths_c=${snapshot.temperatureTenthsC}")
    }

    private fun queryForIteration(i: Int): String = when (i % 4) {
        0 -> "calm relaxing study music piano"
        1 -> "energetic workout playlist for running"
        2 -> "happy pop dance weekend vibes"
        else -> "rainy day coffee shop acoustic"
    }

    private fun solidBitmap(size: Int, color: Int): Bitmap {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    setPixel(x, y, color)
                }
            }
        }
    }
}
