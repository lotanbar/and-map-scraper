package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View

class StopButtonView(context: Context?) : View(context) {
    private val buttonRect = RectF()
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isPressed = false
    private var clickListener: (() -> Unit)? = null

    init {
        buttonTextPaint.color = Color.WHITE
        buttonTextPaint.textSize = 80f
        buttonTextPaint.textAlign = Paint.Align.CENTER
        buttonTextPaint.isFakeBoldText = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buttonRect.set(0f, 0f, w.toFloat(), h.toFloat())
        updateGradient()
    }

    private fun updateGradient() {
        if (buttonRect.width() > 0 && buttonRect.height() > 0) {
            val gradient = LinearGradient(
                0f,
                buttonRect.top,
                0f,
                buttonRect.bottom,
                if (isPressed) Color.rgb(139, 0, 0) else Color.rgb(220, 20, 60), // Crimson red
                if (isPressed) Color.rgb(100, 0, 0) else Color.rgb(178, 34, 34), // Darker red
                Shader.TileMode.CLAMP
            )
            buttonPaint.shader = gradient
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw rounded rectangle button
        canvas.drawRoundRect(buttonRect, CORNER_RADIUS.toFloat(), CORNER_RADIUS.toFloat(), buttonPaint)

        // Draw text
        val textY = buttonRect.centerY() + (buttonTextPaint.textSize / 2) - 10
        canvas.drawText("STOP", buttonRect.centerX(), textY, buttonTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                updateGradient()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                updateGradient()
                invalidate()
                if (buttonRect.contains(event.x, event.y)) {
                    clickListener?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                updateGradient()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setButtonClickListener(listener: () -> Unit) {
        clickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(BUTTON_WIDTH, BUTTON_HEIGHT)
    }

    companion object {
        private const val BUTTON_WIDTH = 500
        private const val BUTTON_HEIGHT = 250
        private const val CORNER_RADIUS = 40
    }
}
