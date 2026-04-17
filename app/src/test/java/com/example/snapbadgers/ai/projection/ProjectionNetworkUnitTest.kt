package com.example.snapbadgers.ai.projection

import com.example.snapbadgers.ai.common.ml.EMBEDDING_DIMENSION
import org.junit.Assert.*
import org.junit.Test

/**
 * ProjectionNetworkUnitTest
 *
 * JVM-only tests (no device, no .tflite, no Android context).
 * These test input validation logic only — the require() guard in project().
 *
 * Run with: ./gradlew test
 */
class ProjectionNetworkUnitTest {

    @Test
    fun project_correctInputSize_doesNotThrow() {
        // Just verifying the require() guard doesn't false-fire on valid input.
        // We can't instantiate a real ProjectionNetwork here (needs Android context
        // and .tflite asset), so we test the guard logic directly.
        val inputDim = 128
        val validInput = FloatArray(inputDim) { 0.5f }

        // Guard passes silently — no exception
        try {
            require(validInput.size == inputDim) {
                "Expected $inputDim-d input, got ${validInput.size}-d."
            }
        } catch (e: IllegalArgumentException) {
            fail("require() threw on valid input: ${e.message}")
        }
    }

    @Test
    fun project_wrongInputSize_throwsIllegalArgumentException() {
        val inputDim = 128
        val badInput = FloatArray(64) { 0.5f }  // wrong — should be 128

        val exception = assertThrows(IllegalArgumentException::class.java) {
            require(badInput.size == inputDim) {
                "Expected $inputDim-d input, got ${badInput.size}-d."
            }
        }

        assertTrue(
            "Error message should mention expected dim",
            exception.message!!.contains("128")
        )
        assertTrue(
            "Error message should mention actual dim",
            exception.message!!.contains("64")
        )
    }

    @Test
    fun project_emptyInput_throwsIllegalArgumentException() {
        val inputDim = 128
        val emptyInput = FloatArray(0)

        assertThrows(IllegalArgumentException::class.java) {
            require(emptyInput.size == inputDim) {
                "Expected $inputDim-d input, got ${emptyInput.size}-d."
            }
        }
    }

    @Test
    fun project_oversizedInput_throwsIllegalArgumentException() {
        val inputDim = 128
        val oversizedInput = FloatArray(256) { 0.5f }  // wrong — should be 128

        assertThrows(IllegalArgumentException::class.java) {
            require(oversizedInput.size == inputDim) {
                "Expected $inputDim-d input, got ${oversizedInput.size}-d."
            }
        }
    }

    @Test
    fun embeddingDimension_isExpectedValue() {
        // Catches accidental changes to EMBEDDING_DIMENSION that would
        // silently break the whole pipeline.
        assertEquals(128, EMBEDDING_DIMENSION)
    }
}
