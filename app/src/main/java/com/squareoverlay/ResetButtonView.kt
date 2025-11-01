package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color

class ResetButtonView(context: Context?) :
    BaseButtonView(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS) {

    init {
        buttonTextPaint.textSize = 70f
    }

    override val gradientStartColor: Int
        get() = Color.rgb(255, 152, 0) // Light orange

    override val gradientEndColor: Int
        get() = Color.rgb(230, 120, 0) // Darker orange

    override val pressedColor: Int
        get() = Color.rgb(200, 100, 0) // Darker orange when pressed

    override fun drawButtonContent(canvas: Canvas?) {
        canvas?.let {
            val textY = buttonRect.centerY() + (buttonTextPaint.textSize / 2) - 10
            it.drawText("R", buttonRect.centerX(), textY, buttonTextPaint)
        }
    }

    companion object {
        private const val BUTTON_WIDTH = 200
        private const val BUTTON_HEIGHT = 200
        private const val CORNER_RADIUS = 30
    }
}
