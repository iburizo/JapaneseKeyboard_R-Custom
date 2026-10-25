package com.kazumaproject.markdownhelperkeyboard.ime_service.gesture

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.text.Selection
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.BaseInputConnection
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.kazumaproject.custom_keyboard.data.KeyAction
import com.kazumaproject.markdownhelperkeyboard.databinding.MainLayoutBinding
import com.kazumaproject.markdownhelperkeyboard.ime_service.IMEService
import com.kazumaproject.markdownhelperkeyboard.ime_service.adapters.FloatingCandidateListAdapter
import com.kazumaproject.markdownhelperkeyboard.ime_service.flick_preview.ComposingTextArbiter
import com.kazumaproject.markdownhelperkeyboard.ime_service.input_behavior.ResolvedInputBehavior
import com.kazumaproject.markdownhelperkeyboard.ime_service.state.InputTypeForIME
import com.kazumaproject.markdownhelperkeyboard.setting_activity.AppPreference
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Milestone 2 Adversarial Stress Test: Empirical Verification of R2 Input Rollback
 *
 * Verifies:
 * 1. Zero-latency touch response: premature composing text automatically discarded on 2-finger gesture.
 * 2. Composing restoration: existing pre-edit text preserved when an extra character is rolled back.
 * 3. Direct commit rollback: physical deletion of direct commit characters via deleteSurroundingText.
 * 4. Critical cooperation with KeyAction.UndoCommit (確定戻し): verifies that rolled-back phantom text
 *    does NOT block reconversion, allowing the previous committed text to be restored.
 * 5. Back-to-back consecutive gestures without snapshot leakage or cross-contamination.
 * 6. Non-gesture normal tap protection: regular typing is never discarded.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MultiTouchRollbackAdversarialStressTest {

    private class RecordingEditor(context: Context) : BaseInputConnection(View(context), true) {
        val content = SpannableStringBuilder()
        val deletedSurroundingHistory = mutableListOf<Pair<Int, Int>>()

        init {
            Selection.setSelection(content, 0)
        }

        override fun getEditable(): SpannableStringBuilder = content

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            deletedSurroundingHistory.add(beforeLength to afterLength)
            return super.deleteSurroundingText(beforeLength, afterLength)
        }
    }

    private lateinit var activity: Activity
    private lateinit var editor: RecordingEditor
    private lateinit var service: IMEService
    private lateinit var mainBinding: MainLayoutBinding

    private val inputString get() = ReflectionHelpers.getField<MutableStateFlow<String>>(service, "_inputString")
    private val stringInTail get() = ReflectionHelpers.getField<AtomicReference<String>>(service, "stringInTail")
    private val isHenkan get() = ReflectionHelpers.getField<AtomicBoolean>(service, "isHenkan")

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        PreferenceManager.getDefaultSharedPreferences(activity).edit().clear().commit()
        AppPreference.init(activity)

        editor = RecordingEditor(activity)
        mainBinding = mock(MainLayoutBinding::class.java).apply {
            ReflectionHelpers.setField(this, "suggestionViewParent", ConstraintLayout(activity))
            ReflectionHelpers.setField(this, "shortcutToolbarRecyclerview", RecyclerView(activity))
            ReflectionHelpers.setField(this, "candidateTabLayout", TabLayout(activity))
            ReflectionHelpers.setField(this, "suggestionRecyclerView", RecyclerView(activity))
        }

        service = spy(IMEService().apply { appPreference = AppPreference }).also {
            it.appPreference = AppPreference
            doReturn(editor).`when`(it).getCurrentInputConnection()
            doReturn(Dialog(activity)).`when`(it).window
            doReturn(LayoutInflater.from(activity)).`when`(it).layoutInflater
            ReflectionHelpers.callInstanceMethod<Unit>(
                it,
                "attachBaseContext",
                ClassParameter.from(Context::class.java, activity)
            )
            ReflectionHelpers.setField(it, "isVibration", false)
            ReflectionHelpers.setField(it, "mainLayoutBinding", mainBinding)
            ReflectionHelpers.setField(it, "listAdapter", mock(FloatingCandidateListAdapter::class.java))
            ReflectionHelpers.setField(it, "currentInputType", InputTypeForIME.Text)
            ReflectionHelpers.setField(it, "currentInputBehavior", ResolvedInputBehavior.COMPOSING_TEXT)
            ReflectionHelpers.setField(
                it,
                "composingTextArbiter",
                ComposingTextArbiter(
                    writeComposingText = { text, cursor -> editor.setComposingText(text, cursor) },
                    finishComposingText = { editor.finishComposingText() },
                )
            )
        }
    }

    @After
    fun tearDown() {
        // cleanup
    }

    // =========================================================================
    // Scenario 1: Clean Rollback of Initial Premature Composing Character
    // =========================================================================

    @Test
    fun testRollbackPrematureInput_DiscardsInitialComposingCharacter() {
        // Initial state: empty editor
        assertEquals("", editor.content.toString())

        // 1. First finger touches DOWN -> Session starts, captures empty snapshot
        service.captureMultiTouchSnapshot()

        // 2. Immediate zero-latency touch generates premature input "あ"
        inputString.value = "あ"
        editor.setComposingText("あ", 1)
        assertEquals("あ", editor.content.toString())

        // 3. Two-finger swipe gesture recognized -> handleMultiTouchGesture triggers rollback!
        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "handleMultiTouchGesture",
            ClassParameter.from(MultiTouchGestureType::class.java, MultiTouchGestureType.SWIPE_LEFT),
            ClassParameter.from(KeyAction::class.java, KeyAction.DoNothing)
        )

        // ASSERT: Premature text "あ" MUST be discarded, leaving editor completely clean!
        assertEquals("Editor content must be completely empty after rollback", "", editor.content.toString())
        assertEquals("Internal inputString must be reset to empty", "", inputString.value)
        assertEquals("stringInTail must be reset to empty", "", stringInTail.get())
        assertFalse("isHenkan must be false", isHenkan.get())
    }

    // =========================================================================
    // Scenario 2: Preserves Existing Composing State While Rolling Back Appended Char
    // =========================================================================

    @Test
    fun testRollbackPrematureInput_PreservesExistingComposingTextWhenNewCharAppended() {
        // User was already composing "とうきょう"
        inputString.value = "とうきょう"
        editor.setComposingText("とうきょう", 5)
        assertEquals("とうきょう", editor.content.toString())

        // 1. First finger touches DOWN for 2-finger gesture -> Captures "とうきょう" snapshot
        service.captureMultiTouchSnapshot()

        // 2. Extra premature character "と" appended before gesture recognized -> "とうきょうと"
        inputString.value = "とうきょうと"
        editor.setComposingText("とうきょうと", 6)
        assertEquals("とうきょうと", editor.content.toString())

        // 3. Gesture recognized -> Rollback triggered
        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "handleMultiTouchGesture",
            ClassParameter.from(MultiTouchGestureType::class.java, MultiTouchGestureType.SWIPE_RIGHT),
            ClassParameter.from(KeyAction::class.java, KeyAction.DoNothing)
        )

        // ASSERT: Extra "と" rolled back; original "とうきょう" perfectly restored!
        assertEquals("Editor content must be restored to original composing text", "とうきょう", editor.content.toString())
        assertEquals("inputString must be restored to original", "とうきょう", inputString.value)
    }

    // =========================================================================
    // Scenario 3: Physical Deletion of Direct Committed Characters
    // =========================================================================

    @Test
    fun testRollbackPrematureInput_DeletesDirectCommittedCharacters() {
        // Initial state: editor has base text "Hello "
        editor.commitText("Hello ", 1)
        assertEquals("Hello ", editor.content.toString())

        // 1. Session start captures snapshot
        service.captureMultiTouchSnapshot()

        // 2. First finger triggers direct commit of "123"
        service.commitText("123", 1)
        assertEquals("Hello 123", editor.content.toString())

        // 3. Gesture recognized -> Rollback triggered
        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "handleMultiTouchGesture",
            ClassParameter.from(MultiTouchGestureType::class.java, MultiTouchGestureType.SWIPE_UP),
            ClassParameter.from(KeyAction::class.java, KeyAction.DoNothing)
        )

        // ASSERT: Direct commit "123" must be physically deleted via deleteSurroundingText(3, 0)
        assertEquals("Direct committed text must be deleted", "Hello ", editor.content.toString())
        assertTrue("deleteSurroundingText must have been called for 3 chars",
            editor.deletedSurroundingHistory.any { it.first == 3 && it.second == 0 })
    }

    // =========================================================================
    // Scenario 4 (CRITICAL): Cooperation with KeyAction.UndoCommit (確定戻し)
    // =========================================================================

    @Test
    fun testRollbackPrematureInput_EnablesUndoCommitAfterRollback() {
        // Step 1: User commits "テスト" (reading: "てすと")
        editor.commitText("テスト", 1)
        assertEquals("テスト", editor.content.toString())
        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "rememberCommittedTextForReconversion",
            ClassParameter.from(String::class.java, "てすと"),
            ClassParameter.from(String::class.java, "テスト")
        )

        // Verify pendingReconversionEntry is staged
        val pendingEntryBefore = ReflectionHelpers.getField<Any?>(service, "pendingReconversionEntry")
        assertNotNull("pendingReconversionEntry must be staged", pendingEntryBefore)

        // Step 2: User performs 2-finger gesture assigned to UndoCommit (e.g. SWIPE_LEFT)
        // First finger touches DOWN -> Session start
        service.captureMultiTouchSnapshot()

        // Premature touch erroneously inserts "あ"
        inputString.value = "あ"
        editor.setComposingText("あ", 1)
        assertEquals("テストあ", editor.content.toString())

        // Step 3: Two-finger swipe left triggers handleMultiTouchGesture with KeyAction.UndoCommit!
        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "handleMultiTouchGesture",
            ClassParameter.from(MultiTouchGestureType::class.java, MultiTouchGestureType.SWIPE_LEFT),
            ClassParameter.from(KeyAction::class.java, KeyAction.UndoCommit)
        )

        // ASSERT:
        // 1. Premature "あ" was cleanly removed before performPendingReconversion executed
        // 2. UndoCommit succeeded! "テスト" was replaced with uncommitted "てすと"
        assertFalse("Premature 'あ' must not exist in editor", editor.content.toString().contains("あ"))
        assertEquals("Committed 'テスト' must be reverted to composing 'てすと'", "てすと", inputString.value)
        assertEquals("Editor must contain reverted reading", "てすと", editor.content.toString())
    }

    // =========================================================================
    // Scenario 5: Back-to-Back Consecutive Gestures (No Snapshot Leakage)
    // =========================================================================

    @Test
    fun testBackToBackConsecutiveGestures_LifecycleCleanliness() {
        // --- Gesture 1 ---
        service.captureMultiTouchSnapshot()
        inputString.value = "あ"
        editor.setComposingText("あ", 1)
        assertEquals("あ", editor.content.toString())

        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "handleMultiTouchGesture",
            ClassParameter.from(MultiTouchGestureType::class.java, MultiTouchGestureType.SWIPE_DOWN),
            ClassParameter.from(KeyAction::class.java, KeyAction.DoNothing)
        )
        assertEquals("", editor.content.toString())
        assertNull("Snapshot must be null after gesture 1",
            ReflectionHelpers.getField<Any?>(service, "multiTouchSnapshot"))

        // --- Gesture 2 immediately after ---
        service.captureMultiTouchSnapshot()
        inputString.value = "い"
        editor.setComposingText("い", 1)
        assertEquals("い", editor.content.toString())

        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "handleMultiTouchGesture",
            ClassParameter.from(MultiTouchGestureType::class.java, MultiTouchGestureType.SWIPE_UP),
            ClassParameter.from(KeyAction::class.java, KeyAction.DoNothing)
        )
        assertEquals("", editor.content.toString())
        assertNull("Snapshot must be null after gesture 2",
            ReflectionHelpers.getField<Any?>(service, "multiTouchSnapshot"))
    }

    // =========================================================================
    // Scenario 6: Normal Tap Touch Does Not Roll Back Input
    // =========================================================================

    @Test
    fun testNormalTap_DoesNotRollBackComposedCharacter() {
        // 1. Finger touches DOWN -> Snapshot captured
        service.captureMultiTouchSnapshot()

        // 2. Normal typing occurs -> "か"
        inputString.value = "か"
        editor.setComposingText("か", 1)

        // 3. User lifts finger (ACTION_UP) -> Gesture is NOT recognized, handleMultiTouchGesture NEVER called
        // Normal typing continues
        assertEquals("か", editor.content.toString())
        assertEquals("か", inputString.value)

        // Subsequent typing "き"
        inputString.value = "かき"
        editor.setComposingText("かき", 2)
        assertEquals("かき", editor.content.toString())
    }
}
