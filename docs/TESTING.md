# Testing

## Strategy

JUnit 4 with AndroidX Test. Three tiers:

* **JVM Unit Tests** (`app/src/test/`): Pure-Kotlin pipeline stages. Vector math, embedding quality, fusion, projection, sensor features, end-to-end heuristic pipeline. No device required.
* **Instrumented Evals** (`app/src/androidTest/.../eval/`): On-device model validation. Inference latency (p50/p95/p99), accuracy, load times, memory profiling. Require TFLite model assets.
* **Instrumented Integration Tests** (`app/src/androidTest/.../integration/`): End-to-end recommendation, NPU validation, stress tests, BertTokenizer integration.
* **UI Tests** (`app/src/androidTest/.../ui/`): Compose component tests for Header, InferenceStatusCard, RecommendationCard, error scenarios.

## Running Tests

### JVM Unit Tests

```bash
./gradlew testDebugUnitTest
```

### JVM Evals Only

```bash
# All JVM evals
./gradlew testDebugUnitTest --tests "*.eval.*"

# Individual eval suites
./gradlew testDebugUnitTest --tests "*.eval.VectorUtilsEvalTest"
./gradlew testDebugUnitTest --tests "*.eval.EmbeddingQualityEvalTest"
./gradlew testDebugUnitTest --tests "*.eval.ProjectionNetworkEvalTest"
./gradlew testDebugUnitTest --tests "*.eval.FusionEngineEvalTest"
./gradlew testDebugUnitTest --tests "*.eval.SensorFeatureExtractorEvalTest"
./gradlew testDebugUnitTest --tests "*.eval.PipelineCorrectnessEvalTest"
```

Evals print metrics to stdout. Filter for lines starting with `EVAL`.

### Instrumented Tests (Device Required)

```bash
# All instrumented tests
./gradlew connectedDebugAndroidTest

# Eval suite only
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.snapbadgers.eval.InferenceLatencyEval
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.snapbadgers.eval.MemoryProfileEval
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.snapbadgers.eval.ModelAccuracyEval
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.snapbadgers.eval.ModelLoadTimeEval
```

Instrumented evals log via `Log.i("EVAL", ...)`. Filter: `adb logcat -s EVAL`.

### Single Test

```bash
./gradlew testDebugUnitTest --tests "*.eval.FusionEngineEvalTest.fusionWeightBalance"
```

## Test Inventory

### JVM Eval Tests

| Test Class | What It Validates | Key Thresholds |
| :--- | :--- | :--- |
| `EmbeddingQualityEvalTest` | Heuristic text embedding: semantic separation, L2 normalization, 128-d output, determinism (100x), non-zero dimension coverage, keyword feature accuracy | Different moods cosine sim < 0.95; L2 norm = 1.0 +/- 1e-5; 100% determinism |
| `PipelineCorrectnessEvalTest` | End-to-end heuristic pipeline: descending ranking, sensor inclusion, MLP projection, 50-run reproducibility, per-stage dimension checks | Rankings strictly descending; L2 norm = 1.0 +/- 1e-4; 50x exactly reproducible; text=128-d, sensor=32-d, fused=128-d, projected=128-d |
| `FusionEngineEvalTest` | Single/multi-modality fusion, weight balance (0.60/0.25/0.15), custom weights, zero-vector safety, 100x consistency, arbitrary dimensions | Text-only cosine sim = 1.0 +/- 1e-4; vision weight > text > sensor; no NaN/Inf; output always 128-d |
| `ProjectionNetworkEvalTest` | Projection MLP output shape, normalization, determinism, weight loading | 128-d output; L2 norm = 1.0 +/- 1e-5; deterministic |
| `VectorUtilsEvalTest` | Cosine similarity, L2 normalization, alignment, dimension handling | Mathematical correctness within floating-point tolerance |
| `SensorFeatureExtractorEvalTest` | Feature extraction from raw sensor readings, dimension correctness, edge cases | 14-d output; handles zero/null sensor data |

### Instrumented Eval Tests

| Test Class | What It Validates | Key Thresholds |
| :--- | :--- | :--- |
| `InferenceLatencyEval` | p50/p95/p99 latency for text, vision, heuristic, and full pipeline. NNAPI delegate status. Per-stage breakdown. | p99 < 10s (sanity); targets below |
| `ModelAccuracyEval` | Text encoder known-input verification, cross-run consistency (10x), vision encoder color inputs (5 colors), vision stability, heuristic keyword feature activation | Non-blank -> non-zero; mood separation sim < 0.99; 9/10 identical; vision deterministic 10x |
| `ModelLoadTimeEval` | Cold/warm load time for 3 TFLite models. Memory delta per model. Input/output tensor shapes. | Cold < 30s; warm < 15s |
| `MemoryProfileEval` | 50-iteration peak memory, monotonic growth leak detection (30 iterations x10 ops), per-encoder memory impact | Max consecutive increases < 20 (leak indicator); peak memory positive |

### Instrumented Integration Tests

| Test Class | What It Validates |
| :--- | :--- |
| `EndToEndRecommendationTest` | Full pipeline from text input to ranked song output on-device |
| `HardwareNpuValidationTest` | Qualcomm SoC detection, NNAPI availability, NPU performance characteristics, thermal throttling, Hexagon DSP |
| `StressTestSuite` | Pipeline under sustained load |
| `BertTokenizerIntegrationTest` | WordPiece tokenization against real `vocab.txt` asset |

### UI Tests

| Test Class | What It Validates |
| :--- | :--- |
| `HeaderTest` | Header composable rendering |
| `InferenceStatusCardTest` | Inference step display and state transitions |
| `RecommendationCardTest` | Song recommendation card layout and content |
| `UiErrorScenarioTest` | Error state handling in UI components |

## Latency Targets

Reference device: Samsung Galaxy S25, Snapdragon 8 Elite.

| Metric | Good | Acceptable | Investigate |
| :--- | :--- | :--- | :--- |
| Text encoder p50 | < 10ms | < 50ms | > 100ms |
| Vision encoder p50 | < 15ms | < 80ms | > 200ms |
| Full pipeline p50 | < 50ms | < 200ms | > 500ms |
| Model cold load | < 500ms | < 2s | > 5s |
| Pipeline memory delta | < 10 MB | -- | > 50 MB |

## Conventions

* JVM evals use `HeuristicTextEmbedding` / `StubTextEncoder` for deterministic, device-independent validation. Model-dependent assertions go in instrumented evals.
* Instrumented evals log with the `EVAL` tag for `adb logcat` filtering.
* Seed 42 throughout for deterministic initialization and fixtures.
* Embedding assertions check both dimensionality (128-d or 32-d) and L2 normalization (1.0 within tolerance).

## Troubleshooting

### Model Assets Missing

**Issue:** Instrumented evals fall back to `StubTextEncoder`.
**Fix:** Place `mobile_bert.tflite`, `vocab.txt`, and `efficientnet_b0_128d_int8.tflite` in `app/src/main/assets/`. Not checked into git due to size.

### Non-ASCII Build Path

**Issue:** `TestWorker failed to load test class` on Windows with non-ASCII usernames.
**Fix:** Build script relocates output to `~/.snapbadgers-build/app` for non-ASCII paths. Verify `android.overridePathCheck=true` in `gradle.properties`.

### Emulator Latency

**Issue:** Instrumented latency evals report 10-100x slower than target.
**Fix:** Targets are calibrated for physical Snapdragon 8 Elite. Emulators lack NNAPI/NPU. Use evals for regression detection on emulators, not absolute comparison.
