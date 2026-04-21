package com.example.snapbadgers.integration

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import com.example.snapbadgers.ui.components.CameraInputCard
import com.example.snapbadgers.ui.i18n.AppI18n
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end integration for camera capture → pipeline wiring.
 *
 * Android's TakePicturePreview contract can't be triggered from a test (it spawns a
 * system camera Activity), so we exercise the hoisted-state contract directly:
 * the parent owns `capturedBitmap`; the card's `onBitmapCaptured` updates it; the
 * updated bitmap is fed straight to RecommendationPipeline.runPipeline.
 */
@RunWith(AndroidJUnit4::class)
class CameraCaptureIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings = AppI18n.forLanguage("English")

    @Test
    fun cameraCardEmptyStateDisablesClearButton() {
        composeTestRule.setContent {
            CameraInputCard(
                capturedBitmap = null,
                enabled = true,
                strings = strings,
                onBitmapCaptured = {}
            )
        }

        composeTestRule.onNodeWithText(strings.capturePhoto).assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithText(strings.clearPhoto).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun clearButtonInvokesCallbackWithNull() {
        var captured: Bitmap? = solidBitmap(64, Color.RED)
        val captures = mutableListOf<Bitmap?>()

        composeTestRule.setContent {
            var state by remember { mutableStateOf(captured) }
            CameraInputCard(
                capturedBitmap = state,
                enabled = true,
                strings = strings,
                onBitmapCaptured = { captures += it; state = it }
            )
        }

        composeTestRule.onNodeWithText(strings.retakePhoto).assertIsDisplayed()
        composeTestRule.onNodeWithText(strings.clearPhoto).assertIsEnabled().performClick()

        composeTestRule.waitForIdle()
        assertEquals(1, captures.size)
        assertNull("Clear must invoke callback with null", captures.single())
    }

    @Test
    fun loadingStateDisablesBothButtons() {
        composeTestRule.setContent {
            CameraInputCard(
                capturedBitmap = solidBitmap(64, Color.BLUE),
                enabled = false,
                strings = strings,
                onBitmapCaptured = {}
            )
        }

        composeTestRule.onNodeWithText(strings.retakePhoto).assertIsNotEnabled()
        composeTestRule.onNodeWithText(strings.clearPhoto).assertIsNotEnabled()
    }

    @Test
    fun capturedBitmapFlowsThroughPipelineAndChangesRecommendation() = runBlocking {
        val pipeline = RecommendationPipeline(context)

        val textOnly = pipeline.runPipeline("calm relaxing study music", onStepUpdate = {})
        val withVision = pipeline.runPipeline(
            "calm relaxing study music",
            imageBitmap = solidBitmap(128, Color.rgb(200, 40, 40)),
            onStepUpdate = {}
        )

        assertFalse("Text-only result must report no vision input", textOnly.usedVisionInput)
        assertTrue("Multimodal result must report vision used", withVision.usedVisionInput)
        assertTrue("Pipeline must return recommendations for both runs",
            textOnly.recommendations.isNotEmpty() && withVision.recommendations.isNotEmpty())
        assertNotNull(withVision.topRecommendation)
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
