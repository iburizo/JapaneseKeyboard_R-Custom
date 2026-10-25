package com.kazumaproject.core.ui.cursor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.appcompat.R as AppCompatR
import androidx.core.graphics.ColorUtils
import com.kazumaproject.core.R
import com.kazumaproject.core.domain.extensions.getThemeColorOrFallback
import com.kazumaproject.core.domain.extensions.isDarkThemeOn

/**
 * カーソルナビゲーションモード中の半透明ガイドオーバーレイおよびピルバッジを描画するレンダラー。
 */
class CursorGuideOverlayRenderer(private val context: Context) {

    companion object {
        private const val TAG = "CursorGuideOverlay"
        private const val DEFAULT_ACCENT_COLOR = 0xFF1976D2.toInt()
    }

    private val density: Float
        get() = context.resources.displayMetrics.density

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val pillRect = RectF()

    /**
     * ガイドオーバーレイを描画する。
     *
     * @param canvas 描画先Canvas
     * @param width キーボードViewの幅
     * @param height キーボードViewの高さ
     * @param isCursorMode カーソル移動モード中か
     * @param isSelectionMode 範囲選択モード中か
     */
    fun drawOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        isCursorMode: Boolean,
        isSelectionMode: Boolean
    ) {
        if (!isCursorMode || width <= 0 || height <= 0) return

        try {
            val isDark = runCatching { context.isDarkThemeOn() }.getOrDefault(false)
            val accentColor = runCatching {
                context.getThemeColorOrFallback(
                    AppCompatR.attr.colorPrimary,
                    DEFAULT_ACCENT_COLOR
                )
            }.getOrDefault(DEFAULT_ACCENT_COLOR)

            val currentDensity = density

            // 1. 半透明スクリム (Keyboard Scrim)
            if (isSelectionMode) {
                scrimPaint.color = ColorUtils.setAlphaComponent(accentColor, 38)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

                val topBarHeight = 3f * currentDensity
                scrimPaint.color = accentColor
                canvas.drawRect(0f, 0f, width.toFloat(), topBarHeight, scrimPaint)
            } else {
                val scrimColor = if (isDark) 0x4D000000 else 0x33000000
                scrimPaint.color = scrimColor
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
            }

            // 2. ピルバッジ (Pill Badge)
            val text = if (isSelectionMode) {
                context.getString(R.string.cursor_guide_selecting_range)
            } else {
                context.getString(R.string.cursor_guide_tap_to_select)
            }

            val textSizePx = 13f * currentDensity
            textPaint.textSize = textSizePx
            textPaint.isFakeBoldText = true

            val textWidth = textPaint.measureText(text)
            val pillPaddingHorizontal = 16f * currentDensity
            val pillPaddingVertical = 8f * currentDensity
            val dotRadius = 3.5f * currentDensity
            val dotSpacing = 8f * currentDensity
            val dotTotalWidth = (dotRadius * 2) + dotSpacing

            val pillWidth = textWidth + (pillPaddingHorizontal * 2) + dotTotalWidth
            val pillHeight = (textSizePx + (pillPaddingVertical * 2)).coerceAtLeast(32f * currentDensity)

            val pillLeft = (width - pillWidth) / 2f
            val pillTop = 14f * currentDensity
            val pillRight = pillLeft + pillWidth
            val pillBottom = pillTop + pillHeight
            val cornerRadius = pillHeight / 2f

            pillRect.set(pillLeft, pillTop, pillRight, pillBottom)

            if (isSelectionMode) {
                pillBgPaint.color = accentColor
                pillStrokePaint.color = 0x80FFFFFF.toInt()
                pillStrokePaint.strokeWidth = 1.2f * currentDensity
                dotPaint.color = 0xFFFFFFFF.toInt()
                textPaint.color = 0xFFFFFFFF.toInt()
            } else {
                pillBgPaint.color = if (isDark) 0xDD2A2A2A.toInt() else 0xEE333333.toInt()
                pillStrokePaint.color = 0x4DFFFFFF.toInt()
                pillStrokePaint.strokeWidth = 1f * currentDensity
                dotPaint.color = 0xCCFFFFFF.toInt()
                textPaint.color = 0xFFFFFFFF.toInt()
            }

            canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillBgPaint)
            canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillStrokePaint)

            val dotCenterX = pillLeft + pillPaddingHorizontal + dotRadius
            val dotCenterY = pillTop + (pillHeight / 2f)
            canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotPaint)

            val textCenterX = pillLeft + pillPaddingHorizontal + dotTotalWidth + (textWidth / 2f)
            val fontMetrics = textPaint.fontMetrics
            val textBaselineY = pillTop + (pillHeight - fontMetrics.bottom + fontMetrics.top) / 2f - fontMetrics.top
            canvas.drawText(text, textCenterX, textBaselineY, textPaint)
        } catch (t: Throwable) {
            Log.w(TAG, "Error drawing cursor guide overlay, safely ignored", t)
        }
    }
}
