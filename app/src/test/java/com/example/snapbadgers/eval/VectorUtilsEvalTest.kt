package com.example.snapbadgers.eval

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import com.example.snapbadgers.ai.common.ml.VectorUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * VectorUtilsEvalTest
 *
 * Evaluates the core vector math layer (VectorUtils) for accuracy,
 * numerical stability, determinism, and throughput.
 *
 * These tests run on JVM without a device.
 */
class VectorUtilsEvalTest {

    // ------------------------------------------------------------------
    // Cosine similarity accuracy
    // ------------------------------------------------------------------

    @Test
    fun `cosine similarity of identical unit vectors is 1`() {
        val v = VectorUtils.normalize(FloatArray(128) { it.toFloat() + 1f })
        val similarity = VectorUtils.cosineSimilarity(v, v)
        assertEquals("Identical vectors should have similarity 1.0", 1.0f, similarity, 1e-6f)
        println("EVAL cosine_identical_vectors: similarity=$similarity")
    }

    @Test
    fun `cosine similarity of orthogonal vectors is 0`() {
        val a = FloatArray(128) { 0f }.also { it[0] = 1f }
        val b = FloatArray(128) { 0f }.also { it[1] = 1f }
        val similarity = VectorUtils.cosineSimilarity(a, b)
        assertEquals("Orthogonal vectors should have similarity 0.0", 0.0f, similarity, 1e-6f)
        println("EVAL cosine_orthogonal_vectors: similarity=$similarity")
    }

    @Test
    fun `cosine similarity of opposite vectors is -1`() {
        val a = FloatArray(128) { it.toFloat() + 1f }
        val b = FloatArray(128) { -(it.toFloat() + 1f) }
        val similarity = VectorUtils.cosineSimilarity(a, b)
        assertEquals("Opposite vectors should have similarity -1.0", -1.0f, similarity, 1e-6f)
        println("EVAL cosine_opposite_vectors: similarity=$similarity")
    }

    @Test
    fun `cosine similarity with known values`() {
        // Hand-computed: a=[1,2,3], b=[4,5,6]
        // dot=32, |a|=sqrt(14), |b|=sqrt(77), cos=32/sqrt(1078)=0.9746...
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(4f, 5f, 6f)
        val expected = 32f / sqrt(14f * 77f)
        val similarity = VectorUtils.cosineSimilarity(a, b)
        assertEquals("Known cosine similarity should match hand-computed value", expected, similarity, 1e-5f)
        println("EVAL cosine_known_pair: expected=$expected actual=$similarity")
    }

    @Test
    fun `cosine similarity returns 0 for empty vectors`() {
        assertEquals(0f, VectorUtils.cosineSimilarity(floatArrayOf(), floatArrayOf()), 1e-8f)
        println("EVAL cosine_empty_vectors: returned 0 as expected")
    }

    @Test
    fun `cosine similarity returns 0 for mismatched dimensions`() {
        val a = floatArrayOf(1f, 2f)
        val b = floatArrayOf(1f, 2f, 3f)
        assertEquals(0f, VectorUtils.cosineSimilarity(a, b), 1e-8f)
        println("EVAL cosine_mismatched_dims: returned 0 as expected")
    }

    // ------------------------------------------------------------------
    // Normalization numerical stability
    // ------------------------------------------------------------------

    @Test
    fun `normalize produces unit-length vector`() {
        val input = FloatArray(128) { (it + 1).toFloat() }
        val normalized = VectorUtils.normalize(input)
        val norm = sqrt(normalized.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("Normalized vector should have unit length", 1.0f, norm, 1e-5f)
        println("EVAL normalize_unit_length: norm=$norm")
    }

    @Test
    fun `normalize very small values does not produce NaN or Inf`() {
        val tiny = FloatArray(128) { 1e-20f }
        val result = VectorUtils.normalize(tiny)
        assertTrue("No NaN in output", result.none { it.isNaN() })
        assertTrue("No Inf in output", result.none { it.isInfinite() })
        // Very small values below threshold should produce zero vector
        val norm = sqrt(result.sumOf { (it * it).toDouble() }).toFloat()
        println("EVAL normalize_tiny_values: norm=$norm containsNaN=false containsInf=false")
    }

    @Test
    fun `normalize very large values does not produce NaN or Inf`() {
        val large = FloatArray(128) { 1e20f }
        val result = VectorUtils.normalize(large)
        assertTrue("No NaN in output", result.none { it.isNaN() })
        assertTrue("No Inf in output", result.none { it.isInfinite() })
        val norm = sqrt(result.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("Normalized large vector should have unit length", 1.0f, norm, 1e-4f)
        println("EVAL normalize_large_values: norm=$norm")
    }

    @Test
    fun `normalize zero vector returns zero vector`() {
        val zero = FloatArray(128) { 0f }
        val result = VectorUtils.normalize(zero)
        assertTrue("Zero vector normalization should return zero vector", result.all { it == 0f })
        println("EVAL normalize_zero_vector: all_zeros=true")
    }

    @Test
    fun `normalize with NaN input handles gracefully`() {
        val input = FloatArray(128) { if (it == 0) Float.NaN else 1f }
        val result = VectorUtils.normalize(input)
        // NaN propagation: NaN * NaN in sumSquares → NaN norm → division by NaN
        // The function doesn't explicitly guard against NaN input; documenting behavior
        println("EVAL normalize_nan_input: output_has_nan=${result.any { it.isNaN() }}")
    }

    // ------------------------------------------------------------------
    // Dimension alignment determinism
    // ------------------------------------------------------------------

    @Test
    fun `alignToEmbeddingDimension is deterministic with same salt`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f)
        val salt = 42

        val first = VectorUtils.alignToEmbeddingDimension(input, salt)
        val second = VectorUtils.alignToEmbeddingDimension(input, salt)

        assertArrayEquals("Same input + salt should produce identical output", first, second, 0f)
        println("EVAL align_determinism_same_salt: identical=true")
    }

