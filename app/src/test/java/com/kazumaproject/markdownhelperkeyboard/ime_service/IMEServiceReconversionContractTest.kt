package com.kazumaproject.markdownhelperkeyboard.ime_service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IMEServiceReconversionContractTest {

    @Test
    fun finishInputEnterKeyRemembersCommittedTextForReconversion() {
        val function = imeServiceSource().functionBody(
            start = "private fun finishInputEnterKey()",
            end = "private fun handleDeleteAfterCursor()"
        )

        assertTrue(function.contains("val committedText = inputString.value + stringInTail.get()"))
        assertTrue(function.contains("rememberCommittedTextForReconversion("))
        assertTrue(function.contains("reading = committedText"))
        assertTrue(function.contains("committedText = committedText"))
        assertTrue(function.contains("rememberZeroQueryKeyAfterCommit(committedText)"))
        assertTrue(function.contains("consumePendingZeroQueryAfterCommit()"))
    }

    @Test
    fun performPendingReconversionWorksRegardlessOfCandidateBarValidationFlag() {
        val function = imeServiceSource().functionBody(
            start = "private fun performPendingReconversion()",
            end = "private fun clearDeletedBuffer()"
        )

        // pendingReconversionValid 単体による早期リターンで特殊キーが弾かれないことを検証
        assertFalse(function.contains("if (!pendingReconversionValid)"))
        assertTrue(function.contains("deleteCommittedTextBeforeCursor(entry.committedText)"))
        assertTrue(function.contains("restoreReadingToPreEdit(entry.reading, mainView)"))
        assertTrue(function.contains("clearPendingReconversionEntry()"))
    }

    @Test
    fun undoCommitActionInvokesPendingReconversion() {
        val source = imeServiceSource()
        assertTrue(source.contains("KeyAction.UndoCommit -> { performPendingReconversion() }"))
    }

    private fun imeServiceSource(): String =
        listOf(
            File("app/src/main/java/com/kazumaproject/markdownhelperkeyboard/ime_service/IMEService.kt"),
            File("src/main/java/com/kazumaproject/markdownhelperkeyboard/ime_service/IMEService.kt")
        ).first { it.isFile }.readText()

    private fun String.functionBody(start: String, end: String): String {
        val startIndex = indexOf(start)
        require(startIndex >= 0) { "Missing start marker: $start" }
        val endIndex = indexOf(end, startIndex + start.length)
        require(endIndex >= 0) { "Missing end marker: $end" }
        return substring(startIndex, endIndex)
    }
}
