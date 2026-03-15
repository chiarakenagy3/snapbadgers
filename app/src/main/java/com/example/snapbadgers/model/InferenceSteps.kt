package com.example.snapbadgers.model

data class InferenceSteps(
    val sensorEncoded: Boolean = false,
    val textEncoded: Boolean = false,
    val fused: Boolean = false,
    val projected: Boolean = false,
    val ranked: Boolean = false
)