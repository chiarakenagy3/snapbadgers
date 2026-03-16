package com.example.snapbadgers.model

data class InferenceSteps(
    val textEncoded: Boolean = false,
    val visionEncoded: Boolean = false,
    val fused: Boolean = false,
    val projected: Boolean = false,
    val ranked: Boolean = false
)
