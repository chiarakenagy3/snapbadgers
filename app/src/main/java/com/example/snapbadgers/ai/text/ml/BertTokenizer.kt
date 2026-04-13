package com.example.snapbadgers.ai.text.ml

import android.content.Context
import com.example.snapbadgers.ai.text.Tokenizer
import java.io.BufferedReader
import java.io.InputStreamReader

class BertTokenizer(
    private val vocab: Map<String, Int>,
    private val doLowerCase: Boolean = true
) : Tokenizer {

    private val unknownToken = "[UNK]"
    private val maxInputCharsPerWord = 100

    override fun tokenize(text: String): IntArray {
        val tokens = mutableListOf<Int>()
        val cleanedText = if (doLowerCase) text.lowercase() else text
        val words = cleanedText.trim().split(Regex("\\s+"))

        for (word in words) {
            if (word.length > maxInputCharsPerWord) {
                tokens.add(vocab[unknownToken] ?: 0)
                continue
            }

            var start = 0
            while (start < word.length) {
                var end = word.length
                var currentSubword: String? = null

                while (start < end) {
                    var substring = word.substring(start, end)
                    if (start > 0) {
                        substring = "##$substring"
                    }

                    if (vocab.containsKey(substring)) {
                        currentSubword = substring
                        break
                    }
                    end--
                }

                if (currentSubword == null) {
                    tokens.add(vocab[unknownToken] ?: 0)
                    break
                }

                tokens.add(vocab.getValue(currentSubword))
                start = end
            }
        }

        return tokens.toIntArray()
    }

    companion object {
        fun load(context: Context, vocabFile: String): BertTokenizer {
            val vocab = HashMap<String, Int>(32768)
            context.assets.open(vocabFile).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var index = 0
                    var line = reader.readLine()
                    while (line != null) {
                        vocab[line.trim()] = index++
                        line = reader.readLine()
                    }
                }
            }
            return BertTokenizer(vocab)
        }
    }
}
