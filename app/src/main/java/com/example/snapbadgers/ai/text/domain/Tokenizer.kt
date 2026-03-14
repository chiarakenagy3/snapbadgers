package com.example.snapbadgers.ai.text.domain

interface Tokenizer {
    fun tokenize(text: String): IntArray
}
