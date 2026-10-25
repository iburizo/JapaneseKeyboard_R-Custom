package com.kazumaproject.markdownhelperkeyboard.ime_service.gesture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MultiTouchGestureRollbackContractTest {

    @Test
    fun handleMultiTouchGestureInvokesRollbackBeforeActionExecution() {
        val function = imeServiceSource().functionBody(
            start = "private fun handleMultiTouchGesture(type: MultiTouchGestureType, action: KeyAction) {",
            end = "private fun loadMultiTouchGestureSettings(): MultiTouchGestureSettings {"
        )

        val rollbackIndex = function.indexOf("rollbackPrematureInput()")
        val vibrateIndex = function.indexOf("vibrate()")
        val whenActionIndex = function.indexOf("when (action) {")

        assertTrue("rollbackPrematureInput() must be called in handleMultiTouchGesture", rollbackIndex >= 0)
        assertTrue("rollback must precede vibrate()", rollbackIndex < vibrateIndex)
        assertTrue("rollback must precede when (action)", rollbackIndex < whenActionIndex)
    }

    @Test
    fun rollbackPrematureInputDiscardsOrRestoresComposingState() {
        val function = imeServiceSource().functionBody(
            start = "private fun rollbackPrematureInput() {",
            end = "private fun discardAllComposingState() {"
        )

        assertTrue("Must check directCommittedTextCount and deleteSurroundingText",
            function.contains("snapshot.directCommittedTextCount > 0") &&
            function.contains("deleteSurroundingText(snapshot.directCommittedTextCount, 0)")
        )
        assertTrue("Must cancel flick preview", function.contains("flickInputPreviewCoordinator.cancel(restore = false)"))
        assertTrue("Must branch on empty initial snapshot to discard or restore",
            function.contains("discardAllComposingState()") &&
            function.contains("restoreComposingState(snapshot)")
        )
        assertTrue("Must stop long press gestures",
            function.contains("stopAllOngoingKeyLongPresses()") &&
            function.contains("cancelLeftLongPress()") &&
            function.contains("cancelRightLongPress()") &&
            function.contains("stopDeleteLongPress()")
        )
    }

    @Test
    fun onMultiTouchSessionStartListenerCapturesSnapshot() {
        val source = imeServiceSource()
        assertTrue("Main layout root must bind onMultiTouchSessionStartListener",
            source.contains("binding.root.onMultiTouchSessionStartListener = {") &&
            source.contains("captureMultiTouchSnapshot()")
        )
        assertTrue("Floating layout root must bind onMultiTouchSessionStartListener",
            source.contains("floatingKeyboardLayoutBinding.root.onMultiTouchSessionStartListener = {")
        )
    }

    @Test
    fun commitTextAdvancesDirectCommittedTextCountDuringActiveGesture() {
        val function = imeServiceSource().functionBody(
            start = "override fun commitText(p0: CharSequence?, p1: Int): Boolean {",
            end = "override fun commitCompletion(p0: CompletionInfo?): Boolean {"
        )

        assertTrue("commitText must advance directCommittedTextCount if snapshot active",
            function.contains("multiTouchSnapshot?.let { snapshot ->") &&
            function.contains("snapshot.directCommittedTextCount += (p0?.length ?: 0)")
        )
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
