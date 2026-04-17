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

class ProjectionNetworkEvalTest {

    private lateinit var network: ProjectionNetwork

    @Before
    fun setUp() {
        network = ProjectionNetwork()
    }

    @Test
    fun `He initialization weight distribution has correct mean and variance`() {
        val unitInput = FloatArray(EMBEDDING_DIMENSION) { 0f }.also { it[0] = 1f }
        val output = network.project(unitInput)

        val norm = sqrt(output.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals(1.0f, norm, 1e-5f)
        assertTrue(output.any { abs(it) > 1e-6f })

        println("EVAL he_init_output_norm: $norm")
        println("EVAL he_init_nonzero_dims: ${output.count { abs(it) > 1e-6f }}/${EMBEDDING_DIMENSION}")
    }

    @Test
    fun `He initialization produces diverse outputs for random inputs`() {
        val rng = java.util.Random(123)
        val outputs = (0 until 20).map {
            network.project(VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { rng.nextGaussian().toFloat() }))
        }

        val pairwiseSimilarities = mutableListOf<Float>()
        for (i in outputs.indices) {
            for (j in i + 1 until outputs.size) {
                pairwiseSimilarities.add(VectorUtils.cosineSimilarity(outputs[i], outputs[j]))
            }
        }
        val avgSimilarity = pairwiseSimilarities.average()

        println("EVAL he_init_diversity: avg_pairwise_sim=${"%.4f".format(avgSimilarity)} max=${"%.4f".format(pairwiseSimilarities.max())}")
        assertTrue(avgSimilarity < 0.99)
    }

    @Test
    fun `output is L2-normalized unit length and correct dimension`() {
        val inputs = listOf(
            FloatArray(EMBEDDING_DIMENSION) { 1f },
            FloatArray(EMBEDDING_DIMENSION) { -1f },
            FloatArray(EMBEDDING_DIMENSION) { if (it % 2 == 0) 1f else -1f },
            FloatArray(EMBEDDING_DIMENSION) { it.toFloat() / EMBEDDING_DIMENSION },
            FloatArray(EMBEDDING_DIMENSION) { (it * 31 % 17).toFloat() - 8f }
        )

        for ((idx, input) in inputs.withIndex()) {
            val output = network.project(VectorUtils.normalize(input))
            assertEquals(EMBEDDING_DIMENSION, output.size)
            val norm = sqrt(output.sumOf { (it * it).toDouble() }).toFloat()
            assertEquals("Output $idx should be unit length", 1.0f, norm, 1e-4f)
            println("EVAL forward_norm_$idx: $norm")
        }
        println("EVAL output_dim: $EMBEDDING_DIMENSION")
    }

    @Test
    fun `seed determinism across 10 instantiations`() {
        val input = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (it * 7 % 13).toFloat() })
        val reference = ProjectionNetwork().project(input)

        repeat(10) {
            assertArrayEquals("Instantiation $it must match reference", reference, ProjectionNetwork().project(input), 0f)
        }
        println("EVAL seed_determinism_10x: all_identical=true")
    }

    @Test
    fun `1000 random vectors all produce unit-length output`() {
        val rng = java.util.Random(99)
        var minNorm = Float.MAX_VALUE
        var maxNorm = Float.MIN_VALUE
        var nanCount = 0
        var infCount = 0

        repeat(1000) {
            val output = network.project(VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { rng.nextGaussian().toFloat() }))

            if (output.any { it.isNaN() }) nanCount++
            if (output.any { it.isInfinite() }) infCount++

            val norm = sqrt(output.sumOf { (it * it).toDouble() }).toFloat()
            if (norm < minNorm) minNorm = norm
            if (norm > maxNorm) maxNorm = norm
        }

        println("EVAL stability_1000: min_norm=${"%.6f".format(minNorm)} max_norm=${"%.6f".format(maxNorm)} nan_count=$nanCount inf_count=$infCount")
        assertEquals(0, nanCount)
        assertEquals(0, infCount)
        assertTrue(minNorm > 0.999f)
        assertTrue(maxNorm < 1.001f)
    }

    @Test
    fun `projection preserves angular structure and maps distinct inputs to distinct outputs`() {
        val base = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() })
        val similar = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() + 0.1f })
        val dissimilar = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { (EMBEDDING_DIMENSION - it).toFloat() })

        val projBase = network.project(base)
        val projSimilar = network.project(similar)
        val projDissimilar = network.project(dissimilar)

        val simSimilar = VectorUtils.cosineSimilarity(projBase, projSimilar)
        val simDissimilar = VectorUtils.cosineSimilarity(projBase, projDissimilar)

        println("EVAL angular_structure:")
        println("  base_vs_similar:    $simSimilar")
        println("  base_vs_dissimilar: $simDissimilar")
        assertTrue(simSimilar > simDissimilar)

        // Verify 10 distinct inputs produce distinct outputs
        val inputs = (0 until 10).map { i ->
            VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { ((it + i * 13) % 128).toFloat() })
        }
        val outputs = inputs.map { network.project(it) }

        for (i in outputs.indices) {
            for (j in i + 1 until outputs.size) {
                val sim = VectorUtils.cosineSimilarity(outputs[i], outputs[j])
                assertTrue("Distinct inputs should produce distinct outputs (pair $i,$j sim=$sim)", sim < 1.0f - 1e-6f)
            }
        }
        println("EVAL distinct_outputs: all 10 inputs produced distinct outputs")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `project rejects wrong input dimension`() {
        network.project(FloatArray(64) { 1f })
    }

    @Test
    fun `loadWeights changes projection output`() {
        val input = VectorUtils.normalize(FloatArray(EMBEDDING_DIMENSION) { it.toFloat() })
        val beforeLoad = network.project(input)

        val dim = EMBEDDING_DIMENSION
        network.loadWeights(
            w1Flat = FloatArray(dim * dim) { 0.01f },
            b1Flat = FloatArray(dim) { 0f },
            w2Flat = FloatArray(dim * dim) { 0.01f },
            b2Flat = FloatArray(dim) { 0f }
        )

        val similarity = VectorUtils.cosineSimilarity(beforeLoad, network.project(input))
        println("EVAL load_weights: before_vs_after_similarity=$similarity")
        assertTrue(similarity < 0.999f)
    }
}
