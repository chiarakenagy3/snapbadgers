package com.example.snapbadgers.ai.common.ml

open class TwoLayerMLP(
    protected val inputDim: Int,
    protected val hiddenDim: Int,
    protected val outputDim: Int
) {
    protected var w1: Array<FloatArray> = Array(hiddenDim) { FloatArray(inputDim) }
    protected var b1: FloatArray = FloatArray(hiddenDim)
    protected var w2: Array<FloatArray> = Array(outputDim) { FloatArray(hiddenDim) }
    protected var b2: FloatArray = FloatArray(outputDim)

    init {
        @Suppress("LeakingThis")
        initWeights()
    }

    protected open fun initWeights() {}

    protected fun forward(input: FloatArray): FloatArray {
        val hidden = FloatArray(hiddenDim)
        for (i in 0 until hiddenDim) {
            var sum = b1[i]
            for (j in 0 until inputDim) sum += w1[i][j] * input[j]
            hidden[i] = if (sum > 0f) sum else 0f
        }
        val output = FloatArray(outputDim)
        for (i in 0 until outputDim) {
            var sum = b2[i]
            for (j in 0 until hiddenDim) sum += w2[i][j] * hidden[j]
            output[i] = sum
        }
        return VectorUtils.normalize(output)
    }

    fun loadWeights(
        w1Flat: FloatArray,
        b1Flat: FloatArray,
        w2Flat: FloatArray,
        b2Flat: FloatArray
    ) {
        require(w1Flat.size == hiddenDim * inputDim) { "w1 size mismatch: expected ${hiddenDim * inputDim}, got ${w1Flat.size}" }
        require(b1Flat.size == hiddenDim) { "b1 size mismatch" }
        require(w2Flat.size == outputDim * hiddenDim) { "w2 size mismatch: expected ${outputDim * hiddenDim}, got ${w2Flat.size}" }
        require(b2Flat.size == outputDim) { "b2 size mismatch" }

        for (i in 0 until hiddenDim)
            for (j in 0 until inputDim)
                w1[i][j] = w1Flat[i * inputDim + j]
        b1Flat.copyInto(b1)

        for (i in 0 until outputDim)
            for (j in 0 until hiddenDim)
                w2[i][j] = w2Flat[i * hiddenDim + j]
        b2Flat.copyInto(b2)
    }
}
