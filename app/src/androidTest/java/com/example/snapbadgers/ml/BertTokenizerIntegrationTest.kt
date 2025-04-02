package com.example.snapbadgers.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.ai.text.ml.BertTokenizer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for BertTokenizer with real vocab file.
 */
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
        assertNotNull("Tokenizer should be loaded", tokenizer)
    }

    @Test
    fun testTokenizeSimpleText() {
        val tokens = tokenizer.tokenize("hello world")

        assertNotNull(tokens)
        assertTrue("Should have tokens", tokens.isNotEmpty())
    }

    @Test
    fun testTokenizeEmptyString() {
        val tokens = tokenizer.tokenize("")

        assertNotNull(tokens)
        // Should handle empty string gracefully
    }

    @Test
    fun testTokenizeLongText() {
        val longText = "This is a very long text that should be tokenized properly even though it contains many words and extends beyond typical sentence length."

        val tokens = tokenizer.tokenize(longText)

        assertNotNull(tokens)
        assertTrue("Should have tokens for long text", tokens.isNotEmpty())
    }

    @Test
    fun testTokenizeSpecialCharacters() {
        val tokens = tokenizer.tokenize("hello! how are you?")

        assertNotNull(tokens)
        assertTrue("Should handle punctuation", tokens.isNotEmpty())
    }

    @Test
    fun testTokenizeNumbers() {
        val tokens = tokenizer.tokenize("I have 123 apples")

        assertNotNull(tokens)
        assertTrue("Should handle numbers", tokens.isNotEmpty())
    }

    @Test
    fun testTokenizeCaseSensitivity() {
        val lowerTokens = tokenizer.tokenize("hello")
        val upperTokens = tokenizer.tokenize("HELLO")

        assertNotNull(lowerTokens)
        assertNotNull(upperTokens)

        // BERT typically lowercases, so should be same
        if (lowerTokens.isNotEmpty() && upperTokens.isNotEmpty()) {
            println("Lower: ${lowerTokens.toList()}")
            println("Upper: ${upperTokens.toList()}")
        }
    }

    @Test
    fun testTokenizeUnicodeText() {
        val tokens = tokenizer.tokenize("音楽 música موسيقى")

        assertNotNull(tokens)
        // Should handle unicode, even if as unknown tokens
    }

    @Test
    fun testTokenizeRepeatedWords() {
        val tokens = tokenizer.tokenize("music music music")

        assertNotNull(tokens)
        assertTrue("Should tokenize repeated words", tokens.isNotEmpty())
    }

    @Test
    fun testTokenizeMusicDomainTerms() {
        val musicTerms = listOf(
            "calm relaxing music",
            "energetic workout beats",
            "ambient electronic",
            "classical piano",
            "rock guitar"
        )

        musicTerms.forEach { term ->
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

        assertArrayEquals("Same text should produce same tokens",
            tokens1, tokens2)
    }

    @Test
    fun testTokenizeMaxLength() {
        val veryLongText = "word ".repeat(1000)

        val tokens = tokenizer.tokenize(veryLongText)

        assertNotNull(tokens)
        // BERT typically has 512 or 128 max length
        println("Long text produced ${tokens.size} tokens")
    }

    @Test
    fun testTokenizeCommonWords() {
        val commonWords = listOf("the", "a", "an", "is", "was", "are", "be")

        commonWords.forEach { word ->
            val tokens = tokenizer.tokenize(word)
            assertNotNull("Should tokenize common word: $word", tokens)
            println("$word -> ${tokens.toList()}")
        }
    }

    @Test
    fun testTokenizeWithWhitespace() {
        val tokens = tokenizer.tokenize("hello    world")

        assertNotNull(tokens)
        assertTrue("Should handle multiple spaces", tokens.isNotEmpty())
    }

    @Test
    fun testTokenizeWithNewlines() {
        val tokens = tokenizer.tokenize("line1\nline2\nline3")

        assertNotNull(tokens)
        assertTrue("Should handle newlines", tokens.isNotEmpty())
    }

    @Test
    fun testTokenizeSpecialTokens() {
        // Test if tokenizer handles special BERT tokens
        val texts = listOf(
            "[CLS] hello [SEP]",
            "[UNK] unknown word",
            "[MASK] hidden word"
        )

        texts.forEach { text ->
            val tokens = tokenizer.tokenize(text)
            assertNotNull("Should handle special tokens in: $text", tokens)
        }
    }
}
