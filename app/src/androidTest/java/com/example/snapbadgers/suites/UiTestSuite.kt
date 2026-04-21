package com.example.snapbadgers.suites

import com.example.snapbadgers.SnapBadgersUiTest
import com.example.snapbadgers.SongEmbeddingUiPerformanceTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    SnapBadgersUiTest::class,
    SongEmbeddingUiPerformanceTest::class
)
class UiTestSuite
