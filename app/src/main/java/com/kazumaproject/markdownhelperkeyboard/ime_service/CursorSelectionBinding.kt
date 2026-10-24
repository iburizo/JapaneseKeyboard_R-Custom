package com.kazumaproject.markdownhelperkeyboard.ime_service

import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts

/**
 * Global binder that connects IMEService's cursor selection state and logic
 * with KeyboardDefaultLayouts handlers for keyboard views (Flick, TenKey, QWERTY).
 */
object CursorSelectionGlobalBinder {
    /**
     * Binds cursor selection handlers to KeyboardDefaultLayouts.
     *
     * @param toggleSelection Called when a second pointer down is detected during cursor mode.
     *                        Toggles cursorSelectionMode and triggers haptic feedback.
     * @param endSelection Called when cursor pointer is lifted (ACTION_UP / ACTION_CANCEL).
     *                     Resets cursorSelectionMode and cursorMovement without collapsing editor selection.
     * @param isSelectionMode Provider returning whether cursorSelectionMode is currently active.
     */
    fun bind(
        toggleSelection: () -> Unit,
        endSelection: () -> Unit,
        isSelectionMode: () -> Boolean
    ) {
        KeyboardDefaultLayouts.cursorSelectionToggleHandler = toggleSelection
        KeyboardDefaultLayouts.cursorSelectionEndHandler = endSelection
        KeyboardDefaultLayouts.isCursorSelectionModeProvider = isSelectionMode
    }

    /**
     * Clears all handlers from KeyboardDefaultLayouts to prevent memory leaks.
     */
    fun unbind() {
        KeyboardDefaultLayouts.cursorSelectionToggleHandler = null
        KeyboardDefaultLayouts.cursorSelectionEndHandler = null
        KeyboardDefaultLayouts.isCursorSelectionModeProvider = null
    }
}
