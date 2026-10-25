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
        if (text.isEmpty()) return

        val anchor = selectionDeleteAnchor.coerceIn(0, text.length)
        val lineMovedPos = moveByLines(text, anchor, deltaCharsY)
        val newPos = (lineMovedPos + deltaCharsX).coerceIn(0, text.length)
        
        ic.setSelection(newPos, anchor)
    }

    private fun moveByLines(text: CharSequence, startPos: Int, deltaLines: Int): Int {
        if (deltaLines == 0 || text.isEmpty()) return startPos
        var currentPos = startPos.coerceIn(0, text.length)
        if (deltaLines < 0) {
            repeat(-deltaLines) {
                val prevNewLine = text.lastIndexOf('\n', (currentPos - 1).coerceAtLeast(0))
                currentPos = if (prevNewLine == -1) {
                    0
                } else {
                    prevNewLine
                }
            }
        } else {
            repeat(deltaLines) {
                val nextNewLine = text.indexOf('\n', currentPos)
                currentPos = if (nextNewLine == -1) {
                    text.length
                } else {
                    nextNewLine + 1
                }
            }
        }
        return currentPos
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
