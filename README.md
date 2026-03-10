# Snapbadgers
This is the team project for Team Qualcomm in CS 620.

Snapbadgers is an Android application that provides personalized music recommendations by fusing user text input with real-time sensor data using a simulated AI pipeline.

## 🚀 Features

- **Multi-modal Inference Pipeline**: Combines text descriptions and environmental sensor data (Accelerometer, Light) to generate recommendations.
- **Sensor Integration**: Utilizes device sensors to capture the user's current "vibe" or environment.
- **Song Repository**: Matches the fused embedding against a curated list of songs to find the best match.
- **Jetpack Compose UI**: A modern, reactive interface that displays inference steps and recommendation results.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Asynchronous Programming**: Kotlin Coroutines
- **Architecture**: Clean Architecture principles with distinct layers for Data, Model, AI Pipeline, and UI.

## 📂 Project Structure

- `ai/`: Contains the logic for text encoding, sensor data collection, and the fusion engine.
- `data/`: Manages song data and similarity calculations.
- `model/`: Defines core data structures like `Song`, `UiState`, and `InferenceSteps`.
- `ui/`: Compose-based screens and components.

## ⚙️ How it Works

1. **Input**: User enters a text description (e.g., "Chill evening").
2. **Sensor Collection**: The app collects brief samples from the accelerometer and light sensor.
3. **Encoding**: Both text and sensor data are transformed into numerical embeddings.
4. **Fusion**: The `FusionEngine` merges these embeddings into a single representation.
5. **Recommendation**: The `SongRepository` performs a similarity search to find the most appropriate song.

## 📝 Setup

1. Clone the repository.
2. Open the project in Android Studio (Iguana or newer recommended).
3. Ensure you have the Android SDK installed.
4. Build and run on a physical device or emulator.

*Note: This project uses `android.overridePathCheck=true` in `gradle.properties` to support build paths containing non-ASCII characters.*
