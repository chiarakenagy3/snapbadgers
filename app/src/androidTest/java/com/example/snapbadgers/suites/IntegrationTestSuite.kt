package com.example.snapbadgers.suites

import com.example.snapbadgers.ai.sensor.SensorEncoderLifecycleTest
import com.example.snapbadgers.ai.vision.VisionEncoderFallbackTest
import com.example.snapbadgers.integration.CameraCaptureIntegrationTest
import com.example.snapbadgers.integration.EndToEndRecommendationTest
import com.example.snapbadgers.ml.BertTokenizerIntegrationTest
import com.example.snapbadgers.ml.QualcommTextEncoderIntegrationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    QualcommTextEncoderIntegrationTest::class,
    BertTokenizerIntegrationTest::class,
    EndToEndRecommendationTest::class,
    CameraCaptureIntegrationTest::class,
    SensorEncoderLifecycleTest::class,
    VisionEncoderFallbackTest::class
)
class IntegrationTestSuite
