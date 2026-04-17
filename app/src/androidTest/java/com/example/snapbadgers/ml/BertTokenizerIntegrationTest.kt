package com.example.snapbadgers.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.text.ml.BertTokenizer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BertTokenizerIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var tokenizer: BertTokenizer

    @Before
    fun setup() {
        tokenizer = BertTokenizer.load(context, "vocab.txt")
    }

    @Test
    fun testTokenizerLoadsSuccessfully() {
        assertNotNull(tokenizer)
    }

    @Test
    fun testTokenizeVariousInputs() {
        listOf(
            "hello world",
            "This is a very long text that should be tokenized properly even though it contains many words and extends beyond typical sentence length.",
            "hello! how are you?",
            "I have 123 apples",
            "音楽 música موسيقى",
            "music music music",
            "hello    world",
            "line1\nline2\nline3"
        ).forEach { input ->
            val tokens = tokenizer.tokenize(input)
            assertNotNull(tokens)
            assertTrue("Should have tokens for: ${input.take(30)}", tokens.isNotEmpty())
        }
    }

    @Test
    fun testTokenizeEmptyString() {
        val tokens = tokenizer.tokenize("")
        assertNotNull(tokens)
    }

    @Test
    fun testTokenizeCaseSensitivity() {
        val lowerTokens = tokenizer.tokenize("hello")
        val upperTokens = tokenizer.tokenize("HELLO")
        assertNotNull(lowerTokens)
        assertNotNull(upperTokens)
        if (lowerTokens.isNotEmpty() && upperTokens.isNotEmpty()) {
            println("Lower: ${lowerTokens.toList()}")
            println("Upper: ${upperTokens.toList()}")
        }
    }

    @Test
    fun testTokenizeMusicDomainTerms() {
        listOf(
            "calm relaxing music",
            "energetic workout beats",
            "ambient electronic",
            "classical piano",
            "rock guitar"
        ).forEach { term ->
            val tokens = tokenizer.tokenize(term)
            assertNotNull("Should tokenize: $term", tokens)
            assertTrue("Should have tokens for: $term", tokens.isNotEmpty())
            println("$term -> ${tokens.size} tokens")
        }
    }

    @Test
    fun testTokenizationIsDeterministic() {
        val text = "deterministic test"
        val tokens1 = tokenizer.tokenize(text)
        val tokens2 = tokenizer.tokenize(text)
        assertArrayEquals("Same text should produce same tokens", tokens1, tokens2)
    }

    @Test
    fun testTokenizeMaxLength() {
        val veryLongText = "word ".repeat(1000)
        val tokens = tokenizer.tokenize(veryLongText)
        assertNotNull(tokens)
        println("Long text produced ${tokens.size} tokens")
    }

    @Test
    fun testTokenizeCommonWords() {
        listOf("the", "a", "an", "is", "was", "are", "be").forEach { word ->
            val tokens = tokenizer.tokenize(word)
            assertNotNull("Should tokenize common word: $word", tokens)
            println("$word -> ${tokens.toList()}")
        }
    }

    @Test
    fun testTokenizeSpecialTokens() {
        listOf(
            "[CLS] hello [SEP]",
            "[UNK] unknown word",
            "[MASK] hidden word"
        ).forEach { text ->
            val tokens = tokenizer.tokenize(text)
            assertNotNull("Should handle special tokens in: $text", tokens)
        }
    }
}
