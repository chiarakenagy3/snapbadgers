package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.ui.theme.SnapBadgersTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecommendationCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSong = Song(
        title = "Test Song",
        artist = "Test Artist",
        embedding = FloatArray(128) { 0.5f },
        similarity = 0.95f
    )

    @Test
    fun displaysAllElements() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 2)
            }
        }

        composeTestRule.onNodeWithText("#2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()
        composeTestRule.onNodeWithText("95.0%").assertIsDisplayed()
    }

    @Test
    fun handlesDifferentRanks() {
        for (rank in 1..10) {
            composeTestRule.setContent {
                SnapBadgersTheme {
                    RecommendationCard(song = testSong, rank = rank)
                }
            }
            composeTestRule.onNodeWithText("#$rank").assertIsDisplayed()
        }
    }

    @Test
    fun handlesSimilarityRanges() {
        listOf(
            0.12f to "12.0%",
            0.999f to "99.9%",
            0.0f to "0.0%"
        ).forEach { (similarity, expected) ->
            val song = testSong.copy(similarity = similarity)
            composeTestRule.setContent {
                SnapBadgersTheme {
                    RecommendationCard(song = song, rank = 1)
                }
            }
            composeTestRule.onNodeWithText(expected).assertIsDisplayed()
        }
    }

    @Test
    fun handlesLongAndSpecialText() {
        listOf(
            testSong.copy(title = "This is a very long song title that should be displayed properly without breaking the layout"),
            testSong.copy(artist = "This is a very long artist name with multiple collaborators and features"),
            testSong.copy(title = "Song w/ Special !@#\$%^&*() Characters"),
        ).forEach { song ->
            composeTestRule.setContent {
                SnapBadgersTheme {
                    RecommendationCard(song = song, rank = 1)
                }
            }
            composeTestRule.onNodeWithText(song.title).assertIsDisplayed()
        }
    }

    @Test
    fun handlesUnicodeText() {
        val unicodeSong = testSong.copy(title = "音楽 Song 🎵", artist = "Artist 名前")
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = unicodeSong, rank = 1)
            }
        }
        composeTestRule.onNodeWithText(unicodeSong.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(unicodeSong.artist).assertIsDisplayed()
    }

    @Test
    fun rendersInBothThemes() {
        listOf(true, false).forEach { dark ->
            composeTestRule.setContent {
                SnapBadgersTheme(darkTheme = dark) {
                    RecommendationCard(song = testSong, rank = 1)
                }
            }
            composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
            composeTestRule.onNodeWithText("95.0%").assertIsDisplayed()
        }
    }

    @Test
    fun cardIsAccessible() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 1)
            }
        }
        composeTestRule.onNodeWithText("Test Song").assertExists()
        composeTestRule.onNodeWithText("Test Artist").assertExists()
    }

    @Test
    fun handlesEmptyFields() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong.copy(title = ""), rank = 1)
            }
        }
        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()

        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong.copy(artist = ""), rank = 1)
            }
        }
        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
    }
}
