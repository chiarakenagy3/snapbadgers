package com.example.snapbadgers.suites

import com.example.snapbadgers.integration.HardwareNpuValidationTest
import com.example.snapbadgers.integration.NpuPerformanceBenchmarkTest
import com.example.snapbadgers.integration.StressTestSuite
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Performance and stress test suite.
 *
 * Run with: ./gradlew connectedAndroidTest --tests "*.PerformanceTestSuite"
 *
 * Requires: Real Snapdragon 8 Elite device (Galaxy S25) for accurate results
 * Tests NPU performance, thermal behavior, memory usage, and stress scenarios.
 * Expected execution time: 5-10 minutes
 *
 * ⚠️ WARNING: These tests are resource-intensive and may heat up the device.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // NPU Hardware Validation
    HardwareNpuValidationTest::class,

    // Performance Benchmarks
    NpuPerformanceBenchmarkTest::class,

    // Stress Testing
    StressTestSuite::class
)
class PerformanceTestSuite
