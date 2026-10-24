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
 * 要件R1 (カーソルスワイプ中の2本指タップによる選択範囲モード切り替え・選択範囲操作) を検証するRobolectric単体テスト。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlickKeyboardViewCursorSelectionTest {

    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val host = FrameLayout(activity).also { activity.setContentView(it) }
    private val dispatchedActions = mutableListOf<KeyAction>()

    private var toggleInvocationCount = 0
    private var endInvocationCount = 0

    @Before
    fun setUp() {
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

    private fun createKeyboard(independentMultiTouch: Boolean = false): FlickKeyboardView {
        dispatchedActions.clear()
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

    @Test
    fun testTwoFingerTapInCursorModeTriggersToggleHandler() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull("な button must exist", naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val secondX = centerX + 150f
        val secondY = centerY

        val downTime = SystemClock.uptimeMillis()
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, secondX, secondY)

        // 1. Primary finger DOWN on 'な'
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // 2. Wait for long press (200ms) to enter cursor mode
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
        assertTrue("Keyboard must be in cursor mode after long press", keyboard.isCursorMode())
        assertEquals("Toggle handler should not have fired yet", 0, toggleInvocationCount)

        // 3. Second finger DOWN (ACTION_POINTER_DOWN)
        val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 260, p0, p1)
        keyboard.onTouchEvent(pDown)
        pDown.recycle()

        assertEquals("Toggle handler must fire exactly once on first 2-finger tap", 1, toggleInvocationCount)
        assertEquals("End handler must not have fired yet", 0, endInvocationCount)
        assertTrue("Cursor mode should still be active", keyboard.isCursorMode())

        // 4. Second finger UP (ACTION_POINTER_UP)
        val pUp = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, downTime, downTime + 300, p0, p1)
        keyboard.onTouchEvent(pUp)
        pUp.recycle()

        assertEquals("Toggle handler count should remain 1", 1, toggleInvocationCount)
        assertEquals("End handler must not fire on second pointer UP", 0, endInvocationCount)
        assertTrue("Cursor mode should still be active after second finger UP", keyboard.isCursorMode())

        // 5. Primary finger UP (ACTION_UP)
        val up = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, downTime + 350, p0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertEquals("End handler must fire on primary finger release", 1, endInvocationCount)
        assertFalse("Cursor mode must terminate after primary finger UP", keyboard.isCursorMode())
    }

    @Test
    fun testSecondTwoFingerTapRetogglesSelectionMode() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull(naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val secondX = centerX + 150f
        val secondY = centerY

        val downTime = SystemClock.uptimeMillis()
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, secondX, secondY)

        // 1. Enter cursor mode
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
        assertTrue(keyboard.isCursorMode())

        // 2. First tap with second finger -> Toggle 1
        val pDown1 = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 260, p0, p1)
        keyboard.onTouchEvent(pDown1)
        pDown1.recycle()
        assertEquals(1, toggleInvocationCount)

        val pUp1 = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, downTime, downTime + 280, p0, p1)
        keyboard.onTouchEvent(pUp1)
        pUp1.recycle()

        // 3. Second tap with second finger -> Toggle 2 (Re-toggle)
        val pDown2 = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 310, p0, p1)
        keyboard.onTouchEvent(pDown2)
        pDown2.recycle()
        assertEquals("Toggle handler must fire a second time on 2nd tap", 2, toggleInvocationCount)

        val pUp2 = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, downTime, downTime + 330, p0, p1)
        keyboard.onTouchEvent(pUp2)
        pUp2.recycle()

        // 4. Release primary finger
        val up = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, downTime + 360, p0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertEquals(1, endInvocationCount)
        assertFalse(keyboard.isCursorMode())
    }

    @Test
    fun testActionCancelTriggersCursorSelectionEndHandler() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull(naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val downTime = SystemClock.uptimeMillis()
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, centerX + 100f, centerY)

        // Enter cursor mode
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)

        // Toggle selection mode
        val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 260, p0, p1)
        keyboard.onTouchEvent(pDown)
        pDown.recycle()
        assertEquals(1, toggleInvocationCount)

        // Window loses focus or system aborts gesture with ACTION_CANCEL
        val cancel = createMotionEvent(MotionEvent.ACTION_CANCEL, 0, downTime, downTime + 300, p0)
        keyboard.onTouchEvent(cancel)
        cancel.recycle()

        assertEquals("End handler must fire on ACTION_CANCEL", 1, endInvocationCount)
        assertFalse("Cursor mode must terminate after ACTION_CANCEL", keyboard.isCursorMode())
    }

    @Test
    fun testPointerDownOutsideCursorModeDoesNotTriggerToggleHandler() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull(naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val downTime = SystemClock.uptimeMillis()
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, centerX + 150f, centerY)

        // Quick multi-touch tap (typing roll-over) BEFORE long-press timeout
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // Immediate 2nd finger down (e.g. 30ms later)
        val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 30, p0, p1)
        keyboard.onTouchEvent(pDown)
        pDown.recycle()

        assertFalse("Must NOT enter cursor mode on quick tap", keyboard.isCursorMode())
        assertEquals("Toggle handler must NOT fire outside cursor mode", 0, toggleInvocationCount)
        assertEquals("End handler must NOT fire", 0, endInvocationCount)
    }

    @Test
    fun testIndependentMultiTouchTwoFingerTapToggleAndEnd() {
        val keyboard = createKeyboard(independentMultiTouch = true)
        val naButton = findNaButton(keyboard)
        assertNotNull(naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val downTime = SystemClock.uptimeMillis()
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, centerX + 150f, centerY)

        // Enter cursor mode
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
        assertTrue(keyboard.isCursorMode())

        // 1st ACTION_POINTER_DOWN
        val pDown1 = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 260, p0, p1)
        keyboard.onTouchEvent(pDown1)
        pDown1.recycle()
        assertEquals(1, toggleInvocationCount)

        // 2nd ACTION_POINTER_DOWN
        val pDown2 = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 300, p0, p1)
        keyboard.onTouchEvent(pDown2)
        pDown2.recycle()
        assertEquals(2, toggleInvocationCount)

        // ACTION_UP
        val up = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, downTime + 350, p0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertEquals(1, endInvocationCount)
        assertFalse(keyboard.isCursorMode())
    }

    @Test
    fun testSwipeMoveWithSelectionModeToggledDispatchesCursorMove() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull(naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        val downTime = SystemClock.uptimeMillis()
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, centerX + 150f, centerY)

        // Enter cursor mode
        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, downTime, p0)
        keyboard.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)

        // Toggle selection mode
        val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, downTime + 260, p0, p1)
        keyboard.onTouchEvent(pDown)
        pDown.recycle()
        val pUp = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, downTime, downTime + 280, p0, p1)
        keyboard.onTouchEvent(pUp)
        pUp.recycle()

        // Move primary finger right by 100px
        val p0Moved = Pointer(0, centerX + 100f, centerY)
        val move = createMotionEvent(MotionEvent.ACTION_MOVE, 0, downTime, downTime + 320, p0Moved)
        keyboard.onTouchEvent(move)
        move.recycle()

        assertTrue(
            "Swiping right after selection toggle should dispatch MoveCursorRight, actual: $dispatchedActions",
            dispatchedActions.contains(KeyAction.MoveCursorRight)
        )

        // Release primary finger
        val up = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, downTime + 360, p0Moved)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertEquals(1, endInvocationCount)
        assertFalse(
            "Releasing after cursor selection swipe must NOT emit 'な' character",
            dispatchedActions.any { (it is KeyAction.Text && it.text == "な") || (it is KeyAction.InputText && it.text == "な") }
        )
    }
}
