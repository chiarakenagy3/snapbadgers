# SnapBadgers: Multi-Modal AI Music Recommendation Engine

SnapBadgers is a personalized music discovery system that bridges the gap between your Spotify library and your current environment. By fusing **Textual Context** (NLP) and **Real-time Motion** (Sensors), it finds the perfect match in your personal library using high-dimensional vector similarity.

---

## 🧩 Deep Technical Architecture

### 1. Data Sync Phase: Building the Vector Index
The app creates a personalized "Music Knowledge Base" by transforming listening history into a searchable vector space.

*   **Spotify Integration**: Uses the `/v1/me/top/tracks` endpoint to fetch the user's `medium_term` (6 months) top 20 tracks.
*   **Acoustic Feature Extraction**: Since Spotify API doesn't provide raw waveforms, SnapBadgers queries the **ReccoBeats API** (`/v1/audio-features/{id}`) for 10 structural features:
    *   *Tempo, Loudness, Danceability, Energy, Acousticness, Valence, etc.*
*   **Vectorization Strategy (`EmbeddingUtils`)**:
    1.  **Normalization**: Loudness and Tempo are mapped to `[0, 1]` ranges.
    2.  **Derived Features**: New features are calculated (e.g., `Danceability * Energy`) to capture non-linear relationships.
    3.  **128-d Projection**: The base features are projected into a 128-dimensional space using a deterministic projection matrix (Sine-based) for consistency.
    4.  **L2 Normalization**: Vectors are normalized to unit length so that **Dot Product** equals **Cosine Similarity**.

### 2. Inference Pipeline: The Vibe Engine
*   **Text Encoding (NLP)**: Uses **MobileBERT** to convert text prompts into a 128-d semantic vector.
*   **Sensor Encoding (Motion)**: Converts 1-second motion windows into a 128-d activity intensity vector.
*   **Vector Fusion (`FusionEngine`)**:
    *   **Method**: **Weighted Element-wise Fusion** (Replaced Concatenation).
    *   **Math**: `Fused[i] = (Text[i] * 0.6) + (Sensor[i] * 0.4)`.
    *   This ensures the sensor data actively influences the final 128-d query vector.
*   **Similarity Search**: Performs a search against your local `tracks_features.json` to find the nearest neighbor.

---

## 📥 Data Schema: `tracks_features.json`

The local database is stored in the app's internal `/files` directory with the following structure:

```json
[
  {
    "trackId": "4pmc2WpYvCu7H9Yv9dy",
    "name": "Anti-Hero",
    "artists": "Taylor Swift",
    "source": "MySpotify",
    "embedding": [0.123, -0.456, 0.789, ...] // 128 floats
  }
]
```

---

## 🛠 Usage Instructions

### 1. Developer Setup
Configure `local.properties` with your Spotify Developer App credentials:

```properties
SPOTIFY_CLIENT_ID=your_id
SPOTIFY_CLIENT_SECRET=your_secret
SPOTIFY_REFRESH_TOKEN=your_token
```

### 2. Initialization & Sync
1.  **Launch**: Open the main screen.
2.  **Auth**: App refreshes the Spotify Access Token automatically.
3.  **Sync**: 
    *   Fetches Top Tracks from Spotify.
    *   Requests features from `api.reccobeats.com`.
4.  **Status Check**: Wait until the log shows: `✓ Recommendations are now based on YOUR Spotify tracks`.

### 3. Running a Vibe Check
1.  Go to the **Recommendation Screen**.
2.  **Prompt**: Enter your current mood (e.g., "Late night coding session").
3.  **Action**: Tap **"Run AI Pipeline"**.
4.  **Result**: The app returns the song from your Top 20 that best matches the combined energy of your prompt and your current physical movement.

---

## 📂 Key Source Components

| Component | Responsibility |
| :--- | :--- |
| `RecommendationPipeline.kt` | Orchestrates the sync, library loading, and inference steps. |
| `ReccoBeatsApi.kt` | High-latency network calls for structural audio analysis. |
| `RecommendationService.kt` | The mathematical engine for 128-d Dot Product ranking. |
| `EmbeddingUtils.kt` | Deterministic projection logic for 128-d vector space consistency. |

---

## ⚠️ Troubleshooting & Limits
*   **ReccoBeats DB**: If your Spotify song is very new or rare, ReccoBeats may return `404`. The app will skip these songs automatically.
*   **Wait for Sync**: The Recommendation Screen will show "Library Sync Required" if you haven't completed the sync on the main screen since the last app install.
*   **Model Accuracy**: On-device MobileBERT requires specific `vocab.txt` and `mobile_bert.tflite` files in the `assets/` folder to function correctly.
