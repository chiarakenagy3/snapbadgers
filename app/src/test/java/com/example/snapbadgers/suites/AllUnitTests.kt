package com.example.snapbadgers.suites

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Master test suite - runs ALL unit tests.
 *
 * Run with: ./gradlew test
 *
 * This is the top-level suite that includes all unit test suites.
 * Use this for comprehensive pre-commit validation.
 *
 * Expected execution time: < 15 seconds
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    UnitTestSuite::class,
    EncoderTestSuite::class
)
class AllUnitTests
