package com.example.snapbadgers.ai.pipeline

import android.content.Context
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.sensor.SensorCollector
import com.example.snapbadgers.ai.sensor.SensorEncoder
import com.example.snapbadgers.ai.text.TextEncoder
import com.example.snapbadgers.data.SongRepository
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.Song
import kotlinx.coroutines.delay

class RecommendationPipeline(context: Context) {

    private val textEncoder = TextEncoder(context)
    private val sensorCollector = SensorCollector(context)
    private val sensorEncoder = SensorEncoder()
    private val fusionEngine = FusionEngine()
    private val songRepository = SongRepository()

    suspend fun runPipeline(
        input: String,
        onStepUpdate: (InferenceSteps) -> Unit
    ): Song {
        var steps = InferenceSteps()
        val startMs = System.currentTimeMillis()

        try {
            sensorCollector.start()

            delay(120)
            val textEmbedding = textEncoder.encode(input)
            steps = steps.copy(textEncoded = true)
            onStepUpdate(steps)

            delay(120)
            val sensorSample = sensorCollector.getLatestSample()
            val sensorEmbedding = sensorEncoder.encode(sensorSample)

            delay(120)
            val fusedEmbedding = fusionEngine.fuse(
                textEmbedding = textEmbedding,
                sensorEmbedding = sensorEmbedding
            )
            steps = steps.copy(fused = true)
            onStepUpdate(steps)

            // 현재는 실제 projection layer가 없으므로
            // demo step 용도로만 유지
            delay(120)
            steps = steps.copy(projected = true)
            onStepUpdate(steps)

            delay(160)
            val song = songRepository.findTopSong(fusedEmbedding)
            steps = steps.copy(ranked = true)
            onStepUpdate(steps)

            val inferenceMs = System.currentTimeMillis() - startMs
            return song.copy(inferenceTimeMs = inferenceMs)

        } finally {
            sensorCollector.stop()
        }
    }
}