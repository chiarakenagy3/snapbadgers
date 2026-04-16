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

/**
 * FusionEngineEvalTest
 *
 * Evaluates the FusionEngine for single/multi-modality fusion behavior,
 * weight balance, zero-vector handling, and consistency.
 *
 * These tests run on JVM without a device.
 */
class FusionEngineEvalTest {

    private lateinit var fusionEngine: FusionEngine

    @Before
    fun setUp() {
        fusionEngine = FusionEngine()
    }

    // ------------------------------------------------------------------
    // Single-modality fusion
    // ------------------------------------------------------------------

    @Test
    fun `text-only fusion produces valid output`() {
        val textEmbedding = HeuristicTextEmbedding.encode("calm relaxing piano")
        val fused = fusionEngine.fuse(
            textEmbedding = textEmbedding,
            visionEmbedding = null,
            sensorEmbedding = null
        )

        assertEquals("Fused output should be $EMBEDDING_DIMENSION-d", EMBEDDING_DIMENSION, fused.size)
        val norm = sqrt(fused.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("Fused output should be L2-normalized", 1.0f, norm, 1e-4f)
        assertTrue("Text-only fusion should produce non-zero output", fused.any { abs(it) > 1e-6f })

        println("EVAL text_only_fusion: norm=$norm nonzero_dims=${fused.count { abs(it) > 1e-6f }}")
    }

    @Test
    fun `null vision and sensor produces text-dominated output`() {
        val textEmbedding = HeuristicTextEmbedding.encode("happy dance party")
        val textOnly = fusionEngine.fuse(
            textEmbedding = textEmbedding,
            visionEmbedding = null,
            sensorEmbedding = null
        )

        // With only text, the fused output should be entirely derived from text
        // (aligned and normalized). Verify it correlates strongly with raw text alignment.
        val textAligned = VectorUtils.alignToEmbeddingDimension(textEmbedding, salt = 11)
        val similarity = VectorUtils.cosineSimilarity(textOnly, textAligned)

        println("EVAL text_dominated: text_alignment_similarity=$similarity")
        // When only text is provided, the fused output should be identical to the
        // text modality path (aligned, weighted, then re-normalized)
        assertEquals(
            "Text-only fusion should match text-aligned output",
            1.0f, similarity, 1e-4f
        )
    }

    // ------------------------------------------------------------------
    // Multi-modality weight balance
    // ------------------------------------------------------------------

    @Test
    fun `multi-modality fusion uses all three modalities`() {
        val text = HeuristicTextEmbedding.encode("energetic workout")
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 3 % 17).toFloat() })
        val sensor = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 7 % 23).toFloat() })

        val fullFusion = fusionEngine.fuse(text, vision, sensor)
        val textOnlyFusion = fusionEngine.fuse(text, null, null)

        val similarity = VectorUtils.cosineSimilarity(fullFusion, textOnlyFusion)
        println("EVAL multi_vs_text_only: similarity=$similarity")
        assertTrue(
            "Full fusion should differ from text-only fusion",
            similarity < 0.999f
        )
    }

    @Test
    fun `weight balance 60-25-15 is reflected in relative influence`() {
        // Create orthogonal-ish modality embeddings
        val text = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { if (it < 43) 1f else 0f })
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { if (it in 43..85) 1f else 0f })
        val sensor = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { if (it > 85) 1f else 0f })

        val fused = fusionEngine.fuse(text, vision, sensor)

        // With default weights (vision=0.60, text=0.25, sensor=0.15), vision should
        // have the strongest influence. We measure by checking correlation with each modality.
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

        // Vision (0.60) should have the largest influence
        assertTrue(
            "Vision (0.60) should have more influence than text (0.25)",
            fusedVsVision > fusedVsText
        )
        assertTrue(
            "Vision (0.60) should have more influence than sensor (0.15)",
            fusedVsVision > fusedVsSensor
        )
    }

    @Test
    fun `custom weights override default balance`() {
        val customEngine = FusionEngine(
            visionWeight = 0.10f,
            textWeight = 0.80f,
            sensorWeight = 0.10f
        )

        val text = HeuristicTextEmbedding.encode("calm study music")
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 3).toFloat() })
        val sensor = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 7).toFloat() })

        val defaultFused = fusionEngine.fuse(text, vision, sensor)
        val customFused = customEngine.fuse(text, vision, sensor)

        val similarity = VectorUtils.cosineSimilarity(defaultFused, customFused)
        println("EVAL custom_weights: default_vs_custom_similarity=$similarity")
        assertTrue("Custom weights should produce different fusion result", similarity < 0.999f)
    }

    // ------------------------------------------------------------------
    // Zero vector handling
    // ------------------------------------------------------------------

    @Test
    fun `fusion with zero sensor does not produce NaN`() {
        val text = HeuristicTextEmbedding.encode("test music")
        val zeroSensor = FloatArray(128) { 0f }

        val fused = fusionEngine.fuse(text, null, zeroSensor)

        assertTrue("No NaN in fused output", fused.none { it.isNaN() })
        assertTrue("No Inf in fused output", fused.none { it.isInfinite() })
        println("EVAL zero_sensor: has_nan=false has_inf=false")
    }

    @Test
    fun `fusion with zero vision does not produce NaN`() {
        val text = HeuristicTextEmbedding.encode("test music")
        val zeroVision = FloatArray(64) { 0f }

        val fused = fusionEngine.fuse(text, zeroVision, null)

        assertTrue("No NaN in fused output", fused.none { it.isNaN() })
        assertTrue("No Inf in fused output", fused.none { it.isInfinite() })
        println("EVAL zero_vision: has_nan=false has_inf=false")
    }

    @Test
    fun `fusion with all zero inputs produces zero vector`() {
        val zeroText = FloatArray(128) { 0f }
        val zeroVision = FloatArray(128) { 0f }
        val zeroSensor = FloatArray(128) { 0f }

        val fused = fusionEngine.fuse(zeroText, zeroVision, zeroSensor)

        assertTrue("No NaN in fused output", fused.none { it.isNaN() })
        assertTrue("All-zero inputs should produce zero output", fused.all { abs(it) < 1e-6f })
        println("EVAL all_zeros: is_zero_vector=true")
    }

    // ------------------------------------------------------------------
    // Consistency
    // ------------------------------------------------------------------

    @Test
    fun `100 fusions with same input produce identical output`() {
        val text = HeuristicTextEmbedding.encode("calm relaxing ambient")
        val vision = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 5 % 11).toFloat() })
        val sensor = VectorUtils.normalize(FloatArray(32) { (it * 3 % 7).toFloat() })

        val reference = fusionEngine.fuse(text, vision, sensor)
        var allIdentical = true

        repeat(100) {
            val current = fusionEngine.fuse(text, vision, sensor)
            if (!current.contentEquals(reference)) {
                allIdentical = false
            }
        }

        assertTrue("100 fusions with identical input should produce identical output", allIdentical)
        println("EVAL consistency_100x: all_identical=$allIdentical")
    }

    @Test
    fun `fusion output dimension is always 128`() {
        val cases = listOf(
            Triple(FloatArray(10) { 1f }, null, null),
            Triple(FloatArray(128) { 1f }, FloatArray(64) { 1f }, null),
            Triple(FloatArray(128) { 1f }, FloatArray(128) { 1f }, FloatArray(32) { 1f }),
            Triple(FloatArray(5) { 1f }, FloatArray(200) { 1f }, FloatArray(8) { 1f })
        )

        for ((idx, triple) in cases.withIndex()) {
            val (text, vision, sensor) = triple
            val fused = fusionEngine.fuse(text, vision, sensor)
            assertEquals("Case $idx: output should be $EMBEDDING_DIMENSION-d", EMBEDDING_DIMENSION, fused.size)
        }
        println("EVAL output_dimension: all cases produce ${EMBEDDING_DIMENSION}-d output")
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    fun `fusion with very small embeddings`() {
        val tiny = FloatArray(3) { 0.001f }
        val fused = fusionEngine.fuse(tiny, null, null)

        assertEquals(EMBEDDING_DIMENSION, fused.size)
        assertTrue("No NaN with tiny input", fused.none { it.isNaN() })
        println("EVAL tiny_input: dim=${fused.size} has_nan=false")
    }

    @Test
    fun `fusion with very large embeddings`() {
        val large = FloatArray(1000) { 100f }
        val fused = fusionEngine.fuse(large, null, null)

        assertEquals(EMBEDDING_DIMENSION, fused.size)
        assertTrue("No NaN with large input", fused.none { it.isNaN() })
        assertTrue("No Inf with large input", fused.none { it.isInfinite() })
        println("EVAL large_input: dim=${fused.size} has_nan=false has_inf=false")
    }
}
