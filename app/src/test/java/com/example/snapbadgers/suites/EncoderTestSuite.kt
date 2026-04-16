package com.example.snapbadgers.suites

import com.example.snapbadgers.ai.sensor.SensorEncoderTest
import com.example.snapbadgers.ai.text.HeuristicTextEmbeddingTest
import com.example.snapbadgers.ai.text.StubTextEncoderTest
import com.example.snapbadgers.ai.text.TextEncoderFactoryTest
import com.example.snapbadgers.ml.QualcommVisionEncoderTest
import com.example.snapbadgers.sensor.SensorEncoderMLPTest
import com.example.snapbadgers.sensor.SensorFeatureExtractorTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for all encoder components (Text, Vision, Sensor).
 *
 * Run with: ./gradlew test --tests "*.EncoderTestSuite"
 *
 * Tests the core encoding pipeline components in isolation.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Text Encoders
    StubTextEncoderTest::class,
    HeuristicTextEmbeddingTest::class,
    TextEncoderFactoryTest::class,

    // Vision Encoders
    QualcommVisionEncoderTest::class,

    // Sensor Encoders
    SensorEncoderTest::class,
    SensorEncoderMLPTest::class,
    SensorFeatureExtractorTest::class
)
class EncoderTestSuite
