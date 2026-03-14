package com.example.snapbadgers.ai.text

interface Tokenizer {
    fun tokenize(text: String): IntArray
}
