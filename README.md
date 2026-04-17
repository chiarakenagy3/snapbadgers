![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK_36-3DDC84?logo=android&logoColor=white)
![TFLite](https://img.shields.io/badge/TFLite-2.16-FF6F00?logo=tensorflow&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

# SnapBadgers

## Overview

**SnapBadgers** is an Android application that recommends songs by fusing multimodal input -- a text query, an optional camera image, and live device sensor readings -- through a 6-stage on-device ML pipeline. All inference runs locally using TensorFlow Lite with NNAPI delegation for NPU acceleration on Qualcomm SoCs.

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
spotify.client.id="YOUR_CLIENT_ID"
spotify.client.secret="YOUR_CLIENT_SECRET"
spotify.refresh.token="YOUR_REFRESH_TOKEN"
```

See [README_SongEmbeddings.md](README_SongEmbeddings.md) for full Spotify setup instructions.

### Running Tests

```bash
# JVM unit tests (no device required)
./gradlew testDebugUnitTest

# JVM eval suite only
./gradlew testDebugUnitTest --tests "*.eval.*"

# Instrumented tests (requires device)
./gradlew connectedDebugAndroidTest
```

## Documentation

* **[ARCHITECTURE.md](docs/ARCHITECTURE.md):** System design, 6-stage pipeline data flow, tech stack decisions, threading model, and hardware acceleration strategy.
* **[TESTING.md](docs/TESTING.md):** Test strategy, eval suite inventory, running tests, latency targets, and troubleshooting.
* **[README_SongEmbeddings.md](README_SongEmbeddings.md):** Spotify sync module setup and embedding generation.

## Project Information

Developed by **Team Qualcomm** for CS 620.
