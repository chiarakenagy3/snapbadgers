package com.example.snapbadgers.embedding

import com.example.snapbadgers.model.AudioFeatures
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * MLPProjector implements a simple Multi-Layer Perceptron to project audio features
 * into a higher-dimensional space (128 dimensions).
 * It uses a fixed seed to ensure the projection is consistent across different runs.
 */
object MLPProjector {
    private const val INPUT_DIM = 15 // 10 base features + 5 derived features
    private const val HIDDEN_DIM = 64
    private const val OUTPUT_DIM = 128
    private const val SEED = 42L

    // Layer 1 weights and biases
    private val weights1 = FloatArray(INPUT_DIM * HIDDEN_DIM)
    private val bias1 = FloatArray(HIDDEN_DIM)

    // Layer 2 weights and biases
    private val weights2 = FloatArray(HIDDEN_DIM * OUTPUT_DIM)
    private val bias2 = FloatArray(OUTPUT_DIM)

    init {
        // Initialize weights with a fixed seed for stability
        val random = Random(SEED)
        
        // Xavier/Glorot initialization scale
        val scale1 = sqrt(2.0f / (INPUT_DIM + HIDDEN_DIM))
        for (i in weights1.indices) {
            weights1[i] = (random.nextFloat() * 2 - 1) * scale1
        }
        
        val scale2 = sqrt(2.0f / (HIDDEN_DIM + OUTPUT_DIM))
        for (i in weights2.indices) {
            weights2[i] = (random.nextFloat() * 2 - 1) * scale2
        }
        
        // Biases are initialized to 0 by default
    }

    /**
     * Performs a forward pass through the 2-layer MLP.
     * Layer 1: Linear + ReLU activation
     * Layer 2: Linear
     */
    fun project(input: FloatArray): FloatArray {
        // Hidden layer computation
        val hidden = FloatArray(HIDDEN_DIM)
        for (j in 0 until HIDDEN_DIM) {
            var sum = bias1[j]
            for (i in 0 until INPUT_DIM) {
                sum += input[i] * weights1[i * HIDDEN_DIM + j]
            }
            // ReLU Activation
            hidden[j] = if (sum > 0) sum else 0f
        }

        // Output layer computation
        val output = FloatArray(OUTPUT_DIM)
        for (k in 0 until OUTPUT_DIM) {
            var sum = bias2[k]
            for (j in 0 until HIDDEN_DIM) {
                sum += hidden[j] * weights2[j * OUTPUT_DIM + k]
            }
            output[k] = sum
        }

        return output
    }
}

/**
 * Normalizes and scales the raw AudioFeatures into a 10-dimensional base vector.
 */
fun buildBaseVector(f: AudioFeatures): FloatArray {
    return floatArrayOf(
        f.danceability,
        f.energy,
        f.speechiness,
        f.acousticness,
        f.instrumentalness,
        f.liveness,
        f.valence,
        (f.tempo / 200f).coerceIn(0f, 1f),
        (f.loudness + 60f).coerceIn(0f, 60f) / 60f,
        (f.duration_ms / 300000f).coerceIn(0f, 2f)
    )
}

/**
 * Calculates 5 additional derived features to capture non-linear relationships.
 */
fun addDerivedFeatures(base: FloatArray): FloatArray {
    val dance = base[0]
    val energy = base[1]
    val acoustic = base[3]
    val live = base[5]
    val valence = base[6]
    val tempo = base[7]

    return floatArrayOf(
        dance * energy,
        valence * energy,
        acoustic * (1f - energy),
        tempo * energy,
        live * valence
    )
}

/**
 * Main entry point to generate a 128-dimensional normalized embedding for a track.
 */
fun getEmbedding(features: AudioFeatures?): FloatArray {
    if (features == null) return FloatArray(128) { 0f }

    // 1. Build base feature vector (10-dim)
    val base = buildBaseVector(features)
    
    // 2. Generate derived features (5-dim)
    val derived = addDerivedFeatures(base)
    
    // 3. Combine into a single input vector (15-dim)
    val combined = base + derived

    // 4. Project to 128-dim using the MLP
    var embedding = MLPProjector.project(combined)
    
    // 5. Apply L2 normalization
    embedding = normalize(embedding)

    return embedding
}

/**
 * Performs L2 normalization on the given vector.
 */
fun normalize(vec: FloatArray): FloatArray {
    var sum = 0f
    for (v in vec) sum += v * v

    val norm = sqrt(sum)
    if (norm < 1e-8f) return FloatArray(vec.size) { 0f }

    return FloatArray(vec.size) { i -> vec[i] / norm }
}
