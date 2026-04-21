package com.example.snapbadgers.ai.text.ml

import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder.Companion.CLS_TOKEN_ID
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder.Companion.PAD_TOKEN_ID
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder.Companion.SEP_TOKEN_ID
import com.example.snapbadgers.ai.text.ml.QualcommTextEncoder.Companion.packBertInputs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-JVM tests for the BERT three-tensor input packing.
 *
 * Covers the intricate buffer layout that replaces the previous single-input-tensor
 * fallback. If the packing is wrong the model will silently emit garbage (no throw),
 * so these assertions are the primary non-device verification that the MobileBERT
 * wiring is correct.
 */
class QualcommTextEncoderPackingTest {

    private val maxLength = 128

    private fun newIntBuffer() = ByteBuffer.allocateDirect(maxLength * 4).order(ByteOrder.nativeOrder())

    private fun ByteBuffer.toIntList(): List<Int> {
        rewind()
        return List(maxLength) { int }
    }

    @Test
    fun packsShortInputWithClsSepAndPad() {
        val ids = newIntBuffer()
        val mask = newIntBuffer()
        val seg = newIntBuffer()
        val tokens = intArrayOf(1234, 5678, 9012)

        packBertInputs(tokens, ids, mask, seg, maxLength)

        val idsList = ids.toIntList()
        val maskList = mask.toIntList()
        val segList = seg.toIntList()

        // Layout: [CLS] 1234 5678 9012 [SEP] [PAD]*123
        assertEquals("ids[0] should be [CLS]", CLS_TOKEN_ID, idsList[0])
        assertEquals("ids[1] should be first body token", 1234, idsList[1])
        assertEquals("ids[2] should be second body token", 5678, idsList[2])
        assertEquals("ids[3] should be third body token", 9012, idsList[3])
        assertEquals("ids[4] should be [SEP]", SEP_TOKEN_ID, idsList[4])
        for (i in 5 until maxLength) {
            assertEquals("ids[$i] should be [PAD]", PAD_TOKEN_ID, idsList[i])
        }

        // Mask: 1 for real positions (0..4), 0 for padded positions (5..127)
        for (i in 0 until 5) {
            assertEquals("mask[$i] should be 1", 1, maskList[i])
        }
        for (i in 5 until maxLength) {
            assertEquals("mask[$i] should be 0", 0, maskList[i])
        }

        // Segment ids: all 0 for single-sentence input
        assertArrayEquals(IntArray(maxLength) { 0 }, segList.toIntArray())
    }

    @Test
    fun packsEmptyTokenListWithOnlyClsAndSep() {
        val ids = newIntBuffer()
        val mask = newIntBuffer()
        val seg = newIntBuffer()

        packBertInputs(IntArray(0), ids, mask, seg, maxLength)

        val idsList = ids.toIntList()
        assertEquals(CLS_TOKEN_ID, idsList[0])
        assertEquals(SEP_TOKEN_ID, idsList[1])
        for (i in 2 until maxLength) {
            assertEquals(PAD_TOKEN_ID, idsList[i])
        }

        val maskList = mask.toIntList()
        assertEquals(1, maskList[0])
        assertEquals(1, maskList[1])
        for (i in 2 until maxLength) {
            assertEquals(0, maskList[i])
        }
    }

    @Test
    fun truncatesBodyTokensToLeaveRoomForClsAndSep() {
        val ids = newIntBuffer()
        val mask = newIntBuffer()
        val seg = newIntBuffer()
        // Provide more tokens than max-2 to force truncation.
        val tokens = IntArray(200) { it + 1000 }

        packBertInputs(tokens, ids, mask, seg, maxLength)

        val idsList = ids.toIntList()
        val bodyMaxLen = maxLength - 2

        assertEquals("ids[0] == [CLS]", CLS_TOKEN_ID, idsList[0])
        for (i in 0 until bodyMaxLen) {
            assertEquals("ids[${i + 1}] should hold body token $i", tokens[i], idsList[i + 1])
        }
        assertEquals("ids[${maxLength - 1}] should be [SEP] at the end", SEP_TOKEN_ID, idsList[maxLength - 1])

        // Entire sequence is real tokens: no padding region, mask should be all 1.
        val maskList = mask.toIntList()
        for (i in 0 until maxLength) {
            assertEquals("mask[$i] should be 1 when sequence is fully packed", 1, maskList[i])
        }
    }

    @Test
    fun packsExactlyBoundaryLengthWithoutPadding() {
        val ids = newIntBuffer()
        val mask = newIntBuffer()
        val seg = newIntBuffer()
        // Exactly max-2 body tokens — perfect fit, no padding.
        val tokens = IntArray(maxLength - 2) { it + 500 }

        packBertInputs(tokens, ids, mask, seg, maxLength)

        val idsList = ids.toIntList()
        assertEquals(CLS_TOKEN_ID, idsList.first())
        assertEquals(SEP_TOKEN_ID, idsList.last())

        val maskList = mask.toIntList()
        assertArrayEquals(IntArray(maxLength) { 1 }, maskList.toIntArray())
    }

    @Test
    fun rewindsBuffersSoTheyAreReadyToFeedInterpreter() {
        val ids = newIntBuffer()
        val mask = newIntBuffer()
        val seg = newIntBuffer()

        packBertInputs(intArrayOf(42), ids, mask, seg, maxLength)

        // Each buffer is sized for maxLength * 4 bytes. After packing we should be able to
        // read maxLength ints from position 0 without further rewinding on the caller side.
        assertEquals("ids position should be 0 after pack", 0, ids.position())
        assertEquals("mask position should be 0 after pack", 0, mask.position())
        assertEquals("seg position should be 0 after pack", 0, seg.position())
    }

    @Test
    fun usesKnownStandardBertTokenIds() {
        // Guard against accidental drift in the special-token constants.
        // Values are verified against app/src/main/assets/vocab.txt.
        assertEquals("[PAD] id", 0, PAD_TOKEN_ID)
        assertEquals("[CLS] id", 101, CLS_TOKEN_ID)
        assertEquals("[SEP] id", 102, SEP_TOKEN_ID)
    }
}
