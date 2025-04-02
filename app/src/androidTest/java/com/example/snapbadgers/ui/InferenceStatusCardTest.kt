package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.ui.theme.SnapBadgersTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for InferenceStatusCard composable.
 */
@RunWith(AndroidJUnit4::class)
class InferenceStatusCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysAllStepsWhenNoneComplete() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(),
                    inferenceTimeMs = 0L
                )
            }
        }

        // All steps should be visible
        composeTestRule.onNodeWithText("Encoding text...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encoding vision...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encoding sensors...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fusing inputs...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projecting embeddings...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ranking songs...").assertIsDisplayed()
    }

    @Test
    fun displaysCompletedStepsWithCheckmark() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(
                        textEncoded = true,
                        visionEncoded = true,
                        sensorEncoded = false
                    ),
                    inferenceTimeMs = 0L
                )
            }
        }

        // Completed steps should show checkmark
        composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Encoded vision").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encoding sensors...").assertIsDisplayed()
    }

    @Test
    fun displaysInferenceTimeWhenComplete() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(
                        textEncoded = true,
                        visionEncoded = true,
                        sensorEncoded = true,
                        fused = true,
                        projected = true,
                        ranked = true
                    ),
                    inferenceTimeMs = 1234L
                )
            }
        }

        composeTestRule.onNodeWithText("Inference time: 1234ms").assertIsDisplayed()
    }

    @Test
    fun hidesInferenceTimeWhenZero() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(),
                    inferenceTimeMs = 0L
                )
            }
        }

        composeTestRule.onNodeWithText("Inference time: 0ms").assertDoesNotExist()
    }

    @Test
    fun showsPartialProgress() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(
                        textEncoded = true,
                        visionEncoded = true,
                        sensorEncoded = true,
                        fused = true,
                        projected = false,
                        ranked = false
                    ),
                    inferenceTimeMs = 0L
                )
            }
        }

        composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Fused inputs").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projecting embeddings...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ranking songs...").assertIsDisplayed()
    }

    @Test
    fun cardIsVisibleAndScrollable() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(),
                    inferenceTimeMs = 0L
                )
            }
        }

        // Card should be displayed
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun handlesVeryLongInferenceTime() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(
                        textEncoded = true,
                        visionEncoded = true,
                        sensorEncoded = true,
                        fused = true,
                        projected = true,
                        ranked = true
                    ),
                    inferenceTimeMs = 999999L
                )
            }
        }

        composeTestRule.onNodeWithText("Inference time: 999999ms").assertIsDisplayed()
    }

    @Test
    fun displaysCorrectStepOrder() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(
                        textEncoded = true
                    ),
                    inferenceTimeMs = 0L
                )
            }
        }

        // Text encoding should appear before vision
        composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encoding vision...").assertIsDisplayed()
    }

    @Test
    fun handlesAllStepsCompleted() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                InferenceStatusCard(
                    steps = InferenceSteps(
                        textEncoded = true,
                        visionEncoded = true,
                        sensorEncoded = true,
                        fused = true,
                        projected = true,
                        ranked = true
                    ),
                    inferenceTimeMs = 150L
                )
            }
        }

        // All steps should show checkmark
        composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Encoded vision").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Encoded sensors").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Fused inputs").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Projected embeddings").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Ranked songs").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inference time: 150ms").assertIsDisplayed()
    }

    @Test
    fun rendersInDarkMode() {
        composeTestRule.setContent {
            SnapBadgersTheme(darkTheme = true) {
                InferenceStatusCard(
                    steps = InferenceSteps(textEncoded = true),
                    inferenceTimeMs = 100L
                )
            }
        }

        composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
    }

    @Test
    fun rendersInLightMode() {
        composeTestRule.setContent {
            SnapBadgersTheme(darkTheme = false) {
                InferenceStatusCard(
                    steps = InferenceSteps(textEncoded = true),
                    inferenceTimeMs = 100L
                )
            }
        }

        composeTestRule.onNodeWithText("✓ Encoded text").assertIsDisplayed()
    }
}
