![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK_36-3DDC84?logo=android&logoColor=white)
![TFLite](https://img.shields.io/badge/TFLite-2.16-FF6F00?logo=tensorflow&logoColor=white)

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

### Toolchain

Pinned versions used to build and verify this app:

| Component | Version |
| :--- | :--- |
| JDK | 17 |
| Android Gradle Plugin | 9.0.1 |
| Kotlin | 2.2.10 |
| Gradle | 8.x |
| `compileSdk` / `targetSdk` | 36 |
| `minSdk` | 24 |

Authoritative source: [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

### Build and Run

```bash
git clone <repository-url>
cd snapbadgers
```

Open in Android Studio, sync Gradle, and run on a physical device or emulator.

### Getting the models

The TFLite model files live under `app/src/main/assets/` but are not packaged with the source distribution. Generate them with the bundled download/export script:

```bash
# 1. Install the Qualcomm AI Hub Python client
pip install qai-hub

# 2. Set your AI Hub API token (do NOT hard-code it)
export QAI_HUB_API_TOKEN="<your-token-from-aihub.qualcomm.com>"

# 3. Run the download / export pipeline
python model_dl.py
```

This produces and places the following into `app/src/main/assets/`:

| File | Purpose |
| :--- | :--- |
| `mobile_bert.tflite` | MobileBERT text encoder |
| `vocab.txt` | WordPiece vocabulary for BertTokenizer |
| `efficientnet_b0_128d_int8.tflite` | EfficientNet-B0 vision encoder (int8 quantized) |

The app functions without these files using heuristic fallback encoders, but on-device acceleration requires the real models.

### Spotify Credentials (Optional)

The song-embedding sync module uses the Spotify Web API to pull a user's top
tracks and derive audio features. If you don't supply credentials the app
falls back to the bundled `sample_songs.json` catalog.

1. Create an app at the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and copy the **Client ID** and **Client Secret**.
2. Generate a **Refresh Token** with the `user-top-read` scope.
3. Add the following to `local.properties` — the build injects them into `BuildConfig`:

```properties
spotify.client.id="YOUR_CLIENT_ID"
spotify.client.secret="YOUR_CLIENT_SECRET"
spotify.refresh.token="YOUR_REFRESH_TOKEN"
```

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

## Third-party assets

Models downloaded by `model_dl.py` are sourced from [Qualcomm AI Hub](https://aihub.qualcomm.com/models) and are released under separate licenses. Refer to each model's page on AI Hub (or the corresponding entry in [`qualcomm/ai-hub-models`](https://github.com/qualcomm/ai-hub-models)) for license details.
