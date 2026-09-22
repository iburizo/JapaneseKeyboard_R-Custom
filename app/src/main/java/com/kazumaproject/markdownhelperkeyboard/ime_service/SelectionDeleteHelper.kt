package com.kazumaproject.markdownhelperkeyboard.ime_service

import android.view.inputmethod.InputConnection

class SelectionDeleteHelper(
    private val inputConnectionProvider: () -> InputConnection?,
    private val onCommit: () -> Unit
) {
    private var initialCursorPos = -1
    private var currentSelectionEnd = -1

    fun startSelection() {
        val ic = inputConnectionProvider() ?: return
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        initialCursorPos = extracted?.selectionStart ?: -1
        currentSelectionEnd = initialCursorPos
    }

    fun updateSelection(dx: Int, dy: Int) {
        val ic = inputConnectionProvider() ?: return
        if (initialCursorPos == -1) return
        
        val charOffset = (dx / 30) + (dy / 30) 
        
        currentSelectionEnd = (initialCursorPos + charOffset).coerceAtLeast(0)
        if (currentSelectionEnd < initialCursorPos) {
            ic.setSelection(currentSelectionEnd, initialCursorPos)
        } else {
            ic.setSelection(initialCursorPos, currentSelectionEnd)
        }
    }

    fun commitDelete() {
        val ic = inputConnectionProvider() ?: return
        if (initialCursorPos != -1 && currentSelectionEnd != -1 && initialCursorPos != currentSelectionEnd) {
            ic.commitText("", 1)
        }
        initialCursorPos = -1
        currentSelectionEnd = -1
        onCommit()
    }
}
