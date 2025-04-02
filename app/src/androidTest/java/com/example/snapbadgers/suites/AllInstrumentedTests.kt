package com.example.snapbadgers.suites

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Master instrumented test suite - runs ALL device tests.
 *
 * Run with: ./gradlew connectedAndroidTest
 *
 * This is the top-level suite for all instrumented tests.
 * Requires: Android device or emulator
 *
 * Expected execution time: 5-15 minutes depending on device
 *
 * For faster iteration, run individual suites:
 * - IntegrationTestSuite: Core functionality (1-3 min)
 * - UiTestSuite: UI components (30-60 sec)
 * - PerformanceTestSuite: Benchmarks and stress tests (5-10 min)
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    IntegrationTestSuite::class,
    UiTestSuite::class,
    PerformanceTestSuite::class
)
class AllInstrumentedTests
