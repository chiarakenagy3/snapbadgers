package com.example.snapbadgers.ml

import androidx.test.platform.app.InstrumentationRegistry
import com.example.snapbadgers.domain.Tokenizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class QualcommTextEncoderIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var tokenizer: BertTokenizer
    private lateinit var encoder: QualcommTextEncoder

    @Before
    fun setup() {
        // Load the real vocab.txt from assets
        tokenizer = BertTokenizer.load(context, "vocab.txt")
        encoder = QualcommTextEncoder(context, tokenizer, "mobile_bert.tflite")
    }

    @Test
    fun testRealInference() = runBlocking {
        try {
            val query = "This is a test query for the S25 NPU"
            val result = encoder.encode(query)
            
            assertNotNull("Inference result should not be null", result)
            assertEquals("Output embedding must be 128 dimensions", 128, result.size)
            
            // Log the execution if successful
            println("Successfully executed inference on device!")
        } catch (e: Exception) {
            if (e.message?.contains("mobile_bert.tflite") == true) {
                throw Exception("MODEL FILE MISSING: Please ensure 'mobile_bert.tflite' is in the assets folder.")
            }
            throw e
        }
    }
}
