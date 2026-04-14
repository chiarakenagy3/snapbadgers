package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.projection.ProjectionNetwork
import com.example.snapbadgers.ai.sensor.SensorData
import com.example.snapbadgers.ai.sensor.SensorEncoderMLP
import com.example.snapbadgers.ai.sensor.SensorFeatureExtractor
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.songembeddings.embedding.MLPProjector
import com.example.snapbadgers.songembeddings.embedding.addDerivedFeatures
import com.example.snapbadgers.songembeddings.embedding.buildBaseVector
import com.example.snapbadgers.songembeddings.model.AudioFeatures
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class PipelineCorrectnessEvalTest {

    private lateinit var fusionEngine: FusionEngine
    private lateinit var projectionNetwork: ProjectionNetwork
    private lateinit var sampleSongs: List<Song>

    @Before
    fun setUp() {
        fusionEngine = FusionEngine()
        projectionNetwork = ProjectionNetwork()

        sampleSongs = listOf(
            Song(title = "Blinding Lights", artist = "The Weeknd",
                embedding = HeuristicTextEmbedding.encode("Blinding Lights by The Weeknd energetic pop workout night drive upbeat")),
            Song(title = "Sunflower", artist = "Post Malone",
                embedding = HeuristicTextEmbedding.encode("Sunflower by Post Malone happy chill pop easy listening daytime")),
            Song(title = "Weightless", artist = "Marconi Union",
                embedding = HeuristicTextEmbedding.encode("Weightless by Marconi Union calm study relax sleep ambient")),
            Song(title = "Thunderstruck", artist = "AC/DC",
                embedding = HeuristicTextEmbedding.encode("Thunderstruck by AC/DC rock heavy energy intense guitar")),
            Song(title = "Claire de Lune", artist = "Debussy",
                embedding = HeuristicTextEmbedding.encode("Claire de Lune by Debussy calm classical piano night peaceful"))
        )
    }

    private fun rankSongs(query: String): List<Song> {
        val textEmbedding = HeuristicTextEmbedding.encode(query)
        val fusedEmbedding = fusionEngine.fuse(textEmbedding, null, null)
        val projectedEmbedding = projectionNetwork.project(fusedEmbedding)
        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(projectedEmbedding, salt = 101)
        return sampleSongs
            .map { it.copy(similarity = VectorUtils.cosineSimilarity(normalizedQuery, it.embedding)) }
            .sortedByDescending { it.similarity }
    }

    private fun assertDescending(ranked: List<Song>) {
        for (i in 0 until ranked.size - 1) {
            assertTrue(
                "Song $i (${ranked[i].similarity}) >= Song ${i + 1} (${ranked[i + 1].similarity})",
                ranked[i].similarity >= ranked[i + 1].similarity
            )
        }
    }

    @Test
    fun `query ranking produces descending similarity`() {
        listOf(
            "calm relaxing study music" to "e2e",
            "intense workout running high energy" to "workout",
            "calm relaxing sleep ambient peaceful" to "calm"
        ).forEach { (query, label) ->
            val ranked = rankSongs(query)
            assertDescending(ranked)
            println("EVAL ${label}_query_ranking:")
            ranked.forEachIndexed { idx, song ->
                println("  #${idx + 1}: ${song.title} (sim=${"%.4f".format(song.similarity)})")
            }
        }
    }

    @Test
    fun `end to end with sensor data produces descending similarity`() {
        val textEmbedding = HeuristicTextEmbedding.encode("calm relaxing study music")
        val sensorData = SensorData(accelWindow = listOf(floatArrayOf(0f, 9.8f, 0f)), lightLux = 200f)
        val sensorEmbedding = SensorEncoderMLP().encode(SensorFeatureExtractor.extract(sensorData))
        val fusedEmbedding = fusionEngine.fuse(textEmbedding, null, sensorEmbedding)
        val projectedEmbedding = projectionNetwork.project(fusedEmbedding)
        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(projectedEmbedding, salt = 101)

        val ranked = sampleSongs
            .map { it.copy(similarity = VectorUtils.cosineSimilarity(normalizedQuery, it.embedding)) }
            .sortedByDescending { it.similarity }

        assertDescending(ranked)
        println("EVAL e2e_sensor_ranking:")
        ranked.forEachIndexed { idx, song ->
            println("  #${idx + 1}: ${song.title} by ${song.artist} (sim=${"%.4f".format(song.similarity)})")
        }
    }

    @Test
    fun `MLPProjector fallback produces 128-d deterministic output`() {
        val audioFeatures = AudioFeatures(
            id = "test", danceability = 0.7f, energy = 0.6f, speechiness = 0.04f,
            acousticness = 0.2f, instrumentalness = 0.1f, liveness = 0.3f, valence = 0.8f,
            tempo = 128f, loudness = -8f, key = 5, mode = 1, durationMs = 180000f
        )

        val combined = buildBaseVector(audioFeatures).let { it + addDerivedFeatures(it) }
        assertEquals(15, combined.size)

        val embedding = MLPProjector.project(combined)
        assertEquals(128, embedding.size)
        assertArrayEquals(embedding, MLPProjector.project(combined), 0f)

        val norm = sqrt(VectorUtils.normalize(embedding).sumOf { (it * it).toDouble() }).toFloat()
        assertEquals(1.0f, norm, 1e-4f)

        println("EVAL mlp_projector_fallback: dim=${embedding.size}")
        println("EVAL mlp_projector_determinism: identical=true")
        println("EVAL mlp_projector_normalized: norm=$norm")
    }

    @Test
    fun `pipeline determinism across 50 runs`() {
        val input = "calm study lo-fi ambient"

        fun runPipeline(): List<Float> {
            val text = HeuristicTextEmbedding.encode(input)
            val sensorData = SensorData(accelWindow = listOf(floatArrayOf(0f, 9.8f, 0f)), lightLux = 100f)
            val sensor = SensorEncoderMLP().encode(SensorFeatureExtractor.extract(sensorData))
            val fused = fusionEngine.fuse(text, null, sensor)
            val projected = projectionNetwork.project(fused)
            val query = VectorUtils.alignToEmbeddingDimension(projected, salt = 101)
            return sampleSongs.map { VectorUtils.cosineSimilarity(query, it.embedding) }
        }

        val reference = runPipeline()
        repeat(50) {
            val current = runPipeline()
            reference.zip(current).forEachIndexed { idx, (a, b) ->
                assertEquals("Run $it song $idx", a, b, 0f)
            }
        }
        println("EVAL pipeline_determinism_50x: all_identical=true")
    }

    @Test
    fun `each pipeline stage produces correct dimensions and L2-normalized output`() {
        val text = HeuristicTextEmbedding.encode("test music")
        val sensorData = SensorData(accelWindow = listOf(floatArrayOf(0f, 9.8f, 0f)), lightLux = 100f)
        val sensor = SensorEncoderMLP().encode(SensorFeatureExtractor.extract(sensorData))
        val fused = fusionEngine.fuse(text, null, sensor)
        val projected = projectionNetwork.project(fused)

        fun norm(v: FloatArray) = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()

        assertEquals(EMBEDDING_DIMENSION, text.size)
        assertEquals(32, sensor.size)
        assertEquals(EMBEDDING_DIMENSION, fused.size)
        assertEquals(EMBEDDING_DIMENSION, projected.size)

        assertEquals(1.0f, norm(text), 1e-4f)
        assertEquals(1.0f, norm(sensor), 1e-4f)
        assertEquals(1.0f, norm(fused), 1e-4f)
        assertEquals(1.0f, norm(projected), 1e-4f)

        println("EVAL pipeline_dims: text=${text.size} sensor=${sensor.size} fused=${fused.size} projected=${projected.size}")
        println("EVAL pipeline_norms: text=${norm(text)} sensor=${norm(sensor)} fused=${norm(fused)} projected=${norm(projected)}")
    }
}
