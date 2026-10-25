package com.kazumaproject.core.ui.cursor

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.assertEquals
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
class CursorGuideOverlayRendererTest {

    private lateinit var activity: Activity
    private lateinit var renderer: CursorGuideOverlayRenderer
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        renderer = CursorGuideOverlayRenderer(activity)
        bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    @Test
    fun testDoNotDrawWhenCursorModeIsFalse() {
        renderer.drawOverlay(
            canvas = canvas,
            width = 800,
            height = 600,
            isCursorMode = false,
            isSelectionMode = false
        )
        assertTrue("When cursor mode is false, pillRect should remain empty", renderer.pillRect.isEmpty)
    }

    @Test
    fun testDoNotDrawWhenWidthOrHeightIsZeroOrNegative() {
        renderer.drawOverlay(
            canvas = canvas,
            width = 0,
            height = 600,
            isCursorMode = true,
            isSelectionMode = false
        )
        assertTrue("When width is 0, pillRect should remain empty", renderer.pillRect.isEmpty)

        renderer.drawOverlay(
            canvas = canvas,
            width = 800,
            height = -10,
            isCursorMode = true,
            isSelectionMode = false
        )
        assertTrue("When height is negative, pillRect should remain empty", renderer.pillRect.isEmpty)
    }

    @Test
    fun testDrawNormalCursorGuideOverlay() {
        val width = 800
        val height = 600
        renderer.drawOverlay(
            canvas = canvas,
            width = width,
            height = height,
            isCursorMode = true,
            isSelectionMode = false
        )

        assertFalse("PillRect should not be empty", renderer.pillRect.isEmpty)
        assertTrue("Pill top should be positive", renderer.pillRect.top > 0)
        assertTrue("Pill height should be at least 32dp", renderer.pillRect.height() >= 32f)

        // Pill should be horizontally centered
        val expectedCenterX = width / 2f
        val actualCenterX = (renderer.pillRect.left + renderer.pillRect.right) / 2f
        assertEquals(expectedCenterX, actualCenterX, 1f)
    }

    @Test
    fun testDrawSelectionModeCursorGuideOverlay() {
        val width = 800
        val height = 600
        renderer.drawOverlay(
            canvas = canvas,
            width = width,
            height = height,
            isCursorMode = true,
            isSelectionMode = true
        )

        assertFalse("PillRect should not be empty", renderer.pillRect.isEmpty)
        assertTrue("Pill top should be positive", renderer.pillRect.top > 0)
        assertTrue("Pill height should be at least 32dp", renderer.pillRect.height() >= 32f)

        // Pill should be horizontally centered
        val expectedCenterX = width / 2f
        val actualCenterX = (renderer.pillRect.left + renderer.pillRect.right) / 2f
        assertEquals(expectedCenterX, actualCenterX, 1f)
    }
}
