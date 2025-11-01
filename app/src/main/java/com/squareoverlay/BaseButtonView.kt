package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

abstract class BaseButtonView @JvmOverloads constructor(
    context: Context?,
    protected var buttonWidth: Int,
    protected var buttonHeight: Int,
    protected var cornerRadius: Int,
    enableLongPress: Boolean = false
) : View(context) {
    protected var buttonPaint: Paint
    @JvmField
    protected var buttonTextPaint: Paint
    protected var shadowPaint: Paint
    @JvmField
    protected var buttonRect: RectF

    protected var isButtonPressed: Boolean = false

    private var enableLongPress = false
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var repeatRunnable: Runnable? = null
    private var customLongClickRunnable: Runnable? = null
    private var longClickTriggered = false

    fun interface OnClickListener {
        fun onClick()
    }

    fun interface OnLongClickListener {
        fun onLongClick(): Boolean
    }

    protected var clickListener: OnClickListener? = null
    protected var longClickListener: OnLongClickListener? = null

    init {
        this.enableLongPress = enableLongPress

        buttonPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        buttonTextPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        shadowPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        buttonRect = RectF()

        if (enableLongPress) {
            longPressRunnable = Runnable {
                repeatRunnable = object : Runnable {
                    override fun run() {
                        clickListener?.onClick()
                        longPressHandler.postDelayed(this, REPEAT_INTERVAL.toLong())
                    }
                }
                repeatRunnable?.let { longPressHandler.post(it) }
            }
        }
    }

    fun setButtonClickListener(listener: OnClickListener?) {
        this.clickListener = listener
    }

    fun setButtonLongClickListener(listener: OnLongClickListener?) {
        this.longClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(buttonWidth, buttonHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        val shadowRect = RectF(
            centerX - buttonWidth / 2f + 4,
            centerY - buttonHeight / 2f + 4,
            centerX + buttonWidth / 2f + 4,
            centerY + buttonHeight / 2f + 4
        )
        canvas.drawRoundRect(
            shadowRect,
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            shadowPaint
        )

        buttonRect.set(
            centerX - buttonWidth / 2f,
            centerY - buttonHeight / 2f,
            centerX + buttonWidth / 2f,
            centerY + buttonHeight / 2f
        )

        if (isButtonPressed) {
            buttonPaint.color = pressedColor
        } else {
            val gradient = LinearGradient(
                buttonRect.left, buttonRect.top,
                buttonRect.left, buttonRect.bottom,
                gradientStartColor,
                gradientEndColor,
                Shader.TileMode.CLAMP
            )
            buttonPaint.shader = gradient
        }

        canvas.drawRoundRect(
            buttonRect,
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            buttonPaint
        )
        buttonPaint.shader = null

        drawButtonContent(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (buttonRect.contains(touchX, touchY)) {
                    isButtonPressed = true
                    longClickTriggered = false
                    invalidate()

                    if (enableLongPress) {
                        longPressRunnable?.let {
                            longPressHandler.postDelayed(it, LONG_PRESS_DELAY.toLong())
                        }
                    }

                    // Set up custom long click listener (2 seconds)
                    longClickListener?.let { listener ->
                        customLongClickRunnable = Runnable {
                            longClickTriggered = true
                            listener.onLongClick()
                        }
                        customLongClickRunnable?.let {
                            longPressHandler.postDelayed(it, CUSTOM_LONG_PRESS_DELAY.toLong())
                        }
                    }

                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                if (isButtonPressed) {
                    isButtonPressed = false
                    invalidate()

                    // Cancel custom long click if set
                    customLongClickRunnable?.let {
                        longPressHandler.removeCallbacks(it)
                        customLongClickRunnable = null
                    }

                    if (enableLongPress) {
                        val wasLongPress = repeatRunnable != null
                        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                        repeatRunnable?.let {
                            longPressHandler.removeCallbacks(it)
                            repeatRunnable = null
                        }

                        if (!wasLongPress && !longClickTriggered
                            && buttonRect.contains(touchX, touchY)) {
                            clickListener?.onClick()
                        }
                    } else {
                        if (!longClickTriggered && buttonRect.contains(touchX, touchY)) {
                            clickListener?.onClick()
                        }
                    }

                    return true
                }
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                isButtonPressed = false
                invalidate()

                // Cancel custom long click if set
                customLongClickRunnable?.let {
                    longPressHandler.removeCallbacks(it)
                    customLongClickRunnable = null
                }

                if (enableLongPress) {
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    repeatRunnable?.let {
                        longPressHandler.removeCallbacks(it)
                        repeatRunnable = null
                    }
                }

                return true
            }
        }

        return false
    }

    protected abstract val gradientStartColor: Int
    protected abstract val gradientEndColor: Int
    protected abstract val pressedColor: Int
    protected abstract fun drawButtonContent(canvas: Canvas?)

    companion object {
        private const val LONG_PRESS_DELAY = 400
        private const val REPEAT_INTERVAL = 200
        private const val CUSTOM_LONG_PRESS_DELAY = 2000
    }
}
