package com.kazumaproject.custom_keyboard.view

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import com.kazumaproject.custom_keyboard.data.KeyboardInputMode
import com.kazumaproject.custom_keyboard.layout.KeyboardDefaultLayouts
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlickKeyboardViewCursorGuideOverlayTest {

    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val host = FrameLayout(activity).also { activity.setContentView(it) }
    private lateinit var keyboardView: FlickKeyboardView

    @Before
    fun setUp() {
        val themedContext = ContextThemeWrapper(
            activity,
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
        keyboardView = FlickKeyboardView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(800, 600)
        }
        host.addView(keyboardView)
        val layout = KeyboardDefaultLayouts.createFinalLayout(
            KeyboardInputMode.HIRAGANA,
            emptyMap(),
            "flick",
            "default"
        )
        keyboardView.setKeyboard(layout)
        keyboardView.measure(
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
        )
        keyboardView.layout(0, 0, 800, 600)
    }

    @After
    fun tearDown() {
        host.removeAllViews()
        KeyboardDefaultLayouts.resetCursorSelectionHandlers()
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
    fun testFallbackToKeyboardDefaultLayoutsSelectionModeProvider() {
        keyboardView.setCursorMode(true)
        keyboardView.cursorSelectionModeProvider = null

        var globalSelectionActive = false
        KeyboardDefaultLayouts.isCursorSelectionModeProvider = { globalSelectionActive }

        assertFalse(keyboardView.isCursorSelectionActive())
        globalSelectionActive = true
        assertTrue("KeyboardDefaultLayouts provider should be used as fallback", keyboardView.isCursorSelectionActive())
    }

    @Test
    fun testDispatchDrawRendersWithoutCrashing() {
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
