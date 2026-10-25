package com.kazumaproject.markdownhelperkeyboard.ime_service.gesture

import android.content.Context
import android.view.MotionEvent
import com.kazumaproject.custom_keyboard.data.KeyAction
import kotlin.math.abs
import kotlin.math.hypot

fun interface OnMultiTouchGestureListener {
    fun onGestureRecognized(type: MultiTouchGestureType, action: KeyAction)
}

class MultiTouchGestureDetector(
    val density: Float = 1.0f
) {
    constructor(context: Context) : this(context.resources.displayMetrics.density)

    var settings: MultiTouchGestureSettings = MultiTouchGestureSettings()
    var listener: OnMultiTouchGestureListener? = null
    var isCursorModeProvider: (() -> Boolean)? = null
    var isTouchInsideValidArea: ((x: Float, y: Float) -> Boolean)? = null
    var onSessionStartListener: (() -> Unit)? = null

    // Thresholds scaled by screen density
    private val swipeMinDistance: Float = 48f * density
    private val swipeMinPointerDistance: Float = 24f * density
    private val pinchThreshold: Float = 35f * density
    private val pinchMinInitialSpan: Float = 32f * density
    private val pinchMinPointerDistance: Float = 14f * density

    companion object {
        private const val MAX_POINTER_DOWN_INTERVAL_MS = 300L
        private const val MAX_GESTURE_DURATION_MS = 800L
        private const val SWIPE_VECTOR_COSINE_MIN = 0.5f
        private const val SWIPE_SPAN_RATIO_MIN = 0.70f
        private const val SWIPE_SPAN_RATIO_MAX = 1.35f
        private const val SWIPE_AXIS_DOMINANCE_RATIO = 1.25f
        private const val PINCH_IN_RATIO_MAX = 0.75f
        private const val PINCH_OUT_RATIO_MIN = 1.30f
        private const val PINCH_DOMINANCE_RATIO = 0.8f
    }

    // Touch tracking state
    private var activePointerId0: Int = MotionEvent.INVALID_POINTER_ID
    private var activePointerId1: Int = MotionEvent.INVALID_POINTER_ID
    private var downTime0: Long = 0L
    private var downTime1: Long = 0L
    private var startX0: Float = 0f
    private var startY0: Float = 0f
    private var startX1: Float = 0f
    private var startY1: Float = 0f
    private var initialSpan: Float = 0f
    private var initialCentroidX: Float = 0f
    private var initialCentroidY: Float = 0f
    private var gestureStartTime: Long = 0L
    private var isGestureRecognized: Boolean = false
    private var isDiscarded: Boolean = false

    fun reset() {
        activePointerId0 = MotionEvent.INVALID_POINTER_ID
        activePointerId1 = MotionEvent.INVALID_POINTER_ID
        downTime0 = 0L
        downTime1 = 0L
        startX0 = 0f
        startY0 = 0f
        startX1 = 0f
        startY1 = 0f
        initialSpan = 0f
        initialCentroidX = 0f
        initialCentroidY = 0f
        gestureStartTime = 0L
        isGestureRecognized = false
        isDiscarded = false
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        // Fast O(1) bypass when all gestures are None (Zero overhead guarantee)
        if (!settings.hasAnyActiveGesture) {
            return false
        }

        // Suppress gestures during cursor movement mode (R1 priority guarantee)
        if (isCursorModeProvider?.invoke() == true) {
            reset()
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                activePointerId0 = event.getPointerId(0)
                downTime0 = event.eventTime
                startX0 = event.getX(0)
                startY0 = event.getY(0)
                if (isTouchInsideValidArea?.invoke(startX0, startY0) == false) {
                    isDiscarded = true
                }
                onSessionStartListener?.invoke()
                return false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isDiscarded || isGestureRecognized) {
                    return false
                }
                if (event.pointerCount > 2) {
                    isDiscarded = true
                    return false
                }
                if (activePointerId1 == MotionEvent.INVALID_POINTER_ID && event.pointerCount == 2) {
                    val actionIndex = event.actionIndex
                    activePointerId1 = event.getPointerId(actionIndex)
                    downTime1 = event.eventTime
                    startX1 = event.getX(actionIndex)
                    startY1 = event.getY(actionIndex)

                    if (isTouchInsideValidArea?.invoke(startX1, startY1) == false) {
                        isDiscarded = true
                        return false
                    }

                    if (abs(downTime1 - downTime0) > MAX_POINTER_DOWN_INTERVAL_MS) {
                        isDiscarded = true
                        return false
                    }

                    val idx0 = event.findPointerIndex(activePointerId0)
                    if (idx0 < 0) {
                        isDiscarded = true
                        return false
                    }
                    val p0x = event.getX(idx0)
                    val p0y = event.getY(idx0)
                    val p1x = startX1
                    val p1y = startY1

                    initialCentroidX = (p0x + p1x) / 2f
                    initialCentroidY = (p0y + p1y) / 2f
                    initialSpan = hypot((p0x - p1x).toDouble(), (p0y - p1y).toDouble()).toFloat()
                    gestureStartTime = event.eventTime
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isGestureRecognized) {
                    return true
                }
                if (isDiscarded || activePointerId0 == MotionEvent.INVALID_POINTER_ID || activePointerId1 == MotionEvent.INVALID_POINTER_ID) {
                    return false
                }
                if (event.eventTime - gestureStartTime > MAX_GESTURE_DURATION_MS) {
                    isDiscarded = true
                    return false
                }

                val idx0 = event.findPointerIndex(activePointerId0)
                val idx1 = event.findPointerIndex(activePointerId1)
                if (idx0 < 0 || idx1 < 0) return false

                val p0x = event.getX(idx0)
                val p0y = event.getY(idx0)
                val p1x = event.getX(idx1)
                val p1y = event.getY(idx1)

                val curCentroidX = (p0x + p1x) / 2f
                val curCentroidY = (p0y + p1y) / 2f
                val curSpan = hypot((p0x - p1x).toDouble(), (p0y - p1y).toDouble()).toFloat()

                val deltaCentroidX = curCentroidX - initialCentroidX
                val deltaCentroidY = curCentroidY - initialCentroidY
                val centroidDistance = hypot(deltaCentroidX.toDouble(), deltaCentroidY.toDouble()).toFloat()

                val deltaSpan = curSpan - initialSpan
                val spanRatio = if (initialSpan > 0f) curSpan / initialSpan else 1f

                val v0x = p0x - startX0
                val v0y = p0y - startY0
                val v1x = p1x - startX1
                val v1y = p1y - startY1
                val dist0 = hypot(v0x.toDouble(), v0y.toDouble()).toFloat()
                val dist1 = hypot(v1x.toDouble(), v1y.toDouble()).toFloat()

                // 1. Evaluate Pinch
                if (initialSpan >= pinchMinInitialSpan &&
                    dist0 >= pinchMinPointerDistance &&
                    dist1 >= pinchMinPointerDistance &&
                    centroidDistance <= abs(deltaSpan) * PINCH_DOMINANCE_RATIO
                ) {
                    // Pinch In
                    if (deltaSpan <= -pinchThreshold && spanRatio <= PINCH_IN_RATIO_MAX) {
                        val action = settings.pinchInAction
                        if (action != KeyAction.DoNothing) {
                            return triggerGesture(MultiTouchGestureType.PINCH_IN, action)
                        }
                    }
                    // Pinch Out
                    if (deltaSpan >= pinchThreshold && spanRatio >= PINCH_OUT_RATIO_MIN) {
                        val action = settings.pinchOutAction
                        if (action != KeyAction.DoNothing) {
                            return triggerGesture(MultiTouchGestureType.PINCH_OUT, action)
                        }
                    }
                }

                // 2. Evaluate Swipe (4 directions)
                if (centroidDistance >= swipeMinDistance &&
                    dist0 >= swipeMinPointerDistance &&
                    dist1 >= swipeMinPointerDistance &&
                    spanRatio in SWIPE_SPAN_RATIO_MIN..SWIPE_SPAN_RATIO_MAX
                ) {
                    val dotProduct = v0x * v1x + v0y * v1y
                    val cosine = if (dist0 * dist1 > 0f) dotProduct / (dist0 * dist1) else 0f
                    if (cosine >= SWIPE_VECTOR_COSINE_MIN) {
                        val absX = abs(deltaCentroidX)
                        val absY = abs(deltaCentroidY)

                        if (absX >= absY * SWIPE_AXIS_DOMINANCE_RATIO) {
                            // Horizontal Swipe
                            val type = if (deltaCentroidX < 0) MultiTouchGestureType.SWIPE_LEFT else MultiTouchGestureType.SWIPE_RIGHT
                            val action = settings.actionFor(type)
                            if (action != KeyAction.DoNothing) {
                                return triggerGesture(type, action)
                            }
                        } else if (absY >= absX * SWIPE_AXIS_DOMINANCE_RATIO) {
                            // Vertical Swipe
                            val type = if (deltaCentroidY < 0) MultiTouchGestureType.SWIPE_UP else MultiTouchGestureType.SWIPE_DOWN
                            val action = settings.actionFor(type)
                            if (action != KeyAction.DoNothing) {
                                return triggerGesture(type, action)
                            }
                        }
                    }
                }
                return false
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val actionIndex = event.actionIndex
                val pointerId = event.getPointerId(actionIndex)
                if (pointerId == activePointerId0 || pointerId == activePointerId1) {
                    isDiscarded = true
                }
                return isGestureRecognized
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val recognized = isGestureRecognized
                reset()
                return recognized
            }
        }
        return false
    }

    private fun triggerGesture(type: MultiTouchGestureType, action: KeyAction): Boolean {
        isGestureRecognized = true
        listener?.onGestureRecognized(type, action)
        return true
    }
}
