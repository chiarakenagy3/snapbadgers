package com.example.snapbadgers.suites

import com.example.snapbadgers.ai.vision.QualcommVisionEncoderIntegrationTest
import com.example.snapbadgers.integration.EndToEndRecommendationTest
import com.example.snapbadgers.integration.FullPipelineIntegrationTest
import com.example.snapbadgers.ml.BertTokenizerIntegrationTest
import com.example.snapbadgers.ml.QualcommTextEncoderIntegrationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Integration test suite for multi-component interactions.
 *
 * Run with: ./gradlew connectedAndroidTest --tests "*.IntegrationTestSuite"
 *
 * Requires: Real Android device or emulator
 * Tests real NPU inference, TFLite models, and component integration.
 * Expected execution time: 1-3 minutes
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // ML Model Integration
    QualcommTextEncoderIntegrationTest::class,
    QualcommVisionEncoderIntegrationTest::class,
    BertTokenizerIntegrationTest::class,

    // Pipeline Integration
    FullPipelineIntegrationTest::class,

    // End-to-End Scenarios
    EndToEndRecommendationTest::class
)
class IntegrationTestSuite
