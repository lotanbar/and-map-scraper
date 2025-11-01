package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class ScrollIncrementInputView(context: Context?) : View(context) {
    private val backgroundPaint: Paint
    private val textPaint: Paint
    private val iconPaint: Paint
    private val backgroundRect: RectF
    private var currentValue = 30
    private var isPressed = false

    fun interface OnClickListener {
        fun onClick()
    }

    private var onClickListener: OnClickListener? = null

    init {
        setWillNotDraw(false)

        backgroundPaint = Paint()
        backgroundPaint.setColor(Color.argb(220, 50, 50, 50))
        backgroundPaint.setStyle(Paint.Style.FILL)
        backgroundPaint.setAntiAlias(true)

        textPaint = Paint()
        textPaint.setColor(Color.WHITE)
        textPaint.setTextSize(32f)
        textPaint.setAntiAlias(true)
        textPaint.setTextAlign(Paint.Align.CENTER)
        textPaint.setFakeBoldText(true)

        iconPaint = Paint()
        iconPaint.setColor(Color.WHITE)
        iconPaint.setTextSize(50f)
        iconPaint.setAntiAlias(true)
        iconPaint.setTextAlign(Paint.Align.CENTER)

        backgroundRect = RectF()
    }

    var value: Int
        get() = currentValue
        set(value) {
            this.currentValue = value
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(WIDTH, HEIGHT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isPressed) {
            backgroundPaint.setColor(Color.argb(255, 70, 70, 70))
        } else {
            backgroundPaint.setColor(Color.argb(220, 50, 50, 50))
        }
        backgroundRect.set(0f, 0f, getWidth().toFloat(), getHeight().toFloat())
        canvas.drawRoundRect(backgroundRect, 15f, 15f, backgroundPaint)

        val icon = "✏"
        val iconY = getHeight() / 2f + 8
        canvas.drawText(icon, getWidth() / 2f, iconY, iconPaint)

        val valueText = currentValue.toString() + "px"
        val textY = (getHeight() - 15).toFloat()
        canvas.drawText(valueText, getWidth() / 2f, textY, textPaint)
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

    fun setInputClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    companion object {
        private const val WIDTH = 100
        private const val HEIGHT = 140
    }
}
