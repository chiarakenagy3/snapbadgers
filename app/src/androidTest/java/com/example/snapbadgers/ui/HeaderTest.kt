package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.snapbadgers.ui.theme.SnapBadgersTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysRequiredElements() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                Header()
            }
        }
        composeTestRule.onNodeWithText("SnapBadgers").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI-Powered Music Recommendations").assertIsDisplayed()
    }

    @Test
    fun rendersInBothThemes() {
        listOf(false, true).forEach { dark ->
            composeTestRule.setContent {
                SnapBadgersTheme(darkTheme = dark) {
                    Header()
                }
            }
            composeTestRule.onNodeWithText("SnapBadgers").assertIsDisplayed()
        }
    }

    @Test
    fun headerIsAccessible() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                Header()
            }
        }
        composeTestRule.onNodeWithText("SnapBadgers")
            .assertExists()
            .assertHasClickAction()
            .assertIsNotEnabled()
    }
}
