# Architecture

---

## Glossary

* **Embedding:** Fixed-length vector (128-d in this project) encoding semantic meaning. L2-normalized, so cosine similarity reduces to dot product.
* **NNAPI:** Android Neural Networks API. Delegates TFLite operations to NPU, DSP, or GPU.
* **NPU:** Neural Processing Unit. On the target Snapdragon 8 Elite: Hexagon NPU, 75 TOPS.
* **TFLite:** TensorFlow Lite. On-device inference runtime for MobileBERT (text) and EfficientNet-B0 (vision).
* **Heuristic Fallback:** Fallback when TFLite model assets are unavailable. Keyword-based feature extraction + feature hashing.
* **Model Inference:** TFLite with NNAPI delegation for hardware-accelerated inference.

---

## System Overview

SnapBadgers is a single-activity Android application that recommends songs based on multimodal input: a text query, an optional camera image, and live device sensor readings. A 6-stage on-device ML pipeline encodes each modality into a 128-d embedding, fuses them, projects the result into song embedding space, and ranks a catalog of pre-embedded tracks by cosine similarity. All inference runs locally via TFLite with NNAPI delegation, falling back to CPU (XNNPack) on unsupported hardware.

## Directory Structure

```
com/example/snapbadgers/
├── ai/
│   ├── common/                 # EncoderUtils (shared logging, asset checks, zero-vector detection)
│   │   └── ml/                 # VectorUtils (128-d math), TwoLayerMLP (shared MLP base class)
│   ├── fusion/                 # FusionEngine — weighted multimodal fusion
│   ├── pipeline/               # RecommendationPipeline — orchestrates the 6-stage flow
│   ├── projection/             # ProjectionNetwork — maps fused embedding to song space
│   ├── sensor/                 # SensorData, SensorEncoder, SensorEncoderMLP, SensorFeatureExtractor
│   ├── text/                   # TextEncoder interface, StubTextEncoder, FallbackTextEncoder,
│   │                           #   HeuristicTextEmbedding, Tokenizer
│   │   └── ml/                 # QualcommTextEncoder, BertTokenizer, ModelLoader
│   └── vision/                 # VisionEncoder, QualcommVisionEncoder, ImageProcessor
├── data/                       # SongRepository, SettingsRepository
├── model/                      # Song, UiState, InferenceSteps, RecommendationResult,
│                               #   EmbeddedTrack, HistoryItem
├── songembeddings/             # Spotify sync feature (independent Activity + network + embedding)
│   ├── embedding/              # EmbeddingUtils
│   ├── model/                  # AudioFeatures
│   ├── network/                # AuthApi, ReccoBeatsApi, SpotifyApi
│   └── repository/             # SpotifyRepository
└── ui/
    ├── MainActivity.kt, SnapBadgersDemoScreen.kt
    ├── components/             # CameraInputCard, Header, HistoryAndLibrary, InferenceStatusCard,
    │                           #   RecommendationCard, SettingsScreen, StateCards
    ├── i18n/                   # AppI18n
    └── theme/                  # Color, Theme, Type
```

48 source files, ~4,358 lines of Kotlin.

## Tech Stack & Decision Record

| Category | Technology | Rationale |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0 | Required by Jetpack Compose. Coroutines for parallel encoder execution. |
| **UI** | Jetpack Compose | Declarative UI with built-in state management. |
| **ML Runtime** | TensorFlow Lite 2.16 | Industry-standard on-device inference. NNAPI delegation and int8 quantization. |
| **Text Model** | MobileBERT (TFLite) | 25M parameters, optimized for mobile. Input: 128 WordPiece tokens. Output: 128-d embedding. |
| **Vision Model** | EfficientNet-B0 int8 (TFLite) | Smallest EfficientNet variant, int8-quantized for NPU compatibility. Input: 224x224x3 float32. Output: 128-d embedding. |
| **Sensor Pipeline** | Pure Kotlin MLP | No framework dependency. 14-d feature vector from accelerometer + light + GPS + clock, projected to 32-d via a 2-layer MLP (14->32->32). |
| **Fusion** | Weighted average | Weights: vision 0.60, text 0.25, sensor 0.15. Missing modalities excluded; weights re-normalized. More interpretable than attention-based fusion at this scale. |
| **Projection** | TwoLayerMLP (128->128->128) | Maps the fused embedding into song embedding space. He-initialized with seed 42 for deterministic output. |
| **Song Embeddings** | Spotify audio features -> MLP | 15-d audio features (danceability, energy, etc.) projected to 128-d via a deterministic MLP. Generated offline by the `songembeddings/` module. |
| **Build** | Gradle 8 + AGP | Standard Android build toolchain. BuildConfig injects Spotify credentials from `local.properties`. |
| **Async** | Kotlin Coroutines | `coroutineScope` with parallel `async` for concurrent encoder execution. `Mutex` for thread-safe lazy initialization. |

## Data Flow

