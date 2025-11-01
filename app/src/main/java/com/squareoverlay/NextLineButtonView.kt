package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color

class NextLineButtonView(context: Context?) :
    BaseButtonView(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS) {

    init {
        buttonTextPaint.textSize = 50f
    }

    override val gradientStartColor: Int
        get() = Color.rgb(33, 150, 243) // Light blue

    override val gradientEndColor: Int
        get() = Color.rgb(0, 120, 200) // Darker blue

    override val pressedColor: Int
        get() = Color.rgb(0, 100, 150) // Darker blue when pressed

    override fun drawButtonContent(canvas: Canvas?) {
        canvas?.let {
            val textY = buttonRect.centerY() + (buttonTextPaint.textSize / 2) - 10
            it.drawText("↓", buttonRect.centerX(), textY, buttonTextPaint)
        }
    }

    companion object {
        private const val BUTTON_WIDTH = 280
        private const val BUTTON_HEIGHT = 200
        private const val CORNER_RADIUS = 30
    }
}
