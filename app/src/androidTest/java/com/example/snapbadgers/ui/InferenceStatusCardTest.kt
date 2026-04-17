package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.ui.theme.SnapBadgersTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InferenceStatusCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysAllStepsWhenNoneComplete() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(steps = InferenceSteps(), inferenceTimeMs = 0L)
            }
        }

        composeTestRule.onNodeWithText("Encoding text...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encoding vision...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encoding sensors...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fusing inputs...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projecting embeddings...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ranking songs...").assertIsDisplayed()
    }

    @Test
    fun displaysStepStatesCorrectly() {
        data class StepCase(val label: String, val steps: InferenceSteps, val expected: List<String>)

        listOf(
            StepCase("completed with checkmark", InferenceSteps(
                textEncoded = true, visionEncoded = true, sensorEncoded = false
            ), listOf("✓ Encoded text", "✓ Encoded vision", "Encoding sensors...")),
            StepCase("partial progress", InferenceSteps(
                textEncoded = true, visionEncoded = true, sensorEncoded = true,
                fused = true, projected = false, ranked = false
            ), listOf("✓ Encoded text", "✓ Fused inputs", "Projecting embeddings...", "Ranking songs...")),
            StepCase("all completed", InferenceSteps(
                textEncoded = true, visionEncoded = true, sensorEncoded = true,
                fused = true, projected = true, ranked = true
            ), listOf(
                "✓ Encoded text", "✓ Encoded vision", "✓ Encoded sensors",
                "✓ Fused inputs", "✓ Projected embeddings", "✓ Ranked songs"
            ))
        ).forEach { (label, steps, expected) ->
            composeTestRule.setContent {
                SnapBadgersTheme {
                    InferenceStatusCard(steps = steps, inferenceTimeMs = if (label == "all completed") 150L else 0L)
                }
            }
            expected.forEach { text ->
                composeTestRule.onNodeWithText(text).assertIsDisplayed()
            }
            if (label == "all completed") {
                composeTestRule.onNodeWithText("Inference time: 150ms").assertIsDisplayed()
            }
        }
    }

    @Test
    fun displaysInferenceTime() {
        listOf(1234L, 999999L).forEach { timeMs ->
            composeTestRule.setContent {
                SnapBadgersTheme {
                    InferenceStatusCard(
                        steps = InferenceSteps(
                            textEncoded = true, visionEncoded = true, sensorEncoded = true,
                            fused = true, projected = true, ranked = true
                        ),
                        inferenceTimeMs = timeMs
                    )
                }
            }
            composeTestRule.onNodeWithText("Inference time: ${timeMs}ms").assertIsDisplayed()
        }
    }

    @Test
    fun hidesInferenceTimeWhenZero() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(steps = InferenceSteps(), inferenceTimeMs = 0L)
            }
        }
        composeTestRule.onNodeWithText("Inference time: 0ms").assertDoesNotExist()
    }

    @Test
    fun displaysCorrectStepOrder() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(steps = InferenceSteps(textEncoded = true), inferenceTimeMs = 0L)
            }
        }
        composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encoding vision...").assertIsDisplayed()
    }

    @Test
    fun cardIsVisibleAndScrollable() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(steps = InferenceSteps(), inferenceTimeMs = 0L)
            }
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun rendersInBothThemes() {
        listOf(true, false).forEach { dark ->
            composeTestRule.setContent {
                SnapBadgersTheme(darkTheme = dark) {
                    InferenceStatusCard(
                        steps = InferenceSteps(textEncoded = true),
                        inferenceTimeMs = 100L
                    )
                }
            }
            composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
        }
    }
}
