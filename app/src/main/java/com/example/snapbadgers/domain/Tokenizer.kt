package com.example.snapbadgers.domain

interface Tokenizer {
    fun tokenize(text: String): IntArray
}
