package com.kazumaproject.qwerty_keyboard.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class QWERTYKeyboardViewCursorGuideOverlayTest {

    private lateinit var keyboardView: QWERTYKeyboardView

    @Before
    fun setUp() {
        val baseContext = RuntimeEnvironment.getApplication()
        val context = ContextThemeWrapper(
            baseContext,
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
        keyboardView = QWERTYKeyboardView(context).apply {
            measure(800, 600)
            layout(0, 0, 800, 600)
        }
    }

    @Test
    fun testOverlayVisibilityTogglesWithCursorMode() {
        assertFalse("Initially overlay should be invisible", keyboardView.isCursorGuideOverlayVisible())

        keyboardView.setCursorMode(true)
        assertTrue("When cursor mode enabled, overlay should be visible", keyboardView.isCursorGuideOverlayVisible())

        keyboardView.setCursorMode(false)
        assertFalse("When cursor mode disabled, overlay should be cleanly dismissed", keyboardView.isCursorGuideOverlayVisible())
    }

    @Test
    fun testSelectionModeProviderReflectedInCursorSelectionActive() {
        keyboardView.setCursorMode(true)
        assertFalse("Initially selection mode should be inactive", keyboardView.isCursorSelectionActive())

        var selectionActive = false
        keyboardView.cursorSelectionModeProvider = { selectionActive }

        assertFalse(keyboardView.isCursorSelectionActive())
        selectionActive = true
        assertTrue("cursorSelectionModeProvider state should be reflected", keyboardView.isCursorSelectionActive())
    }

    @Test
    fun testDrawRendersWithoutCrashing() {
        keyboardView.setCursorMode(true)
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw in normal cursor mode
        keyboardView.draw(canvas)

        // Draw in selection mode
        keyboardView.cursorSelectionModeProvider = { true }
        keyboardView.draw(canvas)
    }
}
