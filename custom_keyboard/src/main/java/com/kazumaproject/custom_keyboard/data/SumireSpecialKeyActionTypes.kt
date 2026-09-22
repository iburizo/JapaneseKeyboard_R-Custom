package com.kazumaproject.custom_keyboard.data

enum class SumireSpecialKeyDirection {
    TAP,
    UP,
    RIGHT,
    DOWN,
    LEFT,
    TAP_COMPOSING,
    UP_COMPOSING,
    RIGHT_COMPOSING,
    DOWN_COMPOSING,
    LEFT_COMPOSING
}

sealed class ResolvedSumireSpecialKeyAction {
    data object Default : ResolvedSumireSpecialKeyAction()
    data object None : ResolvedSumireSpecialKeyAction()
    data class Action(val action: KeyAction) : ResolvedSumireSpecialKeyAction()
    data class InputText(val text: String) : ResolvedSumireSpecialKeyAction()
}

