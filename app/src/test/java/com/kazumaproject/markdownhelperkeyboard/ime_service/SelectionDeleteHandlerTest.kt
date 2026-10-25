package com.kazumaproject.markdownhelperkeyboard.ime_service

import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SelectionDeleteHandlerTest {

    private lateinit var mockInputConnection: InputConnection
    private val deletedTexts = mutableListOf<String>()
    private lateinit var handler: SelectionDeleteHandler

    private fun setupText(text: String, selectionStart: Int, selectionEnd: Int = selectionStart) {
        val extractedText = ExtractedText().apply {
            this.text = text
            this.selectionStart = selectionStart
            this.selectionEnd = selectionEnd
        }
        `when`(mockInputConnection.getExtractedText(any(ExtractedTextRequest::class.java), anyInt()))
            .thenReturn(extractedText)
        org.mockito.Mockito.doAnswer { invocation ->
            val start = invocation.getArgument<Int>(0)
            val end = invocation.getArgument<Int>(1)
            extractedText.selectionStart = start
            extractedText.selectionEnd = end
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

    @Test
    fun testSwipeLeftCrossesNewlinesSmoothly() {
        val text = "first line\nsecond line\nthird line"
        // Cursor at the end of text (length = 33)
        setupText(text, text.length)
        handler.onSwipeStart()

        // Swipe left by 15 characters (should cross newline into 'second line')
        handler.onSwipeUpdate(deltaCharsX = -15, deltaCharsY = 0)
        verify(mockInputConnection).setSelection(text.length - 15, text.length)

        // Swipe left by 30 characters (should cross another newline into 'first line')
        handler.onSwipeUpdate(deltaCharsX = -30, deltaCharsY = 0)
        verify(mockInputConnection).setSelection(text.length - 30, text.length)

        // Swipe left beyond start (clamped to 0)
        handler.onSwipeUpdate(deltaCharsX = -50, deltaCharsY = 0)
        verify(mockInputConnection).setSelection(0, text.length)
    }

    @Test
    fun testSwipeUpCrossesMultipleLines() {
        val text = "line 1\nline 2\nline 3\nline 4"
        val anchor = text.length // 27 (at line 4)
        setupText(text, anchor)
        handler.onSwipeStart()

        // Swipe up 1 line (should go to start of line 4 / newline after line 3: index 20)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -1)
        verify(mockInputConnection).setSelection(20, anchor)

        // Swipe up 2 lines (should go to newline after line 2: index 13)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -2)
        verify(mockInputConnection).setSelection(13, anchor)

        // Swipe up 3 lines (should go to newline after line 1: index 6)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -3)
        verify(mockInputConnection).setSelection(6, anchor)

        // Swipe up 4 lines (should go to start of text: index 0)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -4)
        verify(mockInputConnection).setSelection(0, anchor)
    }

    @Test
    fun testDiagonalSwipeCombinesLineAndCharacterMovement() {
        val text = "line 1\nline 2\nline 3"
        val anchor = text.length // 20
        setupText(text, anchor)
        handler.onSwipeStart()

        // Swipe up 1 line and left 3 chars: line 2 newline (13) - 3 chars = 10
        handler.onSwipeUpdate(deltaCharsX = -3, deltaCharsY = -1)
        verify(mockInputConnection).setSelection(10, anchor)
    }

    @Test
    fun testAllSelectionDeletionReachesBeginningOfMultiLineText() {
        val text = "First paragraph line 1\nSecond paragraph line 2\nThird paragraph line 3"
        val anchor = text.length
        setupText(text, anchor)
        handler.onSwipeStart()

        // When user swipes to the left edge of the keyboard (-1000 chars)
        handler.onSwipeUpdate(deltaCharsX = -1000, deltaCharsY = 0)
        verify(mockInputConnection).setSelection(0, anchor)

        handler.onSwipeEnd()
        verify(mockInputConnection).commitText("", 1)
        assertEquals(1, deletedTexts.size)
        assertEquals(text, deletedTexts[0])
    }

    @Test
    fun testSwipeUpPreservesColumnPositionInMidLine() {
        // Line lengths: line 1 = 10, line 2 = 10
        val text = "0123456789\nabcdefghij"
        // Cursor at column 4 of line 2 (index 15)
        val anchor = 15
        setupText(text, anchor)
        handler.onSwipeStart()

        // Swipe up 1 line: should move to column 4 of line 1 (index 4), NOT line 1 end (10) or text start (0)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -1)
        verify(mockInputConnection).setSelection(4, anchor)
    }

    @Test
    fun testSwipeUpDownPreservesDesiredColumnAcrossShorterLine() {
        // Line 1: length 10 ("0123456789")
        // Line 2: length 3  ("ABC")
        // Line 3: length 10 ("abcdefghij")
        val text = "0123456789\nABC\nabcdefghij"
        // Cursor at column 8 of line 3 (start=15, anchor = 15 + 8 = 23)
        val anchor = 23
        setupText(text, anchor)
        handler.onSwipeStart()

        // Swipe up 1 line (to Line 2): Line 2 length is 3, so clamped to column 3 (index 11 + 3 = 14)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -1)
        verify(mockInputConnection).setSelection(14, anchor)

        // Swipe up 2 lines (to Line 1): Line 1 length is 10, desiredColumn (8) is restored! (index 0 + 8 = 8)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = -2)
        verify(mockInputConnection).setSelection(8, anchor)
    }

    @Test
    fun testSwipeDownMovesToMatchingColumnOnNextLine() {
        val text = "0123456789\nabcdefghij"
        // Cursor at column 5 of line 1 (index 5)
        val anchor = 5
        setupText(text, anchor)
        handler.onSwipeStart()

        // Swipe down 1 line: should move to column 5 of line 2 (index 11 + 5 = 16), NOT line 2 start (11)
        handler.onSwipeUpdate(deltaCharsX = 0, deltaCharsY = 1)
        verify(mockInputConnection).setSelection(16, anchor)
    }
}
