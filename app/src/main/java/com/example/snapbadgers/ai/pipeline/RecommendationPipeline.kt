package com.example.snapbadgers.ai.pipeline

import android.content.Context
import android.graphics.Bitmap
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.sensor.SensorCollector
import com.example.snapbadgers.ai.sensor.SensorEncoder
import com.example.snapbadgers.ai.text.TextEncoder
import com.example.snapbadgers.ai.text.TextEncoderFactory
import com.example.snapbadgers.ai.text.TextEncoderMode
import com.example.snapbadgers.ai.vision.VisionEncoder
import com.example.snapbadgers.data.SongRepository
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.RecommendationResult
import kotlinx.coroutines.delay

class RecommendationPipeline(
    context: Context,
    private val textEncoder: TextEncoder = TextEncoderFactory.create(context),
    private val songRepository: SongRepository = SongRepository(context)
) {

    private val sensorCollector = SensorCollector(context)
    private val sensorEncoder = SensorEncoder()
    private val visionEncoder = VisionEncoder()
    private val fusionEngine = FusionEngine()

    val textEncoderLabel: String
        get() = textEncoder.label

    val isModelBackedTextEncoder: Boolean
        get() = textEncoder.mode == TextEncoderMode.MODEL

    suspend fun runPipeline(
        input: String,
        imageBitmap: Bitmap? = null,
        onStepUpdate: (InferenceSteps) -> Unit
    ): RecommendationResult {
        var steps = InferenceSteps()
        val startMs = System.currentTimeMillis()

        try {
            sensorCollector.start()

            delay(120)
            val textEmbedding = textEncoder.encode(input)
            steps = steps.copy(textEncoded = true)
            onStepUpdate(steps)

            delay(120)
            val visionEmbedding = imageBitmap?.let { bitmap ->
                val embedding = visionEncoder.encode(bitmap)
                steps = steps.copy(visionEncoded = true)
                onStepUpdate(steps)
                embedding
            }

            delay(120)
            val sensorSample = sensorCollector.getLatestSample()
            val sensorEmbedding = sensorEncoder.encode(sensorSample)
            steps = steps.copy(sensorEncoded = true)
            onStepUpdate(steps)

            delay(120)
            val fusedEmbedding = fusionEngine.fuse(
                textEmbedding = textEmbedding,
                visionEmbedding = visionEmbedding,
                sensorEmbedding = sensorEmbedding
            )
            steps = steps.copy(fused = true)
            onStepUpdate(steps)

            val projectedEmbedding = fusedEmbedding

            delay(120)
            steps = steps.copy(projected = true)
            onStepUpdate(steps)

            delay(160)
            val rankedSongs = songRepository.findTopSongs(
                queryEmbedding = projectedEmbedding,
                limit = RECOMMENDATION_LIMIT
            )
            steps = steps.copy(ranked = true)
            onStepUpdate(steps)

            val inferenceMs = System.currentTimeMillis() - startMs
            val recommendations = rankedSongs.map { song ->
                song.copy(inferenceTimeMs = inferenceMs)
            }
            return RecommendationResult(
                recommendations = recommendations,
                inferenceTimeMs = inferenceMs,
                usedVisionInput = imageBitmap != null
            )
        } finally {
            sensorCollector.stop()
        }
    }

    private companion object {
        const val RECOMMENDATION_LIMIT = 3
    }
}
