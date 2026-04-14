package com.example.snapbadgers.suites

import com.example.snapbadgers.EmbeddingTest
import com.example.snapbadgers.ai.common.ml.VectorUtilsTest
import com.example.snapbadgers.ai.fusion.FusionEngineTest
import com.example.snapbadgers.ai.projection.ProjectionNetworkTest
import com.example.snapbadgers.ai.text.FallbackTextEncoderTest
import com.example.snapbadgers.ai.text.HeuristicTextEmbeddingTest
import com.example.snapbadgers.ai.text.StubTextEncoderTest
import com.example.snapbadgers.model.InferenceStepsTest
import com.example.snapbadgers.model.RecommendationResultTest
import com.example.snapbadgers.model.SongTest
import com.example.snapbadgers.ai.sensor.SensorEncoderMLPTest
import com.example.snapbadgers.ai.sensor.SensorFeatureExtractorTest
import com.example.snapbadgers.songembeddings.embedding.SongEmbeddingPerformanceTest
import com.example.snapbadgers.songembeddings.embedding.SongEmbeddingUtilsTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Core AI
    VectorUtilsTest::class,
    FusionEngineTest::class,
    ProjectionNetworkTest::class,

    // Text encoding
    HeuristicTextEmbeddingTest::class,
    StubTextEncoderTest::class,
    FallbackTextEncoderTest::class,

    // Model
    InferenceStepsTest::class,
    SongTest::class,
    RecommendationResultTest::class,

    // Sensor
    SensorFeatureExtractorTest::class,
    SensorEncoderMLPTest::class,

    // Song embeddings
    SongEmbeddingUtilsTest::class,
    SongEmbeddingPerformanceTest::class,

    // Legacy
    EmbeddingTest::class
)
class UnitTestSuite
