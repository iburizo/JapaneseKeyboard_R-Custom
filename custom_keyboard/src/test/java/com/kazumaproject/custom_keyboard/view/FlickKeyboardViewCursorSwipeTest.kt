package com.kazumaproject.custom_keyboard.view

import android.app.Activity
import android.os.Looper
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.kazumaproject.custom_keyboard.data.FlickDirection
import com.kazumaproject.custom_keyboard.data.KeyAction
import com.kazumaproject.custom_keyboard.data.KeyboardInputMode
import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlickKeyboardViewCursorSwipeTest {

    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val host = FrameLayout(activity).also { activity.setContentView(it) }
    private val dispatchedActions = mutableListOf<KeyAction>()

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

    @Test
    fun testTapOnNaKeyInputsNa() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull("な button must exist", naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // Immediate release (tap)
        val upTime = downTime + 50
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertTrue(
            "Tapping 'な' key should emit 'な' character, actual: $dispatchedActions",
            dispatchedActions.any { (it is KeyAction.Text && it.text == "な") || (it is KeyAction.InputText && it.text == "な") }
        )
    }

    @Test
    fun testLongPressOnNaKeyEntersCursorModeAndSwipesWithoutEmittingNa() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull("な button must exist", naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // Wait for long press timeout (200ms set in keyboard)
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)

        // Move right by 100px (sufficient for cursor step)
        val moveTime = downTime + 300
        val move = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, centerX + 100f, centerY, 0)
        keyboard.onTouchEvent(move)
        move.recycle()

        assertTrue(
            "Swiping right after long press should dispatch MoveCursorRight, actual: $dispatchedActions",
            dispatchedActions.contains(KeyAction.MoveCursorRight)
        )

        // Release finger
        val upTime = downTime + 400
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX + 100f, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertFalse(
            "Releasing after cursor swipe must NOT emit 'な' text, actual: $dispatchedActions",
            dispatchedActions.any { (it is KeyAction.Text && it.text == "な") || (it is KeyAction.InputText && it.text == "な") }
        )
    }

    @Test
    fun testIndependentMultiTouchLongPressOnNaKeyEntersCursorMode() {
        val keyboard = createKeyboard(independentMultiTouch = true)
        val naButton = findNaButton(keyboard)
        assertNotNull("な button must exist", naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // Wait for long press timeout
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)

        // Move left by 100px
        val moveTime = downTime + 300
        val move = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, centerX - 100f, centerY, 0)
        keyboard.onTouchEvent(move)
        move.recycle()

        assertTrue(
            "Swiping left in independent multitouch after long press should dispatch MoveCursorLeft, actual: $dispatchedActions",
            dispatchedActions.contains(KeyAction.MoveCursorLeft)
        )

        // Release
        val upTime = downTime + 400
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX - 100f, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertFalse(
            "Releasing after cursor swipe in independent multitouch must NOT emit 'な' text, actual: $dispatchedActions",
            dispatchedActions.any { (it is KeyAction.Text && it.text == "な") || (it is KeyAction.InputText && it.text == "な") }
        )
    }

    @Test
    fun testFlickOnNaKeyInputsNiAndCancelsLongPress() {
        val keyboard = createKeyboard()
        val naButton = findNaButton(keyboard)
        assertNotNull("な button must exist", naButton)

        val centerX = naButton!!.left + naButton.width / 2f
        val centerY = naButton.top + naButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // Flick to left quickly (50ms) before long press timeout (200ms)
        val moveTime = downTime + 50
        val move = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, centerX - 100f, centerY, 0)
        keyboard.onTouchEvent(move)
        move.recycle()

        // Release finger
        val upTime = downTime + 100
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX - 100f, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        // Wait to make sure long press would have expired
        shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS)

        assertTrue(
            "Flicking left on 'な' key should emit 'に', actual: $dispatchedActions",
            dispatchedActions.any { (it is KeyAction.Text && it.text == "に") || (it is KeyAction.InputText && it.text == "に") }
        )
        assertFalse(
            "Quick flick should not enter cursor mode",
            dispatchedActions.contains(KeyAction.MoveCursorLeft)
        )
    }

    @Test
    fun testTapOnArrowKeyDispatchesCursorMove() {
        val keyboard = createKeyboard()
        val cursorButton = keyboard.findCursorButton()
        assertNotNull("Cursor key button must exist", cursorButton)

        val centerX = cursorButton!!.left + cursorButton.width / 2f
        val centerY = cursorButton.top + cursorButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        val upTime = downTime + 50
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertTrue(
            "Tapping arrow cursor key should dispatch MoveCursorRight or MoveCursorLeft, actual: $dispatchedActions",
            dispatchedActions.contains(KeyAction.MoveCursorRight) || dispatchedActions.contains(KeyAction.MoveCursorLeft)
        )
    }

    @Test
    fun testSwipeOnArrowKeyDispatchesCursorMoveAndNoExtraTap() {
        val keyboard = createKeyboard()
        val cursorButton = keyboard.findCursorButton()
        assertNotNull("Cursor key button must exist", cursorButton)

        val centerX = cursorButton!!.left + cursorButton.width / 2f
        val centerY = cursorButton.top + cursorButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // Move right by 100px
        val moveTime = downTime + 50
        val move = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, centerX + 100f, centerY, 0)
        keyboard.onTouchEvent(move)
        move.recycle()

        val actionsAfterMove = dispatchedActions.toList()
        assertTrue(
            "Swiping right on arrow key should dispatch MoveCursorRight, actual: $actionsAfterMove",
            actionsAfterMove.contains(KeyAction.MoveCursorRight)
        )
        val moveCount = actionsAfterMove.count { it == KeyAction.MoveCursorRight }

        // Release finger
        val upTime = downTime + 100
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX + 100f, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        // After UP, there should NOT be an additional MoveCursorRight from tap
        val totalMoveCount = dispatchedActions.count { it == KeyAction.MoveCursorRight }
        assertEquals(
            "Releasing after cursor swipe should not emit an extra tap cursor move, actions: $dispatchedActions",
            moveCount,
            totalMoveCount
        )
    }


    @Test
    fun testIndependentMultiTouchTapOnArrowKeyDispatchesCursorMove() {
        val keyboard = createKeyboard(independentMultiTouch = true)
        val cursorButton = keyboard.findCursorButton()
        assertNotNull("Cursor key button must exist", cursorButton)

        val centerX = cursorButton!!.left + cursorButton.width / 2f
        val centerY = cursorButton.top + cursorButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        val upTime = downTime + 50
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        assertTrue(
            "Tapping arrow cursor key in independent multitouch should dispatch MoveCursorRight or MoveCursorLeft, actual: $dispatchedActions",
            dispatchedActions.contains(KeyAction.MoveCursorRight) || dispatchedActions.contains(KeyAction.MoveCursorLeft)
        )
    }

    @Test
    fun testIndependentMultiTouchSwipeOnArrowKeyDispatchesCursorMoveAndNoExtraTap() {
        val keyboard = createKeyboard(independentMultiTouch = true)
        val cursorButton = keyboard.findCursorButton()
        assertNotNull("Cursor key button must exist", cursorButton)

        val centerX = cursorButton!!.left + cursorButton.width / 2f
        val centerY = cursorButton.top + cursorButton.height / 2f

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        keyboard.onTouchEvent(down)
        down.recycle()

        // Move right by 100px
        val moveTime = downTime + 50
        val move = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, centerX + 100f, centerY, 0)
        keyboard.onTouchEvent(move)
        move.recycle()

        val actionsAfterMove = dispatchedActions.toList()
        assertTrue(
            "Swiping right on arrow key in independent multitouch should dispatch MoveCursorRight, actual: $actionsAfterMove",
            actionsAfterMove.contains(KeyAction.MoveCursorRight)
        )
        val moveCount = actionsAfterMove.count { it == KeyAction.MoveCursorRight }

        // Release finger
        val upTime = downTime + 100
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, centerX + 100f, centerY, 0)
        keyboard.onTouchEvent(up)
        up.recycle()

        // After UP, there should NOT be an additional MoveCursorRight from tap
        val totalMoveCount = dispatchedActions.count { it == KeyAction.MoveCursorRight }
        assertEquals(
            "Releasing after cursor swipe in independent multitouch should not emit an extra tap cursor move, actions: $dispatchedActions",
            moveCount,
            totalMoveCount
        )
    }

    private fun FlickKeyboardView.findCursorButton(): View? {
        val field = FlickKeyboardView::class.java.getDeclaredField("dynamicKeyMap").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = field.get(this) as Map<*, *>
        for ((_, info) in map) {
            val keyDataField = info!!.javaClass.getDeclaredField("keyData").apply { isAccessible = true }
            val keyData = keyDataField.get(info) as? com.kazumaproject.custom_keyboard.data.KeyData
            val action = keyData?.action
            if (action == KeyAction.MoveCursorLeft || action == KeyAction.MoveCursorRight) {
                val viewField = info.javaClass.getDeclaredField("view").apply { isAccessible = true }
                return viewField.get(info) as? View
            }
        }
        val keyInfosField = FlickKeyboardView::class.java.getDeclaredField("keyInfos").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val infos = keyInfosField.get(this) as List<*>
        for (info in infos) {
            val keyDataField = info!!.javaClass.getDeclaredField("keyData").apply { isAccessible = true }
            val keyData = keyDataField.get(info) as? com.kazumaproject.custom_keyboard.data.KeyData
            val action = keyData?.action
            if (action == KeyAction.MoveCursorLeft || action == KeyAction.MoveCursorRight) {
                val viewField = info.javaClass.getDeclaredField("view").apply { isAccessible = true }
                return viewField.get(info) as? View
            }
        }
        return null
    }
}


