package com.example.snapbadgers

import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.snapbadgers.ui.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SongEmbeddingUiPerformanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun wakeScreen() {
        wakeScreenForTest()
    }

    @Test
    fun benchmarkNavigationToLibrary() {
        val startNs = SystemClock.elapsedRealtimeNanos()

        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Music Library").fetchSemanticsNodes().isNotEmpty()
        }

        val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000.0
        println("UI navigation latency (library tab): ${"%.3f".format(elapsedMs)} ms")

        assertTrue(
            "Navigation to library is too slow: ${"%.3f".format(elapsedMs)} ms",
            elapsedMs < 5_000.0
        )
    }
}