1. **Text Encoding.** User query tokenized by `BertTokenizer` (WordPiece, max 128 tokens), passed through `QualcommTextEncoder` (MobileBERT via TFLite/NNAPI). Output: 128-d L2-normalized. Fallback: `StubTextEncoder` via `HeuristicTextEmbedding` (keyword-based feature extraction + feature hashing).

2. **Vision Encoding.** Optional camera frame resized to 224x224, ImageNet-normalized (mean=[0.485,0.456,0.406], std=[0.229,0.224,0.225]), passed through `QualcommVisionEncoder` (EfficientNet-B0 int8 via TFLite/NNAPI). Output: 128-d L2-normalized. Fallback: pixel-based feature extraction (8 color features from 16x16 grid).

3. **Sensor Encoding.** `SensorEncoder` reads accelerometer (20-sample rolling window), light, GPS, clock. `SensorFeatureExtractor` derives 14-d features (motion magnitude, variance, light level, time-of-day). `SensorEncoderMLP` projects to 32-d L2-normalized.

4. **Fusion.** `FusionEngine.fuse()` aligns modality embeddings to 128-d via sparse random projection with per-modality hash seeds (text=11, vision=211, sensor=23), then weighted average (vision 0.60, text 0.25, sensor 0.15). Missing modalities excluded; weights re-normalized. Output: 128-d L2-normalized.

5. **Projection.** `ProjectionNetwork.project()` maps fused 128-d into song embedding space via 2-layer MLP (128->128->128, He init, seed 42). Output: 128-d L2-normalized.

6. **Song Ranking.** `SongRepository.findTopSongs()` computes cosine similarity of projected query against all song embeddings. Returns top 3. Catalog: `tracks_features.json` (Spotify-derived) if available, else `sample_songs.json` (metadata-derived).

### Threading Model

* `RecommendationPipeline.runPipeline()` launches text, vision, and sensor encoding as three parallel `async` blocks within a `coroutineScope`.
* `QualcommTextEncoder.encode()` dispatches to `Dispatchers.Default` for off-main-thread inference.
* `RecommendationPipeline.getOrCreateTextEncoder()` and `VisionEncoder.getOrCreateModelEncoder()` use `Mutex` for thread-safe lazy initialization.
* Sensor listeners fire on the main thread (default Android `SensorManager` behavior). Accelerometer window access is synchronized.
* The UI launches the pipeline from a Compose `rememberCoroutineScope` (main dispatcher). Heavy work is dispatched internally.

### Hardware Acceleration

* Both `QualcommTextEncoder` and `QualcommVisionEncoder` attempt NNAPI delegation first, falling back to CPU (XNNPack) if unavailable.
* Target device: Samsung Galaxy S25, Snapdragon 8 Elite (SM8750), Hexagon NPU 75 TOPS, 12 GB LPDDR5X, Android 15.
* EfficientNet-B0 is int8-quantized for NPU compatibility.
* `NnApiDelegate` is deprecated starting Android 15 (SDK 35). Migration path: QNN delegate -> GPU delegate -> XNNPack.

## Design Decisions

* **Decision:** Fallback encoder pattern for both text and vision.
  * **Alternative Considered:** Fail hard if model assets are missing.
  * **Rationale:** Must be demonstrable on any device, including emulators without model assets. Heuristic fallback produces deterministic, non-zero embeddings that keep cosine similarity meaningful. `FallbackTextEncoder` permanently switches to the stub after any primary failure, avoiding repeated TFLite errors.

* **Decision:** `TwoLayerMLP` as a shared pure-Kotlin base class.
  * **Alternative Considered:** Separate MLP implementations per encoder, or TFLite models for all stages.
  * **Rationale:** `SensorEncoderMLP` (14->32->32) and `ProjectionNetwork` (128->128->128) share identical forward-pass logic. Base class eliminated a duplicated `l2Normalize()` bug and ~40 lines. Pure Kotlin avoids TFLite overhead for networks where inference is <1ms.

* **Decision:** `VectorUtils.alignToEmbeddingDimension()` with modality-specific salts.
  * **Alternative Considered:** Zero-padding or truncation.
  * **Rationale:** Sensor embeddings are 32-d while text/vision are 128-d. Zero-padding concentrates information in the first 32 dimensions, creating structural bias. Sparse random projection distributes each input element across three positions (weights 1.0, 0.5, 0.25) using modular arithmetic with distinct primes (31, 17). Per-modality hash seeds produce distinct projection bases, reducing cross-modal interference.

* **Decision:** Weighted average fusion with fixed weights (0.60/0.25/0.15).
  * **Alternative Considered:** Attention-based fusion, learned weights.
  * **Rationale:** Three modalities do not justify attention complexity. Fixed weights are interpretable and tunable without retraining. Vision weighted highest (most context-specific); sensor lowest (ambient, noisy).

* **Decision:** Song catalog loaded from device-local JSON, not a remote API.
  * **Alternative Considered:** Server-side catalog with API calls.
  * **Rationale:** On-device inference is a core constraint. `songembeddings/` generates `tracks_features.json` offline from Spotify data, eliminating network latency and enabling offline operation.
