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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 敵対的ストレステスト: IMEService / CursorSelectionBinding における要件R1のエッジケース・境界値検証。
 * - 高速連続トグル（100回）における状態整合性と振動コールバック
 * - プレエディット（未確定文字列）存在時のトグル防護
 * - 離脱順序競合や二重離脱時の選択範囲非破壊（エディタ選択の完全保持）
 * - 左右上下の乱数移動におけるアンカー整合性
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CursorSelectionBindingStressTest {

    private val inputConnection = mock<InputConnection>()
    private var selectionStart = 10
    private var selectionEnd = 10
    private val selectionHistory = mutableListOf<Pair<Int, Int>>()

    private var cursorSelectionMode = false
    private var vibrateCount = 0

    @Before
    fun setup() {
        selectionHistory.clear()
        selectionStart = 10
        selectionEnd = 10
        cursorSelectionMode = false
        vibrateCount = 0

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
                text = "01234567890123456789"
                startOffset = 0
                selectionStart = this@CursorSelectionBindingStressTest.selectionStart
                selectionEnd = this@CursorSelectionBindingStressTest.selectionEnd
                partialStartOffset = -1
            }
        }
    }

    @After
    fun tearDown() {
        CursorSelectionGlobalBinder.unbind()
    }

    /**
     * 検証1: 高速100回連続トグルによる状態同期とバイブレーション整合性
     */
    @Test
    fun testRapidSuccessiveToggle100TimesIntegrity() {
        var hasPreedit = false

        fun toggle() {
            if (hasPreedit) return
            cursorSelectionMode = !cursorSelectionMode
            vibrateCount++
        }

        CursorSelectionGlobalBinder.bind(
            toggleSelection = { toggle() },
            endSelection = { cursorSelectionMode = false },
            isSelectionMode = { cursorSelectionMode }
        )

        // 1. プレエディットありの時はトグルがガードされる
        hasPreedit = true
        for (i in 1..20) {
            KeyboardDefaultLayouts.cursorSelectionToggleHandler?.invoke()
            assertFalse("Must NOT toggle when preedit exists", cursorSelectionMode)
            assertEquals(0, vibrateCount)
        }

        // 2. プレエディットなしの時、100回連続トグル
        hasPreedit = false
        for (i in 1..100) {
            KeyboardDefaultLayouts.cursorSelectionToggleHandler?.invoke()
            val expectedMode = (i % 2 != 0)
            assertEquals("cursorSelectionMode mismatch at toggle #$i", expectedMode, cursorSelectionMode)
            assertEquals("isCursorSelectionModeProvider mismatch at toggle #$i", expectedMode, KeyboardDefaultLayouts.isCursorSelectionModeProvider?.invoke())
            assertEquals("vibrateCount mismatch at toggle #$i", i, vibrateCount)
        }

        // 100回トグル後は初期状態（false）に戻っている
        assertFalse(cursorSelectionMode)
        assertFalse(KeyboardDefaultLayouts.isCursorSelectionModeProvider?.invoke() == true)

        // 3. 101回目トグル -> ON
        KeyboardDefaultLayouts.cursorSelectionToggleHandler?.invoke()
        assertTrue(cursorSelectionMode)
        assertEquals(101, vibrateCount)

        // 4. 指離脱 -> OFF
        KeyboardDefaultLayouts.cursorSelectionEndHandler?.invoke()
        assertFalse(cursorSelectionMode)
        assertFalse(KeyboardDefaultLayouts.isCursorSelectionModeProvider?.invoke() == true)
        assertEquals(101, vibrateCount) // endHandlerではバイブレーションは呼ばれない
    }

    /**
     * 検証2: 指離脱競合・二重終了呼び出し時の選択範囲非破壊性の検証
     */
    @Test
    fun testEndSelectionIdempotencyAndSelectionPreservation() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val movement = CursorMovement(Mutex(), { _, a, b ->
            inputConnection.setSelection(a, b)
        }, { _, _ -> true }, dispatcher)

        // 範囲選択モードON
        cursorSelectionMode = true

        // 左に3文字移動（アンカー: 10, キャレット: 7）
        movement.move(inputConnection, CursorDirection.LEFT, cursorSelectionMode) { true }
        movement.move(inputConnection, CursorDirection.LEFT, cursorSelectionMode) { true }
        movement.move(inputConnection, CursorDirection.LEFT, cursorSelectionMode) { true }

        assertEquals(10, selectionStart)
        assertEquals(7, selectionEnd)
        assertEquals(listOf(10 to 9, 10 to 8, 10 to 7), selectionHistory)

        // 離脱ハンドラを連続して3回呼び出す（競合・重複イベントのシミュレーション）
        fun end() {
            cursorSelectionMode = false
            movement.reset()
        }

        end()
        end()
        end()

        // setSelection が余計に呼ばれていないこと（10 to 7 のまま維持）
        assertEquals("Selection start must remain 10 after multiple end calls", 10, selectionStart)
        assertEquals("Selection end must remain 7 after multiple end calls", 7, selectionEnd)
        assertEquals("No extra setSelection should have been dispatched by end calls", 3, selectionHistory.size)
    }

    /**
     * 検証3: 選択モードと通常モードを行き来する複雑な双方向移動時のアンカー維持
     */
    @Test
    fun testBidirectionalComplexMovementsWithToggles() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val movement = CursorMovement(Mutex(), { _, a, b ->
            inputConnection.setSelection(a, b)
        }, { _, _ -> true }, dispatcher)

        // 1. 通常移動で 10 -> 8 へ
        movement.move(inputConnection, CursorDirection.LEFT, false) { true }
        movement.move(inputConnection, CursorDirection.LEFT, false) { true }
        assertEquals(8 to 8, selectionHistory.last())

        // 2. 2本指タップで選択モード ON（アンカー: 8）
        cursorSelectionMode = true
        // 右に 4文字移動（8 -> 12）
        for (i in 1..4) {
            movement.move(inputConnection, CursorDirection.RIGHT, cursorSelectionMode) { true }
        }
        assertEquals(8 to 12, selectionHistory.last())

        // 3. 再度2本指タップで選択モード OFF（通常カーソル移動へ復帰、移動しない限り 8..12 を維持）
        cursorSelectionMode = false
        assertEquals(8, selectionStart)
        assertEquals(12, selectionEnd)

        // 4. 通常移動が実行された瞬間、方向側の端（12）へ安全にコラプス（選択解除）
        movement.move(inputConnection, CursorDirection.RIGHT, cursorSelectionMode) { true }
        assertEquals("First normal move collapses selection to 12..12", 12 to 12, selectionHistory.last())

        // 5. 続く通常移動で 13..13 へ移動
        movement.move(inputConnection, CursorDirection.RIGHT, cursorSelectionMode) { true }
        assertEquals("Subsequent normal move moves to 13..13", 13 to 13, selectionHistory.last())
    }
}
