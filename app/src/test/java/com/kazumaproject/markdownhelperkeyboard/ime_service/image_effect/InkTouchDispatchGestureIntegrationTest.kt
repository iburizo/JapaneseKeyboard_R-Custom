package com.kazumaproject.markdownhelperkeyboard.ime_service.image_effect

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.kazumaproject.custom_keyboard.data.KeyAction
import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts
import com.kazumaproject.markdownhelperkeyboard.ime_service.gesture.MultiTouchGestureSettings
import com.kazumaproject.markdownhelperkeyboard.ime_service.gesture.MultiTouchGestureType
import org.junit.After
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
 * Milestone 2 Challenger 2: Adversarial Integration Stress Tests
 *
 * Focuses on:
 * 1. R1 Cursor Mode Priority & Exclusion (isCursorModeActive == true suppresses gestures & protects R1 range selection)
 * 2. All-None Gesture Fast Bypass (O(1) immediate return, zero touch interference)
 * 3. Exact MotionEvent.ACTION_CANCEL issuance upon gesture recognition to prevent false keystroke commit
 * 4. Post-recognition touch absorption and full reset on ACTION_UP
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InkTouchDispatchGestureIntegrationTest {

    private lateinit var context: Context
    private lateinit var root: InkTouchDispatchFrameLayout
    private lateinit var child: RecordingKeyboardView
    private var lastRecognizedType: MultiTouchGestureType? = null
    private var lastRecognizedAction: KeyAction? = null
    private val recognizedHistory = mutableListOf<Pair<MultiTouchGestureType, KeyAction>>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        KeyboardDefaultLayouts.resetCursorSelectionHandlers()
        KeyboardDefaultLayouts.isCursorModeActive = false

        root = InkTouchDispatchFrameLayout(context)
        child = RecordingKeyboardView(context)

        root.addView(
            child,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        layoutView(root, child, 1080, 800)

        root.onMultiTouchGestureListener = { type, action ->
            lastRecognizedType = type
            lastRecognizedAction = action
            recognizedHistory.add(type to action)
        }
        lastRecognizedType = null
        lastRecognizedAction = null
        recognizedHistory.clear()
    }

    @After
    fun tearDown() {
        KeyboardDefaultLayouts.resetCursorSelectionHandlers()
        KeyboardDefaultLayouts.isCursorModeActive = false
    }

    private fun configureActiveGestures() {
        root.multiTouchGestureDetector.settings = MultiTouchGestureSettings(
            swipeLeftAction = KeyAction.UndoCommit,
            swipeRightAction = KeyAction.SelectAll,
            swipeUpAction = KeyAction.Undo,
            swipeDownAction = KeyAction.Redo,
            pinchInAction = KeyAction.Copy,
            pinchOutAction = KeyAction.Paste
        )
    }

    private fun layoutView(root: View, child: View, width: Int, height: Int) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, width, height)
        child.layout(0, 0, width, height)
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
    // 1. R1 Cursor Mode Exclusion (isCursorModeActive == true)
    // =========================================================================

    @Test
    fun testCursorModeActive_SuppressesTwoFingerSwipe_ProtectsR1RangeSelection() {
        configureActiveGestures()
        // Activate R1 Cursor mode
        KeyboardDefaultLayouts.isCursorModeActive = true

        val downTime = 1000L
        // Finger 0 DOWN
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (200f to 400f)))
        root.dispatchTouchEvent(evDown)

        // Finger 1 POINTER_DOWN (represents two-finger tap in R1 to toggle range selection)
        val evPointerDown = createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (200f to 400f), 1 to (400f to 400f)),
            actionIndex = 1
        )
        root.dispatchTouchEvent(evPointerDown)

        // Both fingers move left 100px (swipe gesture trajectory)
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (100f to 400f), 1 to (300f to 400f))
        )
        root.dispatchTouchEvent(evMove)

        // ASSERT: Gesture detector must NOT trigger any gesture
        assertNull("Gesture must NOT trigger while isCursorModeActive is true", lastRecognizedType)
        assertTrue("No gesture in history", recognizedHistory.isEmpty())

        // ASSERT: Child view MUST have received ACTION_POINTER_DOWN and ACTION_MOVE, and NEVER received ACTION_CANCEL
        assertTrue("Child view must receive POINTER_DOWN for R1 selection toggle",
            child.dispatchedActions.contains(MotionEvent.ACTION_POINTER_DOWN))
        assertTrue("Child view must receive MOVE for R1 selection cursor navigation",
            child.dispatchedActions.contains(MotionEvent.ACTION_MOVE))
        assertFalse("Child view must NEVER receive ACTION_CANCEL during cursor mode",
            child.dispatchedActions.contains(MotionEvent.ACTION_CANCEL))
    }

    @Test
    fun testCursorModeActive_SuppressesPinchInAndOut() {
        configureActiveGestures()
        KeyboardDefaultLayouts.isCursorModeActive = true

        val downTime = 1000L
        root.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (200f to 400f))))
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (200f to 400f), 1 to (600f to 400f)),
            actionIndex = 1
        ))

        // Pinch In trajectory: span decreases from 400px to 100px
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (350f to 400f), 1 to (450f to 400f))
        )
        root.dispatchTouchEvent(evMove)

        assertNull("Pinch In must NOT trigger during cursor mode", lastRecognizedType)
        assertFalse("Child must not be cancelled during pinch in cursor mode",
            child.dispatchedActions.contains(MotionEvent.ACTION_CANCEL))
    }

    @Test
    fun testCursorModeActivatedMidTouch_AbortsPendingGesture() {
        configureActiveGestures()
        KeyboardDefaultLayouts.isCursorModeActive = false

        val downTime = 1000L
        root.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (200f to 400f))))
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (200f to 400f), 1 to (400f to 400f)),
            actionIndex = 1
        ))

        // Long press triggers cursor mode before swipe finishes!
        KeyboardDefaultLayouts.isCursorModeActive = true

        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (100f to 400f), 1 to (300f to 400f))
        )
        root.dispatchTouchEvent(evMove)

        assertNull("Gesture must be aborted when cursor mode activates mid-touch", lastRecognizedType)
        assertFalse("Child must not receive CANCEL", child.dispatchedActions.contains(MotionEvent.ACTION_CANCEL))
    }

    // =========================================================================
    // 2. All-None Gesture Fast Bypass (O(1) Immediate Return)
    // =========================================================================

    @Test
    fun testAllGesturesNone_CompleteBypass_DirectChildDispatch() {
        // Default settings: all None (KeyAction.DoNothing)
        root.multiTouchGestureDetector.settings = MultiTouchGestureSettings()
        assertFalse("hasAnyActiveGesture must be false", root.multiTouchGestureDetector.settings.hasAnyActiveGesture)

        val downTime = 1000L
        // 1. Normal single tap
        val evDown = createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f)))
        val handledDown = root.dispatchTouchEvent(evDown)
        assertTrue("Down handled by child", handledDown)
        assertEquals(listOf(MotionEvent.ACTION_DOWN), child.dispatchedActions)

        // 2. Single finger flick
        val evMove = createMotionEvent(MotionEvent.ACTION_MOVE, downTime, 1050L, listOf(0 to (30f to 200f)))
        val handledMove = root.dispatchTouchEvent(evMove)
        assertTrue("Move handled by child", handledMove)

        val evUp = createMotionEvent(MotionEvent.ACTION_UP, downTime, 1100L, listOf(0 to (30f to 200f)))
        val handledUp = root.dispatchTouchEvent(evUp)
        assertTrue("Up handled by child", handledUp)

        assertEquals(
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP),
            child.dispatchedActions
        )
        assertNull("No gesture recognized", lastRecognizedType)
    }

    @Test
    fun testAllGesturesNone_MultiTouchRollover_NeverInterrupted() {
        root.multiTouchGestureDetector.settings = MultiTouchGestureSettings()

        val downTime = 1000L
        // Finger 0 DOWN
        root.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (100f to 200f))))
        // Fast typing rollover: Finger 1 DOWN before Finger 0 UP
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (100f to 200f), 1 to (300f to 200f)),
            actionIndex = 1
        ))
        // Both move (e.g. slight movement during fast typing)
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1100L,
            listOf(0 to (90f to 200f), 1 to (290f to 200f))
        ))
        // Finger 0 UP
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_POINTER_UP,
            downTime,
            1150L,
            listOf(0 to (90f to 200f), 1 to (290f to 200f)),
            actionIndex = 0
        ))
        // Finger 1 UP
        root.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, downTime, 1200L, listOf(1 to (290f to 200f))))

        // Child should have received full normal multi-touch stream without any CANCEL
        assertFalse("Child must never receive CANCEL when gestures are None",
            child.dispatchedActions.contains(MotionEvent.ACTION_CANCEL))
        assertEquals(5, child.dispatchedActions.size)
        assertEquals(MotionEvent.ACTION_DOWN, child.dispatchedActions[0])
        assertEquals(MotionEvent.ACTION_POINTER_DOWN, child.dispatchedActions[1])
        assertEquals(MotionEvent.ACTION_MOVE, child.dispatchedActions[2])
        assertEquals(MotionEvent.ACTION_POINTER_UP, child.dispatchedActions[3])
        assertEquals(MotionEvent.ACTION_UP, child.dispatchedActions[4])
    }

    // =========================================================================
    // 3. Exact ACTION_CANCEL Issuance Upon Gesture Recognition
    // =========================================================================

    @Test
    fun testGestureRecognized_IssuesActionCancelToChild_AndAbsorbsSubsequentTouches() {
        configureActiveGestures()

        val downTime = 1000L
        // 1. Finger 0 DOWN -> Child enters pressed state
        root.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (200f to 300f))))
        assertEquals(listOf(MotionEvent.ACTION_DOWN), child.dispatchedActions)
        assertTrue("Child should be in pressed state initially", child.isPressedState)

        // 2. Finger 1 POINTER_DOWN -> Child receives POINTER_DOWN
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (200f to 300f), 1 to (350f to 300f)),
            actionIndex = 1
        ))
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN), child.dispatchedActions)

        // 3. Both fingers swipe left by 80px -> Gesture recognized!
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (120f to 300f), 1 to (270f to 300f))
        )
        val handledByRoot = root.dispatchTouchEvent(evMove)

        assertTrue("Root must consume the gesture event", handledByRoot)
        assertEquals("Swipe left recognized", MultiTouchGestureType.SWIPE_LEFT, lastRecognizedType)
        assertEquals(KeyAction.UndoCommit, lastRecognizedAction)

        // CRUCIAL ASSERTION: Child view MUST have received ACTION_CANCEL!
        assertTrue("Child view MUST receive ACTION_CANCEL immediately upon gesture recognition",
            child.dispatchedActions.contains(MotionEvent.ACTION_CANCEL))
        assertFalse("Child pressed state MUST be cancelled to prevent false character commit", child.isPressedState)

        // 4. Subsequent touches (more MOVE, POINTER_UP, UP) must be consumed by root and NOT sent to child
        val countBeforeSubsequent = child.dispatchedActions.size

        // Another MOVE after recognition
        val evMove2 = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1200L,
            listOf(0 to (100f to 300f), 1 to (250f to 300f))
        )
        val handledMove2 = root.dispatchTouchEvent(evMove2)
        assertTrue("Subsequent MOVE must be consumed by root", handledMove2)
        assertEquals("Child must NOT receive post-recognition MOVE", countBeforeSubsequent, child.dispatchedActions.size)

        // Finger 0 UP
        val evPointerUp = createMotionEvent(
            MotionEvent.ACTION_POINTER_UP,
            downTime,
            1250L,
            listOf(0 to (100f to 300f), 1 to (250f to 300f)),
            actionIndex = 0
        )
        val handledPointerUp = root.dispatchTouchEvent(evPointerUp)
        assertTrue("Subsequent POINTER_UP must be consumed by root", handledPointerUp)
        assertEquals("Child must NOT receive post-recognition POINTER_UP", countBeforeSubsequent, child.dispatchedActions.size)

        // Finger 1 UP (gesture complete)
        val evUp = createMotionEvent(MotionEvent.ACTION_UP, downTime, 1300L, listOf(1 to (250f to 300f)))
        val handledUp = root.dispatchTouchEvent(evUp)
        assertTrue("Subsequent UP must be consumed by root", handledUp)
        assertEquals("Child must NOT receive post-recognition UP", countBeforeSubsequent, child.dispatchedActions.size)

        // 5. Next new touch session starts cleanly
        val newDownTime = 2000L
        child.reset()
        val evNewDown = createMotionEvent(MotionEvent.ACTION_DOWN, newDownTime, 2000L, listOf(0 to (150f to 150f)))
        val handledNewDown = root.dispatchTouchEvent(evNewDown)
        assertTrue("New touch must dispatch normally to child", handledNewDown)
        assertEquals(listOf(MotionEvent.ACTION_DOWN), child.dispatchedActions)
        assertTrue("Child enters pressed state again", child.isPressedState)
    }

    @Test
    fun testPinchOut_IssuesActionCancelToChild() {
        configureActiveGestures()

        val downTime = 1000L
        // Initial span = 80px (200f to 280f)
        root.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (200f to 300f))))
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (200f to 300f), 1 to (280f to 300f)),
            actionIndex = 1
        ))

        // Pinch Out: span expands to 180px (+100px)
        val evMove = createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (150f to 300f), 1 to (330f to 300f))
        )
        val handled = root.dispatchTouchEvent(evMove)

        assertTrue(handled)
        assertEquals(MultiTouchGestureType.PINCH_OUT, lastRecognizedType)
        assertEquals(KeyAction.Paste, lastRecognizedAction)
        assertTrue("Child MUST receive ACTION_CANCEL on pinch recognition",
            child.dispatchedActions.contains(MotionEvent.ACTION_CANCEL))
    }

    // =========================================================================
    // 4. Invalid Touch Area Exclusion (e.g. Suggestion bar)
    // =========================================================================

    @Test
    fun testTouchStartingInInvalidArea_DoesNotRecognize_AndDoesNotCancelChild() {
        configureActiveGestures()
        // Only touches starting at y >= 200 are valid keyboard touches
        root.multiTouchGestureDetector.isTouchInsideValidArea = { _, y -> y >= 200f }

        val downTime = 1000L
        // Touch starts in suggestion bar (y = 50f)
        root.dispatchTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, downTime, 1000L, listOf(0 to (200f to 50f))))
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_POINTER_DOWN,
            downTime,
            1050L,
            listOf(0 to (200f to 50f), 1 to (400f to 50f)),
            actionIndex = 1
        ))

        // Large 2-finger swipe left
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_MOVE,
            downTime,
            1150L,
            listOf(0 to (100f to 50f), 1 to (300f to 50f))
        ))

        assertNull("Touches in invalid area must NOT trigger gesture", lastRecognizedType)
        assertFalse("Child must NOT be cancelled for touches in suggestion bar",
            child.dispatchedActions.contains(MotionEvent.ACTION_CANCEL))
    }

    @Test
    fun testOnMultiTouchSessionStartListenerFiresOnActionDown() {
        configureActiveGestures()
        var sessionStartInvocationCount = 0
        root.onMultiTouchSessionStartListener = {
            sessionStartInvocationCount++
        }

        val downTime = 1000L
        root.dispatchTouchEvent(createMotionEvent(
            MotionEvent.ACTION_DOWN,
            downTime,
            downTime,
            listOf(0 to (400f to 400f))
        ))

        assertEquals("onMultiTouchSessionStartListener must fire on ACTION_DOWN", 1, sessionStartInvocationCount)
    }

    // =========================================================================
    // Helper Mock View
    // =========================================================================

    private class RecordingKeyboardView(context: Context) : View(context) {
        val dispatchedActions = mutableListOf<Int>()
        var isPressedState = false

        fun reset() {
            dispatchedActions.clear()
            isPressedState = false
        }

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            val action = event.actionMasked
            dispatchedActions.add(action)

            when (action) {
                MotionEvent.ACTION_DOWN -> isPressedState = true
                MotionEvent.ACTION_POINTER_DOWN -> isPressedState = true
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> isPressedState = false
            }
            return true
        }
    }
}
