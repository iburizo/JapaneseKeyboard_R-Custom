package com.kazumaproject.markdownhelperkeyboard.ime_service.gesture

import android.view.MotionEvent
import com.kazumaproject.custom_keyboard.data.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MultiTouchGestureDetectorTest {

    private lateinit var detector: MultiTouchGestureDetector
    private var lastRecognizedType: MultiTouchGestureType? = null
    private var lastRecognizedAction: KeyAction? = null

    @Before
    fun setUp() {
        detector = MultiTouchGestureDetector(density = 1.0f)
        detector.listener = OnMultiTouchGestureListener { type, action ->
            lastRecognizedType = type
            lastRecognizedAction = action
        }
        lastRecognizedType = null
        lastRecognizedAction = null
    }

    private fun configureAllGestures() {
        detector.settings = MultiTouchGestureSettings(
            swipeLeftAction = KeyAction.UndoCommit,
            swipeRightAction = KeyAction.SelectAll,
            swipeUpAction = KeyAction.Undo,
            swipeDownAction = KeyAction.Redo,
            pinchInAction = KeyAction.Copy,
            pinchOutAction = KeyAction.Paste
        )
    }

    private fun createMotionEvent(
        action: Int,
        downTime: Long,
        eventTime: Long,
        p0: Pair<Float, Float>,
        p1: Pair<Float, Float>? = null,
        p2: Pair<Float, Float>? = null,
        actionIndex: Int = 0
    ): MotionEvent {
        val count = when {
            p2 != null -> 3
            p1 != null -> 2
            else -> 1
        }
        val props = Array(count) { i ->
            MotionEvent.PointerProperties().apply {
                id = i
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coords = Array(count) { i ->
            val p = when (i) {
                0 -> p0
                1 -> p1!!
                else -> p2!!
            }
            MotionEvent.PointerCoords().apply {
                x = p.first
                y = p.second
                pressure = 1.0f
                size = 1.0f
            }
        }
        val finalAction = if (count > 1 && (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)) {
            action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        } else {
            action
        }
        return MotionEvent.obtain(
            downTime,
            eventTime,
            finalAction,
            count,
            props,
            coords,
            0,
            0,
            1.0f,
            1.0f,
            0,
            0,
            0,
            0
        )
    }

    @Test
    fun testFastBypassWhenAllGesturesNone() {
        // Default settings: all DoNothing
        detector.settings = MultiTouchGestureSettings()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse("Should return false on DOWN when bypass active", detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse("Should return false on POINTER_DOWN when bypass active", detector.onTouchEvent(evPointerDown))

        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 30f to 200f, 130f to 200f)
        assertFalse("Should return false on MOVE when bypass active", detector.onTouchEvent(evMove))

        assertEquals("No gesture should be recognized", null, lastRecognizedType)
    }

    @Test
    fun testSuppressionDuringCursorMode() {
        configureAllGestures()
        detector.isCursorModeProvider = { true }

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Large 2-finger swipe to the left
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 30f to 200f, 130f to 200f)
        assertFalse("Must suppress gesture during cursor mode", detector.onTouchEvent(evMove))

        assertEquals("No gesture should be recognized during cursor mode", null, lastRecognizedType)
    }

    @Test
    fun testSwipeLeftRecognition() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 150f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 150f to 200f, 250f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Both fingers move left by 70px (dx = -70, dy = 0)
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 80f to 200f, 180f to 200f)
        val recognized = detector.onTouchEvent(evMove)

        assertTrue("Swipe left should be recognized", recognized)
        assertEquals(MultiTouchGestureType.SWIPE_LEFT, lastRecognizedType)
        assertEquals(KeyAction.UndoCommit, lastRecognizedAction)
    }

    @Test
    fun testSwipeRightRecognition() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Both fingers move right by 70px (dx = +70, dy = 0)
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 170f to 200f, 270f to 200f)
        val recognized = detector.onTouchEvent(evMove)

        assertTrue("Swipe right should be recognized", recognized)
        assertEquals(MultiTouchGestureType.SWIPE_RIGHT, lastRecognizedType)
        assertEquals(KeyAction.SelectAll, lastRecognizedAction)
    }

    @Test
    fun testSwipeUpRecognition() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 250f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 250f, 200f to 250f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Both fingers move up by 70px (dx = 0, dy = -70)
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 100f to 180f, 200f to 180f)
        val recognized = detector.onTouchEvent(evMove)

        assertTrue("Swipe up should be recognized", recognized)
        assertEquals(MultiTouchGestureType.SWIPE_UP, lastRecognizedType)
        assertEquals(KeyAction.Undo, lastRecognizedAction)
    }

    @Test
    fun testSwipeDownRecognition() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 150f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 150f, 200f to 150f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Both fingers move down by 70px (dx = 0, dy = +70)
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 100f to 220f, 200f to 220f)
        val recognized = detector.onTouchEvent(evMove)

        assertTrue("Swipe down should be recognized", recognized)
        assertEquals(MultiTouchGestureType.SWIPE_DOWN, lastRecognizedType)
        assertEquals(KeyAction.Redo, lastRecognizedAction)
    }

    @Test
    fun testPinchInRecognition() {
        configureAllGestures()

        val downTime = 1000L
        // Initial span = 160px (x0=100, x1=260)
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 260f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Move towards each other: span decreases from 160px to 80px (delta = -80px <= -35px, ratio = 0.5 <= 0.75)
        // Centroid stays at 180f
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 140f to 200f, 220f to 200f)
        val recognized = detector.onTouchEvent(evMove)

        assertTrue("Pinch in should be recognized", recognized)
        assertEquals(MultiTouchGestureType.PINCH_IN, lastRecognizedType)
        assertEquals(KeyAction.Copy, lastRecognizedAction)
    }

    @Test
    fun testPinchOutRecognition() {
        configureAllGestures()

        val downTime = 1000L
        // Initial span = 60px (x0=150, x1=210)
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 150f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 150f to 200f, 210f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Move away from each other: span increases from 60px to 130px (delta = +70px >= 35px, ratio = 2.16 >= 1.30)
        // Centroid stays at 180f
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 115f to 200f, 245f to 200f)
        val recognized = detector.onTouchEvent(evMove)

        assertTrue("Pinch out should be recognized", recognized)
        assertEquals(MultiTouchGestureType.PINCH_OUT, lastRecognizedType)
        assertEquals(KeyAction.Paste, lastRecognizedAction)
    }

    @Test
    fun testRejectSingleFingerFlick() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        // Single finger moves far left
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1100L, 20f to 200f)
        assertFalse("Single finger move should not trigger gesture", detector.onTouchEvent(evMove))

        val evUp = createMotionEvent(MotionEvent.ACTION_UP, downTime, 1150L, 20f to 200f)
        assertFalse(detector.onTouchEvent(evUp))

        assertEquals("No gesture should be recognized for single finger", null, lastRecognizedType)
    }

    @Test
    fun testRejectAsymmetricFlick() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Finger 0 moves far left (-80px), but Finger 1 barely moves (-5px)
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 20f to 200f, 195f to 200f)
        assertFalse("Asymmetric movement should be rejected", detector.onTouchEvent(evMove))

        assertEquals("No gesture should be recognized for asymmetric movement", null, lastRecognizedType)
    }

    @Test
    fun testRejectOppositeDirections() {
        // Configure swipe gestures only so pinch gestures do not mask swipe vector validation
        detector.settings = MultiTouchGestureSettings(
            swipeLeftAction = KeyAction.UndoCommit,
            swipeRightAction = KeyAction.SelectAll,
            swipeUpAction = KeyAction.Undo,
            swipeDownAction = KeyAction.Redo,
            pinchInAction = KeyAction.DoNothing,
            pinchOutAction = KeyAction.DoNothing
        )

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Finger 0 moves left (-60px), Finger 1 moves right (+60px)
        // Vector cosine will be negative (-1.0), so swipe must be rejected
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 40f to 200f, 260f to 200f)
        assertFalse("Opposite direction swipe should be rejected", detector.onTouchEvent(evMove))

        assertEquals("No swipe should be recognized when directions oppose", null, lastRecognizedType)
    }

    @Test
    fun testRejectDelayedSecondFinger() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        // Second finger arrives 350ms after first finger (> 300ms window)
        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1350L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Both move together
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1500L, 30f to 200f, 130f to 200f)
        assertFalse("Late second pointer should discard gesture", detector.onTouchEvent(evMove))

        assertEquals("No gesture should be recognized if second finger delayed", null, lastRecognizedType)
    }

    @Test
    fun testRejectGestureDurationTimeout() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        // Move occurs at 1900ms (> 800ms after gesture start at 1050ms)
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1900L, 30f to 200f, 130f to 200f)
        assertFalse("Move after 800ms timeout should be discarded", detector.onTouchEvent(evMove))

        assertEquals("No gesture should be recognized after timeout", null, lastRecognizedType)
    }

    @Test
    fun testRejectThreePointers() {
        configureAllGestures()

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 200f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown2 = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 200f, 200f to 200f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown2))

        // Third finger touches down
        val evPointerDown3 = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1100L,
            100f to 200f,
            200f to 200f,
            300f to 200f,
            actionIndex = 2
        )
        assertFalse(detector.onTouchEvent(evPointerDown3))

        // Move after 3 pointers
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1200L,
            30f to 200f,
            130f to 200f,
            230f to 200f
        )
        assertFalse("3 pointers should discard gesture", detector.onTouchEvent(evMove))

        assertEquals("No gesture should be recognized with 3 pointers", null, lastRecognizedType)
    }

    @Test
    fun testTouchOutsideValidAreaDiscarded() {
        configureAllGestures()
        // Valid area is only y >= 100 (e.g. keyboard surface, below suggestion bar)
        detector.isTouchInsideValidArea = { _, y -> y >= 100f }

        val downTime = 1000L
        // Touch starts in suggestion bar area (y = 50f)
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, 100f to 50f)
        assertFalse(detector.onTouchEvent(evDown))

        val evPointerDown = createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, downTime, 1050L, 100f to 50f, 200f to 50f, actionIndex = 1)
        assertFalse(detector.onTouchEvent(evPointerDown))

        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1200L, 30f to 50f, 130f to 50f)
        assertFalse("Touch outside valid area must be discarded", detector.onTouchEvent(evMove))

        assertEquals("No gesture recognized for touch outside valid area", null, lastRecognizedType)
    }
}
