package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.fusion.FusionEngine
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class FusionEngineEvalTest {

    private lateinit var fusionEngine: FusionEngine

    @Before
    fun setUp() {
        fusionEngine = FusionEngine()
    }

    @Test
    fun `text-only fusion produces valid output and matches text-aligned path`() {
        val textEmbedding = HeuristicTextEmbedding.encode("calm relaxing piano")
        val fused = fusionEngine.fuse(textEmbedding = textEmbedding, visionEmbedding = null, sensorEmbedding = null)

        assertEquals(EMBEDDING_DIMENSION, fused.size)
        val norm = sqrt(fused.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals(1.0f, norm, 1e-4f)
        assertTrue(fused.any { abs(it) > 1e-6f })
        println("EVAL text_only_fusion: norm=$norm nonzero_dims=${fused.count { abs(it) > 1e-6f }}")

        // With only text, fused output should match text alignment path exactly
        val textAligned = VectorUtils.alignToEmbeddingDimension(textEmbedding, salt = 11)
        val similarity = VectorUtils.cosineSimilarity(fused, textAligned)
        println("EVAL text_dominated: text_alignment_similarity=$similarity")
        assertEquals(1.0f, similarity, 1e-4f)
    }

    @Test
    fun `multi-modality fusion uses all three modalities`() {
        val text = HeuristicTextEmbedding.encode("energetic workout")
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 3 % 17).toFloat() })
        val sensor = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 7 % 23).toFloat() })

        val similarity = VectorUtils.cosineSimilarity(
            fusionEngine.fuse(text, vision, sensor),
            fusionEngine.fuse(text, null, null)
        )
        println("EVAL multi_vs_text_only: similarity=$similarity")
        assertTrue(similarity < 0.999f)
    }

    @Test
    fun `weight balance 60-25-15 is reflected in relative influence`() {
        val text = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { if (it < 43) 1f else 0f })
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { if (it in 43..85) 1f else 0f })
        val sensor = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { if (it > 85) 1f else 0f })

        val fused = fusionEngine.fuse(text, vision, sensor)

        val textAligned = VectorUtils.alignToEmbeddingDimension(text, salt = 11)
        val visionAligned = VectorUtils.alignToEmbeddingDimension(vision, salt = 19)
        val sensorAligned = VectorUtils.alignToEmbeddingDimension(sensor, salt = 23)

        val fusedVsText = VectorUtils.cosineSimilarity(fused, textAligned)
        val fusedVsVision = VectorUtils.cosineSimilarity(fused, visionAligned)
        val fusedVsSensor = VectorUtils.cosineSimilarity(fused, sensorAligned)

        println("EVAL weight_balance:")
        println("  fused_vs_text:   $fusedVsText   (weight=0.25)")
        println("  fused_vs_vision: $fusedVsVision (weight=0.60)")
        println("  fused_vs_sensor: $fusedVsSensor (weight=0.15)")

        assertTrue(fusedVsVision > fusedVsText)
        assertTrue(fusedVsVision > fusedVsSensor)
    }

    @Test
    fun `custom weights override default balance`() {
        val customEngine = FusionEngine(visionWeight = 0.10f, textWeight = 0.80f, sensorWeight = 0.10f)
        val text = HeuristicTextEmbedding.encode("calm study music")
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 3).toFloat() })
        val sensor = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 7).toFloat() })

        val similarity = VectorUtils.cosineSimilarity(
            fusionEngine.fuse(text, vision, sensor),
            customEngine.fuse(text, vision, sensor)
        )
        println("EVAL custom_weights: default_vs_custom_similarity=$similarity")
        assertTrue(similarity < 0.999f)
    }

    @Test
    fun `fusion with zero inputs does not produce NaN`() {
        listOf(
            Triple("zero_sensor", HeuristicTextEmbedding.encode("test music"), Triple<FloatArray?, FloatArray?, Boolean>(null, FloatArray(128) { 0f }, false)),
            Triple("zero_vision", HeuristicTextEmbedding.encode("test music"), Triple<FloatArray?, FloatArray?, Boolean>(FloatArray(64) { 0f }, null, false)),
            Triple("all_zeros", FloatArray(128) { 0f }, Triple<FloatArray?, FloatArray?, Boolean>(FloatArray(128) { 0f }, FloatArray(128) { 0f }, true))
        ).forEach { (label, text, modalities) ->
            val (vision, sensor, expectZero) = modalities
            val fused = fusionEngine.fuse(text, vision, sensor)

            assertTrue("No NaN in $label", fused.none { it.isNaN() })
            assertTrue("No Inf in $label", fused.none { it.isInfinite() })
            if (expectZero) assertTrue("$label should produce zero output", fused.all { abs(it) < 1e-6f })
            println("EVAL $label: has_nan=false has_inf=false${if (expectZero) " is_zero_vector=true" else ""}")
        }
    }

    @Test
    fun `100 fusions with same input produce identical output`() {
        val text = HeuristicTextEmbedding.encode("calm relaxing ambient")
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 5 % 11).toFloat() })
        val sensor = VectorUtils.normalize(FloatArray(32) { (it * 3 % 7).toFloat() })

        val reference = fusionEngine.fuse(text, vision, sensor)
        repeat(100) {
            assertTrue(fusionEngine.fuse(text, vision, sensor).contentEquals(reference))
        }
        println("EVAL consistency_100x: all_identical=true")
    }

    @Test
    fun `fusion output dimension is always 128`() {
        listOf(
            Triple(FloatArray(10) { 1f }, null, null),
            Triple(FloatArray(128) { 1f }, FloatArray(64) { 1f }, null),
            Triple(FloatArray(128) { 1f }, FloatArray(128) { 1f }, FloatArray(32) { 1f }),
            Triple(FloatArray(5) { 1f }, FloatArray(200) { 1f }, FloatArray(8) { 1f })
        ).forEachIndexed { idx, (text, vision, sensor) ->
            assertEquals("Case $idx: output should be $EMBEDDING_DIMENSION-d", EMBEDDING_DIMENSION, fusionEngine.fuse(text, vision, sensor).size)
        }
        println("EVAL output_dimension: all cases produce ${EMBEDDING_DIMENSION}-d output")
    }

    @Test
    fun `fusion with very small and very large embeddings`() {
        listOf(
            "tiny" to FloatArray(3) { 0.001f },
            "large" to FloatArray(1000) { 100f }
        ).forEach { (label, input) ->
            val fused = fusionEngine.fuse(input, null, null)
            assertEquals(EMBEDDING_DIMENSION, fused.size)
            assertTrue("No NaN with $label input", fused.none { it.isNaN() })
            assertTrue("No Inf with $label input", fused.none { it.isInfinite() })
            println("EVAL ${label}_input: dim=${fused.size} has_nan=false has_inf=false")
        }
    }
}
