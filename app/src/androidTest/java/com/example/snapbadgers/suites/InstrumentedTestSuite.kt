package com.example.snapbadgers.suites

import com.example.snapbadgers.ExampleInstrumentedTest
import com.example.snapbadgers.SnapBadgersUiTest
import com.example.snapbadgers.SongEmbeddingUiPerformanceTest
import com.example.snapbadgers.ml.QualcommTextEncoderIntegrationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    ExampleInstrumentedTest::class,
    SnapBadgersUiTest::class,
    SongEmbeddingUiPerformanceTest::class,
    QualcommTextEncoderIntegrationTest::class
)
class InstrumentedTestSuite
