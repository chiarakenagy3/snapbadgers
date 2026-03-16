package com.example.snapbadgers.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BertTokenizerTest {

    private val vocab = mapOf(
        "[PAD]" to 0,
        "[UNK]" to 1,
        "[CLS]" to 2,
        "[SEP]" to 3,
        "the" to 1996,
        "cat" to 4937,
        "play" to 2377,
        "##ing" to 2075
    )
    private val tokenizer = BertTokenizer(vocab)

    @Test
    fun `tokenize simple words`() {
        val text = "the cat"
        val result = tokenizer.tokenize(text)
        
        // "the" -> 1996, "cat" -> 4937
        val expected = intArrayOf(1996, 4937)
        assertArrayEquals(expected, result)
    }

    @Test
    fun `tokenize subwords`() {
        val text = "playing"
        val result = tokenizer.tokenize(text)
        
        // "play" -> 2377, "##ing" -> 2075
        val expected = intArrayOf(2377, 2075)
        assertArrayEquals(expected, result)
    }

    @Test
    fun `tokenize unknown word`() {
        val text = "unknown_word_xyz"
        val result = tokenizer.tokenize(text)
        
        // "[UNK]" -> 1
        val expected = intArrayOf(1)
        assertArrayEquals(expected, result)
    }
}
