package com.kazumaproject.markdownhelperkeyboard.ime_service

import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class SelectionDeleteAdversarialTest {

    private lateinit var mockInputConnection: InputConnection
    private val deletedTexts = mutableListOf<String>()
    private lateinit var handler: SelectionDeleteHandler
    private var lastExtractedText: ExtractedText? = null

    private fun setupText(text: String, selectionStart: Int, selectionEnd: Int = selectionStart) {
        val extractedText = ExtractedText().apply {
            this.text = text
            this.selectionStart = selectionStart
            this.selectionEnd = selectionEnd
        }
        lastExtractedText = extractedText
        `when`(mockInputConnection.getExtractedText(any(ExtractedTextRequest::class.java), anyInt()))
            .thenAnswer { lastExtractedText }
        org.mockito.Mockito.doAnswer { invocation ->
            val start = invocation.getArgument<Int>(0)
            val end = invocation.getArgument<Int>(1)
            lastExtractedText?.selectionStart = start
            lastExtractedText?.selectionEnd = end
            true
        }.`when`(mockInputConnection).setSelection(anyInt(), anyInt())
    }

    @Before
    fun setUp() {
        mockInputConnection = mock(InputConnection::class.java)
        deletedTexts.clear()
        handler = SelectionDeleteHandler(
            inputConnectionProvider = { mockInputConnection },
            isComposingActive = { false },
            finishComposing = {},
            onDeletedText = { deletedTexts.add(it) }
        )
    }

    // ==========================================
    // 1. 空文字列・1文字・改行のみ・空行のエッジケース
    // ==========================================

    @Test
    fun testEmptyStringNeverCrashes() {
        setupText("", 0)
        handler.onSwipeStart()
        handler.onSwipeUpdate(deltaCharsX = -10, deltaCharsY = -5)
        handler.onSwipeUpdate(deltaCharsX = 10, deltaCharsY = 5)
        handler.onSwipeEnd()
        assertTrue(deletedTexts.isEmpty())
    }

    @Test
    fun testSingleCharTextNavigation() {
        setupText("A", 1)
        handler.onSwipeStart()
        handler.onSwipeUpdate(deltaCharsX = -1, deltaCharsY = 0)
        assertEquals(0, lastExtractedText?.selectionStart)
        assertEquals(1, lastExtractedText?.selectionEnd)

        handler.onSwipeEnd()
        assertEquals(1, deletedTexts.size)
        assertEquals("A", deletedTexts[0])
    }

    @Test
    fun testNewlineOnlyText() {
        // Text consisting only of newlines: "\n\n\n"
        setupText("\n\n\n", 3) // cursor at end
        handler.onSwipeStart()

        // Move up line by line
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -1)
        assertEquals(2, lastExtractedText?.selectionStart)

        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -2)
        assertEquals(1, lastExtractedText?.selectionStart)

        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -3)
        assertEquals(0, lastExtractedText?.selectionStart)

        handler.onSwipeEnd()
        assertEquals(1, deletedTexts.size)
        assertEquals("\n\n\n", deletedTexts[0])
    }

    @Test
    fun testEmptyLinesBetweenParagraphs() {
        // Line 0: "Line1" (len 5, 0..5)
        // Line 1: ""      (len 0, 6..6)
        // Line 2: ""      (len 0, 7..7)
        // Line 3: "Line3" (len 5, 8..13)
        val text = "Line1\n\n\nLine3"
        // Cursor at Line 3, col 3 ("Lin|e3", index 8 + 3 = 11)
        setupText(text, 11)
        handler.onSwipeStart()

        // Move to Line 2 (empty line) -> clamped to col 0 (index 7)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -1)
        assertEquals(7, lastExtractedText?.selectionStart)

        // Move to Line 1 (empty line) -> clamped to col 0 (index 6)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -2)
        assertEquals(6, lastExtractedText?.selectionStart)

        // Move to Line 0 (non-empty line) -> restored to col 3 (index 0 + 3 = 3)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -3)
        assertEquals(3, lastExtractedText?.selectionStart)
    }

    // ==========================================
    // 2. 水平スワイプ時の改行通過リニア性検証（数学的連続性）
    // ==========================================

    @Test
    fun testHorizontalLinearityAcrossMultipleNewlines() {
        // Text with multiple newlines and irregular lengths
        val text = "ABC\nDEFGH\n\nIJKLMN\nOP"
        val anchor = text.length // at end
        setupText(text, anchor)
        handler.onSwipeStart()

        // Scan backwards step-by-step from anchor to 0
        // Every single step must decrement position by exactly 1 with NO jumps
        for (step in 1..text.length) {
            handler.onSwipeUpdate(deltaCharsX = -step, deltaCharsY = 0)
            val expectedPos = anchor - step
            assertEquals(
                "Step -$step failed linearity check!",
                expectedPos,
                lastExtractedText?.selectionStart
            )
        }

        // Overshoot to negative
        handler.onSwipeUpdate(deltaCharsX = -(text.length + 50), deltaCharsY = 0)
        assertEquals(0, lastExtractedText?.selectionStart)

        // Scan forwards from 0 to text.length
        setupText(text, 0)
        handler.onSwipeStart()
        for (step in 1..text.length) {
            handler.onSwipeUpdate(deltaCharsX = step, deltaCharsY = 0)
            assertEquals(
                "Step +$step failed linearity check!",
                step,
                lastExtractedText?.selectionStart
            )
        }

        // Overshoot to positive
        handler.onSwipeUpdate(deltaCharsX = text.length + 50, deltaCharsY = 0)
        assertEquals(text.length, lastExtractedText?.selectionStart)
    }

    // ==========================================
    // 3. 垂直スワイプ時のカラム保持・復元（ジグザグ移動）
    // ==========================================

    @Test
    fun testVerticalColumnPreservationZigZag() {
        // 5 lines with wildly different lengths
        // Line 0: "0123456789" (len 10, index 0..10)
        // Line 1: "A"          (len 1,  index 11..12)
        // Line 2: ""           (len 0,  index 13..13)
        // Line 3: "ABC"        (len 3,  index 14..17)
        // Line 4: "abcdefghij" (len 10, index 18..28)
        val text = "0123456789\nA\n\nABC\nabcdefghij"
        // Anchor at Line 4, col 8 (index 18 + 8 = 26)
        val anchor = 26
        setupText(text, anchor)
        handler.onSwipeStart()

        // 1. Move up to Line 3 (len 3): desiredCol=8 -> clamped to min(8,3)=3 -> index 14+3=17
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -1)
        assertEquals(17, lastExtractedText?.selectionStart)

        // 2. Move up to Line 2 (len 0): clamped to min(8,0)=0 -> index 13+0=13
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -2)
        assertEquals(13, lastExtractedText?.selectionStart)

        // 3. Move up to Line 1 (len 1): clamped to min(8,1)=1 -> index 11+1=12
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -3)
        assertEquals(12, lastExtractedText?.selectionStart)

        // 4. Move up to Line 0 (len 10): desiredCol=8 restored! -> index 0+8=8
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -4)
        assertEquals(8, lastExtractedText?.selectionStart)

        // 5. Move down back to Line 3: desiredCol=8 -> min(8,3)=3 -> index 14+3=17
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -1)
        assertEquals(17, lastExtractedText?.selectionStart)

        // 6. Move back to Line 4: desiredCol=8 restored! -> index 18+8=26
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = 0)
        assertEquals(26, lastExtractedText?.selectionStart)
    }

    // ==========================================
    // 4. 超長文テキスト・パフォーマンステスト
    // ==========================================

    @Test
    fun testLargeDocumentPerformanceAndStress() {
        // Construct 5,000 lines (approx 100,000 characters)
        val sb = StringBuilder()
        for (i in 0 until 5000) {
            sb.append("Line ").append(i).append(" of stress test text\n")
        }
        val text = sb.toString()
        val anchor = text.length / 2 // cursor in the middle
        setupText(text, anchor)

        val initTime = measureTimeMillis {
            handler.onSwipeStart()
        }
        // SwipeStart should be relatively fast (< 50ms)
        assertTrue("onSwipeStart took ${initTime}ms", initTime < 100)

        // Perform 100 rapid swipe updates across different lines
        val updateTime = measureTimeMillis {
            for (i in 1..100) {
                val deltaY = (i % 200) - 100
                val deltaX = (i % 40) - 20
                handler.onSwipeUpdate(deltaCharsX = deltaX, deltaCharsY = deltaY)
            }
        }
        // 100 updates should complete reasonably fast (< 500ms)
        assertTrue("100 onSwipeUpdates took ${updateTime}ms", updateTime < 1000)

        handler.onSwipeEnd()
    }

    // ==========================================
    // 5. 境界値・極端値・不正入力に対する例外安全性（Zero-Crash）
    // ==========================================

    @Test
    fun testExtremeDeltaValuesNeverThrow() {
        val text = "Hello world\nSecond line"
        setupText(text, 5)
        handler.onSwipeStart()

        // Extremes of Int
        handler.onSwipeUpdate(deltaCharsX = Int.MAX_VALUE, deltaCharsY = Int.MAX_VALUE)
        assertTrue(lastExtractedText!!.selectionStart in 0..text.length)

        handler.onSwipeUpdate(deltaCharsX = Int.MIN_VALUE, deltaCharsY = Int.MIN_VALUE)
        assertTrue(lastExtractedText!!.selectionStart in 0..text.length)

        handler.onSwipeUpdate(deltaCharsX = Int.MAX_VALUE, deltaCharsY = -100)
        assertTrue(lastExtractedText!!.selectionStart in 0..text.length)

        handler.onSwipeUpdate(deltaCharsX = -100, deltaCharsY = Int.MAX_VALUE)
        assertTrue(lastExtractedText!!.selectionStart in 0..text.length)

        handler.onSwipeEnd()
    }

    @Test
    fun testSurrogatePairEmojiTextNavigation() {
        // Text containing surrogate pairs: 😀 (2 chars), 🇯🇵 (4 chars)
        val text = "A😀B🇯🇵C"
        // String length is 1 + 2 + 1 + 4 + 1 = 9
        val anchor = text.length
        setupText(text, anchor)
        handler.onSwipeStart()

        for (step in 1..text.length) {
            handler.onSwipeUpdate(deltaCharsX = -step, deltaCharsY = 0)
            assertEquals(anchor - step, lastExtractedText?.selectionStart)
        }
        handler.onSwipeEnd()
        assertEquals(1, deletedTexts.size)
        assertEquals(text, deletedTexts[0])
    }

    /**
     * 【脆弱性修正・安全性検証テスト】
     * 外部クライアントや非同期テキスト変更等で selectionStart/End が text.length を超えるか負数の場合でも、
     * SelectionDeleteHelper.kt において coerceIn による厳密な境界値クランプが行われ、
     * StringIndexOutOfBoundsException をスローすることなく安全に処理されることを検証。
     */
    @Test
    fun testCorruptedSelectionIndicesInSwipeEndHandledSafely() {
        val text = "Sample text"
        setupText(text, 2)
        handler.onSwipeStart()
        handler.onSwipeUpdate(deltaCharsX = 3, deltaCharsY = 0)

        // Corrupt selection indices to out of bounds (simulating asynchronous text deletion or faulty IME host)
        lastExtractedText?.selectionStart = -5
        lastExtractedText?.selectionEnd = 20

        var caughtException: Throwable? = null
        try {
            handler.onSwipeEnd()
        } catch (e: Throwable) {
            caughtException = e
        }
        assertTrue("No exception should be thrown on corrupted selection indices", caughtException == null)
        assertEquals(1, deletedTexts.size)
        // クランプされた範囲 (0..11) のテキストが安全に削除される
        assertEquals("Sample text", deletedTexts[0])
    }

    @Test
    fun testCorruptedSelectionIndicesBothNegativeHandledSafely() {
        val text = "Sample text"
        setupText(text, 2)
        handler.onSwipeStart()

        lastExtractedText?.selectionStart = -10
        lastExtractedText?.selectionEnd = -5

        var caughtException: Throwable? = null
        try {
            handler.onSwipeEnd()
        } catch (e: Throwable) {
            caughtException = e
        }
        assertTrue("No exception should be thrown when both indices are negative", caughtException == null)
        assertTrue("No text should be deleted when indices clamp to the same point", deletedTexts.isEmpty())
    }

    @Test
    fun testCorruptedSelectionIndicesBothExceedingLengthHandledSafely() {
        val text = "Sample text"
        setupText(text, 2)
        handler.onSwipeStart()

        lastExtractedText?.selectionStart = 50
        lastExtractedText?.selectionEnd = 100

        var caughtException: Throwable? = null
        try {
            handler.onSwipeEnd()
        } catch (e: Throwable) {
            caughtException = e
        }
        assertTrue("No exception should be thrown when both indices exceed length", caughtException == null)
        assertTrue("No text should be deleted when indices clamp to the same point", deletedTexts.isEmpty())
    }

    @Test
    fun testCalculateSelectionIndicesClamping() {
        val text = "0123456789" // len = 10
        assertEquals(Pair(0, 10), handler.calculateSelectionIndices(text, -10, 50))
        assertEquals(Pair(2, 7), handler.calculateSelectionIndices(text, 7, 2))
        assertEquals(Pair(0, 5), handler.calculateSelectionIndices(text, -1, 5))
        assertEquals(Pair(5, 10), handler.calculateSelectionIndices(text, 5, 20))
        assertEquals(Pair(0, 0), handler.calculateSelectionIndices(text, -1, -1))
        assertEquals(Pair(10, 10), handler.calculateSelectionIndices(text, 15, 20))
        assertEquals(Pair(0, 0), handler.calculateSelectionIndices("", -5, 5))
    }

    // ==========================================
    // 6. 追加の極限コーナーケース検証 (Round 2 Empirical Challenge)
    // ==========================================

    @Test
    fun testCRLFMixedTextNavigation() {
        val text = "Line1\r\nLine2\r\nLine3"
        setupText(text, 0)
        handler.onSwipeStart()

        // 垂直移動で各行へ移動
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = 1)
        assertTrue(lastExtractedText!!.selectionStart in 0..text.length)

        handler.onSwipeUpdate(deltaCharsX = 2, deltaCharsY = 1)
        assertTrue(lastExtractedText!!.selectionStart in 0..text.length)

        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = 2)
        assertTrue(lastExtractedText!!.selectionStart in 0..text.length)

        handler.onSwipeEnd()
    }

    @Test
    fun testAsynchronousTextDisappearanceDuringSwipe() {
        val text = "Long text initially"
        setupText(text, 5)
        handler.onSwipeStart()
        handler.onSwipeUpdate(deltaCharsX = 2, deltaCharsY = 0)

        // テキストが非同期に突然空になる
        lastExtractedText?.text = ""
        lastExtractedText?.selectionStart = 0
        lastExtractedText?.selectionEnd = 0

        // onSwipeUpdate でもクラッシュしない
        handler.onSwipeUpdate(deltaCharsX = 5, deltaCharsY = 1)

        // onSwipeEnd でもクラッシュしない
        handler.onSwipeEnd()
        assertTrue(deletedTexts.isEmpty())
    }

    @Test
    fun testAsynchronousTextBecomesNullDuringSwipe() {
        val text = "Initial text"
        setupText(text, 3)
        handler.onSwipeStart()

        // text が null に化ける
        lastExtractedText?.text = null
        lastExtractedText?.selectionStart = 0
        lastExtractedText?.selectionEnd = 0

        handler.onSwipeUpdate(deltaCharsX = 1, deltaCharsY = 1)
        handler.onSwipeEnd()
        assertTrue(deletedTexts.isEmpty())
    }

    @Test
    fun testSwipeStartWithExceedingAnchor() {
        val text = "Short"
        // 外部IMEがテキスト長(5)を超えるカーソル位置(100)を返してきた場合
        setupText(text, 100)
        handler.onSwipeStart()

        // 更新しても例外が出ないこと
        handler.onSwipeUpdate(deltaCharsX = -2, deltaCharsY = 0)
        assertEquals(3, lastExtractedText?.selectionStart)

        handler.onSwipeEnd()
    }

    @Test
    fun testDiagonalChaosMovement() {
        val text = "AAAA\nBBBB\nCCCC\nDDDD\nEEEE"
        setupText(text, 12)
        handler.onSwipeStart()

        // 激しい対角移動を100回ランダム/極値で実行
        val rand = Random(42)
        for (i in 0 until 100) {
            val dx = rand.nextInt(-50, 50)
            val dy = rand.nextInt(-10, 10)
            handler.onSwipeUpdate(deltaCharsX = dx, deltaCharsY = dy)
            val pos = lastExtractedText!!.selectionStart
            assertTrue("Position $pos out of bounds [0, ${text.length}]", pos in 0..text.length)
        }
        handler.onSwipeEnd()
    }

    @Test
    fun testCalculateSelectionIndicesExtremeInverted() {
        val text = "ABCDE"
        // rawStart > rawEnd かつ負数・超過
        assertEquals(Pair(0, 0), handler.calculateSelectionIndices(text, -5, -20))
        assertEquals(Pair(5, 5), handler.calculateSelectionIndices(text, 100, 50))
        assertEquals(Pair(0, 5), handler.calculateSelectionIndices(text, 50, -10))
        assertEquals(Pair(2, 4), handler.calculateSelectionIndices(text, 4, 2))
    }
}



