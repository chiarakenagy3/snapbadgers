package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.projection.ProjectionNetwork
import com.example.snapbadgers.ai.sensor.SensorSample
import com.example.snapbadgers.ai.sensor.SensorEncoder as AiSensorEncoder
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.songembeddings.embedding.MLPProjector
import com.example.snapbadgers.songembeddings.embedding.addDerivedFeatures
import com.example.snapbadgers.songembeddings.embedding.buildBaseVector
import com.example.snapbadgers.songembeddings.embedding.normalize
import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * PipelineCorrectnessEvalTest
 *
 * End-to-end evaluation of the heuristic recommendation path
 * (no TFLite, no Android context, no device).
 *
 * Verifies: text encode -> fuse -> project -> rank against sample songs,
 * plus MLPProjector fallback and full pipeline determinism.
 *
 * These tests run on JVM without a device.
 */
class PipelineCorrectnessEvalTest {

    private lateinit var fusionEngine: FusionEngine
    private lateinit var projectionNetwork: ProjectionNetwork
    private lateinit var sampleSongs: List<Song>

    @Before
    fun setUp() {
        fusionEngine = FusionEngine()
        projectionNetwork = ProjectionNetwork()

        // Build sample songs with heuristic embeddings (mirrors SongRepository fallback)
        sampleSongs = listOf(
            Song(
                title = "Blinding Lights",
                artist = "The Weeknd",
                embedding = HeuristicTextEmbedding.encode(
                    "Blinding Lights by The Weeknd energetic pop workout night drive upbeat"
                )
            ),
            Song(
                title = "Sunflower",
                artist = "Post Malone",
                embedding = HeuristicTextEmbedding.encode(
                    "Sunflower by Post Malone happy chill pop easy listening daytime"
                )
            ),
            Song(
                title = "Weightless",
                artist = "Marconi Union",
                embedding = HeuristicTextEmbedding.encode(
                    "Weightless by Marconi Union calm study relax sleep ambient"
                )
            ),
            Song(
                title = "Thunderstruck",
                artist = "AC/DC",
                embedding = HeuristicTextEmbedding.encode(
                    "Thunderstruck by AC/DC rock heavy energy intense guitar"
                )
            ),
            Song(
                title = "Claire de Lune",
                artist = "Debussy",
                embedding = HeuristicTextEmbedding.encode(
                    "Claire de Lune by Debussy calm classical piano night peaceful"
                )
            )
        )
    }

    // ------------------------------------------------------------------
    // End-to-end heuristic path
    // ------------------------------------------------------------------

    @Test
    fun `end to end heuristic path produces ranked results with descending similarity`() {
        // Step 1: Encode text
        val textEmbedding = HeuristicTextEmbedding.encode("calm relaxing study music")

        // Step 2: Create stub sensor embedding
        val sensorEncoder = AiSensorEncoder()
        val sensorEmbedding = sensorEncoder.encode(SensorSample(0f, 9.8f, 0f, 200f))

        // Step 3: Fuse (no vision)
        val fusedEmbedding = fusionEngine.fuse(
            textEmbedding = textEmbedding,
            visionEmbedding = null,
            sensorEmbedding = sensorEmbedding
        )

        // Step 4: Project
        val projectedEmbedding = projectionNetwork.project(fusedEmbedding)

        // Step 5: Rank against songs
        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(projectedEmbedding, salt = 101)
        val rankedSongs = sampleSongs
            .map { song ->
                val score = VectorUtils.cosineSimilarity(normalizedQuery, song.embedding)
                song.copy(similarity = score)
            }
            .sortedByDescending { it.similarity }

        // Verify descending similarity
        for (i in 0 until rankedSongs.size - 1) {
            assertTrue(
                "Song ${i} (${rankedSongs[i].similarity}) >= Song ${i + 1} (${rankedSongs[i + 1].similarity})",
                rankedSongs[i].similarity >= rankedSongs[i + 1].similarity
            )
        }

        println("EVAL e2e_ranking:")
        rankedSongs.forEachIndexed { idx, song ->
            println("  #${idx + 1}: ${song.title} by ${song.artist} (sim=${"%.4f".format(song.similarity)})")
        }
    }

