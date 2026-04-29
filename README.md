# SnapBadgers

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK_36-3DDC84?logo=android&logoColor=white)
![TFLite](https://img.shields.io/badge/TFLite-2.16-FF6F00?logo=tensorflow&logoColor=white)

**SnapBadgers** is an Android application that recommends songs by fusing multimodal input -- a text query, an optional camera image, and live device sensor readings -- through a 6-stage on-device ML pipeline. All inference runs locally using TensorFlow Lite with NNAPI delegation for NPU acceleration on Qualcomm SoCs.

### [Project Repo Link](https://github.com/chiarakenagy3/snapbadgers)

## Assignment Checklist (README Requirements)

- **Link to your repository**: `https://github.com/chiarakenagy3/snapbadgers`
- **Set-up steps**: See **Setup** (includes model assets + optional Spotify credentials)
- **Overview of how the code works**: See **How It Works** and **Code Overview** (especially `docs/ARCHITECTURE.md`)
- **What works & what doesn’t**: See **What Works** and **What Doesn't**
- **What would you work on next**: See **What's Next**

### How It Works

1. **Text Encoding.** A user query (e.g., "energetic workout music") is tokenized and encoded by MobileBERT into a 128-d embedding.
2. **Vision Encoding.** An optional camera frame is processed by EfficientNet-B0 (int8 quantized) into a 128-d embedding.
3. **Sensor Encoding.** Accelerometer, light, GPS, and clock readings are extracted into a 14-d feature vector, then projected to 32-d via a two-layer MLP.
4. **Fusion.** Modality embeddings are aligned to 128-d and combined via weighted average (vision 0.60, text 0.25, sensor 0.15).
5. **Projection.** A projection network maps the fused embedding into song embedding space (128-d).
6. **Ranking.** Cosine similarity against pre-computed song embeddings returns the top 3 matches.

When model assets are unavailable (e.g., on emulators), the pipeline falls back to a deterministic heuristic encoder that maintains full pipeline functionality.

### Features

* **Multimodal inference** -- text, vision, and sensor modalities fused into a single query embedding
* **On-device ML** -- MobileBERT and EfficientNet-B0 via TFLite, no server required
* **NPU acceleration** -- NNAPI delegation for Qualcomm Hexagon NPU/DSP
* **Graceful fallback** -- heuristic encoders activate automatically when model assets are missing or inference fails
* **Spotify integration** -- offline song embedding generation from Spotify audio features via a separate sync module
* **Eval suite** -- 12 eval test classes covering latency, accuracy, memory, and pipeline correctness

## Setup

### Prerequisites

* Android Studio Iguana (2023.2.1) or newer
* Android SDK 36 (compile) / SDK 24+ (min)
* Physical device recommended (Samsung Galaxy S25 or other Qualcomm SoC for NPU acceleration)

### Android Configuration

1. **Gradle Sync**: Open the project in Android Studio and ensure `Gradle JDK` is set to **Java 17** in `Settings > Build, Execution, Deployment > Build Tools > Gradle`.
2. **Local Properties**: Create or update `local.properties` in the root directory to include Spotify credentials (optional, see below) and ensure the `sdk.dir` is correct.
3. **TFLite Assets**: Ensure `.tflite` files are recognized as non-compressible by the build system. This is already configured in `app/build.gradle.kts` via `androidResources.noCompress += "tflite"`.

### Build and Run

```bash
git clone <repository-url>
cd snapbadgers
```

Open in Android Studio, sync Gradle, and run on a physical device or emulator.

### Model Assets

The TFLite model files are not checked into git. Place these in `app/src/main/assets/`:

| File | Purpose |
| :--- | :--- |
| `mobile_bert.tflite` | MobileBERT text encoder |
| `vocab.txt` | WordPiece vocabulary for BertTokenizer |
| `efficientnet_b0_128d_int8.tflite` | EfficientNet-B0 vision encoder (int8 quantized) |

The app functions without these files using heuristic fallback encoders.

### Spotify Credentials (Optional)

For the song embedding sync feature, add to `local.properties`:

```properties
spotify.token="YOUR_SPOTIFY_BEARER_TOKEN"          # optional (some flows may use this)
spotify.client.id="YOUR_CLIENT_ID"
spotify.client.secret="YOUR_CLIENT_SECRET"
spotify.refresh.token="YOUR_REFRESH_TOKEN"
```

See [README_SongEmbeddings.md](README_SongEmbeddings.md) for full Spotify setup instructions.

### Code Overview

If you want to trace the “6-stage pipeline” through the actual code, these are the most direct entry points:

- **UI entry**: `app/src/main/java/com/example/snapbadgers/ui/MainActivity.kt` launches `SnapBadgersDemoScreen`
- **Pipeline orchestrator**: `app/src/main/java/com/example/snapbadgers/ai/pipeline/RecommendationPipeline.kt`
  - Runs text/vision/sensor encoders in parallel coroutines, then **fusion → projection → ranking**
- **Encoders**:
  - Text: `ai/text/` + `ai/text/ml/` (MobileBERT + tokenizer + fallback)
  - Vision: `ai/vision/` (EfficientNet + preprocessing)
  - Sensor: `ai/sensor/` (feature extraction + small MLP)
- **Fusion & projection**: `ai/fusion/FusionEngine.kt`, `ai/projection/ProjectionNetwork.kt`
- **Ranking / catalog**: `data/SongRepository.kt` loads JSON catalog and performs cosine similarity top-k

For a deeper walkthrough, see `docs/ARCHITECTURE.md` (directory tree + data flow + threading + hardware acceleration notes).

### Running Tests

```bash
# JVM unit tests (no device required)
./gradlew testDebugUnitTest

# JVM eval suite only
./gradlew testDebugUnitTest --tests "*.eval.*"

# Instrumented tests (requires device)
./gradlew connectedDebugAndroidTest
```
## What Works
* **End-to-end multimodal pipeline** — all six stages (text, vision, sensor, fusion, projection, ranking) function cohesively on-device  
* **Modular architecture** — each stage is decoupled, making the pipeline easy to test, debug, and extend  
* **Evaluation coverage** — test suite validates latency, correctness, and pipeline behavior across multiple scenarios  
* **Recommendation system** — app generates top 3 song recommendations using fused multimodal embeddings and cosine similarity  

## What Doesn't 
* **Limited personalization** — recommendations are not yet adapted to individual user preferences or feedback  
* **Similarity performance** — similarity score quality still shows room for improvement, particularly in edge or ambiguous cases  
* **Cold-start limitations** — without strong input signals (e.g., no image or weak query), results can be generic  

## What's Next
* **Streaming integration** — enable direct playback and deeper integration with platforms like Spotify  
* **Model optimization** — further reduce latency and memory footprint through pruning, quantization, and delegate tuning  
* **User-controlled sensor input** — allow users to customize which sensors are shared and used at runtime  
* **Expanded modality support** — integrate audio snippets or microphone input for real-time ambient sound context  

## Documentation

* **[ARCHITECTURE.md](docs/ARCHITECTURE.md):** System design, 6-stage pipeline data flow, tech stack decisions, threading model, and hardware acceleration strategy.
* **[TESTING.md](docs/TESTING.md):** Test strategy, eval suite inventory, running tests, latency targets, and troubleshooting.
* **[README_SongEmbeddings.md](README_SongEmbeddings.md):** Spotify sync module setup and embedding generation.

## Project Information

Developed by **Team Qualcomm** for CS 620.
