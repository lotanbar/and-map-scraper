package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color

class ScreenshotButtonView(context: Context?) :
    BaseButtonView(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS) {

    init {
        buttonTextPaint.textSize = 60f
    }

    override val gradientStartColor: Int
        get() = Color.rgb(76, 175, 80) // Light green

    override val gradientEndColor: Int
        get() = Color.rgb(56, 142, 60) // Darker green

    override val pressedColor: Int
        get() = Color.rgb(34, 139, 34) // Darker green when pressed

    override fun drawButtonContent(canvas: Canvas?) {
        canvas?.let {
            val textY = buttonRect.centerY() + (buttonTextPaint.textSize / 2) - 10
            it.drawText("📷", buttonRect.centerX(), textY, buttonTextPaint)
        }
    }

    companion object {
        private const val BUTTON_WIDTH = 280
        private const val BUTTON_HEIGHT = 200
        private const val CORNER_RADIUS = 30
    }
}
