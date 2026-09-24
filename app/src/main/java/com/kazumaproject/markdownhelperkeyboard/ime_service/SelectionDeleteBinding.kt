package com.kazumaproject.markdownhelperkeyboard.ime_service

import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts

object SelectionDeleteGlobalBinder {
    fun bind(
        inputConnectionProvider: () -> android.view.inputmethod.InputConnection?,
        isComposingActive: () -> Boolean,
        finishComposing: () -> Unit,
        pushEditHistory: (String) -> Unit
    ) {
        val handler = SelectionDeleteHandler(
            inputConnectionProvider = inputConnectionProvider,
            isComposingActive = isComposingActive,
            finishComposing = finishComposing,
            onDeletedText = pushEditHistory
        )
        KeyboardDefaultLayouts.onSelectionDeleteSwipeStartHandler = { handler.onSwipeStart() }
        KeyboardDefaultLayouts.onSelectionDeleteSwipeUpdateHandler = { deltaX, deltaY -> handler.onSwipeUpdate(deltaX, deltaY) }
        KeyboardDefaultLayouts.onSelectionDeleteSwipeEndHandler = { handler.onSwipeEnd() }
    }
}
