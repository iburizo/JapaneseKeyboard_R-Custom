package com.kazumaproject.markdownhelperkeyboard.ime_service.gesture

import com.kazumaproject.custom_keyboard.data.KeyAction

data class MultiTouchGestureSettings(
    val swipeLeftAction: KeyAction = KeyAction.DoNothing,
    val swipeRightAction: KeyAction = KeyAction.DoNothing,
    val swipeUpAction: KeyAction = KeyAction.DoNothing,
    val swipeDownAction: KeyAction = KeyAction.DoNothing,
    val pinchInAction: KeyAction = KeyAction.DoNothing,
    val pinchOutAction: KeyAction = KeyAction.DoNothing
) {
    val hasAnyActiveGesture: Boolean
        get() = swipeLeftAction != KeyAction.DoNothing ||
                swipeRightAction != KeyAction.DoNothing ||
                swipeUpAction != KeyAction.DoNothing ||
                swipeDownAction != KeyAction.DoNothing ||
                pinchInAction != KeyAction.DoNothing ||
                pinchOutAction != KeyAction.DoNothing

    fun actionFor(type: MultiTouchGestureType): KeyAction = when (type) {
        MultiTouchGestureType.SWIPE_LEFT -> swipeLeftAction
        MultiTouchGestureType.SWIPE_RIGHT -> swipeRightAction
        MultiTouchGestureType.SWIPE_UP -> swipeUpAction
        MultiTouchGestureType.SWIPE_DOWN -> swipeDownAction
        MultiTouchGestureType.PINCH_IN -> pinchInAction
        MultiTouchGestureType.PINCH_OUT -> pinchOutAction
    }
}
