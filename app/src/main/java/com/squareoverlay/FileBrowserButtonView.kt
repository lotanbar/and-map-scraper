package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color

class FileBrowserButtonView(context: Context?) :
    BaseButtonView(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS) {

    init {
        buttonTextPaint.textSize = 50f
    }

    override val gradientStartColor: Int
        get() = Color.rgb(156, 39, 176) // Light purple

    override val gradientEndColor: Int
        get() = Color.rgb(123, 31, 162) // Darker purple

    override val pressedColor: Int
        get() = Color.rgb(100, 50, 150) // Darker purple when pressed

    override fun drawButtonContent(canvas: Canvas?) {
        canvas?.let {
            val textY = buttonRect.centerY() + (buttonTextPaint.textSize / 2) - 10
            it.drawText("📁", buttonRect.centerX(), textY, buttonTextPaint)
        }
    }

    companion object {
        private const val BUTTON_WIDTH = 200
        private const val BUTTON_HEIGHT = 200
        private const val CORNER_RADIUS = 30
    }
}
