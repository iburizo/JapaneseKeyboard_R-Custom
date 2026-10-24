package com.kazumaproject.markdownhelperkeyboard.ime_service.gesture

enum class MultiTouchGestureType {
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    PINCH_IN,
    PINCH_OUT;

    companion object {
        fun fromKey(key: String): MultiTouchGestureType? = when (key) {
            "gesture_two_finger_swipe_left_preference", "gesture_two_finger_swipe_left" -> SWIPE_LEFT
            "gesture_two_finger_swipe_right_preference", "gesture_two_finger_swipe_right" -> SWIPE_RIGHT
            "gesture_two_finger_swipe_up_preference", "gesture_two_finger_swipe_up" -> SWIPE_UP
            "gesture_two_finger_swipe_down_preference", "gesture_two_finger_swipe_down" -> SWIPE_DOWN
            "gesture_two_finger_pinch_in_preference", "gesture_two_finger_pinch_in" -> PINCH_IN
            "gesture_two_finger_pinch_out_preference", "gesture_two_finger_pinch_out" -> PINCH_OUT
            else -> null
        }
    }
}
