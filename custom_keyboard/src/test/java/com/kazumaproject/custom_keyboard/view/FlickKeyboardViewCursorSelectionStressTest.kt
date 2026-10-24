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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlickKeyboardViewCursorSelectionStressTest {

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

    /**
     * 検証1: 高速で2本目指を20回連続タップした際、正しく20回トグルが反復されるかを検証。
     */
    @Test
    fun testRapidSuccessiveTwoFingerTapsToggleCorrectly() {
        for (independent in listOf(false, true)) {
            toggleInvocationCount = 0
            endInvocationCount = 0
            val keyboard = createKeyboard(independentMultiTouch = independent)
            val naButton = findNaButton(keyboard)
            assertNotNull(naButton)

            val centerX = naButton!!.left + naButton.width / 2f
            val centerY = naButton.top + naButton.height / 2f
            var time = SystemClock.uptimeMillis()
            val downTime = time
            val p0 = Pointer(0, centerX, centerY)
            val p1 = Pointer(1, centerX + 150f, centerY)

            val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, time, p0)
            keyboard.onTouchEvent(down)
            down.recycle()
            shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
            time += 250
            assertTrue(keyboard.isCursorMode())

            val tapCount = 20
            for (i in 1..tapCount) {
                time += 5
                val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, time, p0, p1)
                keyboard.onTouchEvent(pDown)
                pDown.recycle()

                assertEquals("Toggle invocation count mismatch after tap #$i (independent=$independent)", i, toggleInvocationCount)
                assertEquals(0, endInvocationCount)
                assertTrue(keyboard.isCursorMode())

                time += 5
                val pUp = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, downTime, time, p0, p1)
                keyboard.onTouchEvent(pUp)
                pUp.recycle()

                assertEquals(0, endInvocationCount)
                assertTrue(keyboard.isCursorMode())
            }

            time += 10
            val up = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, time, p0)
            keyboard.onTouchEvent(up)
            up.recycle()

            assertEquals("End handler must fire exactly once upon primary release", 1, endInvocationCount)
            assertFalse(keyboard.isCursorMode())
        }
    }

    /**
     * 検証2: 主指が離脱する瞬間に2本目指が触れていた場合の離脱順序（ポインタアップの競合）。
     * 主指(pointer 0)が先に ACTION_POINTER_UP で離脱し、その後に2本目指(pointer 1)が ACTION_UP で離脱する。
     */
    @Test
    fun testPrimaryFingerLiftsBeforeSecondFingerPointerRace() {
        for (independent in listOf(false, true)) {
            toggleInvocationCount = 0
            endInvocationCount = 0
            dispatchedActions.clear()
            val keyboard = createKeyboard(independentMultiTouch = independent)
            val naButton = findNaButton(keyboard)
            assertNotNull(naButton)

            val centerX = naButton!!.left + naButton.width / 2f
            val centerY = naButton.top + naButton.height / 2f
            var time = SystemClock.uptimeMillis()
            val downTime = time
            val p0 = Pointer(0, centerX, centerY)
            val p1 = Pointer(1, centerX + 150f, centerY)

            val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, time, p0)
            keyboard.onTouchEvent(down)
            down.recycle()
            shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
            time += 250
            assertTrue(keyboard.isCursorMode())

            time += 10
            val pDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, time, p0, p1)
            keyboard.onTouchEvent(pDown)
            pDown.recycle()
            assertEquals(1, toggleInvocationCount)
            assertEquals(0, endInvocationCount)

            // Primary lifts first via ACTION_POINTER_UP (actionIndex = 0)
            time += 10
            val primaryLifts = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 0, downTime, time, p0, p1)
            keyboard.onTouchEvent(primaryLifts)
            primaryLifts.recycle()

            assertFalse("Cursor mode must terminate when primary finger lifts (independent=$independent)", keyboard.isCursorMode())
            assertEquals("End handler must fire when primary finger lifts", 1, endInvocationCount)

            // Second finger lifts last via ACTION_UP
            time += 10
            val secondLifts = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, time, p1)
            keyboard.onTouchEvent(secondLifts)
            secondLifts.recycle()

            assertFalse("Cursor mode must stay false", keyboard.isCursorMode())
            assertEquals("End handler must NOT fire a second time", 1, endInvocationCount)
            assertFalse(
                "Residual second finger release must not emit any character input, dispatched: $dispatchedActions",
                dispatchedActions.any { it is KeyAction.Text || it is KeyAction.InputText }
            )
        }
    }

    /**
     * 検証3: 3本指マルチタッチ競合
     */
    @Test
    fun testThreeFingersConcurrentTaps() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull(naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f
        var time = SystemClock.uptimeMillis()
        val downTime = time
        val p0 = Pointer(0, centerX, centerY)
        val p1 = Pointer(1, centerX + 100f, centerY)
        val p2 = Pointer(2, centerX + 200f, centerY)

        val down = createMotionEvent(MotionEvent.ACTION_DOWN, 0, downTime, time, p0)
        keyboard.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)
        time += 250

        // Pointer 1 DOWN -> Toggle 1
        time += 10
        val p1Down = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, downTime, time, p0, p1)
        keyboard.onTouchEvent(p1Down)
        p1Down.recycle()
        assertEquals(1, toggleInvocationCount)

        // Pointer 2 DOWN -> Toggle 2
        time += 10
        val p2Down = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 2, downTime, time, p0, p1, p2)
        keyboard.onTouchEvent(p2Down)
        p2Down.recycle()
        assertEquals(2, toggleInvocationCount)

        // Pointer 1 UP
        time += 10
        val p1Up = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, downTime, time, p0, p1, p2)
        keyboard.onTouchEvent(p1Up)
        p1Up.recycle()
        assertTrue(keyboard.isCursorMode())
        assertEquals(0, endInvocationCount)

        // Pointer 2 UP
        time += 10
        val p2Up = createMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, downTime, time, p0, p2)
        keyboard.onTouchEvent(p2Up)
        p2Up.recycle()
        assertTrue(keyboard.isCursorMode())
        assertEquals(0, endInvocationCount)

        // Pointer 0 UP
        time += 10
        val p0Up = createMotionEvent(MotionEvent.ACTION_UP, 0, downTime, time, p0)
        keyboard.onTouchEvent(p0Up)
        p0Up.recycle()
        assertFalse(keyboard.isCursorMode())
        assertEquals(1, endInvocationCount)
    }
}
