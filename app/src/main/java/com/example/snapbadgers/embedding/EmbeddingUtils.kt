package com.example.snapbadgers.embedding

import com.example.snapbadgers.model.AudioFeatures
import kotlin.math.sin
import kotlin.math.sqrt

// 构建基础向量
fun buildBaseVector(f: AudioFeatures): FloatArray {
    return floatArrayOf(
        f.danceability,
        f.energy,
        f.speechiness,
        f.acousticness,
        f.instrumentalness,
        f.liveness,
        f.valence,
        f.tempo / 200f,
        // 归一化 Loudness: 通常范围在 -60 到 0 之间
        (f.loudness + 60f).coerceIn(0f, 60f) / 60f,
        // 如果 API 确实没给时长，这个值将使用模型里的默认值
        f.duration_ms / 300000f
    )
}

fun getEmbedding(features: AudioFeatures?): FloatArray {
    if (features == null) return FloatArray(128) { 0f }

    val base = buildBaseVector(features)
    val derived = addDerivedFeatures(base)
    val combined = base + derived

    var embedding = projectTo128(combined)
    embedding = normalize(embedding)

    return embedding
}

// Add derived features
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

// Project to 128 dimensions
fun projectTo128(vec: FloatArray, dim: Int = 128): FloatArray {
    val out = FloatArray(dim)

    for (i in 0 until dim) {
        var sum = 0f
        for (j in vec.indices) {
            val rand = sin(i * 13.37 + j * 7.13)
            sum += vec[j] * rand.toFloat()
        }
        out[i] = sum
    }

    return out
}

// Normalize vector
fun normalize(vec: FloatArray): FloatArray {
    var sum = 0f
    for (v in vec) sum += v * v

    val norm = sqrt(sum)
    if (norm == 0f) return vec

    return FloatArray(vec.size) { i -> vec[i] / norm }
}
