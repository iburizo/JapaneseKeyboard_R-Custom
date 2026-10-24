package com.kazumaproject.markdownhelperkeyboard.ime_service

import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts
import com.kazumaproject.markdownhelperkeyboard.ime_service.floating_cursor.CursorDirection
import com.kazumaproject.markdownhelperkeyboard.ime_service.floating_cursor.CursorMovement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CursorSelectionEditorPreserveTest {

    private val inputConnection = mock<InputConnection>()
    private var selectionStart = 5
    private var selectionEnd = 5
    private val selectionHistory = mutableListOf<Pair<Int, Int>>()

    private var cursorSelectionMode = false

    @Before
    fun setup() {
        selectionHistory.clear()
        selectionStart = 5
        selectionEnd = 5
        cursorSelectionMode = false

        whenever(inputConnection.setSelection(any(), any())).thenAnswer {
            val start = it.getArgument<Int>(0)
            val end = it.getArgument<Int>(1)
            selectionStart = start
            selectionEnd = end
            selectionHistory += (start to end)
            true
        }

        whenever(inputConnection.getExtractedText(any(), eq(0))).thenAnswer {
            ExtractedText().apply {
                text = "0123456789"
                startOffset = 0
                selectionStart = this@CursorSelectionEditorPreserveTest.selectionStart
                selectionEnd = this@CursorSelectionEditorPreserveTest.selectionEnd
                partialStartOffset = -1
            }
        }
    }

    @After
    fun tearDown() {
        KeyboardDefaultLayouts.cursorSelectionToggleHandler = null
        KeyboardDefaultLayouts.cursorSelectionEndHandler = null
    }

    @Test
    fun testCursorSelectionExpandsAndPreservesSelectionOnFingerUp() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val movement = CursorMovement(Mutex(), { _, a, b ->
            inputConnection.setSelection(a, b)
        }, { _, _ -> true }, dispatcher)

        // 1. 2本指タップで選択範囲モード ON
        cursorSelectionMode = true

        // 2. 左へ2文字移動（アンカー: 5、キャレット: 3）
        movement.move(inputConnection, CursorDirection.LEFT, cursorSelectionMode) { true }
        movement.move(inputConnection, CursorDirection.LEFT, cursorSelectionMode) { true }

        assertEquals("Selection should expand to 5..3", listOf(5 to 4, 5 to 3), selectionHistory)
        assertEquals(5, selectionStart)
        assertEquals(3, selectionEnd)

        // 3. 指離脱（ACTION_UP / ACTION_CANCEL に伴う cursorSelectionEndHandler 実行）
        // 重要: 離脱ハンドラは cursorSelectionMode を false にするだけで、エディタの選択範囲を解除しない
        cursorSelectionMode = false

        // エディタの選択状態が 5..3 のまま維持されていることの検証
        assertEquals("Selection in editor must remain 5..3 after finger release", 5, selectionStart)
        assertEquals("Selection in editor must remain 5..3 after finger release", 3, selectionEnd)

        // 4. 再度通常カーソル移動（非選択モード）が行われた場合のみ、現在キャレットへ安全にコラプス
        movement.move(inputConnection, CursorDirection.LEFT, cursorSelectionMode) { true }
        assertEquals("Subsequent normal move collapses selection to 3..3", 3 to 3, selectionHistory.last())
    }

    @Test
    fun testReToggleReturnsToNormalMovementWhilePreservingSelectionUntilMoved() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val movement = CursorMovement(Mutex(), { _, a, b ->
            inputConnection.setSelection(a, b)
        }, { _, _ -> true }, dispatcher)

        // 1. 選択モード ON -> 移動
        cursorSelectionMode = true
        movement.move(inputConnection, CursorDirection.LEFT, cursorSelectionMode) { true }
        assertEquals(5 to 4, selectionHistory.last())

        // 2. 2回目の2本指タップで再トグル（通常カーソル移動に復帰）
        cursorSelectionMode = false

        // 移動していない間は選択範囲 5..4 が保持される
        assertEquals(5, selectionStart)
        assertEquals(4, selectionEnd)
    }
}