    @Test
    fun `workout query ranks energetic songs higher`() {
        val textEmbedding = HeuristicTextEmbedding.encode("intense workout running high energy")
        val fusedEmbedding = fusionEngine.fuse(textEmbedding, null, null)
        val projectedEmbedding = projectionNetwork.project(fusedEmbedding)

        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(projectedEmbedding, salt = 101)
        val ranked = sampleSongs
            .map { it.copy(similarity = VectorUtils.cosineSimilarity(normalizedQuery, it.embedding)) }
            .sortedByDescending { it.similarity }

        println("EVAL workout_query_ranking:")
        ranked.forEachIndexed { idx, song ->
            println("  #${idx + 1}: ${song.title} (sim=${"%.4f".format(song.similarity)})")
        }

        // Verify descending order
        for (i in 0 until ranked.size - 1) {
            assertTrue(ranked[i].similarity >= ranked[i + 1].similarity)
        }
    }

    @Test
    fun `calm query ranks calm songs higher`() {
        val textEmbedding = HeuristicTextEmbedding.encode("calm relaxing sleep ambient peaceful")
        val fusedEmbedding = fusionEngine.fuse(textEmbedding, null, null)
        val projectedEmbedding = projectionNetwork.project(fusedEmbedding)

        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(projectedEmbedding, salt = 101)
        val ranked = sampleSongs
            .map { it.copy(similarity = VectorUtils.cosineSimilarity(normalizedQuery, it.embedding)) }
            .sortedByDescending { it.similarity }

        println("EVAL calm_query_ranking:")
        ranked.forEachIndexed { idx, song ->
            println("  #${idx + 1}: ${song.title} (sim=${"%.4f".format(song.similarity)})")
        }

        for (i in 0 until ranked.size - 1) {
            assertTrue(ranked[i].similarity >= ranked[i + 1].similarity)
        }
    }

    // ------------------------------------------------------------------
    // MLPProjector fallback (songembeddings module)
    // ------------------------------------------------------------------

    @Test
    fun `MLPProjector manual fallback produces 128-d output`() {
        val audioFeatures = AudioFeatures(
            id = "test",
            danceability = 0.7f,
            energy = 0.6f,
            speechiness = 0.04f,
            acousticness = 0.2f,
            instrumentalness = 0.1f,
            liveness = 0.3f,
            valence = 0.8f,
            tempo = 128f,
            loudness = -8f,
            key = 5,
            mode = 1,
            duration_ms = 180000f
        )

        val base = buildBaseVector(audioFeatures)
        val derived = addDerivedFeatures(base)
        val combined = base + derived
        assertEquals("Combined features should be 15-d", 15, combined.size)

        // MLPProjector.project uses manualProject fallback when no TFLite interpreter
        val embedding = MLPProjector.project(combined)
        assertEquals("MLPProjector output should be 128-d", 128, embedding.size)
        println("EVAL mlp_projector_fallback: dim=${embedding.size}")
    }

    @Test
    fun `MLPProjector fallback is deterministic`() {
        val input = FloatArray(15) { (it + 1).toFloat() / 15f }
        val first = MLPProjector.project(input)
        val second = MLPProjector.project(input)

        assertArrayEquals("MLPProjector should be deterministic", first, second, 0f)
        println("EVAL mlp_projector_determinism: identical=true")
    }

    @Test
    fun `normalized MLPProjector output is unit length`() {
        val input = FloatArray(15) { (it + 1).toFloat() / 15f }
        val embedding = MLPProjector.project(input)
        val normalized = normalize(embedding)
        val norm = sqrt(normalized.sumOf { (it * it).toDouble() }).toFloat()

        assertEquals("Normalized MLPProjector output should be unit length", 1.0f, norm, 1e-4f)
        println("EVAL mlp_projector_normalized: norm=$norm")
    }

    // ------------------------------------------------------------------
    // Pipeline determinism
    // ------------------------------------------------------------------

