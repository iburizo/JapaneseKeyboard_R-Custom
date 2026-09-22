package com.kazumaproject.custom_keyboard.data

fun FlickDirection.toSumireSpecialKeyDirectionOrNull(isComposing: Boolean = false): SumireSpecialKeyDirection? {
    return when (this) {
        FlickDirection.TAP -> if (isComposing) SumireSpecialKeyDirection.TAP_COMPOSING else SumireSpecialKeyDirection.TAP
        FlickDirection.UP -> if (isComposing) SumireSpecialKeyDirection.UP_COMPOSING else SumireSpecialKeyDirection.UP
        FlickDirection.DOWN -> if (isComposing) SumireSpecialKeyDirection.DOWN_COMPOSING else SumireSpecialKeyDirection.DOWN
        FlickDirection.UP_LEFT,
        FlickDirection.UP_LEFT_FAR -> if (isComposing) SumireSpecialKeyDirection.LEFT_COMPOSING else SumireSpecialKeyDirection.LEFT
        FlickDirection.UP_RIGHT,
        FlickDirection.UP_RIGHT_FAR -> if (isComposing) SumireSpecialKeyDirection.RIGHT_COMPOSING else SumireSpecialKeyDirection.RIGHT
    }
}

