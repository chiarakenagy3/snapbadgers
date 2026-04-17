package com.example.snapbadgers.suites

import com.example.snapbadgers.ui.HeaderTest
import com.example.snapbadgers.ui.InferenceStatusCardTest
import com.example.snapbadgers.ui.RecommendationCardTest
import com.example.snapbadgers.ui.UiErrorScenarioTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    HeaderTest::class,
    InferenceStatusCardTest::class,
    RecommendationCardTest::class,
    UiErrorScenarioTest::class
)
class UiTestSuite