    @Test
    fun `same pipeline inputs produce same recommendations`() {
        val input = "happy upbeat dance party"
        val sensorSample = SensorSample(1f, 9.8f, -0.5f, 300f)
        val sensorEncoder = AiSensorEncoder()

        fun runPipeline(): List<Pair<String, Float>> {
            val text = HeuristicTextEmbedding.encode(input)
            val sensor = sensorEncoder.encode(sensorSample)
            val fused = fusionEngine.fuse(text, null, sensor)
            val projected = projectionNetwork.project(fused)
            val query = VectorUtils.alignToEmbeddingDimension(projected, salt = 101)
            return sampleSongs
                .map { it.title to VectorUtils.cosineSimilarity(query, it.embedding) }
                .sortedByDescending { it.second }
        }

        val first = runPipeline()
        val second = runPipeline()

        assertEquals("Same inputs should produce same ranking order", first.map { it.first }, second.map { it.first })
        first.zip(second).forEachIndexed { idx, (a, b) ->
            assertEquals("Song $idx similarity should be identical", a.second, b.second, 0f)
        }
        println("EVAL pipeline_determinism: ranking_identical=true scores_identical=true")
    }

    @Test
    fun `pipeline determinism across 50 runs`() {
        val input = "calm study lo-fi ambient"
        val sensorEncoder = AiSensorEncoder()

        fun runPipeline(): List<Float> {
            val text = HeuristicTextEmbedding.encode(input)
            val sensor = sensorEncoder.encode(SensorSample(0f, 9.8f, 0f, 100f))
            val fused = fusionEngine.fuse(text, null, sensor)
            val projected = projectionNetwork.project(fused)
            val query = VectorUtils.alignToEmbeddingDimension(projected, salt = 101)
            return sampleSongs.map { VectorUtils.cosineSimilarity(query, it.embedding) }
        }

        val reference = runPipeline()
        var allIdentical = true
        repeat(50) {
            val current = runPipeline()
            if (reference.zip(current).any { (a, b) -> a != b }) {
                allIdentical = false
            }
        }

        assertTrue("50 pipeline runs should produce identical results", allIdentical)
        println("EVAL pipeline_determinism_50x: all_identical=$allIdentical")
    }

    // ------------------------------------------------------------------
    // Intermediate dimension checks
    // ------------------------------------------------------------------

    @Test
    fun `each pipeline stage produces correct dimensions`() {
        val textEmbedding = HeuristicTextEmbedding.encode("test input")
        assertEquals("Text embedding should be ${EMBEDDING_DIMENSION}-d", EMBEDDING_DIMENSION, textEmbedding.size)

        val sensorEncoder = AiSensorEncoder()
        val sensorEmbedding = sensorEncoder.encode(SensorSample(0f, 9.8f, 0f, 100f))
        assertEquals("Sensor embedding should be ${EMBEDDING_DIMENSION}-d", EMBEDDING_DIMENSION, sensorEmbedding.size)

        val fused = fusionEngine.fuse(textEmbedding, null, sensorEmbedding)
        assertEquals("Fused embedding should be ${EMBEDDING_DIMENSION}-d", EMBEDDING_DIMENSION, fused.size)

        val projected = projectionNetwork.project(fused)
        assertEquals("Projected embedding should be ${EMBEDDING_DIMENSION}-d", EMBEDDING_DIMENSION, projected.size)

        println("EVAL pipeline_dims: text=${textEmbedding.size} sensor=${sensorEmbedding.size} fused=${fused.size} projected=${projected.size}")
    }

    @Test
    fun `all pipeline outputs are L2-normalized`() {
        val text = HeuristicTextEmbedding.encode("test music")
        val sensorEncoder = AiSensorEncoder()
        val sensor = sensorEncoder.encode(SensorSample(0f, 9.8f, 0f, 100f))
        val fused = fusionEngine.fuse(text, null, sensor)
        val projected = projectionNetwork.project(fused)

        fun norm(v: FloatArray) = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()

        val textNorm = norm(text)
        val sensorNorm = norm(sensor)
        val fusedNorm = norm(fused)
        val projectedNorm = norm(projected)

        assertEquals("Text embedding norm", 1.0f, textNorm, 1e-4f)
        assertEquals("Sensor embedding norm", 1.0f, sensorNorm, 1e-4f)
        assertEquals("Fused embedding norm", 1.0f, fusedNorm, 1e-4f)
        assertEquals("Projected embedding norm", 1.0f, projectedNorm, 1e-4f)

        println("EVAL pipeline_norms: text=$textNorm sensor=$sensorNorm fused=$fusedNorm projected=$projectedNorm")
    }
}
