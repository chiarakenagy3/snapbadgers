# Testing

## Strategy

JUnit 4 with AndroidX Test. Three tiers:

* **JVM Unit Tests** (`app/src/test/`): Pure-Kotlin pipeline stages. Vector math, embedding quality, fusion, projection, sensor features, end-to-end heuristic pipeline. No device required.
* **Instrumented Evals** (`app/src/androidTest/.../eval/`): On-device model validation. Inference latency (p50/p95/p99), accuracy, load times, memory profiling. Require TFLite model assets.
* **Instrumented Integration Tests** (`app/src/androidTest/.../integration/`): End-to-end recommendation, NPU validation, stress tests, BertTokenizer integration.
* **UI Tests** (`app/src/androidTest/.../ui/`): Compose component error-state tests.

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

Use `:app:onDeviceTest` — it installs via plain `adb install` and runs via `am instrument`.
This is the canonical path for this repo because AGP 9's bundled ddmlib `SplitApkInstaller`
fails with `Failed to install-write all apks` against usbipd-forwarded devices (WSL2 rigs),
and the `onDeviceTest` path works identically on native-USB rigs as well.

```bash
# All instrumented tests
./gradlew :app:onDeviceTest

# Single class
./gradlew :app:onDeviceTest -PtestClass=com.example.snapbadgers.eval.InferenceLatencyEval

# Exclude a class (e.g. to skip the long battery eval)
./gradlew :app:onDeviceTest -PnotClass=com.example.snapbadgers.eval.BatteryImpactEval
```

`connectedDebugAndroidTest` is the AGP default but is NOT recommended on this repo — it
fails over usbipd. If you are on a native-USB rig and prefer it, it still works there.

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
| `SustainedLoadEval` | 200-iteration full-pipeline run; early/late window drift ratio as thermal throttling proxy | Drift ratio <= 3.0 |
| `BatteryImpactEval` | BatteryManager deltas (capacity%, energy, charge, current, temperature) across 150 pipeline iterations | Capacity delta >= -5% while discharging |

### Instrumented Integration Tests

| Test Class | What It Validates |
| :--- | :--- |
| `EndToEndRecommendationTest` | Full pipeline from text input to ranked song output on-device |
| `HardwareNpuValidationTest` | Qualcomm SoC detection, NNAPI availability, NPU performance characteristics, thermal throttling, Hexagon DSP |
| `StressTestSuite` | Pipeline under sustained load |
| `BertTokenizerIntegrationTest` | WordPiece tokenization against real `vocab.txt` asset |
| `CameraCaptureIntegrationTest` | CameraInputCard hoisted-state contract and pipeline vision-input wiring |
| `SensorEncoderLifecycleTest` | Sensor encoder start/stop idempotency, concurrent-embedding safety, 14-d features |
| `VisionEncoderFallbackTest` | VisionEncoder stub fallback when model asset is missing, determinism |

### UI Tests

| Test Class | What It Validates |
| :--- | :--- |
| `SnapBadgersUiTest` | Main screen initialization, sidebar navigation (Library/History/Settings), text input + Analyze button |
| `SongEmbeddingUiPerformanceTest` | Navigation latency to Music Library tab |

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
