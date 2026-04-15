package com.example.snapbadgers.ai.pipeline

import android.content.Context
import android.graphics.Bitmap
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.projection.ProjectionNetwork
import com.example.snapbadgers.ai.sensor.SensorCollector
import com.example.snapbadgers.ai.sensor.SensorEncoder
import com.example.snapbadgers.ai.text.TextEncoderDescriptor
import com.example.snapbadgers.ai.text.TextEncoder
import com.example.snapbadgers.ai.text.TextEncoderFactory
import com.example.snapbadgers.ai.text.TextEncoderMode
import com.example.snapbadgers.ai.text.StubTextEncoder
import com.example.snapbadgers.ai.vision.VisionEncoder
import com.example.snapbadgers.data.SongRepository
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.RecommendationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecommendationPipeline(
    context: Context,
    private val songRepository: SongRepository = SongRepository(context)
) {
    private val appContext = context.applicationContext
    private val useHeuristicTextEncoder = !songRepository.hasEmbeddedCatalog

    private var textEncoder: TextEncoder? = null
    private val textEncoderInitMutex = Mutex()
    private val textEncoderDescriptor: TextEncoderDescriptor = if (useHeuristicTextEncoder) {
        TextEncoderDescriptor(
            mode = TextEncoderMode.STUB,
            label = "Stub heuristic encoder (catalog-aligned)"
        )
    } else {
        TextEncoderFactory.describe(appContext)
    }

    private val sensorCollector   = SensorCollector(appContext)
    private val sensorEncoder     = SensorEncoder()
    private val visionEncoder     = VisionEncoder(appContext)  
    private val fusionEngine      = FusionEngine()
    private val projectionNetwork = ProjectionNetwork(appContext)

    val textEncoderLabel: String
        get() = textEncoder?.label ?: textEncoderDescriptor.label

    val isModelBackedTextEncoder: Boolean
        get() = (textEncoder?.mode ?: textEncoderDescriptor.mode) == TextEncoderMode.MODEL

    suspend fun warmUp() {
        getOrCreateTextEncoder()
    }

    suspend fun runPipeline(
        input: String,
        imageBitmap: Bitmap? = null,
        onStepUpdate: (InferenceSteps) -> Unit
    ): RecommendationResult {
        var steps = InferenceSteps()
        val startMs = System.currentTimeMillis()

        try {
            sensorCollector.start()

            // Step 1: Text encoding
            delay(120)
            val activeTextEncoder = getOrCreateTextEncoder()
            val textEmbedding = activeTextEncoder.encode(input)
            steps = steps.copy(textEncoded = true)
            onStepUpdate(steps)

            // Step 2: Vision encoding (only if bitmap provided)
            delay(120)
            val visionEmbedding = imageBitmap?.let { bitmap ->
                val embedding = visionEncoder.encode(bitmap)
                steps = steps.copy(visionEncoded = true)
                onStepUpdate(steps)
                embedding
            }

            // Step 3: Sensor encoding
            delay(120)
            val sensorSample = sensorCollector.getLatestSample()
            val sensorEmbedding = sensorEncoder.encode(sensorSample)
            steps = steps.copy(sensorEncoded = true)
            onStepUpdate(steps)

            // Step 4: Fusion → 128-d fused context embedding
            delay(120)
            val fusedEmbedding = fusionEngine.fuse(
                textEmbedding   = textEmbedding,
                visionEmbedding = visionEmbedding,
                sensorEmbedding = sensorEmbedding
            )
            steps = steps.copy(fused = true)
            onStepUpdate(steps)

            // Step 5: Projection → map fused embedding into song embedding space
            // Skip projection when using heuristic encoder — the MLP was trained on
            // model-backed encoder outputs, and sample song embeddings are also in
            // heuristic space, so both sides must stay in the same space.
            delay(120)
            val projectedEmbedding = if (useHeuristicTextEncoder) {
                fusedEmbedding
            } else {
                projectionNetwork.project(fusedEmbedding)
            }
            steps = steps.copy(projected = true)
            onStepUpdate(steps)

            // Step 6: Song ranking via cosine similarity
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

    private suspend fun getOrCreateTextEncoder(): TextEncoder {
        textEncoder?.let { return it }

        return textEncoderInitMutex.withLock {
            textEncoder ?: if (useHeuristicTextEncoder) {
                StubTextEncoder("Stub heuristic encoder (catalog-aligned)").also { initializedEncoder ->
                    textEncoder = initializedEncoder
                }
            } else {
                TextEncoderFactory.createAsync(appContext).also { initializedEncoder ->
                    textEncoder = initializedEncoder
                }
            }
        }
    }
}
