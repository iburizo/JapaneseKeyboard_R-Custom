package com.kazumaproject.markdownhelperkeyboard.ime_service

import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import kotlin.math.max
import kotlin.math.min

class SelectionDeleteHandler(
    private val inputConnectionProvider: () -> InputConnection?,
    private val isComposingActive: () -> Boolean,
    private val finishComposing: () -> Unit,
    private val onDeletedText: (String) -> Unit
) {
    private var selectionDeleteAnchor: Int = -1
    private var currentSelectionPos: Int = -1

    fun onSwipeStart() {
        val ic = inputConnectionProvider() ?: return
        if (isComposingActive()) {
            finishComposing()
        }
        val before = ic.getTextBeforeCursor(10000, 0) ?: ""
        selectionDeleteAnchor = before.length
        currentSelectionPos = selectionDeleteAnchor
    }

    fun onSwipeUpdate(deltaCharsX: Int, deltaCharsY: Int) {
        val ic = inputConnectionProvider() ?: return
        if (selectionDeleteAnchor == -1) return

        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        val text = extracted?.text ?: ""
        
        var newPos: Int
        if (deltaCharsY > 0) {
            val nextNewLine = text.indexOf('\n', selectionDeleteAnchor)
            newPos = if (nextNewLine != -1) nextNewLine else text.length
        } else if (deltaCharsY < 0) {
            val prevNewLine = text.lastIndexOf('\n', selectionDeleteAnchor - 1)
            newPos = if (prevNewLine != -1) prevNewLine else 0
        } else {
            newPos = (selectionDeleteAnchor + deltaCharsX).coerceIn(0, text.length)
        }
        currentSelectionPos = newPos

        val start = min(selectionDeleteAnchor, currentSelectionPos)
        val end = max(selectionDeleteAnchor, currentSelectionPos)
        ic.setSelection(start, end)
    }

    fun onSwipeEnd() {
        val ic = inputConnectionProvider() ?: return
        if (selectionDeleteAnchor == -1) return

        if (selectionDeleteAnchor != currentSelectionPos) {
            val start = min(selectionDeleteAnchor, currentSelectionPos)
            val end = max(selectionDeleteAnchor, currentSelectionPos)
            
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
            val textToDelete = if (extracted != null && extracted.text != null) {
                val safeStart = min(start, extracted.text.length)
                val safeEnd = min(end, extracted.text.length)
                extracted.text.substring(safeStart, safeEnd)
            } else {
                null
            }

            // To actually delete the selection properly on all editors, we can also use finishComposingText and then commitText
            ic.finishComposingText()
            ic.commitText("", 1)

            if (!textToDelete.isNullOrEmpty()) {
                onDeletedText(textToDelete)
            }
        }
        selectionDeleteAnchor = -1
        currentSelectionPos = -1
    }
}
