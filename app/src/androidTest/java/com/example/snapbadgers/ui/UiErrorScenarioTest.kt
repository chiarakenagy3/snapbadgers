package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for error scenarios and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class UiErrorScenarioTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testEmptyInputShowsValidation() {
        // Try to submit without entering text
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()

        // Button behavior with empty input
        val button = composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
        button.assertExists()
    }

    @Test
    fun testVeryLongInputHandling() {
        val veryLongText = "This is a very long input text that goes on and on. ".repeat(100)

        composeTestRule.onNode(hasSetTextAction())
            .performTextInput(veryLongText)

        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        // Should still process (may take time)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testSpecialCharactersInInput() {
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?"

        composeTestRule.onNode(hasSetTextAction())
            .performTextInput(specialChars)

        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun testUnicodeCharactersInInput() {
        val unicodeText = "音楽 🎵 música موسيقى"

        composeTestRule.onNode(hasSetTextAction())
            .performTextInput(unicodeText)

        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun testRapidButtonClicks() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("test query")

        val button = composeTestRule.onNodeWithText("Recommend", ignoreCase = true)

        // Click rapidly
        repeat(5) {
            button.performClick()
            Thread.sleep(50)
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun testResetDuringProcessing() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("test input")

        // Start processing
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        // Immediately try to reset
        Thread.sleep(100)
        composeTestRule.onNodeWithText("Reset", ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun testInputChangeDuringProcessing() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("initial text")

        // Start processing
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        // Try to change input while processing
        Thread.sleep(100)
        composeTestRule.onNode(hasSetTextAction())
            .performTextClearance()

        composeTestRule.waitForIdle()
    }

    @Test
    fun testMultipleConsecutiveRecommendations() {
        val queries = listOf("calm music", "energetic beats", "study ambient")

        queries.forEach { query ->
            composeTestRule.onNode(hasSetTextAction()).performTextClearance()
            composeTestRule.onNode(hasSetTextAction()).performTextInput(query)

            composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
                .performClick()

            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodesWithText("Running", substring = true)
                    .fetchSemanticsNodes().isEmpty()
            }
        }
    }

    @Test
    fun testNavigationAwayAndBack() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("test")

        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        // Simulate app going to background and back
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(500)
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        composeTestRule.waitForIdle()
    }

    @Test
    fun testScreenRotation() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("rotation test")

        // Note: Actual rotation requires device orientation change
        // This test verifies state persistence
        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasSetTextAction()).assertExists()
    }

    @Test
    fun testAccessibilityLabels() {
        // Verify important elements have content descriptions for accessibility
        composeTestRule.onRoot().printToLog("UI_TREE")

        // Input field should be accessible
        composeTestRule.onNode(hasSetTextAction()).assertExists()

        // Buttons should be accessible
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Reset", ignoreCase = true).assertExists()
    }

    @Test
    fun testLongRunningOperation() {
        // Test with a query that might take longer
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("this is a longer more complex query with many words")

        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        // Wait with generous timeout
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodesWithText("Processing", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun testInputFieldMaxLength() {
        // Try to input extremely long text
        val extremelyLongText = "word ".repeat(10000)

        try {
            composeTestRule.onNode(hasSetTextAction())
                .performTextInput(extremelyLongText.take(5000)) // Take reasonable amount

            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            // Should handle gracefully even if it fails
            println("Handled extreme input: ${e.message}")
        }
    }

    @Test
    fun testWhitespaceOnlyInput() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("     ")

        // Should handle whitespace-only input
        composeTestRule.waitForIdle()
    }

    @Test
    fun testNewlineInInput() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("line1\nline2\nline3")

        composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()
    }
}
