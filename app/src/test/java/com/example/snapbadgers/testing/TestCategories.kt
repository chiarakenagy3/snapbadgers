package com.example.snapbadgers.testing

/**
 * Test category markers for organizing and filtering tests.
 *
 * Usage:
 * ```
 * @Category(UnitTest::class)
 * class MyTest { ... }
 * ```
 *
 * Run specific categories:
 * ```
 * ./gradlew test --tests "*.category.UnitTest"
 * ```
 */

/** Fast unit tests with no external dependencies */
interface UnitTest

/** Integration tests with multiple components */
interface IntegrationTest

/** End-to-end user scenario tests */
interface E2ETest

/** Performance and benchmark tests */
interface PerformanceTest

/** UI and Compose component tests */
interface UiTest

/** Tests requiring NPU/Qualcomm hardware */
interface NpuTest

/** Stress tests and edge case validation */
interface StressTest

/** Tests that are slow (> 1 second) */
interface SlowTest

/** Tests requiring real TFLite models */
interface ModelTest