    @Test
    fun `alignToEmbeddingDimension with different salts produces different outputs`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f)
        val result1 = VectorUtils.alignToEmbeddingDimension(input, salt = 0)
        val result2 = VectorUtils.alignToEmbeddingDimension(input, salt = 99)

        val similarity = VectorUtils.cosineSimilarity(result1, result2)
        assertTrue("Different salts should produce distinguishable outputs", similarity < 0.999f)
        println("EVAL align_different_salts: cosine_similarity=$similarity")
    }

    @Test
    fun `alignToEmbeddingDimension outputs correct dimension`() {
        val input = floatArrayOf(1f, 2f, 3f)
        val result = VectorUtils.alignToEmbeddingDimension(input, salt = 7)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        println("EVAL align_output_dimension: size=${result.size} expected=$EMBEDDING_DIMENSION")
    }

    @Test
    fun `alignToEmbeddingDimension with matching dimension normalizes only`() {
        val input = FloatArray(EMBEDDING_DIMENSION) { (it + 1).toFloat() }
        val result = VectorUtils.alignToEmbeddingDimension(input, salt = 0)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        val norm = sqrt(result.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals("Matching-dimension input should just be normalized", 1.0f, norm, 1e-5f)
        println("EVAL align_matching_dim: norm=$norm")
    }

    @Test
    fun `alignToEmbeddingDimension determinism across multiple seeds`() {
        val input = floatArrayOf(0.5f, -0.3f, 0.8f, 1.2f, -0.1f, 0.7f)
        val seeds = listOf(0, 1, 7, 42, 100, 999)
        val allDeterministic = seeds.all { seed ->
            val a = VectorUtils.alignToEmbeddingDimension(input, salt = seed)
            val b = VectorUtils.alignToEmbeddingDimension(input, salt = seed)
            a.contentEquals(b)
        }
        assertTrue("All seeds should produce deterministic results", allDeterministic)
        println("EVAL align_multi_seed_determinism: seeds_tested=${seeds.size} all_deterministic=$allDeterministic")
    }

    @Test
    fun `alignToEmbeddingDimension empty input returns zero vector`() {
        val result = VectorUtils.alignToEmbeddingDimension(floatArrayOf(), salt = 0)
        assertEquals(EMBEDDING_DIMENSION, result.size)
        assertTrue("Empty input should produce zero vector", result.all { it == 0f })
        println("EVAL align_empty_input: all_zeros=true")
    }

    // ------------------------------------------------------------------
    // positiveModulo correctness
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Performance: normalize throughput
    // ------------------------------------------------------------------

    @Test
    fun `normalize throughput 1000 calls`() {
        val input = FloatArray(128) { it.toFloat() + 1f }
        val iterations = 1000

        // Warm up
        repeat(100) { VectorUtils.normalize(input) }

        val startNs = System.nanoTime()
        repeat(iterations) { VectorUtils.normalize(input) }
        val elapsedNs = System.nanoTime() - startNs

        val avgUs = elapsedNs / 1000.0 / iterations
        println("EVAL normalize_throughput: iterations=$iterations avg_us=${"%.2f".format(avgUs)} total_ms=${"%.2f".format(elapsedNs / 1e6)}")
        // Sanity: normalize 128-d should be well under 1ms each on modern JVM
        assertTrue("Normalize should be under 100us per call", avgUs < 100.0)
    }

    @Test
    fun `cosine similarity throughput 1000 calls`() {
        val a = VectorUtils.normalize(FloatArray(128) { it.toFloat() })
        val b = VectorUtils.normalize(FloatArray(128) { (128 - it).toFloat() })
        val iterations = 1000

        repeat(100) { VectorUtils.cosineSimilarity(a, b) }

        val startNs = System.nanoTime()
        repeat(iterations) { VectorUtils.cosineSimilarity(a, b) }
        val elapsedNs = System.nanoTime() - startNs

        val avgUs = elapsedNs / 1000.0 / iterations
        println("EVAL cosine_throughput: iterations=$iterations avg_us=${"%.2f".format(avgUs)} total_ms=${"%.2f".format(elapsedNs / 1e6)}")
        assertTrue("Cosine similarity should be under 100us per call", avgUs < 100.0)
    }

    @Test
    fun `alignToEmbeddingDimension throughput 1000 calls`() {
        val input = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val iterations = 1000

        repeat(100) { VectorUtils.alignToEmbeddingDimension(input, salt = 42) }

        val startNs = System.nanoTime()
        repeat(iterations) { VectorUtils.alignToEmbeddingDimension(input, salt = 42) }
        val elapsedNs = System.nanoTime() - startNs

        val avgUs = elapsedNs / 1000.0 / iterations
        println("EVAL align_throughput: iterations=$iterations avg_us=${"%.2f".format(avgUs)} total_ms=${"%.2f".format(elapsedNs / 1e6)}")
        assertTrue("Align should be under 200us per call", avgUs < 200.0)
    }
}
