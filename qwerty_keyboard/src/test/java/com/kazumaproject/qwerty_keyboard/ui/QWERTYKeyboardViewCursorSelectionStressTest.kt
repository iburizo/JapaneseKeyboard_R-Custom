package com.kazumaproject.qwerty_keyboard.ui

import android.os.Looper
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import com.kazumaproject.core.domain.listener.QWERTYKeyListener
import com.kazumaproject.core.domain.qwerty.QWERTYKey
import com.kazumaproject.qwerty_keyboard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

/**
 * 敵対的ストレステスト: QWERTYKeyboardView における要件R1のエッジケース・境界値検証。
 * - 高速連続タップ（20回）によるトグル整合性
 * - 主指先行離脱（ポインタ競合アップ）時のクラッシュ・二重終了防止・文字誤入力抑止（suppressedPointerIdsの検証）
 * - 通常タッチおよび独立マルチタッチ双方での耐久性
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class QWERTYKeyboardViewCursorSelectionStressTest {

    @Test
    fun testQwertyRapidSuccessiveTwoFingerTapsToggleCorrectly() {
        for (independent in listOf(false, true)) {
            val fixture = Fixture(independent)
            fixture.enterCursorModeWithSpace()
            assertEquals("Toggle handler should not fire on entry", 0, fixture.toggleCount)
            assertEquals("End handler should not fire on entry", 0, fixture.endCount)

            // Rapid 20 taps with 2nd finger on key_a
            val tapCount = 20
            for (i in 1..tapCount) {
                // POINTER_DOWN (index 1)
                fixture.send(MotionEvent.ACTION_POINTER_DOWN, 1, fixture.space, fixture.keyA)
                assertEquals("Toggle invocation count mismatch after tap #$i (independent=$independent)", i, fixture.toggleCount)
                assertEquals("End handler must not fire on toggle (independent=$independent)", 0, fixture.endCount)
                assertTrue("Cursor mode must remain active (independent=$independent)", fixture.cursorMode())

                // POINTER_UP (index 1)
                fixture.send(MotionEvent.ACTION_POINTER_UP, 1, fixture.space, fixture.keyA)
                assertEquals("End handler must not fire on 2nd finger release", 0, fixture.endCount)
                assertTrue("Cursor mode must remain active", fixture.cursorMode())
            }

            // Release primary finger (space)
            fixture.send(MotionEvent.ACTION_UP, 0, fixture.space)
            assertFalse("Cursor mode must terminate upon space release", fixture.cursorMode())
            assertEquals("End handler must fire exactly once upon space release", 1, fixture.endCount)
        }
    }

    @Test
    fun testQwertyPrimaryFingerLiftsBeforeSecondFingerPointerRace() {
        for (independent in listOf(false, true)) {
            val fixture = Fixture(independent)
            fixture.enterCursorModeWithSpace()

            // 1. Second finger touches down (toggle #1)
            fixture.send(MotionEvent.ACTION_POINTER_DOWN, 1, fixture.space, fixture.keyA)
            assertEquals(1, fixture.toggleCount)
            assertEquals(0, fixture.endCount)

            // 2. Race condition: Primary finger (space, index 0, id 19) lifts FIRST!
            // Notice: Event contains both pointers, but ACTION_POINTER_UP is for index 0 (space)
            val tapsBeforeRace = fixture.taps.toList()
            fixture.released.clear()

            fixture.send(MotionEvent.ACTION_POINTER_UP, 0, fixture.space, fixture.keyA)
            assertFalse("Cursor mode must terminate immediately when space lifts (independent=$independent)", fixture.cursorMode())
            assertEquals("End handler must fire exactly once on primary lift", 1, fixture.endCount)

            // 3. Second finger (keyA) moves and then lifts last via ACTION_UP
            fixture.send(MotionEvent.ACTION_MOVE, 0, fixture.keyA.copy(x = fixture.keyA.x + 20f))
            fixture.send(MotionEvent.ACTION_UP, 0, fixture.keyA)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))

            assertFalse("Cursor mode must remain false", fixture.cursorMode())
            assertEquals("End handler must NOT fire a second time", 1, fixture.endCount)

            // Residual finger must be suppressed, emitting NO characters or releases
            assertEquals("Residual finger must not emit text taps (independent=$independent)", tapsBeforeRace, fixture.taps)
            assertTrue(
                "Residual finger must not emit key releases (independent=$independent), released: ${fixture.released}",
                fixture.released.isEmpty()
            )
        }
    }

    private data class Pointer(val id: Int, val x: Float, val y: Float)

    private class Fixture(independent: Boolean) {
        val released = mutableListOf<QWERTYKey>()
        val taps = mutableListOf<Char>()
        private val held = mutableListOf<QWERTYKey>()
        private val downTime = SystemClock.uptimeMillis()

        var toggleCount = 0
        var endCount = 0

        val keyboard = QWERTYKeyboardView(
            ContextThemeWrapper(
                RuntimeEnvironment.getApplication(),
                com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
            )
        ).apply {
            setPopUpViewState(false)
            setIndependentMultiTouchEnabled(independent)
            setRomajiMode(false)
            setLongPressTimeout(200)
            cursorSelectionToggleHandler = { toggleCount++ }
            cursorSelectionEndHandler = { endCount++ }
            setOnQWERTYKeyListener(object : QWERTYKeyListener {
                override fun onPressedQWERTYKey(qwertyKey: QWERTYKey) = Unit
                override fun onReleasedQWERTYKey(qwertyKey: QWERTYKey, tap: Char?, variations: List<Char>?) {
                    released += qwertyKey
                    tap?.let { taps += it }
                }
                override fun onLongPressQWERTYKey(qwertyKey: QWERTYKey) {
                    held += qwertyKey
                    if (qwertyKey == QWERTYKey.QWERTYKeySpace) setCursorMode(true)
                }
                override fun onFlickUPQWERTYKey(qwertyKey: QWERTYKey, tap: Char?, variations: List<Char>?) = Unit
                override fun onFlickDownQWERTYKey(qwertyKey: QWERTYKey, character: Char) = Unit
            })
            measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, 1080, 500)
        }

        val space = center(R.id.key_space, 19)
        val keyA = center(R.id.key_a, 5)

        fun enterCursorModeWithSpace() {
            send(MotionEvent.ACTION_DOWN, 0, space)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
            assertTrue("Space long press should trigger", QWERTYKey.QWERTYKeySpace in held)
            assertTrue("Should enter cursor mode", cursorMode())
        }

        fun cursorMode() = field("isCursorMode") as Boolean

        private fun field(name: String) = keyboard.javaClass.getDeclaredField(name)
            .apply { isAccessible = true }.get(keyboard)

        private fun center(id: Int, pointerId: Int): Pointer {
            val key = keyboard.findViewById<View>(id)
            assertTrue("Key $id must exist and have positive size", key.width > 0 && key.height > 0)
            return Pointer(pointerId, key.left + key.width / 2f, key.top + key.height / 2f)
        }

        fun send(action: Int, index: Int, vararg pointers: Pointer) {
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
            val event = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                action or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
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
            try {
                keyboard.onTouchEvent(event)
            } finally {
                event.recycle()
            }
        }
    }
}
