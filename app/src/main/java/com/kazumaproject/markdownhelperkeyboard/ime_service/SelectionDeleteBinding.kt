package com.kazumaproject.markdownhelperkeyboard.ime_service

import com.kazumaproject.custom_keyboard.view.FlickKeyboardView
import android.view.inputmethod.InputConnection

class SelectionDeleteBinding(
    private val flickKeyboardView: FlickKeyboardView,
    private val inputConnectionProvider: () -> InputConnection?
) {
    private val helper = SelectionDeleteHelper(inputConnectionProvider) {
    }

    fun bind() {
        flickKeyboardView.selectionDeleteHandler = object : FlickKeyboardView.SelectionDeleteHandler {
            override fun onStart() {
                helper.startSelection()
            }

            override fun onUpdate(dx: Int, dy: Int) {
                helper.updateSelection(dx, dy)
            }

            override fun onCommit() {
                helper.commitDelete()
            }
        }
    }
}
