package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class CounterDisplayView(context: Context?) : View(context) {
    private val backgroundPaint: Paint
    private val textPaint: Paint
    private val backgroundRect: RectF
    private var counterValue = 0

    init {
        backgroundPaint = Paint()
        backgroundPaint.setColor(Color.argb(220, 0, 0, 0))
        backgroundPaint.setStyle(Paint.Style.FILL)
        backgroundPaint.setAntiAlias(true)

        textPaint = Paint()
        textPaint.setColor(Color.WHITE)
        textPaint.setTextSize(50f)
        textPaint.setAntiAlias(true)
        textPaint.setTextAlign(Paint.Align.CENTER)
        textPaint.setFakeBoldText(true)

        backgroundRect = RectF()
    }

    fun setCounter(value: Int) {
        this.counterValue = value
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(WIDTH, HEIGHT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = getWidth() / 2f
        val centerY = getHeight() / 2f

        backgroundRect.set(0f, 0f, getWidth().toFloat(), getHeight().toFloat())
        canvas.drawRoundRect(backgroundRect, 20f, 20f, backgroundPaint)

        val text = counterValue.toString() + "px"
        val textY = centerY + (textPaint.getTextSize() / 2) - 5
        canvas.drawText(text, centerX, textY, textPaint)
    }

    companion object {
        private const val WIDTH = 300
        private const val HEIGHT = 140
    }
}
