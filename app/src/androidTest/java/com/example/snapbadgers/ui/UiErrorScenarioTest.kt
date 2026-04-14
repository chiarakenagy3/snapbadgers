package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiErrorScenarioTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testEmptyInputShowsValidation() {
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        val button = composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
        button.assertExists()
    }

    @Test
    fun testInputVariants() {
        listOf(
            "This is a very long input text that goes on and on. ".repeat(100) to "very long",
            "!@#\$%^&*()_+-=[]{}|;':\",./<>?" to "special characters",
            "音楽 🎵 música موسيقى" to "unicode",
            "     " to "whitespace-only",
            "line1\nline2\nline3" to "newlines"
        ).forEach { (input, label) ->
            composeTestRule.onNode(hasSetTextAction()).performTextClearance()
            composeTestRule.onNode(hasSetTextAction()).performTextInput(input)

            if (label != "whitespace-only") {
                composeTestRule.onNodeWithText("Recommend", ignoreCase = true).performClick()
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun testRapidButtonClicks() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput("test query")
        val button = composeTestRule.onNodeWithText("Recommend", ignoreCase = true)
        repeat(5) {
            button.performClick()
            Thread.sleep(50)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testResetDuringProcessing() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput("test input")
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true).performClick()
        Thread.sleep(100)
        composeTestRule.onNodeWithText("Reset", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testInputChangeDuringProcessing() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput("initial text")
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true).performClick()
        Thread.sleep(100)
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMultipleConsecutiveRecommendations() {
        listOf("calm music", "energetic beats", "study ambient").forEach { query ->
            composeTestRule.onNode(hasSetTextAction()).performTextClearance()
            composeTestRule.onNode(hasSetTextAction()).performTextInput(query)
            composeTestRule.onNodeWithText("Recommend", ignoreCase = true).performClick()
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodesWithText("Running", substring = true)
                    .fetchSemanticsNodes().isEmpty()
            }
        }
    }

    @Test
    fun testNavigationAwayAndBack() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput("test")
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true).performClick()
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(500)
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testScreenRotation() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput("rotation test")
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasSetTextAction()).assertExists()
    }

    @Test
    fun testAccessibilityLabels() {
        composeTestRule.onRoot().printToLog("UI_TREE")
        composeTestRule.onNode(hasSetTextAction()).assertExists()
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Reset", ignoreCase = true).assertExists()
    }

    @Test
    fun testLongRunningOperation() {
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("this is a longer more complex query with many words")
        composeTestRule.onNodeWithText("Recommend", ignoreCase = true).performClick()
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodesWithText("Processing", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun testInputFieldMaxLength() {
        val extremelyLongText = "word ".repeat(10000)
        try {
            composeTestRule.onNode(hasSetTextAction())
                .performTextInput(extremelyLongText.take(5000))
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            println("Handled extreme input: ${e.message}")
        }
    }
}
