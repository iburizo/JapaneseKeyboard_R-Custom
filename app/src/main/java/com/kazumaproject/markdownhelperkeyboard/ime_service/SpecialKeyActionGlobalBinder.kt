package com.kazumaproject.markdownhelperkeyboard.ime_service

import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts
import com.kazumaproject.markdownhelperkeyboard.converter.utility.DialTimeDateConverter

object SpecialKeyActionGlobalBinder {
    fun bind(
        performReconversion: () -> Unit,
        getComposingText: () -> String,
        commitTextAndClear: (String) -> Unit
    ) {
        KeyboardDefaultLayouts.undoCommitHandler = {
            performReconversion()
        }
        KeyboardDefaultLayouts.commitDialNumbersHandler = {
            val text = getComposingText()
            if (text.isNotEmpty()) {
                val sb = StringBuilder()
                for (ch in text) {
                    val converted = DialTimeDateConverter.toDialNumbers(ch.toString())
                    if (converted != null) {
                        sb.append(converted)
                    } else {
                        sb.append(ch)
                    }
                }
                commitTextAndClear(sb.toString())
            }
        }
    }
}
