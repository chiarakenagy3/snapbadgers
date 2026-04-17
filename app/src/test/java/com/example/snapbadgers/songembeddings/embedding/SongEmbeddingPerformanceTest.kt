package com.example.snapbadgers.songembeddings.embedding

import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.assertTrue
import org.junit.Test

class SongEmbeddingPerformanceTest {

    @Test
    fun benchmarkGetEmbeddingThroughput() {
        val features = AudioFeatures(
            id = "perf-track",
            danceability = 0.63f,
            energy = 0.74f,
            speechiness = 0.07f,
            acousticness = 0.16f,
            instrumentalness = 0.05f,
            liveness = 0.2f,
            valence = 0.66f,
            tempo = 126f,
            loudness = -7.5f,
            key = 7,
            mode = 1,
            durationMs = 202_000f
        )

        val warmupRuns = 300
        repeat(warmupRuns) { getEmbedding(features) }

        val measuredRuns = 2_000
        val startNs = System.nanoTime()
        repeat(measuredRuns) { getEmbedding(features) }
        val elapsedNs = System.nanoTime() - startNs

        val avgMs = elapsedNs.toDouble() / measuredRuns / 1_000_000.0
        println("Song embedding average latency: ${"%.6f".format(avgMs)} ms/call over $measuredRuns runs")

        assertTrue(
            "Average song embedding latency is too high: ${"%.6f".format(avgMs)} ms/call",
            avgMs < 10.0
        )
    }
}
