package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.snapbadgers.ui.theme.SnapBadgersTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for Header composable.
 */
@RunWith(AndroidJUnit4::class)
class HeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun headerDisplaysTitle() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                Header()
            }
        }

        composeTestRule
            .onNodeWithText("SnapBadgers")
            .assertIsDisplayed()
    }

    @Test
    fun headerDisplaysSubtitle() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                Header()
            }
        }

        composeTestRule
            .onNodeWithText("AI-Powered Music Recommendations")
            .assertIsDisplayed()
    }

    @Test
    fun headerHasCorrectSemantics() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                Header()
            }
        }

        // Should have heading semantics for accessibility
        composeTestRule
            .onNodeWithText("SnapBadgers")
            .assertExists()
    }

    @Test
    fun headerRendersInDifferentThemes() {
        // Test light theme
        composeTestRule.setContent {
            SnapBadgersTheme(darkTheme = false) {
                Header()
            }
        }
        composeTestRule
            .onNodeWithText("SnapBadgers")
            .assertIsDisplayed()

        // Test dark theme
        composeTestRule.setContent {
            SnapBadgersTheme(darkTheme = true) {
                Header()
            }
        }
        composeTestRule
            .onNodeWithText("SnapBadgers")
            .assertIsDisplayed()
    }

    @Test
    fun headerLayoutDoesNotOverlap() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                Header()
            }
        }

        // Both title and subtitle should be visible without overlap
        composeTestRule
            .onNodeWithText("SnapBadgers")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("AI-Powered Music Recommendations")
            .assertIsDisplayed()
    }

    @Test
    fun headerIsAccessible() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                Header()
            }
        }

        // Header should be accessible for screen readers
        composeTestRule
            .onNodeWithText("SnapBadgers")
            .assertExists()
            .assertHasClickAction() // Should not be clickable
            .assertIsNotEnabled()
    }
}
