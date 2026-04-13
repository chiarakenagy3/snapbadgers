package com.example.snapbadgers.suites

import com.example.snapbadgers.ui.HeaderTest
import com.example.snapbadgers.ui.InferenceStatusCardTest
import com.example.snapbadgers.ui.MainActivityInstrumentedTest
import com.example.snapbadgers.ui.RecommendationCardTest
import com.example.snapbadgers.ui.UiErrorScenarioTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * UI and Compose component test suite.
 *
 * Run with: ./gradlew connectedAndroidTest --tests "*.UiTestSuite"
 *
 * Requires: Android device or emulator
 * Tests Compose UI components and user interactions.
 * Expected execution time: 30-60 seconds
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Compose Component Tests
    HeaderTest::class,
    InferenceStatusCardTest::class,
    RecommendationCardTest::class,

    // Activity Tests
    MainActivityInstrumentedTest::class,

    // UI Error Scenarios
    UiErrorScenarioTest::class
)
class UiTestSuite
