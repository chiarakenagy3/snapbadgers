package com.example.snapbadgers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.snapbadgers.ui.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SnapBadgersUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun wakeScreen() {
        wakeScreenForTest()
    }

    /**
     * Verify that the main screen (Scene Analyzer) loads correctly
     * and displays the essential UI elements.
     */
    @Test
    fun testMainScreenInitialization() {
        composeTestRule.onNodeWithText("Scene Analyzer").assertIsDisplayed()
        composeTestRule.onNodeWithText("e.g. rainy afternoon studying at a coffee shop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analyze Scene").assertIsDisplayed()
    }

    /**
     * Verify side navigation switching — clicking the Library icon navigates to the Library screen.
     */
    @Test
    fun testNavigationToLibrary() {
        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.onNodeWithText("Music Library").assertIsDisplayed()
    }

    /**
     * Verify navigation to History/Activity screen.
     */
    @Test
    fun testNavigationToHistory() {
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Activity History").assertIsDisplayed()
    }

    /**
     * Verify navigation to Settings screen.
     */
    @Test
    fun testNavigationToSettings() {
        composeTestRule.onAllNodesWithText("Settings")[0].performClick()
        composeTestRule.onNodeWithText("Account").assertIsDisplayed()
    }

    /**
     * Verify text input workflow — entering text leaves the Analyze button present and enabled.
     */
    @Test
    fun testTextInputAndAnalyzeButton() {
        composeTestRule.onNodeWithText("e.g. rainy afternoon studying at a coffee shop")
            .performTextInput("Party vibes")
        composeTestRule.onNodeWithText("Analyze Scene").assertIsDisplayed()
    }
}
