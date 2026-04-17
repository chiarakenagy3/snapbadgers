package com.example.snapbadgers.ai.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.projection.ProjectionNetwork
import com.example.snapbadgers.ai.sensor.SensorEncoder
import com.example.snapbadgers.ai.text.StubTextEncoder
import com.example.snapbadgers.ai.text.TextEncoder
import com.example.snapbadgers.ai.text.TextEncoderDescriptor
import com.example.snapbadgers.ai.text.TextEncoderFactory
import com.example.snapbadgers.ai.text.TextEncoderMode
import com.example.snapbadgers.ai.vision.VisionEncoder
import com.example.snapbadgers.data.SongRepository
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.RecommendationResult
import com.example.snapbadgers.model.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecommendationPipeline(
    context: Context,
    private val songRepository: SongRepository = SongRepository(context)
) : AutoCloseable {
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

    private val sensorEncoder = SensorEncoder(appContext)
    private val visionEncoder = VisionEncoder(appContext)
    private val fusionEngine = FusionEngine()
    private val projectionNetwork = ProjectionNetwork()

    val textEncoderLabel: String
        get() = textEncoder?.label ?: textEncoderDescriptor.label

    val isModelBackedTextEncoder: Boolean
        get() = (textEncoder?.mode ?: textEncoderDescriptor.mode) == TextEncoderMode.MODEL

    suspend fun warmUp() {
        val encoder = getOrCreateTextEncoder()
        encoder.encode("")
    }

    fun getAllSongs(): List<Song> = songRepository.getAllSongs()

    suspend fun runPipeline(
        input: String,
        imageBitmap: Bitmap? = null,
        onStepUpdate: (InferenceSteps) -> Unit
    ): RecommendationResult {
        var steps = InferenceSteps()
        val startMs = System.currentTimeMillis()

        try {
            sensorEncoder.start()
            Log.d(TAG, "Pipeline started. InputLength: ${input.length}, HasImage: ${imageBitmap != null}")

            val (textEmbedding, visionEmbedding, sensorEmbedding) = coroutineScope {
                val textDeferred = async {
                    val activeTextEncoder = getOrCreateTextEncoder()
                    val embedding = activeTextEncoder.encode(input)
                    Log.d(TAG, "Step 1: Text encoded. Embedding size: ${embedding.size}")
                    steps = steps.copy(textEncoded = true)
                    onStepUpdate(steps)
                    embedding
                }

                val visionDeferred = async {
                    imageBitmap?.let { bitmap ->
                        val embedding = visionEncoder.encode(bitmap)
                        Log.d(TAG, "Step 2: Vision encoded. Embedding size: ${embedding.size}")
                        steps = steps.copy(visionEncoded = true)
                        onStepUpdate(steps)
                        embedding
                    } ?: run {
                        Log.d(TAG, "Step 2: Vision skipped (no bitmap)")
                        null
                    }
                }

                val sensorDeferred = async {
                    val embedding = sensorEncoder.getEmbedding()
                    Log.d(TAG, "Step 3: Sensor encoded. Embedding size: ${embedding.size}")
                    steps = steps.copy(sensorEncoded = true)
                    onStepUpdate(steps)
                    embedding
                }

                Triple(textDeferred.await(), visionDeferred.await(), sensorDeferred.await())
            }

            val fusedEmbedding = fusionEngine.fuse(
                textEmbedding = textEmbedding,
                visionEmbedding = visionEmbedding,
                sensorEmbedding = sensorEmbedding
            )
            Log.d(TAG, "Step 4: Fusion complete. Embedding size: ${fusedEmbedding.size}")
            steps = steps.copy(fused = true)
            onStepUpdate(steps)

            // Heuristic fallback catalog embeddings are already in heuristic space,
            // so projecting only the query would make cosine scores meaningless.
            val projectedEmbedding = if (useHeuristicTextEncoder) {
                fusedEmbedding
            } else {
                projectionNetwork.project(fusedEmbedding)
            }
            Log.d(TAG, "Step 5: Projection complete. Embedding size: ${projectedEmbedding.size}")
            steps = steps.copy(projected = true)
            onStepUpdate(steps)

            Log.d(TAG, "Step 6: Ranking songs. Catalog size: ${songRepository.getAllSongs().size}")
            val rankedSongs = songRepository.findTopSongs(
                queryEmbedding = projectedEmbedding,
                limit = RECOMMENDATION_LIMIT
            )
            Log.d(TAG, "Step 6: Ranking complete. Top song: ${rankedSongs.firstOrNull()?.title} (Score: ${rankedSongs.firstOrNull()?.similarity})")
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
        } catch (e: Exception) {
            Log.e(TAG, "Pipeline failed", e)
            throw e
        } finally {
            sensorEncoder.stop()
        }
    }

    override fun close() {
        (textEncoder as? AutoCloseable)?.close()
        visionEncoder.close()
    }

    private companion object {
        const val TAG = "RecommendationPipeline"
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
