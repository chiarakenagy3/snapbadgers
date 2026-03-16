package com.example.snapbadgers.ml

import com.example.snapbadgers.domain.Tokenizer
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Context

class BertTokenizer(
    private val vocab: Map<String, Int>,
    private val doLowerCase: Boolean = true
) : Tokenizer {

    private val unknownToken = "[UNK]"
    private val maxInputCharsPerWord = 100

    override fun tokenize(text: String): IntArray {
        val tokens = mutableListOf<Int>()
        val cleanedText = if (doLowerCase) text.lowercase() else text
        
        // Basic whitespace splitting
        val words = cleanedText.trim().split(Regex("\\s+"))
        
        for (word in words) {
            if (word.length > maxInputCharsPerWord) {
                tokens.add(vocab[unknownToken] ?: 0)
                continue
            }

            var start = 0
            while (start < word.length) {
                var end = word.length
                var curSubword: String? = null
                
                while (start < end) {
                    var substr = word.substring(start, end)
                    if (start > 0) {
                        substr = "##$substr"
                    }
                    
                    if (vocab.containsKey(substr)) {
                        curSubword = substr
                        break
                    }
                    end--
                }
                
                if (curSubword == null) {
                    tokens.add(vocab[unknownToken] ?: 0)
                    break
                }
                
                tokens.add(vocab[curSubword]!!)
                start = end
            }
        }
        
        return tokens.toIntArray()
    }

    companion object {
        fun load(context: Context, vocabFile: String): BertTokenizer {
            val vocab = mutableMapOf<String, Int>()
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
