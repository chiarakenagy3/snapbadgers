package com.example.snapbadgers.suites

import com.example.snapbadgers.integration.HardwareNpuValidationTest
import com.example.snapbadgers.integration.StressTestSuite
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    HardwareNpuValidationTest::class,
    StressTestSuite::class
)
class PerformanceTestSuite
