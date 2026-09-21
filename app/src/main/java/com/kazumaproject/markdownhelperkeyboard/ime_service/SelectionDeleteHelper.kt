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

    fun onSwipeStart() {
        val ic = inputConnectionProvider() ?: return
        if (isComposingActive()) {
            finishComposing()
        }
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        selectionDeleteAnchor = extracted?.selectionStart ?: -1
    }

    fun onSwipeUpdate(deltaCharsX: Int, deltaCharsY: Int) {
        val ic = inputConnectionProvider() ?: return
        if (selectionDeleteAnchor == -1) return
        
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0) ?: return
        val text = extracted.text ?: ""
        
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
        
        ic.setSelection(newPos, selectionDeleteAnchor)
    }

    fun onSwipeEnd() {
        val ic = inputConnectionProvider() ?: return
        if (selectionDeleteAnchor == -1) return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null && extracted.selectionStart != extracted.selectionEnd) {
            val start = min(extracted.selectionStart, extracted.selectionEnd)
            val end = max(extracted.selectionStart, extracted.selectionEnd)
            val deletedText = extracted.text?.substring(start, end)
            
            ic.commitText("", 1)
            
            if (!deletedText.isNullOrEmpty()) {
                onDeletedText(deletedText)
            }
        }
        selectionDeleteAnchor = -1
    }
}
