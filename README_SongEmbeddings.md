# SnapBadger: Song Embedding Generation System 🎵🦡

SnapBadger is an Android-based pipeline designed to transform music metadata into high-dimensional vector embeddings. It bridges the gap between Spotify user data and the ReccoBeats music database to create a searchable, feature-rich dataset for music recommendation tasks.

## 🚀 Core Features

- **Spotify User Integration**: Synchronizes with the Spotify Web API to fetch a user's top-played tracks.
- **Deep Search Logic**: A robust multi-stage retrieval algorithm (Artist -> Album -> Track) that ensures matches even when standard search indexing fails.
- **Audio Feature Extraction**: Retrieves 10+ granular acoustic metrics (danceability, energy, acousticness, etc.) via ReccoBeats.
- **MLP-based Projection**: Maps raw audio features into a normalized **128-dimensional** embedding space using a deterministic 2-layer Multi-Layer Perceptron (MLP).
- **JSON Export**: Automatically persists processed track data and embeddings to the device's internal storage for downstream use.

## 🛠 Configuration & Setup

To use this system, you must configure your Spotify Developer credentials in the `local.properties` file at the root of the project.

### 1. Spotify Developer Setup
1. Visit the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. Create an application and obtain your **Client ID** and **Client Secret**.
3. Generate a **Refresh Token** with the `user-top-read` scope.

### 2. Local Properties Configuration
Add the following to your `local.properties` file:

```properties
spotify.client.id="YOUR_CLIENT_ID"
spotify.client.secret="YOUR_CLIENT_SECRET"
spotify.refresh.token="YOUR_REFRESH_TOKEN"
```

The build system will automatically inject these into the `BuildConfig` class during compilation.

## 📖 How it Works

1. **Authentication**: Upon launch, the app uses the refresh token to negotiate a secure session with Spotify.
2. **Track Discovery**: The system identifies the user's top tracks (Medium Term).
3. **Database Matching**:
    - It first attempts a direct search on ReccoBeats.
    - If no match is found, it triggers **Deep Search**, scanning the artist's discography album-by-album to locate the specific track ID.
4. **Embedding Generation**: 
    - The app calculates 5 derived features from the raw data.
    - The 15-dimensional input is projected to 128 dimensions via the MLP.
    - The final vector is L2-normalized for cosine similarity compatibility.
5. **Persistence**: The resulting `tracks_features.json` is saved in the app's internal file directory (`/data/data/com.example.snapbadgers/files/`).

## 🔬 Technical Specifications

- **Embedding Dimension**: 128
- **Projection Model**: 2-Layer MLP (Input 15 -> Hidden 64 [ReLU] -> Output 128 [Linear]).
- **Determinism**: Weight initialization uses a fixed seed (`42L`), ensuring that identical audio features always yield identical embeddings regardless of the device.

## 👥 Project Information
This project is developed by **Team Qualcomm** for CS 620.
