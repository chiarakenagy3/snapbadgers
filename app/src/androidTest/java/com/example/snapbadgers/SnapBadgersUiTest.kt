package com.example.snapbadgers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.snapbadgers.ui.MainActivity
import org.junit.Rule
import org.junit.Test

class SnapBadgersUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test Case 1: Verify that the main screen (Scene Analyzer) loads correctly
     * and displays the essential UI elements.
     */
    @Test
    fun testMainScreenInitialization() {
        // Check if the title is displayed
        composeTestRule.onNodeWithText("Scene Analyzer").assertIsDisplayed()
        
        // Check if the input field placeholder is visible
        composeTestRule.onNodeWithText("e.g. A rainy afternoon in a cozy cafe").assertIsDisplayed()
        
        // Check if the analyze button is present
        composeTestRule.onNodeWithText("Analyze Scene").assertIsDisplayed()
    }

    /**
     * Test Case 2: Verify side navigation switching.
     * Checks if clicking the Library icon navigates to the Library screen.
     */
    @Test
    fun testNavigationToLibrary() {
        // Find the Library icon in the sidebar by its content description and click it
        composeTestRule.onNodeWithContentDescription("library").performClick()

        // Verify that the Library screen title is now visible
        composeTestRule.onNodeWithText("Music Library").assertIsDisplayed()
    }

    /**
     * Test Case 3: Verify navigation to History/Activity screen.
     */
    @Test
    fun testNavigationToHistory() {
        // Navigate to Activity History tab
        composeTestRule.onNodeWithContentDescription("activity").performClick()

        // Verify History screen title
        composeTestRule.onNodeWithText("Activity History").assertIsDisplayed()
    }

    /**
     * Test Case 4: Verify navigation to Settings screen.
     */
    @Test
    fun testNavigationToSettings() {
        // Navigate to Settings tab
        composeTestRule.onNodeWithContentDescription("settings").performClick()

        // Verify Settings screen title
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Check if common settings sections are visible
        composeTestRule.onNodeWithText("Account").assertIsDisplayed()
    }

    /**
     * Test Case 5: Basic workflow test.
     * Enters text and ensures the UI reacts (button remains present/enabled).
     */
    @Test
    fun testTextInputAndAnalyzeButton() {
        val testInput = "Party vibes"
        
        // Enter text into the description field
        composeTestRule.onNodeWithText("e.g. A rainy afternoon in a cozy cafe")
            .performTextInput(testInput)

        // Verify the button is still there and clickable
        composeTestRule.onNodeWithText("Analyze Scene").assertIsDisplayed()
    }
}
