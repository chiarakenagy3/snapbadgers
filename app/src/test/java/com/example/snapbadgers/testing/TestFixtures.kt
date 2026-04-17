package com.example.snapbadgers.testing

import android.graphics.Bitmap
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.sensor.SensorData
import com.example.snapbadgers.songembeddings.model.AudioFeatures

/**
 * Shared test fixtures and data builders for consistent test data across the test suite.
 *
 * Industry best practice: Centralize test data creation to ensure consistency,
 * reduce duplication, and make tests more maintainable.
 */
object TestFixtures {

    // ========== Songs ==========

    fun createTestSong(
        title: String = "Test Song",
        artist: String = "Test Artist",
        embeddingDim: Int = 128,
        similarity: Float = 0.95f
    ) = Song(
        title = title,
        artist = artist,
        embedding = FloatArray(embeddingDim) { 0.5f },
        similarity = similarity
    )

    fun createCalmSong() = createTestSong(
        title = "Calm Waters",
        artist = "Peaceful Artist",
        similarity = 0.92f
    )

    fun createEnergeticSong() = createTestSong(
        title = "High Energy",
        artist = "Energetic Artist",
        similarity = 0.88f
    )

    fun createTestSongList(count: Int = 5): List<Song> =
        (1..count).map { i ->
            createTestSong(
                title = "Song $i",
                artist = "Artist $i",
                similarity = 1.0f - (i * 0.05f)
            )
        }

    // ========== Embeddings ==========

    fun createNormalizedEmbedding(dim: Int = 128, seed: Int = 42): FloatArray {
        val embedding = FloatArray(dim) { (it + seed) * 0.01f }
        return normalizeVector(embedding)
    }

    fun createZeroEmbedding(dim: Int = 128) = FloatArray(dim) { 0f }

    fun createRandomEmbedding(dim: Int = 128): FloatArray {
        val embedding = FloatArray(dim) { kotlin.random.Random.nextFloat() }
        return normalizeVector(embedding)
    }

    // ========== Sensor Data ==========

    fun createStationarySensorData(
        lightLux: Float = 100f,
        hourOfDay: Int = 12,
        latitude: Double = 37.7749,
        longitude: Double = -122.4194
    ) = SensorData(
        accelWindow = listOf(floatArrayOf(0f, 0f, 9.8f)),  // Gravity only
        lightLux = lightLux,
        hourOfDay = hourOfDay,
        latitude = latitude,
        longitude = longitude
    )

    fun createMovingSensorData(
        intensity: Float = 2.0f,
        lightLux: Float = 500f,
        hourOfDay: Int = 14
    ) = SensorData(
        accelWindow = List(10) {
            floatArrayOf(
                intensity * kotlin.random.Random.nextFloat(),
                intensity * kotlin.random.Random.nextFloat(),
                9.8f + intensity * kotlin.random.Random.nextFloat()
            )
        },
        lightLux = lightLux,
        hourOfDay = hourOfDay,
        latitude = 37.7749,
        longitude = -122.4194
    )

    fun createWorkoutSensorData() = createMovingSensorData(
        intensity = 5.0f,
        lightLux = 800f,
        hourOfDay = 18
    )

    // ========== Audio Features ==========

    fun createTestAudioFeatures(
        id: String = "test_track",
        danceability: Float = 0.5f,
        energy: Float = 0.5f,
        speechiness: Float = 0.05f,
        acousticness: Float = 0.3f,
        instrumentalness: Float = 0.0f,
        liveness: Float = 0.1f,
        valence: Float = 0.5f,
        tempo: Float = 120f,
        loudness: Float = -5f,
        duration_ms: Int = 200000
    ) = AudioFeatures(
        id = id,
        danceability = danceability,
        energy = energy,
        key = 0,
        loudness = loudness,
        mode = 1,
        speechiness = speechiness,
        acousticness = acousticness,
        instrumentalness = instrumentalness,
        liveness = liveness,
        valence = valence,
        tempo = tempo,
        duration_ms = duration_ms,
        time_signature = 4
    )

    fun createCalmAudioFeatures() = createTestAudioFeatures(
        id = "calm_track",
        danceability = 0.2f,
        energy = 0.1f,
        acousticness = 0.9f,
        valence = 0.3f,
        tempo = 70f,
        loudness = -12f
    )

    fun createEnergeticAudioFeatures() = createTestAudioFeatures(
        id = "energetic_track",
        danceability = 0.95f,
        energy = 0.9f,
        acousticness = 0.1f,
        valence = 0.85f,
        tempo = 140f,
        loudness = -3f
    )

    // ========== Test Bitmaps (for Vision Encoding) ==========

    /**
     * Creates a test bitmap for vision encoding tests.
     * Note: In unit tests, use mocked Bitmap. In instrumented tests, use real Bitmap.
     */
    fun createTestBitmapConfig(
        width: Int = 224,
        height: Int = 224
    ) = Triple(width, height, Bitmap.Config.ARGB_8888)

    // ========== Utilities ==========

    private fun normalizeVector(vec: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vec) sumSquares += v * v
        val norm = kotlin.math.sqrt(sumSquares)
        return if (norm < 1e-8f) {
            FloatArray(vec.size) { 0f }
        } else {
            FloatArray(vec.size) { vec[it] / norm }
        }
    }

    /**
     * Asserts that an embedding is properly normalized (L2 norm ≈ 1.0).
     */
    fun assertEmbeddingIsNormalized(embedding: FloatArray, tolerance: Float = 0.01f) {
        val magnitude = kotlin.math.sqrt(embedding.map { it * it }.sum())
        assert(magnitude in (1.0f - tolerance)..(1.0f + tolerance)) {
            "Embedding not normalized: magnitude = $magnitude"
        }
    }

    /**
     * Asserts that all values in the array are finite (not NaN or Infinity).
     */
    fun assertAllFinite(array: FloatArray) {
        assert(array.all { it.isFinite() }) {
            "Array contains non-finite values: ${array.filter { !it.isFinite() }}"
        }
    }

    /**
     * Asserts that all values are in the specified range.
     */
    fun assertInRange(array: FloatArray, min: Float, max: Float) {
        assert(array.all { it in min..max }) {
            "Array contains out-of-range values: ${array.filter { it !in min..max }}"
        }
    }
}

/**
 * Builder pattern for creating complex test objects.
 */
class SongBuilder {
    private var title: String = "Test Song"
    private var artist: String = "Test Artist"
    private var embedding: FloatArray = FloatArray(128) { 0.5f }
    private var similarity: Float = 0.95f

    fun withTitle(title: String) = apply { this.title = title }
    fun withArtist(artist: String) = apply { this.artist = artist }
    fun withEmbedding(embedding: FloatArray) = apply { this.embedding = embedding }
    fun withSimilarity(similarity: Float) = apply { this.similarity = similarity }

    fun build() = Song(title, artist, embedding, similarity)
}

/**
 * Extension function for fluent test data creation.
 */
fun song(block: SongBuilder.() -> Unit): Song =
    SongBuilder().apply(block).build()
