package com.example.snapbadgers.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.ui.theme.SnapBadgersTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for RecommendationCard composable.
 */
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
    fun displaysSongTitle() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
    }

    @Test
    fun displaysSongArtist() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()
    }

    @Test
    fun displaysSimilarityScore() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText("95.0%").assertIsDisplayed()
    }

    @Test
    fun displaysRankNumber() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText("#1").assertIsDisplayed()
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
    fun handlesLowSimilarity() {
        val lowSimSong = testSong.copy(similarity = 0.12f)
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = lowSimSong, rank = 3)
            }
        }

        composeTestRule.onNodeWithText("12.0%").assertIsDisplayed()
    }

    @Test
    fun handlesHighSimilarity() {
        val highSimSong = testSong.copy(similarity = 0.999f)
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = highSimSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText("99.9%").assertIsDisplayed()
    }

    @Test
    fun handlesZeroSimilarity() {
        val zeroSimSong = testSong.copy(similarity = 0.0f)
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = zeroSimSong, rank = 5)
            }
        }

        composeTestRule.onNodeWithText("0.0%").assertIsDisplayed()
    }

    @Test
    fun handlesLongSongTitle() {
        val longTitleSong = testSong.copy(
            title = "This is a very long song title that should be displayed properly without breaking the layout"
        )
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = longTitleSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText(longTitleSong.title).assertIsDisplayed()
    }

    @Test
    fun handlesLongArtistName() {
        val longArtistSong = testSong.copy(
            artist = "This is a very long artist name with multiple collaborators and features"
        )
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = longArtistSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText(longArtistSong.artist).assertIsDisplayed()
    }

    @Test
    fun handlesSpecialCharactersInTitle() {
        val specialSong = testSong.copy(title = "Song w/ Special !@#$%^&*() Characters")
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = specialSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText(specialSong.title).assertIsDisplayed()
    }

    @Test
    fun handlesUnicodeInTitle() {
        val unicodeSong = testSong.copy(
            title = "音楽 Song 🎵",
            artist = "Artist 名前"
        )
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = unicodeSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText(unicodeSong.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(unicodeSong.artist).assertIsDisplayed()
    }

    @Test
    fun cardRendersInDarkMode() {
        composeTestRule.setContent {
            SnapBadgersTheme(darkTheme = true) {
                RecommendationCard(song = testSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("95.0%").assertIsDisplayed()
    }

    @Test
    fun cardRendersInLightMode() {
        composeTestRule.setContent {
            SnapBadgersTheme(darkTheme = false) {
                RecommendationCard(song = testSong, rank = 1)
            }
        }

        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("95.0%").assertIsDisplayed()
    }

    @Test
    fun cardIsAccessible() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 1)
            }
        }

        // Card should be accessible for screen readers
        composeTestRule.onNodeWithText("Test Song").assertExists()
        composeTestRule.onNodeWithText("Test Artist").assertExists()
    }

    @Test
    fun displaysAllElementsSimultaneously() {
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = testSong, rank = 2)
            }
        }

        // All elements should be visible at once
        composeTestRule.onNodeWithText("#2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()
        composeTestRule.onNodeWithText("95.0%").assertIsDisplayed()
    }

    @Test
    fun handlesEmptyTitle() {
        val emptySong = testSong.copy(title = "")
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = emptySong, rank = 1)
            }
        }

        // Should still render without crashing
        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()
    }

    @Test
    fun handlesEmptyArtist() {
        val emptySong = testSong.copy(artist = "")
        composeTestRule.setContent {
            SnapBadgersTheme {
                RecommendationCard(song = emptySong, rank = 1)
            }
        }

        // Should still render without crashing
        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
    }
}
