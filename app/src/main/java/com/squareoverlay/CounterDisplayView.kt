package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class CounterDisplayView(context: Context?) : View(context) {
    private val backgroundPaint: Paint
    private val textPaint: Paint
    private val backgroundRect: RectF
    private var counterValue = 0
    private var isPressed = false

    fun interface OnClickListener {
        fun onClick()
    }

    private var onClickListener: OnClickListener? = null

    init {
        setWillNotDraw(false)

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

        // Change background color when pressed
        if (isPressed) {
            backgroundPaint.setColor(Color.argb(255, 40, 40, 40))
        } else {
            backgroundPaint.setColor(Color.argb(220, 0, 0, 0))
        }

        backgroundRect.set(0f, 0f, getWidth().toFloat(), getHeight().toFloat())
        canvas.drawRoundRect(backgroundRect, 20f, 20f, backgroundPaint)

        val text = counterValue.toString() + "px"
        val textY = centerY + (textPaint.getTextSize() / 2) - 5
        canvas.drawText(text, centerX, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                isPressed = false
                invalidate()
                if (onClickListener != null) {
                    onClickListener!!.onClick()
                }
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun setCounterClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    companion object {
        private const val WIDTH = 300
        private const val HEIGHT = 140
    }
}
