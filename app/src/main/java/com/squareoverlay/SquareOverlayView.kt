package com.squareoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class SquareOverlayView(
    context: Context?,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private var squareSize: Float
) : View(context) {
    private val squarePaint: Paint
    private val textPaint: Paint
    private val textBackgroundPaint: Paint

    private var windowManager: WindowManager? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private val squareX = 0f
    private val squareY = 0f

    private var absoluteScreenX: Float
    private var absoluteScreenY: Float

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var isDragging = false

    private var isResizing = false

    fun interface ScreenshotCallback {
        fun onScreenshotRequested(
            xPercent: Float,
            yPercent: Float,
            widthPercent: Float,
            heightPercent: Float,
            onHidden: Runnable?
        )
    }

    private var screenshotCallback: ScreenshotCallback? = null

    init {
        absoluteScreenX = (screenWidth - squareSize) / 2
        absoluteScreenY = (screenHeight - squareSize) / 2

        squarePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }

        textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            isAntiAlias = true
        }

        textBackgroundPaint = Paint().apply {
            color = Color.argb(200, 0, 0, 0)
            style = Paint.Style.FILL
        }
    }

    fun setScreenshotCallback(callback: ScreenshotCallback?) {
        this.screenshotCallback = callback
    }

    fun setWindowManager(wm: WindowManager?, params: WindowManager.LayoutParams?) {
        this.windowManager = wm
        this.windowParams = params
    }

    fun triggerScreenshot() {
        screenshotCallback?.onScreenshotRequested(
            (absoluteScreenX / screenWidth) * 100,
            (absoluteScreenY / screenHeight) * 100,
            (squareSize / screenWidth) * 100,
            (squareSize / screenHeight) * 100,
            Runnable {}
        )
    }

    fun setSquareSize(size: Float) {
        this.squareSize = size
        requestLayout()
        invalidate()
    }

    val squareXPercent: Float
        get() {
            windowParams?.let {
                absoluteScreenX = it.x + squareX
            }
            return (absoluteScreenX / screenWidth) * 100
        }

    val squareYPercent: Float
        get() {
            windowParams?.let {
                absoluteScreenY = it.y + squareY
            }
            return (absoluteScreenY / screenHeight) * 100
        }

    val squareWidthPercent: Float
        get() = (squareSize / screenWidth) * 100

    val squareHeightPercent: Float
        get() = (squareSize / screenHeight) * 100

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(squareSize.toInt(), squareSize.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(squareX, squareY, squareX + squareSize, squareY + squareSize, squarePaint)

        squarePaint.apply {
            style = Paint.Style.FILL
            color = Color.BLUE
        }
        canvas.drawCircle(squareX + squareSize, squareY + squareSize, 55f, squarePaint)
        squarePaint.apply {
            color = Color.RED
            style = Paint.Style.STROKE
        }

        windowParams?.let {
            absoluteScreenX = it.x + squareX
            absoluteScreenY = it.y + squareY
        }

        val xPercent = (absoluteScreenX / screenWidth) * 100
        val yPercent = (absoluteScreenY / screenHeight) * 100
        val sizePercent = (squareSize / screenWidth) * 100

        val line1 = "Position: ${absoluteScreenX.toInt()}, ${absoluteScreenY.toInt()}"
        val line2 = "Size: ${squareSize.toInt()} x ${squareSize.toInt()}"
        val line3 = String.format("Crop: %.1f%%, %.1f%%, %.1f%%", xPercent, yPercent, sizePercent)

        val textX = squareX + 20
        val textY = squareY + 60
        val lineHeight = 50f

        val textWidth = 380f
        val textHeight = 3 * lineHeight + 20
        canvas.drawRect(
            textX - 10,
            textY - 45,
            textX + textWidth,
            textY + textHeight - 45,
            textBackgroundPaint
        )

        canvas.drawText(line1, textX, textY, textPaint)
        canvas.drawText(line2, textX, textY + lineHeight, textPaint)
        canvas.drawText(line3, textX, textY + lineHeight * 2, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val wm = windowManager ?: return false
        val params = windowParams ?: return false

        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val cornerX = squareX + squareSize
                val cornerY = squareY + squareSize
                val distanceToCorner = sqrt(
                    (touchX - cornerX).pow(2) + (touchY - cornerY).pow(2)
                )

                return when {
                    distanceToCorner < CORNER_SIZE -> {
                        isResizing = true
                        isDragging = false
                        true
                    }
                    touchX >= squareX && touchX <= squareX + squareSize
                            && touchY >= squareY && touchY <= squareY + squareSize -> {
                        isDragging = true
                        isResizing = false
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        initialWindowX = params.x
                        initialWindowY = params.y
                        true
                    }
                    else -> false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when {
                    isResizing -> {
                        var newSize = max(200f, touchX - squareX)
                        newSize = min(newSize, min(screenWidth * 0.9f, screenHeight * 0.9f))

                        if (newSize != squareSize) {
                            squareSize = newSize
                            params.width = squareSize.toInt()
                            params.height = squareSize.toInt()
                            wm.updateViewLayout(this, params)
                        }
                        true
                    }
                    isDragging -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        params.x = (initialWindowX + deltaX.toInt()).coerceIn(
                            0,
                            screenWidth - squareSize.toInt()
                        )
                        params.y = (initialWindowY + deltaY.toInt()).coerceIn(
                            0,
                            screenHeight - squareSize.toInt()
                        )

                        wm.updateViewLayout(this, params)
                        invalidate()
                        true
                    }
                    else -> false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isResizing = false
                true
            }

            else -> false
        }

        return false
    }

    companion object {
        private const val CORNER_SIZE = 160
    }
}
