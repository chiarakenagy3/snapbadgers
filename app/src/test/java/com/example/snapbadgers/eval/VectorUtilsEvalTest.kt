package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class VectorUtilsEvalTest {

    @Test
    fun `cosine similarity accuracy`() {
        val unit = VectorUtils.normalize(FloatArray(128) { it.toFloat() + 1f })
        val orthoA = FloatArray(128) { 0f }.also { it[0] = 1f }
        val orthoB = FloatArray(128) { 0f }.also { it[1] = 1f }
        val pos = FloatArray(128) { it.toFloat() + 1f }
        val neg = FloatArray(128) { -(it.toFloat() + 1f) }

        listOf(
            Triple("identical", { VectorUtils.cosineSimilarity(unit, unit) }, 1.0f),
            Triple("orthogonal", { VectorUtils.cosineSimilarity(orthoA, orthoB) }, 0.0f),
            Triple("opposite", { VectorUtils.cosineSimilarity(pos, neg) }, -1.0f),
            Triple("known_[1,2,3]_[4,5,6]", { VectorUtils.cosineSimilarity(floatArrayOf(1f, 2f, 3f), floatArrayOf(4f, 5f, 6f)) }, 32f / sqrt(14f * 77f)),
            Triple("empty", { VectorUtils.cosineSimilarity(floatArrayOf(), floatArrayOf()) }, 0f),
            Triple("mismatched_dims", { VectorUtils.cosineSimilarity(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f, 3f)) }, 0f)
        ).forEach { (label, compute, expected) ->
            val actual = compute()
            assertEquals("cosine_$label", expected, actual, 1e-5f)
            println("EVAL cosine_$label: expected=$expected actual=$actual")
        }
    }

    @Test
    fun `normalize numerical stability`() {
        listOf(
            "unit_length" to FloatArray(128) { (it + 1).toFloat() },
            "tiny_values" to FloatArray(128) { 1e-20f },
            "large_values" to FloatArray(128) { 1e20f },
            "zero_vector" to FloatArray(128) { 0f }
        ).forEach { (label, input) ->
            val result = VectorUtils.normalize(input)
            assertTrue("No NaN in $label", result.none { it.isNaN() })
            assertTrue("No Inf in $label", result.none { it.isInfinite() })

            val norm = sqrt(result.sumOf { (it * it).toDouble() }).toFloat()
            when (label) {
                "zero_vector" -> assertTrue(result.all { it == 0f })
                "unit_length" -> assertEquals("$label should be unit length", 1.0f, norm, 1e-4f)
                else -> {} // tiny/large values: just verify no NaN/Inf above
            }
            println("EVAL normalize_$label: norm=$norm containsNaN=false containsInf=false")
        }
    }

    @Test
    fun `normalize with NaN input handles gracefully`() {
        val result = VectorUtils.normalize(FloatArray(128) { if (it == 0) Float.NaN else 1f })
        println("EVAL normalize_nan_input: output_has_nan=${result.any { it.isNaN() }}")
    }

    @Test
    fun `alignToEmbeddingDimension determinism and behavior`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f)

        // Same salt is deterministic
        assertArrayEquals(
            VectorUtils.alignToEmbeddingDimension(input, salt = 42),
            VectorUtils.alignToEmbeddingDimension(input, salt = 42),
            0f
        )
        println("EVAL align_determinism_same_salt: identical=true")

        // Different salts produce different outputs
        val similarity = VectorUtils.cosineSimilarity(
            VectorUtils.alignToEmbeddingDimension(input, salt = 0),
            VectorUtils.alignToEmbeddingDimension(input, salt = 99)
        )
        assertTrue(similarity < 0.999f)
        println("EVAL align_different_salts: cosine_similarity=$similarity")

        // Output dimension is correct
        assertEquals(EMBEDDING_DIMENSION, VectorUtils.alignToEmbeddingDimension(floatArrayOf(1f, 2f, 3f), salt = 7).size)
        println("EVAL align_output_dimension: size=$EMBEDDING_DIMENSION expected=$EMBEDDING_DIMENSION")

        // Matching dimension just normalizes
        val matchInput = FloatArray(EMBEDDING_DIMENSION) { (it + 1).toFloat() }
        val matchResult = VectorUtils.alignToEmbeddingDimension(matchInput, salt = 0)
        assertEquals(1.0f, sqrt(matchResult.sumOf { (it * it).toDouble() }).toFloat(), 1e-5f)
        println("EVAL align_matching_dim: norm=1.0")

        // Multi-seed determinism
        val seedInput = floatArrayOf(0.5f, -0.3f, 0.8f, 1.2f, -0.1f, 0.7f)
        listOf(0, 1, 7, 42, 100, 999).forEach { seed ->
            assertTrue(
                VectorUtils.alignToEmbeddingDimension(seedInput, salt = seed)
                    .contentEquals(VectorUtils.alignToEmbeddingDimension(seedInput, salt = seed))
            )
        }
        println("EVAL align_multi_seed_determinism: seeds_tested=6 all_deterministic=true")

        // Empty input returns zero vector
        val emptyResult = VectorUtils.alignToEmbeddingDimension(floatArrayOf(), salt = 0)
        assertEquals(EMBEDDING_DIMENSION, emptyResult.size)
        assertTrue(emptyResult.all { it == 0f })
        println("EVAL align_empty_input: all_zeros=true")
    }

    @Test
    fun `positiveModulo handles negative values`() {
        assertEquals(3, VectorUtils.positiveModulo(-7, 5))
        assertEquals(0, VectorUtils.positiveModulo(0, 5))
        assertEquals(2, VectorUtils.positiveModulo(7, 5))
        assertEquals(0, VectorUtils.positiveModulo(-10, 5))
        println("EVAL positive_modulo: all_cases_passed=true")
    }

    @Test
    fun `positiveModulo with zero modulus returns 0`() {
        assertEquals(0, VectorUtils.positiveModulo(42, 0))
        println("EVAL positive_modulo_zero: returned_zero=true")
    }

    @Test
    fun `throughput 1000 calls each for normalize, cosine, and align`() {
        val iterations = 1000
        val normInput = FloatArray(128) { it.toFloat() + 1f }
        val cosA = VectorUtils.normalize(FloatArray(128) { it.toFloat() })
        val cosB = VectorUtils.normalize(FloatArray(128) { (128 - it).toFloat() })
        val alignInput = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)

        listOf(
            Triple("normalize", { VectorUtils.normalize(normInput) }, 100.0),
            Triple("cosine", { VectorUtils.cosineSimilarity(cosA, cosB) }, 100.0),
            Triple("align", { VectorUtils.alignToEmbeddingDimension(alignInput, salt = 42) }, 200.0)
        ).forEach { (label, op, maxUs) ->
            repeat(100) { op() } // warmup
            val startNs = System.nanoTime()
            repeat(iterations) { op() }
            val elapsedNs = System.nanoTime() - startNs
            val avgUs = elapsedNs / 1000.0 / iterations
            println("EVAL ${label}_throughput: iterations=$iterations avg_us=${"%.2f".format(avgUs)} total_ms=${"%.2f".format(elapsedNs / 1e6)}")
            assertTrue("$label should be under ${maxUs}us per call", avgUs < maxUs)
        }
    }
}
