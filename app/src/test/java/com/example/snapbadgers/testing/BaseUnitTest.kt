package com.example.snapbadgers.testing

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness

/**
 * Base class for all unit tests.
 *
 * Provides common setup, teardown, and utilities for unit tests.
 * Industry best practice: Centralize common test infrastructure.
 */
abstract class BaseUnitTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    @Before
    open fun setUp() {
        // Override in subclasses for specific setup
    }

    @After
    open fun tearDown() {
        // Override in subclasses for specific teardown
    }

    /**
     * Assert that a float array is normalized (L2 norm ≈ 1.0).
     */
    protected fun assertNormalized(embedding: FloatArray, tolerance: Float = 0.01f) {
        TestFixtures.assertEmbeddingIsNormalized(embedding, tolerance)
    }

    /**
     * Assert that all values are finite.
     */
    protected fun assertAllFinite(array: FloatArray) {
        TestFixtures.assertAllFinite(array)
    }

    /**
     * Assert that all values are in range.
     */
    protected fun assertInRange(array: FloatArray, min: Float, max: Float) {
        TestFixtures.assertInRange(array, min, max)
    }
}
