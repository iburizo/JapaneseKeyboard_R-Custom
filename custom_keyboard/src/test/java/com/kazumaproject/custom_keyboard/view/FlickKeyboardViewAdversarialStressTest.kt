package com.kazumaproject.custom_keyboard.view

import android.app.Activity
import android.os.Looper
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.kazumaproject.custom_keyboard.data.FlickDirection
import com.kazumaproject.custom_keyboard.data.KeyAction
import com.kazumaproject.custom_keyboard.data.KeyboardInputMode
import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * 敵対的ストレステスター (m1_challenger_2) による R1 要件の検証:
 * 1. 通常の高速タイピング中に2本指がほぼ同時に触れた場合、絶対に選択トグルが誤爆しないか (通常 & 独立マルチタッチ)
 * 2. ACTION_CANCEL イベントが飛んできた場合にクリーンに終了し、状態のリークがないか (通常 & 独立マルチタッチ)
 * 3. 「な」キー長押し判定前のマルチタッチ接触によるカーソル突入キャンセル
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlickKeyboardViewAdversarialStressTest {

    private lateinit var activity: Activity
    private lateinit var host: FrameLayout
    private val dispatchedActions = mutableListOf<KeyAction>()

    private var toggleInvocationCount = 0
    private var endInvocationCount = 0

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        host = FrameLayout(activity).also { activity.setContentView(it) }
        dispatchedActions.clear()
        toggleInvocationCount = 0
        endInvocationCount = 0
        KeyboardDefaultLayouts.cursorSelectionToggleHandler = {
            toggleInvocationCount++
        }
        KeyboardDefaultLayouts.cursorSelectionEndHandler = {
            endInvocationCount++
        }
    }

    @After
    fun tearDown() {
        KeyboardDefaultLayouts.cursorSelectionToggleHandler = null
        KeyboardDefaultLayouts.cursorSelectionEndHandler = null
        KeyboardDefaultLayouts.isCursorSelectionModeProvider = null
    }

    private fun createKeyboard(independentMultiTouch: Boolean): FlickKeyboardView {
        val keyboard = FlickKeyboardView(
            ContextThemeWrapper(
                activity,
                com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
            )
        ).apply {
            setLongPressTimeout(200)
            setIndependentMultiTouchEnabled(independentMultiTouch)
            setOnKeyboardActionListener(object : FlickKeyboardView.OnKeyboardActionListener {
                override fun onPress(action: KeyAction) = Unit
                override fun onAction(action: KeyAction, isFlick: Boolean) {
                    dispatchedActions += action
                }
                override fun onActionLongPress(action: KeyAction) = Unit
                override fun onActionUpAfterLongPress(action: KeyAction) = Unit
                override fun onFlickDirectionChanged(direction: FlickDirection) = Unit
                override fun onFlickActionLongPress(action: KeyAction) = Unit
                override fun onFlickActionUpAfterLongPress(action: KeyAction, isFlick: Boolean) = Unit
            })
        }
        host.removeAllViews()
        host.addView(keyboard)

        val layout = KeyboardDefaultLayouts.createFinalLayout(
            KeyboardInputMode.HIRAGANA,
            emptyMap(),
            "flick",
            "default"
        )
        keyboard.setKeyboard(layout)
        keyboard.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        )
        keyboard.layout(0, 0, 1000, 800)
        return keyboard
    }

    private fun findNaButton(keyboard: FlickKeyboardView): AutoSizeButton? {
        for (i in 0 until keyboard.childCount) {
            val child = keyboard.getChildAt(i)
            if (child is AutoSizeButton && child.text.toString() == "な") {
                return child
            }
        }
        return null
    }

    private fun FlickKeyboardView.isCursorMode(): Boolean {
        val field = FlickKeyboardView::class.java.getDeclaredField("isCursorMode").apply { isAccessible = true }
        return field.getBoolean(this)
    }

    private data class Pointer(val id: Int, val x: Float, val y: Float)

    private fun createMotionEvent(
        action: Int,
        actionIndex: Int,
        downTime: Long,
        eventTime: Long,
        vararg pointers: Pointer
    ): MotionEvent {
        val properties = Array(pointers.size) { n ->
            MotionEvent.PointerProperties().apply {
                id = pointers[n].id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coordinates = Array(pointers.size) { n ->
            MotionEvent.PointerCoords().apply {
                x = pointers[n].x
                y = pointers[n].y
                pressure = 1f
                size = 1f
            }
        }
        val maskedAction = action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        return MotionEvent.obtain(
            downTime,
            eventTime,
            maskedAction,
            pointers.size,
            properties,
            coordinates,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )
    }

    // =========================================================================
    // 1. 通常の高速タイピング中に2本指がほぼ同時に触れた場合の誤爆検証
    // =========================================================================

    @Test
    fun testHighSpeedTypingRollover_LegacyMode_NeverTriggersToggleOrCursor() {
        val keyboard = createKeyboard(independentMultiTouch = false)
        val naButton = findNaButton(keyboard)
        assertNotNull("な button must exist", naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val pA = Pointer(0, centerX, centerY)
        val pB = Pointer(1, centerX + 150f, centerY)

        var time = SystemClock.uptimeMillis()

        for (i in 0 until 5) {
            val downTime = time
            // 1. 指A DOWN
            val downA = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, time, pA)
            keyboard.onTouchEvent(downA)
            downA.recycle()

            // 2. 指B DOWN (20ms後, ACTION_POINTER_DOWN)
            time += 20
            val downB = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, time, pA, pB)
            keyboard.onTouchEvent(downB)
            downB.recycle()

            // 3. 指A UP (20ms後, ACTION_POINTER_UP)
            time += 20
            val upA = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 0, downTime, time, pA, pB)
            keyboard.onTouchEvent(upA)
            upA.recycle()

            // 4. 指B UP (20ms後, ACTION_UP)
            time += 20
            val upB = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, time, pB)
            keyboard.onTouchEvent(upB)
            upB.recycle()

            assertFalse("Cursor mode must NEVER be active during fast typing (legacy, iter=$i)", keyboard.isCursorMode())
            assertEquals("Toggle handler must NEVER fire during fast typing (legacy, iter=$i)", 0, toggleInvocationCount)
            assertEquals("End handler must NEVER fire during fast typing (legacy, iter=$i)", 0, endInvocationCount)
            time += 50
        }
    }

    @Test
    fun testHighSpeedTypingRollover_IndependentMode_NeverTriggersToggleOrCursor() {
        val keyboard = createKeyboard(independentMultiTouch = true)
        val naButton = findNaButton(keyboard)
        assertNotNull("な button must exist", naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val pA = Pointer(0, centerX, centerY)
        val pB = Pointer(1, centerX + 150f, centerY)

        var time = SystemClock.uptimeMillis()

        for (i in 0 until 5) {
            val downTime = time
            // 1. 指A DOWN
            val downA = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, time, pA)
            keyboard.onTouchEvent(downA)
            downA.recycle()

            // 2. 指B DOWN (20ms後, ACTION_POINTER_DOWN)
            time += 20
            val downB = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, time, pA, pB)
            keyboard.onTouchEvent(downB)
            downB.recycle()

            // 3. 指A UP (20ms後, ACTION_POINTER_UP)
            time += 20
            val upA = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 0, downTime, time, pA, pB)
            keyboard.onTouchEvent(upA)
            upA.recycle()

            // 4. 指B UP (20ms後, ACTION_UP)
            time += 20
            val upB = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, time, pB)
            keyboard.onTouchEvent(upB)
            upB.recycle()

            assertFalse("Cursor mode must NEVER be active during fast typing (independent, iter=$i)", keyboard.isCursorMode())
            assertEquals("Toggle handler must NEVER fire during fast typing (independent, iter=$i)", 0, toggleInvocationCount)
            assertEquals("End handler must NEVER fire during fast typing (independent, iter=$i)", 0, endInvocationCount)
            time += 50
        }
    }

    @Test
    fun testNaKeyFastTypingBeforeTimeoutCancelsCursorModeInLegacy() {
        val keyboard = createKeyboard(independentMultiTouch = false)
        val naButton = findNaButton(keyboard)
        assertNotNull(naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val pNa = Pointer(0, centerX, centerY)
        val pTa = Pointer(1, centerX + 150f, centerY)

        val downTime = SystemClock.uptimeMillis()

        // 1. 「な」キーを押下 (通常モード)
        val downNa = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, pNa)
        keyboard.onTouchEvent(downNa)
        downNa.recycle()

        // 2. タイムアウト前 (50ms後) に第2指接触 -> cancelPendingCursorSwipe() が発動
        val downTa = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 50, pNa, pTa)
        keyboard.onTouchEvent(downTa)
        downTa.recycle()

        // 3. 300ms 経過 (本来の200msタイムアウトを超過)
        shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)

        // 通常モードでは第2指接触によりカーソル待機がキャンセルされるためカーソルモードに入らない
        assertFalse("Cursor mode must NOT activate in legacy mode when cancelled by second pointer", keyboard.isCursorMode())
        assertEquals("Toggle handler must not fire", 0, toggleInvocationCount)

        // 指を離す
        val upNa = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 0, downTime, downTime + 360, pNa, pTa)
        keyboard.onTouchEvent(upNa)
        upNa.recycle()

        val upTa = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, downTime + 380, pTa)
        keyboard.onTouchEvent(upTa)
        upTa.recycle()

        assertFalse("Cursor mode remains false", keyboard.isCursorMode())
        assertEquals("End handler must not fire", 0, endInvocationCount)
    }

    // =========================================================================
    // 2. ACTION_CANCEL イベントによるクリーンアップと状態リーク検証
    // =========================================================================

    @Test
    fun testActionCancelInLegacyCursorModeCleansUpStateWithoutLeak() {
        val keyboard = createKeyboard(independentMultiTouch = false)
        val naButton = findNaButton(keyboard)!!
        val centerX = naButton.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, centerX + 120f, centerY)

        val downTime = SystemClock.uptimeMillis()

        // 1. 「な」長押しでカーソルモードへ突入
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
        assertTrue("Must enter cursor mode (legacy)", keyboard.isCursorMode())

        // 2. 第2指タップで範囲選択モードをトグル (ON)
        val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 260, p0, p1)
        keyboard.onTouchEvent(pDown)
        pDown.recycle()
        assertEquals("Toggle count must be 1 (legacy)", 1, toggleInvocationCount)

        // 3. アプリ中断・着信・通知展開等による ACTION_CANCEL の発生
        val cancel = createMotionEvent(MotionEvent.ACTION_CANCEL, 0, downTime, downTime + 300, p0, p1)
        keyboard.onTouchEvent(cancel)
        cancel.recycle()

        // 検証: カーソルモードは false、endHandler は正確に1回呼ばれる
        assertFalse("Cursor mode must be cleaned up on ACTION_CANCEL (legacy)", keyboard.isCursorMode())
        assertEquals("End handler must fire exactly once on ACTION_CANCEL (legacy)", 1, endInvocationCount)

        // 4. ACTION_CANCEL 後に再度通常のタップを行った場合、正常に動作しカーソルモードがリークしないこと
        val tapTime = downTime + 500
        val newTapDown = createMotionEvent(MotionEvent.ACTION_DOWN, 0, tapTime, tapTime, p0)
        keyboard.onTouchEvent(newTapDown)
        newTapDown.recycle()

        assertFalse("Cursor mode must NOT leak into subsequent touch events (legacy)", keyboard.isCursorMode())

        val newTapUp = createMotionEvent(MotionEvent.ACTION_UP, 0, tapTime, tapTime + 50, p0)
        keyboard.onTouchEvent(newTapUp)
        newTapUp.recycle()

        assertEquals("Toggle count must remain 1 (legacy)", 1, toggleInvocationCount)
        assertEquals("End handler count must remain 1 (legacy)", 1, endInvocationCount)
    }

    @Test
    fun testActionCancelInIndependentCursorModeCleansUpStateWithoutLeak() {
        val keyboard = createKeyboard(independentMultiTouch = true)
        val naButton = findNaButton(keyboard)!!
        val centerX = naButton.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, centerX + 120f, centerY)

        val downTime = SystemClock.uptimeMillis()

        // 1. 「な」長押しでカーソルモードへ突入
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
        assertTrue("Must enter cursor mode (independent)", keyboard.isCursorMode())

        // 2. 第2指タップで範囲選択モードをトグル (ON)
        val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 260, p0, p1)
        keyboard.onTouchEvent(pDown)
        pDown.recycle()
        assertEquals("Toggle count must be 1 (independent)", 1, toggleInvocationCount)

        // 3. アプリ中断・着信・通知展開等による ACTION_CANCEL の発生
        val cancel = createMotionEvent(MotionEvent.ACTION_CANCEL, 0, downTime, downTime + 300, p0, p1)
        keyboard.onTouchEvent(cancel)
        cancel.recycle()

        // 検証: カーソルモードは false、endHandler は正確に1回呼ばれる
        assertFalse("Cursor mode must be cleaned up on ACTION_CANCEL (independent)", keyboard.isCursorMode())
        assertEquals("End handler must fire exactly once on ACTION_CANCEL (independent)", 1, endInvocationCount)

        // 4. ACTION_CANCEL 後に再度通常のタップを行った場合、正常に動作しカーソルモードがリークしないこと
        val tapTime = downTime + 500
        val newTapDown = createMotionEvent(MotionEvent.ACTION_DOWN, 0, tapTime, tapTime, p0)
        keyboard.onTouchEvent(newTapDown)
        newTapDown.recycle()

        assertFalse("Cursor mode must NOT leak into subsequent touch events (independent)", keyboard.isCursorMode())

        val newTapUp = createMotionEvent(MotionEvent.ACTION_UP, 0, tapTime, tapTime + 50, p0)
        keyboard.onTouchEvent(newTapUp)
        newTapUp.recycle()

        assertEquals("Toggle count must remain 1 (independent)", 1, toggleInvocationCount)
        assertEquals("End handler count must remain 1 (independent)", 1, endInvocationCount)
    }
}
