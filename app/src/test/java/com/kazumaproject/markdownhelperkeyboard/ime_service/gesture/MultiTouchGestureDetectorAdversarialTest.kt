package com.kazumaproject.markdownhelperkeyboard.ime_service.gesture

import android.view.MotionEvent
import com.kazumaproject.custom_keyboard.data.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Milestone 2 Challenger 1: Adversarial and Boundary Stress Tests
 *
 * Verifies strict robustness against:
 * 1. Pointer down timing window (300ms boundary vs 301ms rollover)
 * 2. Opposing/orthogonal vectors (cosine boundary & zero cross-trigger)
 * 3. Asymmetric flick / one finger resting (< 14px / < 24px)
 * 4. Multi-pointer rejection (> 2 pointers, stickiness until full UP)
 * 5. Strict mutual exclusion between swipe and pinch
 * 6. Gesture duration timeout (800ms boundary vs 801ms)
 * 7. Diagonal axis dominance (1.25 ratio guard)
 * 8. Pointer index swap tolerance
 * 9. Dynamic cursor mode interruption
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MultiTouchGestureDetectorAdversarialTest {

    private lateinit var detector: MultiTouchGestureDetector
    private val recognizedGestures = mutableListOf<Pair<MultiTouchGestureType, KeyAction>>()

    @Before
    fun setUp() {
        detector = MultiTouchGestureDetector(density = 1.0f)
        detector.listener = OnMultiTouchGestureListener { type, action ->
            recognizedGestures.add(type to action)
        }
        recognizedGestures.clear()
        configureAllGestures()
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
        pointers: List<Pair<Int, Pair<Float, Float>>>,
        actionIndex: Int = 0
    ): MotionEvent {
        val count = pointers.size
        val props = Array(count) { i ->
            MotionEvent.PointerProperties().apply {
                id = pointers[i].first
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coords = Array(count) { i ->
            val p = pointers[i].second
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

    // =========================================================================
    // 1. Landing Interval Boundary Tests (300ms vs 301ms)
    // =========================================================================

    @Test
    fun testLandingIntervalExactBoundary300ms_ShouldRecognize() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        // Exactly 300ms interval (1300L - 1000L = 300ms)
        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1300L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Both move right 60px at 1400L
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1400L,
            listOf(0 to (160f to 200f), 1 to (260f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertTrue("300ms exact interval should be accepted", recognized)
        assertEquals(1, recognizedGestures.size)
        assertEquals(MultiTouchGestureType.SWIPE_RIGHT, recognizedGestures[0].first)
    }

    @Test
    fun testLandingIntervalBoundary301ms_ShouldDiscard() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        // 301ms interval (> 300ms)
        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1301L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Both move right 60px
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1400L,
            listOf(0 to (160f to 200f), 1 to (260f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("301ms interval must be discarded", recognized)
        assertTrue("No gesture should fire for 301ms landing gap", recognizedGestures.isEmpty())
    }

    // =========================================================================
    // 2. Opposing and Orthogonal Directions (No Swipe)
    // =========================================================================

    @Test
    fun testVerticalOpposingMovement_SwipeGesturesOnly_ShouldNotRecognize() {
        detector.settings = MultiTouchGestureSettings(
            swipeLeftAction = KeyAction.UndoCommit,
            swipeRightAction = KeyAction.SelectAll,
            swipeUpAction = KeyAction.Undo,
            swipeDownAction = KeyAction.Redo,
            pinchInAction = KeyAction.DoNothing,
            pinchOutAction = KeyAction.DoNothing
        )

        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Finger 0 moves up 60px (dy = -60), Finger 1 moves down 60px (dy = +60)
        // Cosine is -1.0, centroid moves 0px
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (100f to 140f), 1 to (200f to 260f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("Vertical opposing movement must not trigger swipe", recognized)
        assertTrue("No gesture should be recognized when pinch is disabled", recognizedGestures.isEmpty())
    }

    @Test
    fun testVerticalOpposingMovement_AllGesturesConfigured_MustNeverRecognizeSwipeUpOrDown() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Finger 0 moves up 60px, Finger 1 moves down 60px
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (100f to 140f), 1 to (200f to 260f))
        )
        detector.onTouchEvent(evMove)
        assertTrue(
            "Opposing movement must never trigger SWIPE_UP or SWIPE_DOWN",
            recognizedGestures.none { it.first == MultiTouchGestureType.SWIPE_UP || it.first == MultiTouchGestureType.SWIPE_DOWN }
        )
    }

    @Test
    fun testOrthogonalMovement_ShouldNotRecognizeSwipe() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Finger 0 moves right 60px (dx = +60, dy = 0), Finger 1 moves up 60px (dx = 0, dy = -60)
        // Dot product is 0, cosine is 0.0 < 0.5
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (160f to 200f), 1 to (200f to 140f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("Orthogonal movement must not trigger swipe", recognized)
        assertTrue(recognizedGestures.isEmpty())
    }

    // =========================================================================
    // 3. Asymmetric Flick / One Finger Resting
    // =========================================================================

    @Test
    fun testOneFingerRestingCompletely_ShouldNotRecognizeAnyGesture() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Finger 0 moves 80px right, Finger 1 does NOT move at all (0px)
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (180f to 200f), 1 to (200f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("Resting finger must block swipe & pinch recognition", recognized)
        assertTrue("No gesture should fire when one finger is stationary", recognizedGestures.isEmpty())
    }

    @Test
    fun testOneFingerSubThresholdMovement_ShouldNotRecognize() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Finger 0 moves 80px left, Finger 1 moves only 10px left (< 14px pinch min, < 24px swipe min)
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (20f to 200f), 1 to (190f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("Sub-threshold movement on second finger must be rejected", recognized)
        assertTrue(recognizedGestures.isEmpty())
    }

    // =========================================================================
    // 4. Three or More Pointers Discard and Sticky Discard
    // =========================================================================

    @Test
    fun testThreePointersDiscard_StaysDiscardedUntilFullUp() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        val evPointerDown2 = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown2)

        // Third pointer lands
        val evPointerDown3 = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1100L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f), 2 to (300f to 200f)),
            actionIndex = 2
        )
        detector.onTouchEvent(evPointerDown3)

        // Pointer 2 leaves (back to 2 fingers)
        val evPointerUp2 = createMotionEvent(
            MotionEvent.ACTION_POINTER_UP,
            downTime,
            1150L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f), 2 to (300f to 200f)),
            actionIndex = 2
        )
        detector.onTouchEvent(evPointerUp2)

        // Now the remaining 2 fingers perform a perfect swipe
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1200L,
            listOf(0 to (160f to 200f), 1 to (260f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("Once discarded by 3 pointers, session must remain discarded until full UP", recognized)
        assertTrue(recognizedGestures.isEmpty())

        // Full UP resets state
        val evUp = createMotionEvent(MotionEvent.ACTION_UP, downTime, 1300L, listOf(0 to (160f to 200f)))
        detector.onTouchEvent(evUp)

        // New fresh 2-finger session should succeed
        val newDownTime = 2000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, newDownTime, 2000L, listOf(0 to (100f to 200f))))
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_POINTER_DOWN, newDownTime, 2050L, listOf(0 to (100f to 200f), 1 to (200f to 200f)), actionIndex = 1))
        val newMove = detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_MOVE, newDownTime, 2150L, listOf(0 to (160f to 200f), 1 to (260f to 200f))))
        assertTrue("Subsequent session after ACTION_UP must work normally", newMove)
        assertEquals(1, recognizedGestures.size)
        assertEquals(MultiTouchGestureType.SWIPE_RIGHT, recognizedGestures[0].first)
    }

    // =========================================================================
    // 5. Strict Mutual Exclusion Between Swipe and Pinch
    // =========================================================================

    @Test
    fun testPinchIn_DoesNotTriggerSwipe() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        // Initial span = 160px
        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (260f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Pinch In: fingers move towards each other, deltaSpan = -80px
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (140f to 200f), 1 to (220f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertTrue(recognized)
        assertEquals(1, recognizedGestures.size)
        assertEquals(MultiTouchGestureType.PINCH_IN, recognizedGestures[0].first)

        // Subsequent moves in same gesture do not fire duplicate or swipe gestures
        val evMove2 = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1180L,
            listOf(0 to (200f to 200f), 1 to (280f to 200f))
        )
        detector.onTouchEvent(evMove2)
        assertEquals("Exactly one gesture must fire per multi-touch session", 1, recognizedGestures.size)
    }

    @Test
    fun testSwipeRight_DoesNotTriggerPinch() {
        val downTime = 1000L
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        detector.onTouchEvent(evDown)

        // Initial span = 100px
        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (200f to 200f)),
            actionIndex = 1
        )
        detector.onTouchEvent(evPointerDown)

        // Parallel movement: deltaSpan = 0, deltaCentroid = +70px
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (170f to 200f), 1 to (270f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertTrue(recognized)
        assertEquals(1, recognizedGestures.size)
        assertEquals(MultiTouchGestureType.SWIPE_RIGHT, recognizedGestures[0].first)
    }

    // =========================================================================
    // 6. Gesture Duration Timeout Boundary (800ms vs 801ms)
    // =========================================================================

    @Test
    fun testGestureDurationExactBoundary800ms_ShouldRecognize() {
        val downTime = 1000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))

        // gestureStartTime = 1050L
        detector.onTouchEvent(
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                downTime,
                1050L,
                listOf(0 to (100f to 200f), 1 to (200f to 200f)),
                actionIndex = 1
            )
        )

        // Exactly 800ms after gestureStartTime (1050L + 800L = 1850L)
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1850L,
            listOf(0 to (160f to 200f), 1 to (260f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertTrue("800ms duration should be recognized", recognized)
        assertEquals(MultiTouchGestureType.SWIPE_RIGHT, recognizedGestures[0].first)
    }

    @Test
    fun testGestureDurationBoundary801ms_ShouldDiscard() {
        val downTime = 1000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))

        // gestureStartTime = 1050L
        detector.onTouchEvent(
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                downTime,
                1050L,
                listOf(0 to (100f to 200f), 1 to (200f to 200f)),
                actionIndex = 1
            )
        )

        // 801ms after gestureStartTime (1050L + 801L = 1851L)
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1851L,
            listOf(0 to (160f to 200f), 1 to (260f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("801ms duration must be discarded", recognized)
        assertTrue(recognizedGestures.isEmpty())
    }

    // =========================================================================
    // 7. Diagonal Axis Dominance Guard (1.25 ratio)
    // =========================================================================

    @Test
    fun testDiagonalMovement45Degrees_ShouldNotRecognize() {
        val downTime = 1000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))
        detector.onTouchEvent(
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                downTime,
                1050L,
                listOf(0 to (100f to 200f), 1 to (200f to 200f)),
                actionIndex = 1
            )
        )

        // Both move diagonally down-right by 60px (dx = +60, dy = +60). Ratio is 1.0 < 1.25
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (160f to 260f), 1 to (260f to 260f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("45-degree diagonal movement must be rejected by axis dominance guard", recognized)
        assertTrue(recognizedGestures.isEmpty())
    }

    @Test
    fun testDiagonalMovementSubDominanceRatio_ShouldNotRecognize() {
        val downTime = 1000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))
        detector.onTouchEvent(
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                downTime,
                1050L,
                listOf(0 to (100f to 200f), 1 to (200f to 200f)),
                actionIndex = 1
            )
        )

        // dx = 60, dy = 50 -> ratio = 1.20 < 1.25
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (160f to 250f), 1 to (260f to 250f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("Movement with axis ratio 1.20 must be rejected", recognized)
        assertTrue(recognizedGestures.isEmpty())
    }

    @Test
    fun testDiagonalMovementDominanceRatio130_ShouldRecognizeHorizontalSwipe() {
        val downTime = 1000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))
        detector.onTouchEvent(
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                downTime,
                1050L,
                listOf(0 to (100f to 200f), 1 to (200f to 200f)),
                actionIndex = 1
            )
        )

        // dx = 65, dy = 50 -> ratio = 1.30 >= 1.25 -> Swipe Right
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (165f to 250f), 1 to (265f to 250f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertTrue("Movement with axis ratio 1.30 should recognize horizontal swipe", recognized)
        assertEquals(MultiTouchGestureType.SWIPE_RIGHT, recognizedGestures[0].first)
    }

    // =========================================================================
    // 8. Pointer Index Swapping Resilience
    // =========================================================================

    @Test
    fun testPointerIndexOrderSwapInMotionEvent_ResolvesCorrectly() {
        val downTime = 1000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))
        detector.onTouchEvent(
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                downTime,
                1050L,
                listOf(0 to (100f to 200f), 1 to (200f to 200f)),
                actionIndex = 1
            )
        )

        // MOVE event where pointers are swapped in the array: index 0 is pointerId 1, index 1 is pointerId 0
        val swappedPointers = listOf(
            1 to (260f to 200f), // pointerId 1 (moved right 60px)
            0 to (160f to 200f)  // pointerId 0 (moved right 60px)
        )
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1150L, swappedPointers)
        val recognized = detector.onTouchEvent(evMove)
        assertTrue("Pointer ID lookup must handle swapped indices transparently", recognized)
        assertEquals(MultiTouchGestureType.SWIPE_RIGHT, recognizedGestures[0].first)
    }

    // =========================================================================
    // 9. Dynamic Cursor Mode Interruption
    // =========================================================================

    @Test
    fun testCursorModeActivatedMidGesture_AbortsGestureImmediately() {
        var inCursorMode = false
        detector.isCursorModeProvider = { inCursorMode }

        val downTime = 1000L
        detector.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))
        detector.onTouchEvent(
            createMotionEvent(
                MotionEvent.ACTION_POINTER_DOWN,
                downTime,
                1050L,
                listOf(0 to (100f to 200f), 1 to (200f to 200f)),
                actionIndex = 1
            )
        )

        // Cursor mode activates right before MOVE
        inCursorMode = true

        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (160f to 200f), 1 to (260f to 200f))
        )
        val recognized = detector.onTouchEvent(evMove)
        assertFalse("Cursor mode mid-gesture must abort gesture", recognized)
        assertTrue(recognizedGestures.isEmpty())
    }
}
