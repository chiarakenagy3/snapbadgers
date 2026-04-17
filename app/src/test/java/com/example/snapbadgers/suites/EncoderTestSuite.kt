package com.example.snapbadgers.suites

import com.example.snapbadgers.ai.text.HeuristicTextEmbeddingTest
import com.example.snapbadgers.ai.text.StubTextEncoderTest
import com.example.snapbadgers.ai.text.TextEncoderFactoryTest
import com.example.snapbadgers.ai.sensor.SensorEncoderMLPTest
import com.example.snapbadgers.ai.sensor.SensorFeatureExtractorTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Text Encoders
    StubTextEncoderTest::class,
    HeuristicTextEmbeddingTest::class,
    TextEncoderFactoryTest::class,

    // Sensor Encoders
    SensorEncoderMLPTest::class,
    SensorFeatureExtractorTest::class
)
class EncoderTestSuite
