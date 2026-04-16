package com.example.snapbadgers.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceStepsTest {

    @Test
    fun `default state has all steps false`() {
        val steps = InferenceSteps()
        assertFalse(steps.textEncoded)
        assertFalse(steps.visionEncoded)
        assertFalse(steps.sensorEncoded)
        assertFalse(steps.fused)
        assertFalse(steps.projected)
        assertFalse(steps.ranked)
    }

    @Test
    fun `copy updates individual steps`() {
        val steps = InferenceSteps()
            .copy(textEncoded = true)
            .copy(sensorEncoded = true)

        assertTrue(steps.textEncoded)
        assertTrue(steps.sensorEncoded)
        assertFalse(steps.visionEncoded)
        assertFalse(steps.fused)
    }

    @Test
    fun `full pipeline progression`() {
        var steps = InferenceSteps()
        steps = steps.copy(textEncoded = true)
        steps = steps.copy(visionEncoded = true)
        steps = steps.copy(sensorEncoded = true)
        steps = steps.copy(fused = true)
        steps = steps.copy(projected = true)
        steps = steps.copy(ranked = true)

        assertTrue(steps.textEncoded)
        assertTrue(steps.visionEncoded)
        assertTrue(steps.sensorEncoded)
        assertTrue(steps.fused)
        assertTrue(steps.projected)
        assertTrue(steps.ranked)
    }

    @Test
    fun `data class equality`() {
        val a = InferenceSteps(textEncoded = true, fused = true)
        val b = InferenceSteps(textEncoded = true, fused = true)
        assertEquals(a, b)
    }
}
