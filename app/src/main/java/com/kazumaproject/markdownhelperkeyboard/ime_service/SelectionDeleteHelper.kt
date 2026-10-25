package com.kazumaproject.markdownhelperkeyboard.ime_service

import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import kotlin.math.max
import kotlin.math.min

internal data class LineRange(val start: Int, val end: Int) {
    val length: Int get() = end - start
}

class SelectionDeleteHandler(
    private val inputConnectionProvider: () -> InputConnection?,
    private val isComposingActive: () -> Boolean,
    private val finishComposing: () -> Unit,
    private val onDeletedText: (String) -> Unit
) {
    private var selectionDeleteAnchor: Int = -1
    private var initialDesiredColumn: Int = -1

    fun onSwipeStart() {
        val ic = inputConnectionProvider() ?: return
        if (isComposingActive()) {
            finishComposing()
        }
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        val text = extracted?.text ?: ""
        val start = extracted?.selectionStart ?: -1
        if (start >= 0) {
            val anchor = start.coerceIn(0, text.length)
            selectionDeleteAnchor = anchor
            val lines = parseLines(text)
            val lineIdx = findLineIndex(lines, anchor)
            initialDesiredColumn = (anchor - lines[lineIdx].start).coerceIn(0, lines[lineIdx].length)
        } else {
            selectionDeleteAnchor = -1
            initialDesiredColumn = -1
        }
    }

    fun onSwipeUpdate(deltaCharsX: Int, deltaCharsY: Int) {
        val ic = inputConnectionProvider() ?: return
        if (selectionDeleteAnchor == -1) return
        
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0) ?: return
        val text = extracted.text ?: ""
        if (text.isEmpty()) return

        val anchor = selectionDeleteAnchor.coerceIn(0, text.length)
        val lines = parseLines(text)
        val anchorLineIdx = findLineIndex(lines, anchor)

        val desiredCol = if (initialDesiredColumn >= 0) {
            initialDesiredColumn
        } else {
            (anchor - lines[anchorLineIdx].start).coerceIn(0, lines[anchorLineIdx].length)
        }

        val rawTargetLineIdx = anchorLineIdx + deltaCharsY
        val verticalPos = when {
            rawTargetLineIdx < 0 -> 0
            rawTargetLineIdx >= lines.size -> text.length
            else -> {
                val targetLine = lines[rawTargetLineIdx]
                val targetColumn = min(desiredCol, targetLine.length)
                targetLine.start + targetColumn
            }
        }

        val newPos = (verticalPos + deltaCharsX).coerceIn(0, text.length)
        ic.setSelection(newPos, anchor)
    }

    private fun parseLines(text: CharSequence): List<LineRange> {
        val lines = ArrayList<LineRange>()
        var start = 0
        val len = text.length
        while (start <= len) {
            val nextNewline = text.indexOf('\n', start)
            if (nextNewline == -1) {
                lines.add(LineRange(start, len))
                break
            } else {
                lines.add(LineRange(start, nextNewline))
                start = nextNewline + 1
            }
        }
        if (lines.isEmpty()) {
            lines.add(LineRange(0, 0))
        }
        return lines
    }

    private fun findLineIndex(lines: List<LineRange>, pos: Int): Int {
        for (i in lines.indices) {
            val line = lines[i]
            if (pos in line.start..line.end) {
                return i
            }
        }
        return lines.lastIndex.coerceAtLeast(0)
    }

    fun calculateSelectionIndices(text: CharSequence, rawStart: Int, rawEnd: Int): Pair<Int, Int> {
        val textLength = text.length
        val cursorPosition = rawStart.coerceIn(0, textLength)
        val targetIndex = rawEnd.coerceIn(0, textLength)
        val start = min(cursorPosition, targetIndex)
        val end = max(cursorPosition, targetIndex)
        return Pair(start, end)
    }

    fun onSwipeEnd() {
        val ic = inputConnectionProvider() ?: return
        if (selectionDeleteAnchor == -1) return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null && extracted.selectionStart != extracted.selectionEnd) {
            val text = extracted.text ?: ""
            val (start, end) = calculateSelectionIndices(text, extracted.selectionStart, extracted.selectionEnd)
            if (start < end) {
                val deletedText = text.substring(start, end)
                ic.commitText("", 1)
                if (deletedText.isNotEmpty()) {
                    onDeletedText(deletedText)
                }
            }
        }
        selectionDeleteAnchor = -1
        initialDesiredColumn = -1
    }
}
