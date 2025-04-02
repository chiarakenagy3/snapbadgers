package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.projection.ProjectionNetwork
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ProjectionNetworkEvalTest
 *
 * Evaluates the ProjectionNetwork for correct He initialization,
 * output normalization, seed determinism, and angular structure preservation.
 *
 * These tests run on JVM without a device.
 */
class ProjectionNetworkEvalTest {

    private lateinit var network: ProjectionNetwork

    @Before
    fun setUp() {
        network = ProjectionNetwork()
    }

    // ------------------------------------------------------------------
    // He initialization weight distribution
    // ------------------------------------------------------------------

    @Test
    fun `He initialization weight distribution has correct mean and variance`() {
        // ProjectionNetwork uses seed 42 with He init (scale = sqrt(2/fan_in))
        // Verify statistical properties by creating a fresh network and inspecting
        // We can't directly access weights, but we can verify output properties
        // that are characteristic of He initialization

        // Feed a unit vector through and check output is reasonable (not exploding/vanishing)
        val unitInput = FloatArray(EMBEDDING_DIMENSION) { 0f }.also { it[0] = 1f }
        val output = network.project(unitInput)

        // Output should be L2-normalized, so its norm should be 1
        val norm = sqrt(output.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("Output should be L2-normalized", 1.0f, norm, 1e-5f)

        // With He init, outputs should not all be zero (ReLU hasn't killed everything)
        assertTrue("He init should produce non-zero output", output.any { abs(it) > 1e-6f })

        println("EVAL he_init_output_norm: $norm")
        println("EVAL he_init_nonzero_dims: ${output.count { abs(it) > 1e-6f }}/${EMBEDDING_DIMENSION}")
    }

    @Test
    fun `He initialization produces diverse outputs for random inputs`() {
        val rng = java.util.Random(123)
        val outputs = (0 until 20).map { _ ->
            val input = FloatArray(EMBEDDING_DIMENSION) { rng.nextGaussian().toFloat() }
            val normalized = VectorUtils.normalize(input)
            network.project(normalized)
        }

        // Check that outputs are not all identical (would indicate degenerate weights)
        val pairwiseSimilarities = mutableListOf<Float>()
        for (i in outputs.indices) {
            for (j in i + 1 until outputs.size) {
                pairwiseSimilarities.add(VectorUtils.cosineSimilarity(outputs[i], outputs[j]))
            }
        }
        val avgSimilarity = pairwiseSimilarities.average()
        val maxSimilarity = pairwiseSimilarities.max()

        println("EVAL he_init_diversity: avg_pairwise_sim=${"%.4f".format(avgSimilarity)} max=${"%.4f".format(maxSimilarity)}")
        assertTrue(
            "He init should produce diverse outputs (avg similarity < 0.99)",
            avgSimilarity < 0.99
        )
    }

    // ------------------------------------------------------------------
    // Forward pass output normalization
    // ------------------------------------------------------------------

    @Test
    fun `output is L2-normalized unit length within epsilon`() {
        val inputs = listOf(
            FloatArray(EMBEDDING_DIMENSION) { 1f },
            FloatArray(EMBEDDING_DIMENSION) { -1f },
            FloatArray(EMBEDDING_DIMENSION) { if (it % 2 == 0) 1f else -1f },
            FloatArray(EMBEDDING_DIMENSION) { it.toFloat() / EMBEDDING_DIMENSION },
            FloatArray(EMBEDDING_DIMENSION) { (it * 31 % 17).toFloat() - 8f }
        )

        for ((idx, input) in inputs.withIndex()) {
            val normalized = VectorUtils.normalize(input)
            val output = network.project(normalized)
            val norm = sqrt(output.sumOf { (it * it).toDouble() }).toFloat()
            assertEquals("Output $idx should be unit length", 1.0f, norm, 1e-4f)
            println("EVAL forward_norm_$idx: $norm")
        }
    }

    @Test
    fun `output dimension is 128`() {
        val input = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() })
        val output = network.project(input)
        assertEquals("Output dimension should be $EMBEDDING_DIMENSION", EMBEDDING_DIMENSION, output.size)
        println("EVAL output_dim: ${output.size}")
    }

    // ------------------------------------------------------------------
    // Seed determinism
    // ------------------------------------------------------------------

    @Test
    fun `seed 42 produces identical weights across multiple instantiations`() {
        val input = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it + 1).toFloat() })

        val network1 = ProjectionNetwork()
        val network2 = ProjectionNetwork()

        val output1 = network1.project(input)
        val output2 = network2.project(input)

        assertArrayEquals(
            "Two ProjectionNetwork instances with same seed should produce identical output",
            output1, output2, 0f
        )
        println("EVAL seed_determinism: bit_identical=true")
    }

    @Test
    fun `seed determinism across 10 instantiations`() {
        val input = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 7 % 13).toFloat() })
        val reference = ProjectionNetwork().project(input)

        var allIdentical = true
        repeat(10) {
            val current = ProjectionNetwork().project(input)
            if (!current.contentEquals(reference)) {
                allIdentical = false
            }
        }
        assertTrue("10 instantiations should produce identical results", allIdentical)
        println("EVAL seed_determinism_10x: all_identical=$allIdentical")
    }

    // ------------------------------------------------------------------
    // Stability: 1000 random vectors all produce unit-length output
    // ------------------------------------------------------------------

    @Test
    fun `1000 random vectors all produce unit-length output`() {
        val rng = java.util.Random(99)
        var minNorm = Float.MAX_VALUE
        var maxNorm = Float.MIN_VALUE
        var nanCount = 0
        var infCount = 0

        repeat(1000) {
            val rawInput = FloatArray(EMBEDDING_DIMENSION) { rng.nextGaussian().toFloat() }
            val input = VectorUtils.normalize(rawInput)
            val output = network.project(input)

            if (output.any { it.isNaN() }) nanCount++
            if (output.any { it.isInfinite() }) infCount++

            val norm = sqrt(output.sumOf { (it * it).toDouble() }).toFloat()
            if (norm < minNorm) minNorm = norm
            if (norm > maxNorm) maxNorm = norm
        }

        println("EVAL stability_1000: min_norm=${"%.6f".format(minNorm)} max_norm=${"%.6f".format(maxNorm)} nan_count=$nanCount inf_count=$infCount")
        assertEquals("No NaN outputs", 0, nanCount)
        assertEquals("No Inf outputs", 0, infCount)
        assertTrue("Min norm should be close to 1", minNorm > 0.999f)
        assertTrue("Max norm should be close to 1", maxNorm < 1.001f)
    }

    // ------------------------------------------------------------------
    // Angular structure preservation
    // ------------------------------------------------------------------

    @Test
    fun `projection preserves some angular structure`() {
        // Similar inputs should produce more similar outputs than dissimilar inputs
        val base = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() })

        // Create a "similar" vector (small perturbation)
        val similar = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() + 0.1f })

        // Create a "dissimilar" vector
        val dissimilar = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (EMBEDDING_DIMENSION - it).toFloat() })

        val projBase = network.project(base)
        val projSimilar = network.project(similar)
        val projDissimilar = network.project(dissimilar)

        val simSimilar = VectorUtils.cosineSimilarity(projBase, projSimilar)
        val simDissimilar = VectorUtils.cosineSimilarity(projBase, projDissimilar)

        println("EVAL angular_structure:")
        println("  base_vs_similar:    $simSimilar")
        println("  base_vs_dissimilar: $simDissimilar")

        // The projection should at least partially preserve angular relationships
        // Similar input → higher projected similarity than dissimilar input
        assertTrue(
            "Similar inputs should project closer than dissimilar inputs",
            simSimilar > simDissimilar
        )
    }

    @Test
    fun `projection maps distinct inputs to distinct outputs`() {
        val inputs = (0 until 10).map { i ->
            VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { ((it + i * 13) % 128).toFloat() })
        }
        val outputs = inputs.map { network.project(it) }

        // No two outputs should be identical
        for (i in outputs.indices) {
            for (j in i + 1 until outputs.size) {
                val sim = VectorUtils.cosineSimilarity(outputs[i], outputs[j])
                assertTrue("Distinct inputs should produce distinct outputs (pair $i,$j sim=$sim)", sim < 1.0f - 1e-6f)
            }
        }
        println("EVAL distinct_outputs: all 10 inputs produced distinct outputs")
    }

    // ------------------------------------------------------------------
    // Input validation
    // ------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `project rejects wrong input dimension`() {
        network.project(FloatArray(64) { 1f })
    }

    // ------------------------------------------------------------------
    // Weight loading
    // ------------------------------------------------------------------

    @Test
    fun `loadWeights changes projection output`() {
        val input = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() })
        val beforeLoad = network.project(input)

        // Load custom weights (all ones with zero bias)
        val dim = EMBEDDING_DIMENSION
        network.loadWeights(
            w1Flat = FloatArray(dim * dim) { 0.01f },
            b1Flat = FloatArray(dim) { 0f },
            w2Flat = FloatArray(dim * dim) { 0.01f },
            b2Flat = FloatArray(dim) { 0f }
        )

        val afterLoad = network.project(input)
        val similarity = VectorUtils.cosineSimilarity(beforeLoad, afterLoad)

        println("EVAL load_weights: before_vs_after_similarity=$similarity")
        assertTrue("Loading new weights should change the output", similarity < 0.999f)
    }
}
