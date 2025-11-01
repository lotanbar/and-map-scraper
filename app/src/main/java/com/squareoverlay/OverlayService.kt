package com.squareoverlay

import android.R
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.squareoverlay.ScrollAccessibilityService.Companion.instance
import com.squareoverlay.SquareOverlayView.ScreenshotCallback
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.util.Arrays
import java.util.Locale
import kotlin.math.max

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: SquareOverlayView? = null
    private var screenshotButton: ScreenshotButtonView? = null
    private var minusButton: AdjustButtonView? = null
    private var plusButton: AdjustButtonView? = null
    private var vMinusButton: AdjustButtonView? = null
    private var vPlusButton: AdjustButtonView? = null
    private var resetButton: ResetButtonView? = null
    private var hTestButton: TestButtonView? = null
    private var vTestButton: TestButtonView? = null
    private var counterDisplay: CounterDisplayView? = null
    private var vCounterDisplay: CounterDisplayView? = null
    private var scrollIncrementInput: ScrollIncrementInputView? = null
    private var vScrollIncrementInput: ScrollIncrementInputView? = null
    private var nextLineButton: NextLineButtonView? = null
    private var fileBrowserButton: FileBrowserButtonView? = null
    private var nextZoomLevelButton: NextZoomLevelButtonView? = null
    private var screenshotService: ScreenshotService? = null

    private var scrollDistance: Int =
        DEFAULT_SCROLL_DISTANCE // Horizontal scroll distance in pixels
    private var verticalScrollDistance: Int =
        DEFAULT_VERTICAL_SCROLL_DISTANCE // Vertical scroll distance in pixels
    private var scrollIncrement = 30 // Horizontal scroll increment, default 30 pixels
    private var verticalScrollIncrement = 30 // Vertical scroll increment, default 30 pixels
    private var screenshotCount =
        0 // Track number of screenshots taken (number of horizontal scrolls)
    private var screenshotNumber = 1 // Sequential number for screenshot filenames (1, 2, 3...)
    private var nextScreenshotIsLineStart = false // Flag to mark next screenshot with 'z' suffix
    private var currentZoomFolder: String? = null // Track current zoom folder (e.g., "zoom1", "zoom2")
    private var multiScreenshotCount = 1 // Number of screenshots to take on next click (default 1)
    private var multiScreenshotRows = 1 // Number of rows to capture (default 1)
    private var isMultiScreenshotInProgress =
        false // Flag to prevent re-entry during multi-screenshot

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getAction() != null) {
            val action = intent.getAction()

            if ("SHOW" == action) {
                showOverlay()
            } else if ("HIDE" == action) {
                hideOverlay()
            } else if ("SET_PROJECTION" == action) {
                val resultCode = intent.getIntExtra("resultCode", 0)
                val data = intent.getParcelableExtra<Intent?>("data")
                if (screenshotService != null) {
                    screenshotService!!.startProjection(resultCode, data!!)
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Square Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setDescription("Running square overlay with screenshot capability")

            val manager = getSystemService<NotificationManager?>(NotificationManager::class.java)
            if (manager != null) {
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun startForeground() {
        val builder: Notification.Builder?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = Notification.Builder(this, CHANNEL_ID)
        } else {
            builder = Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("Square Overlay Active")
            .setContentText("Tap screenshot button to capture")
            .setSmallIcon(R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlay() {
        if (overlayView != null) {
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager?

        val displayMetrics = DisplayMetrics()
        windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Default square size and position
        val initialSquareSize: Float = DEFAULT_SQUARE_SIZE
        val squareX: Int = ((screenWidth - initialSquareSize) / 2).toInt() // Center horizontally
        val squareY: Int = DEFAULT_SQUARE_Y

        val overlayWidth = initialSquareSize.toInt()
        val overlayHeight = initialSquareSize.toInt()

        overlayView = SquareOverlayView(this, screenWidth, screenHeight, initialSquareSize)

        val layoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            overlayWidth,
            overlayHeight,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = squareX
        params.y = squareY

        overlayView!!.setWindowManager(windowManager, params)

        screenshotService = ScreenshotService(this)

        overlayView!!.setScreenshotCallback(ScreenshotCallback { xPercent: Float, yPercent: Float, widthPercent: Float, heightPercent: Float, onHidden: Runnable? ->
            if (screenshotService != null) {
                // Only hide UI if not in multi-screenshot mode (UI already hidden)
                if (!isMultiScreenshotInProgress) {
                    setAllViewsVisible(false)
                }

                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    screenshotService!!.captureScreenshot(
                        xPercent,
                        yPercent,
                        widthPercent,
                        heightPercent,
                        screenshotNumber,
                        nextScreenshotIsLineStart
                    )
                    nextScreenshotIsLineStart = false
                    screenshotCount++
                    screenshotNumber++
                    // Wait for screenshot to complete, then scroll
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        scrollHorizontallyBySquareWidth()
                        // Wait for scroll to complete
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            // Only restore UI if not in multi-screenshot mode
                            if (!isMultiScreenshotInProgress) {
                                setAllViewsVisible(true)
                            }
                        }, 1100)
                    }, 1100)
                }, 50)
            } else {
                Toast.makeText(this, "Screenshot not initialized", Toast.LENGTH_SHORT).show()
            }
        })

        windowManager!!.addView(overlayView, params)

        // Create screenshot button at bottom center
        screenshotButton = ScreenshotButtonView(this)

        val buttonLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buttonLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            buttonLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            buttonLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        buttonParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        buttonParams.y = 200 // Larger gap - closer to bottom
        buttonParams.x = 280 // Screenshot button - 4th position

        screenshotButton?.setButtonClickListener {
            if (overlayView != null && !isMultiScreenshotInProgress) {
                if (multiScreenshotCount > 1 || multiScreenshotRows > 1) {
                    // Perform multiple screenshots (potentially multiple rows)
                    isMultiScreenshotInProgress = true
                    // Hide UI at the very start of multi-screenshot process
                    setAllViewsVisible(false)
                    // Small delay to ensure UI is hidden
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        performMultipleRowsRecursive(0, multiScreenshotRows)
                    }, 50)
                } else {
                    // Single screenshot
                    overlayView!!.triggerScreenshot()
                }
            }
        }

        // Add long-click listener for multi-screenshot mode (2 seconds)
        screenshotButton?.setButtonLongClickListener {
            showMultiScreenshotDialog()
            true
        }

        windowManager!!.addView(screenshotButton, buttonParams)

        // Create minus button (left of screenshot button)
        minusButton = AdjustButtonView(this, "H-")

        val minusLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            minusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            minusLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val minusParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            minusLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        minusParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        minusParams.y = 450 // Horizontal calibration row (middle row)
        minusParams.x = 240 // Fourth position: Minus button (200px wide, center at 240)

        minusButton?.setButtonClickListener {
            // Scroll left by scrollIncrement
            scrollDistance -= scrollIncrement
            if (counterDisplay != null) {
                counterDisplay!!.setCounter(scrollDistance)
            }
            performSmallScroll(-scrollIncrement)
        }

        windowManager!!.addView(minusButton, minusParams)

        // Create plus button (right of screenshot button)
        plusButton = AdjustButtonView(this, "H+")

        val plusLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            plusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            plusLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val plusParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            plusLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        plusParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        plusParams.y = 450 // Horizontal calibration row (middle row)
        plusParams.x = 480 // Fifth position: Plus button (200px wide, center at 480)

        plusButton?.setButtonClickListener {
            // Scroll right by scrollIncrement
            if (scrollDistance + scrollIncrement <= MAX_SCROLL_DISTANCE) {
                scrollDistance += scrollIncrement
                if (counterDisplay != null) {
                    counterDisplay!!.setCounter(scrollDistance)
                }
                performSmallScroll(scrollIncrement)
            } else {
                Toast.makeText(
                    this,
                    "Maximum scroll distance reached: " + MAX_SCROLL_DISTANCE + "px",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        windowManager!!.addView(plusButton, plusParams)

        // Create V- button (vertical calibration minus)
        vMinusButton = AdjustButtonView(this, "V-")

        val vMinusLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vMinusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            vMinusLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val vMinusParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            vMinusLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        vMinusParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        vMinusParams.y = 650 // Vertical calibration row (top row)
        vMinusParams.x = 240 // Same horizontal position as minus button

        vMinusButton?.setButtonClickListener {
            // Decrease vertical scroll distance by verticalScrollIncrement AND scroll up
            verticalScrollDistance -= verticalScrollIncrement
            if (vCounterDisplay != null) {
                vCounterDisplay!!.setCounter(verticalScrollDistance)
            }
            performSmallVerticalScroll(-verticalScrollIncrement) // Negative = scroll UP
        }

        windowManager!!.addView(vMinusButton, vMinusParams)

        // Create V+ button (vertical calibration plus)
        vPlusButton = AdjustButtonView(this, "V+")

        val vPlusLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vPlusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            vPlusLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val vPlusParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            vPlusLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        vPlusParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        vPlusParams.y = 650 // Vertical calibration row (same as V-)
        vPlusParams.x = 480 // Same horizontal position as plus button

        vPlusButton?.setButtonClickListener {
            // Increase vertical scroll distance by verticalScrollIncrement AND scroll down
            if (verticalScrollDistance + verticalScrollIncrement <= MAX_SCROLL_DISTANCE) {
                verticalScrollDistance += verticalScrollIncrement
                if (vCounterDisplay != null) {
                    vCounterDisplay!!.setCounter(verticalScrollDistance)
                }
                performSmallVerticalScroll(verticalScrollIncrement) // Positive = scroll DOWN
            } else {
                Toast.makeText(
                    this,
                    "Maximum vertical scroll distance reached: " + MAX_SCROLL_DISTANCE + "px",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        windowManager!!.addView(vPlusButton, vPlusParams)

        // Create vertical scroll increment input
        vScrollIncrementInput = ScrollIncrementInputView(this)
        vScrollIncrementInput!!.value = verticalScrollIncrement // Set default value

        val vInputLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vInputLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            vInputLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val vInputParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            vInputLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        vInputParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        vInputParams.y = 650 // Vertical calibration row
        vInputParams.x = -530 // Same horizontal position as scroll increment input

        // Set up listener to update verticalScrollIncrement when value changes

        // Handle click to open dialog for editing
        vScrollIncrementInput?.setInputClickListener {
            showScrollIncrementDialog(true)
        }

        windowManager!!.addView(vScrollIncrementInput, vInputParams)

        // Create vertical test button
        vTestButton = TestButtonView(this)

        val vTestLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vTestLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            vTestLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val vTestParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            vTestLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        vTestParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        vTestParams.y = 650 // Vertical calibration row (top row)
        vTestParams.x = 0 // Same horizontal position as horizontal test button

        vTestButton?.setButtonClickListener {
            performVerticalTest()
        }

        windowManager!!.addView(vTestButton, vTestParams)

        // Create scroll increment input (to the left of minus button)
        scrollIncrementInput = ScrollIncrementInputView(this)
        scrollIncrementInput!!.value = scrollIncrement // Set default value

        val inputLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            inputLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            inputLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val inputParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            inputLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        inputParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        inputParams.y = 450 // Horizontal calibration row (middle row)
        inputParams.x = -530 // First position: Edit button (100px wide, center at -530)

        // Set up listener to update scrollIncrement when value changes

        // Handle click to open dialog for editing
        scrollIncrementInput?.setInputClickListener {
            showScrollIncrementDialog(false)
        }

        windowManager!!.addView(scrollIncrementInput, inputParams)

        // Create horizontal test button
        hTestButton = TestButtonView(this)

        val hTestLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hTestLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            hTestLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val hTestParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            hTestLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        hTestParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        hTestParams.y = 450 // Horizontal calibration row (middle row)
        hTestParams.x = 0 // Third position: Test button (200px wide, centered at 0)

        hTestButton?.setButtonClickListener {
            performHorizontalTest()
        }

        windowManager!!.addView(hTestButton, hTestParams)

        // Create counter display above the button row
        counterDisplay = CounterDisplayView(this)
        counterDisplay!!.setCounter(scrollDistance) // Set initial value to default scrollDistance

        val counterLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            counterLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            counterLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val counterParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            counterLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        counterParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        counterParams.y = 450 // Horizontal calibration row (middle row)
        counterParams.x = -290 // Second position: Counter display (300px wide, center at -290)

        // Add click listener to open dialog for editing scroll distance
        counterDisplay?.setCounterClickListener {
            showScrollDistanceDialog(false)
        }

        windowManager!!.addView(counterDisplay, counterParams)

        // Create vertical counter display (shows vertical scroll distance)
        vCounterDisplay = CounterDisplayView(this)
        vCounterDisplay!!.setCounter(verticalScrollDistance) // Set initial value to default vertical scrollDistance

        val vCounterLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vCounterLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            vCounterLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val vCounterParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            vCounterLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        vCounterParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        vCounterParams.y = 650 // Vertical calibration row
        vCounterParams.x = -290 // Same horizontal position as regular counter display

        // Add click listener to open dialog for editing vertical scroll distance
        vCounterDisplay?.setCounterClickListener {
            showScrollDistanceDialog(true)
        }

        windowManager!!.addView(vCounterDisplay, vCounterParams)

        // Create next line button (to the right of plus button)
        nextLineButton = NextLineButtonView(this)

        val nextLineLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nextLineLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            nextLineLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val nextLineParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            nextLineLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        nextLineParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        nextLineParams.y = 200 // Same row as screenshot button
        nextLineParams.x = -40 // Go down a line button - 3rd position

        nextLineButton?.setButtonClickListener {
            // Mark next screenshot to have 'z' suffix
            nextScreenshotIsLineStart = true
            goDownOneLine()
        }

        windowManager!!.addView(nextLineButton, nextLineParams)

        // Create reset button (to the left of file browser button)
        resetButton = ResetButtonView(this)

        val resetLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            resetLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            resetLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val resetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            resetLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        resetParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        resetParams.y = 200 // Same row as screenshot button
        resetParams.x = 560 // Reset button - 5th position (rightmost)

        resetButton?.setButtonClickListener {
            // Reset scroll distances to defaults
            scrollDistance = DEFAULT_SCROLL_DISTANCE
            verticalScrollDistance = DEFAULT_VERTICAL_SCROLL_DISTANCE
            if (counterDisplay != null) {
                counterDisplay!!.setCounter(scrollDistance)
            }
            if (vCounterDisplay != null) {
                vCounterDisplay!!.setCounter(verticalScrollDistance)
            }

            // Reset scroll increments to defaults
            scrollIncrement = 30
            verticalScrollIncrement = 30
            if (scrollIncrementInput != null) {
                scrollIncrementInput!!.value = scrollIncrement
            }
            if (vScrollIncrementInput != null) {
                vScrollIncrementInput!!.value = verticalScrollIncrement
            }

            // Reset square position and size to defaults
            if (overlayView != null && windowManager != null) {
                val overlayParams = overlayView!!.getLayoutParams() as WindowManager.LayoutParams?
                if (overlayParams != null) {
                    // Calculate centered X position
                    val displayMetrics = DisplayMetrics()
                    windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
                    val screenWidth = displayMetrics.widthPixels
                    val centeredX = ((screenWidth - DEFAULT_SQUARE_SIZE) / 2).toInt()

                    // Reset position
                    overlayParams.x = centeredX
                    overlayParams.y = DEFAULT_SQUARE_Y

                    // Reset size
                    overlayParams.width = DEFAULT_SQUARE_SIZE.toInt()
                    overlayParams.height = DEFAULT_SQUARE_SIZE.toInt()

                    // Apply changes
                    windowManager!!.updateViewLayout(overlayView, overlayParams)

                    // Reset the overlay view's internal size
                    overlayView!!.setSquareSize(DEFAULT_SQUARE_SIZE)
                }
            }

            // Reset zoom folder and screenshot numbering
            currentZoomFolder = null
            screenshotCount = 0
            screenshotNumber = 1
            screenshotService?.setZoomFolder(null)
        }

        windowManager!!.addView(resetButton, resetParams)

        // Create file browser button (to the right of reset button)
        fileBrowserButton = FileBrowserButtonView(this)

        val fileBrowserLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fileBrowserLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            fileBrowserLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val fileBrowserParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            fileBrowserLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        fileBrowserParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        fileBrowserParams.y = 200 // Same row as other buttons
        fileBrowserParams.x = -560 // Gallery - 1st position (leftmost)

        fileBrowserButton?.setButtonClickListener {
            openScreenshotFolder()
        }

        windowManager!!.addView(fileBrowserButton, fileBrowserParams)

        // Create next zoom level button (to the right of screenshot button)
        nextZoomLevelButton = NextZoomLevelButtonView(this)

        val nextZoomLevelLayoutType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nextZoomLevelLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            nextZoomLevelLayoutType = WindowManager.LayoutParams.TYPE_PHONE
        }

        val nextZoomLevelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            nextZoomLevelLayoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        nextZoomLevelParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        nextZoomLevelParams.y = 200 // Same row as screenshot button
        nextZoomLevelParams.x = -320 // Z button - 2nd position

        nextZoomLevelButton?.setButtonClickListener {
            processNextZoomLevel()
        }

        windowManager!!.addView(nextZoomLevelButton, nextZoomLevelParams)
    }

    private fun hideOverlay() {
        if (overlayView != null && windowManager != null) {
            windowManager!!.removeView(overlayView)
            overlayView = null
        }
        if (screenshotButton != null && windowManager != null) {
            windowManager!!.removeView(screenshotButton)
            screenshotButton = null
        }
        if (minusButton != null && windowManager != null) {
            windowManager!!.removeView(minusButton)
            minusButton = null
        }
        if (plusButton != null && windowManager != null) {
            windowManager!!.removeView(plusButton)
            plusButton = null
        }
        if (resetButton != null && windowManager != null) {
            windowManager!!.removeView(resetButton)
            resetButton = null
        }
        if (counterDisplay != null && windowManager != null) {
            windowManager!!.removeView(counterDisplay)
            counterDisplay = null
        }
        if (scrollIncrementInput != null && windowManager != null) {
            windowManager!!.removeView(scrollIncrementInput)
            scrollIncrementInput = null
        }
        if (nextLineButton != null && windowManager != null) {
            windowManager!!.removeView(nextLineButton)
            nextLineButton = null
        }
        if (fileBrowserButton != null && windowManager != null) {
            windowManager!!.removeView(fileBrowserButton)
            fileBrowserButton = null
        }
        if (nextZoomLevelButton != null && windowManager != null) {
            windowManager!!.removeView(nextZoomLevelButton)
            nextZoomLevelButton = null
        }
        if (vMinusButton != null && windowManager != null) {
            windowManager!!.removeView(vMinusButton)
            vMinusButton = null
        }
        if (vPlusButton != null && windowManager != null) {
            windowManager!!.removeView(vPlusButton)
            vPlusButton = null
        }
        if (vCounterDisplay != null && windowManager != null) {
            windowManager!!.removeView(vCounterDisplay)
            vCounterDisplay = null
        }
        if (vScrollIncrementInput != null && windowManager != null) {
            windowManager!!.removeView(vScrollIncrementInput)
            vScrollIncrementInput = null
        }
        if (hTestButton != null && windowManager != null) {
            windowManager!!.removeView(hTestButton)
            hTestButton = null
        }
        if (vTestButton != null && windowManager != null) {
            windowManager!!.removeView(vTestButton)
            vTestButton = null
        }
    }

    private fun scrollHorizontallyBySquareWidth() {
        if (overlayView == null || windowManager == null) {
            return
        }

        val accessibilityService = instance
        if (accessibilityService == null) {
            Toast.makeText(
                this,
                "Please enable Square Overlay accessibility service in Settings",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            val displayMetrics = DisplayMetrics()
            windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Perform gesture in the area between Y 1600-2000, spanning most of the X axis
            val gestureY = 1900 // 100px lower than before
            val gestureStartX = screenWidth - 100 // Start from right side with 100px margin
            val swipeDistance = scrollDistance // Use the accumulated calibration value
            val startX = gestureStartX
            val endX = startX - swipeDistance

            // Use slower duration for more precise scrolling
            val duration = 500

            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, duration)
        } catch (e: Exception) {
        }
    }

    private fun performSmallScroll(distance: Int) {
        val accessibilityService = instance
        if (accessibilityService == null) {
            return
        }

        if (windowManager == null || overlayView == null) {
            return
        }

        try {
            val displayMetrics = DisplayMetrics()
            windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels

            // Perform gesture in the area between Y 1600-2000, spanning most of the X axis
            val gestureY = 1900 // 100px lower than before
            val gestureStartX = screenWidth - 100 // Start from right side with 100px margin
            val startX = gestureStartX
            val endX = startX - distance // distance can be positive or negative

            // Slower gesture for better scroll recognition (300ms is typical for scroll gestures)
            val duration = 300

            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, duration)
        } catch (e: Exception) {
        }
    }

    private fun performSmallVerticalScroll(distance: Int) {
        val accessibilityService = instance
        if (accessibilityService == null) {
            return
        }

        if (windowManager == null) {
            return
        }

        try {
            val displayMetrics = DisplayMetrics()
            windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Perform gesture at center of screen, adjusted 250px lower
            val centerX = screenWidth / 2
            val centerY = screenHeight / 2 + 250

            // distance > 0 = scroll DOWN (swipe UP)
            // distance < 0 = scroll UP (swipe DOWN)
            val startY = centerY + (distance / 2)
            val endY = centerY - (distance / 2)

            // Slower gesture for better scroll recognition
            val duration = 300

            accessibilityService.performVerticalScroll(centerX, startY, centerX, endY, duration)
        } catch (e: Exception) {
        }
    }

    private fun openScreenshotFolder() {
        try {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "SquareOverlay")

            // Create directory if it doesn't exist
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            // Simple approach: Just open any file manager and let user navigate
            // Use ACTION_OPEN_DOCUMENT_TREE or ACTION_VIEW with the Pictures directory
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3APictures%2FSquareOverlay"),
                "vnd.android.document/directory"
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Launch OnePlus File Manager (what was working before)

            try {
                val intent = Intent()
                intent.setClassName(
                    "com.oneplus.filemanager",
                    "com.oplus.filemanager.main.ui.MainActivity"
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, "Could not open file manager", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goDownOneLine(restoreUIAfter: Boolean = true) {
        if (screenshotCount == 0) {
            Toast.makeText(this, "No screenshots taken yet", Toast.LENGTH_SHORT).show()
            return
        }

        val accessibilityService = instance
        if (accessibilityService == null) {
            Toast.makeText(
                this,
                "Please enable Square Overlay accessibility service in Settings",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (overlayView == null || windowManager == null) {
            return
        }

        // Hide all UI elements to prevent accidental clicks (only if not already hidden)
        if (restoreUIAfter) {
            setAllViewsVisible(false)
        }

        // Wait 1100ms before starting gesture
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            try {
                val displayMetrics = DisplayMetrics()
                windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                // Perform gestures in the CENTER of the screen for consistent scroll distance
                val centerX = screenWidth / 2
                val centerY = screenHeight / 2 + 250 // 250px lower

                // First, scroll down by the square size (one line)
                // Swipe UP to scroll DOWN content
                val verticalDistance = verticalScrollDistance // Use separate vertical calibration
                val startY = centerY + (verticalDistance / 2)
                val endY = centerY - (verticalDistance / 2)


                accessibilityService.performVerticalScroll(centerX, startY, centerX, endY, 500)

                // Wait for vertical scroll to complete with momentum before starting horizontal scrolls
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    // Now scroll left, one scroll at a time with delays
                    scrollLeftSequentially(0, screenshotCount, restoreUIAfter)
                }, 1100)
            } catch (e: Exception) {
                // If error, restore UI
                setAllViewsVisible(true)
            }
        }, 1100)
    }

    private fun scrollLeftSequentially(currentScroll: Int, totalScrolls: Int, restoreUIAfter: Boolean = true) {
        if (currentScroll >= totalScrolls) {
            // Done with all scrolls, reset screenshot counter
            screenshotCount = 0
            Toast.makeText(this, "Moved to next line", Toast.LENGTH_SHORT).show()
            // Wait 1100ms before restoring UI (only if requested)
            if (restoreUIAfter) {
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    setAllViewsVisible(true)
                }, 1100)
            }
            return
        }


        val accessibilityService = instance
        if (accessibilityService == null || windowManager == null) {
            // Restore UI if error (only if we were supposed to restore it)
            if (restoreUIAfter) {
                setAllViewsVisible(true)
            }
            return
        }

        try {
            // Perform gestures in the area between Y 1600-2000
            val displayMetrics = DisplayMetrics()
            windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels

            val gestureY = 1900 // 100px lower than before

            // Swipe RIGHT to scroll LEFT (back to beginning of line)
            val startX = 100 // Start from left side with 100px margin
            val endX = startX + scrollDistance

            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, 500)

            // Wait for scroll to complete with momentum, then do next scroll
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                scrollLeftSequentially(currentScroll + 1, totalScrolls, restoreUIAfter)
            }, 1100)
        } catch (e: Exception) {
            // Restore UI if error (only if we were supposed to restore it)
            if (restoreUIAfter) {
                setAllViewsVisible(true)
            }
        }
    }

    private fun processNextZoomLevel() {
        try {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "SquareOverlay")

            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            // Find the next zoom level number
            var nextZoomLevel = 1
            while (File(appDir, "zoom" + nextZoomLevel).exists()) {
                nextZoomLevel++
            }

            // Create the zoom folder
            val zoomFolderName = "zoom" + nextZoomLevel
            val zoomFolder = File(appDir, zoomFolderName)
            if (!zoomFolder.mkdirs()) {
                Toast.makeText(this, "Failed to create zoom folder", Toast.LENGTH_SHORT).show()
                return
            }

            // Set current zoom folder and reset screenshot numbering
            currentZoomFolder = zoomFolderName
            screenshotCount = 0
            screenshotNumber = 1

            // Pass the zoom folder to screenshot service
            screenshotService?.setZoomFolder(zoomFolderName)

            Toast.makeText(this, "Zoom " + nextZoomLevel + " - screenshots will be saved here", Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create zoom level", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stitchImages(sourceFolder: File, outputFile: File?) {
        Thread(Runnable {
            try {
                // Get all image files and sort them

                val imageFiles =
                    sourceFolder.listFiles(FilenameFilter { dir: File?, name: String? ->
                        name!!.lowercase(
                            Locale.getDefault()
                        ).endsWith(".png")
                    })

                if (imageFiles == null || imageFiles.size == 0) {
                    return@Runnable
                }

                // Sort files numerically (extract number from filename)
                Arrays.sort<File?>(imageFiles, Comparator { f1: File?, f2: File? ->
                    val name1 = f1!!.getName().replace("[^0-9]".toRegex(), "")
                    val name2 = f2!!.getName().replace("[^0-9]".toRegex(), "")
                    val num1 = if (name1.isEmpty()) 0 else name1.toInt()
                    val num2 = if (name2.isEmpty()) 0 else name2.toInt()
                    Integer.compare(num1, num2)
                })

                // Group files into rows (files with 'z' suffix start new rows)
                val rows: MutableList<MutableList<File>> = ArrayList()
                var currentRow: MutableList<File> = ArrayList()

                for (file in imageFiles) {
                    val name = file.getName()

                    // Check if this file starts a new row (has 'z' before .png)
                    if (name.contains("z.png") && !currentRow.isEmpty()) {
                        rows.add(currentRow)
                        currentRow = ArrayList()
                    }

                    currentRow.add(file)
                }

                // Add the last row
                if (!currentRow.isEmpty()) {
                    rows.add(currentRow)
                }


                // Create row bitmaps
                val rowBitmaps: MutableList<Bitmap> = ArrayList<Bitmap>()

                for (i in rows.indices) {
                    val row = rows.get(i)

                    // Load all bitmaps for this row
                    val bitmapsInRow: MutableList<Bitmap> = ArrayList<Bitmap>()
                    var totalWidth = 0
                    var maxHeight = 0

                    for (file in row) {
                        val bitmap = BitmapFactory.decodeFile(file.getAbsolutePath())
                        if (bitmap != null) {
                            bitmapsInRow.add(bitmap)
                            totalWidth += bitmap.getWidth()
                            maxHeight = max(maxHeight, bitmap.getHeight())
                        } else {
                        }
                    }

                    // Stitch this row horizontally
                    if (!bitmapsInRow.isEmpty()) {
                        val rowBitmap = Bitmap.createBitmap(
                            totalWidth, maxHeight, Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(rowBitmap)

                        var xOffset = 0
                        for (bitmap in bitmapsInRow) {
                            canvas.drawBitmap(bitmap, xOffset.toFloat(), 0f, null)
                            xOffset += bitmap.getWidth()
                            bitmap.recycle() // Free memory
                        }

                        rowBitmaps.add(rowBitmap)
                    }
                }

                // Now stitch all rows vertically
                if (!rowBitmaps.isEmpty()) {
                    var maxWidth = 0
                    var totalHeight = 0

                    for (rowBitmap in rowBitmaps) {
                        maxWidth = max(maxWidth, rowBitmap.getWidth())
                        totalHeight += rowBitmap.getHeight()
                    }


                    val finalBitmap = Bitmap.createBitmap(
                        maxWidth, totalHeight, Bitmap.Config.ARGB_8888
                    )
                    val finalCanvas = Canvas(finalBitmap)

                    var yOffset = 0
                    for (rowBitmap in rowBitmaps) {
                        finalCanvas.drawBitmap(rowBitmap, 0f, yOffset.toFloat(), null)
                        yOffset += rowBitmap.getHeight()
                        rowBitmap.recycle() // Free memory
                    }

                    // Save the final bitmap
                    val fos = FileOutputStream(outputFile)
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    finalBitmap.recycle()


                    // Show success message on UI thread
                    Handler(Looper.getMainLooper()).post(Runnable {
                        Toast.makeText(
                            this@OverlayService,
                            "Zoom level stitched successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                    })
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    Toast.makeText(
                        this@OverlayService,
                        "Stitching failed: " + e.message,
                        Toast.LENGTH_LONG
                    ).show()
                })
            }
        }).start()
    }

    private fun performHorizontalTest() {
        if (overlayView == null || screenshotService == null) {
            Toast.makeText(this, "Not ready for test", Toast.LENGTH_SHORT).show()
            return
        }

        val accessibilityService = instance
        if (accessibilityService == null) {
            Toast.makeText(
                this,
                "Please enable Square Overlay accessibility service in Settings",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Hide all UI elements for clean screenshot
        setAllViewsVisible(false)

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            // Get square coordinates
            val xPercent = overlayView!!.squareXPercent
            val yPercent = overlayView!!.squareYPercent
            val widthPercent = overlayView!!.squareWidthPercent
            val heightPercent = overlayView!!.squareHeightPercent

            // Take first screenshot
            screenshotService!!.captureTestScreenshot(
                xPercent, yPercent, widthPercent, heightPercent,
                "horizontal test", "before.png"
            )

            // Wait for screenshot to complete, then scroll
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                // Perform horizontal scroll using EXACT same logic as screenshot mode
                try {
                    val displayMetrics = DisplayMetrics()
                    windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
                    val screenWidth = displayMetrics.widthPixels

                    val gestureY = 1900
                    val swipeDistance = scrollDistance // Use the accumulated calibration value (same as screenshot mode)
                    val startX = screenWidth - 100
                    val endX = startX - swipeDistance

                    accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, 500)
                } catch (e: Exception) {
                }

                // Wait for scroll to complete (500ms scroll duration + 600ms for momentum/settling)
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    screenshotService!!.captureTestScreenshot(
                        xPercent, yPercent, widthPercent, heightPercent,
                        "horizontal test", "after.png"
                    )
                    // Wait for second screenshot to complete, then stitch and rotate
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        stitchAndRotateTestImages()
                        setAllViewsVisible(true)
                    }, 150)
                }, 1100)
            }, 1100)
        }, 50)
    }

    private fun performVerticalTest() {
        if (overlayView == null || screenshotService == null) {
            Toast.makeText(this, "Not ready for test", Toast.LENGTH_SHORT).show()
            return
        }

        val accessibilityService = instance
        if (accessibilityService == null) {
            Toast.makeText(
                this,
                "Please enable Square Overlay accessibility service in Settings",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Hide all UI elements for clean screenshot
        setAllViewsVisible(false)

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            // Get square coordinates
            val xPercent = overlayView!!.squareXPercent
            val yPercent = overlayView!!.squareYPercent
            val widthPercent = overlayView!!.squareWidthPercent
            val heightPercent = overlayView!!.squareHeightPercent

            // Take first screenshot
            screenshotService!!.captureTestScreenshot(
                xPercent, yPercent, widthPercent, heightPercent,
                "vertical test", "before.png"
            )

            // Wait for screenshot to complete, then scroll
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                // Perform vertical scroll using the same method as goDownOneLine
                try {
                    val displayMetrics = DisplayMetrics()
                    windowManager!!.getDefaultDisplay().getMetrics(displayMetrics)
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels

                    val centerX = screenWidth / 2
                    val centerY = screenHeight / 2 + 250 // 250px lower

                    // Scroll down by verticalScrollDistance
                    val verticalDistance = verticalScrollDistance
                    val startY = centerY + (verticalDistance / 2)
                    val endY = centerY - (verticalDistance / 2)

                    accessibilityService.performVerticalScroll(centerX, startY, centerX, endY, 500)
                } catch (e: Exception) {
                }

                // Wait for scroll to complete (500ms scroll duration + 600ms for momentum/settling)
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    screenshotService!!.captureTestScreenshot(
                        xPercent, yPercent, widthPercent, heightPercent,
                        "vertical test", "after.png"
                    )
                    // Wait for second screenshot to complete, then stitch
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        stitchTestImages("vertical test")
                        setAllViewsVisible(true)
                    }, 150)
                }, 1100)
            }, 1100)
        }, 50)
    }

    private fun stitchAndRotateTestImages() {
        stitchTestImages("horizontal test", true)
    }

    private fun stitchTestImages(testFolder: String, stitchHorizontally: Boolean = false) {
        Thread(Runnable {
            try {
                val picturesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "SquareOverlay")
                val testDir = File(appDir, testFolder)

                val beforeFile = File(testDir, "before.png")
                val afterFile = File(testDir, "after.png")

                if (!beforeFile.exists() || !afterFile.exists()) {
                    return@Runnable
                }

                // Load the two images
                val beforeBitmap = BitmapFactory.decodeFile(beforeFile.getAbsolutePath())
                val afterBitmap = BitmapFactory.decodeFile(afterFile.getAbsolutePath())

                if (beforeBitmap == null || afterBitmap == null) {
                    return@Runnable
                }

                val stitchedBitmap: Bitmap?
                val canvas: Canvas?
                val linePaint = Paint()
                linePaint.setColor(Color.RED)
                linePaint.setStrokeWidth(3f)

                if (stitchHorizontally) {
                    // Stitch horizontally (side by side) - for horizontal test
                    val totalWidth = beforeBitmap.getWidth() + afterBitmap.getWidth()
                    val maxHeight = max(beforeBitmap.getHeight(), afterBitmap.getHeight())

                    stitchedBitmap = Bitmap.createBitmap(
                        totalWidth, maxHeight, Bitmap.Config.ARGB_8888
                    )
                    canvas = Canvas(stitchedBitmap)

                    // Draw before image on the left
                    canvas.drawBitmap(beforeBitmap, 0f, 0f, null)
                    // Draw after image on the right
                    canvas.drawBitmap(afterBitmap, beforeBitmap.getWidth().toFloat(), 0f, null)

                    // Draw vertical line at stitch point
                    val stitchX = beforeBitmap.getWidth()
                    canvas.drawLine(
                        stitchX.toFloat(),
                        0f,
                        stitchX.toFloat(),
                        maxHeight.toFloat(),
                        linePaint
                    )
                } else {
                    // Stitch vertically (before above, after below) - for vertical test
                    val maxWidth = max(beforeBitmap.getWidth(), afterBitmap.getWidth())
                    val totalHeight = beforeBitmap.getHeight() + afterBitmap.getHeight()

                    stitchedBitmap = Bitmap.createBitmap(
                        maxWidth, totalHeight, Bitmap.Config.ARGB_8888
                    )
                    canvas = Canvas(stitchedBitmap)

                    // Draw before image on top
                    canvas.drawBitmap(beforeBitmap, 0f, 0f, null)
                    // Draw after image on bottom
                    canvas.drawBitmap(afterBitmap, 0f, beforeBitmap.getHeight().toFloat(), null)

                    // Draw horizontal line at stitch point
                    val stitchY = beforeBitmap.getHeight()
                    canvas.drawLine(
                        0f,
                        stitchY.toFloat(),
                        maxWidth.toFloat(),
                        stitchY.toFloat(),
                        linePaint
                    )
                }

                // Save stitched image
                val stitchedFile = File(testDir, "stitched.png")
                val fos = FileOutputStream(stitchedFile)
                stitchedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()

                // Clean up
                beforeBitmap.recycle()
                afterBitmap.recycle()
                stitchedBitmap.recycle()

                // Show success message and open the image on UI thread
                val testName = testFolder.substring(0, 1)
                    .uppercase(Locale.getDefault()) + testFolder.substring(1)
                Handler(Looper.getMainLooper()).post(Runnable {
                    Toast.makeText(this@OverlayService, testName + " complete", Toast.LENGTH_SHORT)
                        .show()
                    openImageFile(stitchedFile)
                })
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    Toast.makeText(
                        this@OverlayService,
                        "Stitching failed: " + e.message,
                        Toast.LENGTH_LONG
                    ).show()
                })
            }
        }).start()
    }

    private fun openImageFile(imageFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android N and above
                uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                uri = Uri.fromFile(imageFile)
            }

            intent.setDataAndType(uri, "image/*")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open image: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAllViewsVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.INVISIBLE

        if (overlayView != null) overlayView!!.setVisibility(visibility)
        if (screenshotButton != null) screenshotButton!!.setVisibility(visibility)
        if (minusButton != null) minusButton!!.setVisibility(visibility)
        if (plusButton != null) plusButton!!.setVisibility(visibility)
        if (resetButton != null) resetButton!!.setVisibility(visibility)
        if (counterDisplay != null) counterDisplay!!.setVisibility(visibility)
        if (scrollIncrementInput != null) scrollIncrementInput!!.setVisibility(visibility)
        if (nextLineButton != null) nextLineButton!!.setVisibility(visibility)
        if (fileBrowserButton != null) fileBrowserButton!!.setVisibility(visibility)
        if (nextZoomLevelButton != null) nextZoomLevelButton!!.setVisibility(visibility)
        if (vMinusButton != null) vMinusButton!!.setVisibility(visibility)
        if (vPlusButton != null) vPlusButton!!.setVisibility(visibility)
        if (vCounterDisplay != null) vCounterDisplay!!.setVisibility(visibility)
        if (vScrollIncrementInput != null) vScrollIncrementInput!!.setVisibility(visibility)
        if (hTestButton != null) hTestButton!!.setVisibility(visibility)
        if (vTestButton != null) vTestButton!!.setVisibility(visibility)
    }

    private fun showScrollIncrementDialog(isVertical: Boolean) {
        val builder = AlertDialog.Builder(this, R.style.Theme_Material_Dialog_Alert)
        builder.setTitle("Set " + (if (isVertical) "Vertical " else "") + "Scroll Increment (px)")

        val input = EditText(this)
        input.setInputType(InputType.TYPE_CLASS_NUMBER)
        input.setText((if (isVertical) verticalScrollIncrement else scrollIncrement).toString())
        input.setSelection(input.getText().length)
        builder.setView(input)

        builder.setPositiveButton(
            "OK",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                try {
                    val newValue = input.getText().toString().toInt()
                    if (newValue > 0 && newValue <= 500) {
                        if (isVertical) {
                            verticalScrollIncrement = newValue
                            if (vScrollIncrementInput != null) {
                                vScrollIncrementInput!!.value = verticalScrollIncrement
                            }
                        } else {
                            scrollIncrement = newValue
                            if (scrollIncrementInput != null) {
                                scrollIncrementInput!!.value = scrollIncrement
                            }
                        }
                    }
                } catch (e: NumberFormatException) {
                }
            })

        builder.setNegativeButton(
            "Cancel",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.cancel() })

        val dialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow()!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.getWindow()!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        dialog.show()
    }

    private fun showScrollDistanceDialog(isVertical: Boolean) {
        val builder = AlertDialog.Builder(this, R.style.Theme_Material_Dialog_Alert)
        builder.setTitle("Set " + (if (isVertical) "Vertical " else "Horizontal ") + "Scroll Distance (px)")

        val input = EditText(this)
        input.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
        input.setText((if (isVertical) verticalScrollDistance else scrollDistance).toString())
        input.setSelection(input.getText().length)
        builder.setView(input)

        builder.setPositiveButton(
            "OK",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                try {
                    val newValue = input.getText().toString().toInt()
                    if (newValue >= -MAX_SCROLL_DISTANCE && newValue <= MAX_SCROLL_DISTANCE) {
                        if (isVertical) {
                            verticalScrollDistance = newValue
                            if (vCounterDisplay != null) {
                                vCounterDisplay!!.setCounter(verticalScrollDistance)
                            }
                        } else {
                            scrollDistance = newValue
                            if (counterDisplay != null) {
                                counterDisplay!!.setCounter(scrollDistance)
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Value must be between -" + MAX_SCROLL_DISTANCE + " and " + MAX_SCROLL_DISTANCE,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show()
                }
            })

        builder.setNegativeButton(
            "Cancel",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.cancel() })

        val dialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow()!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.getWindow()!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        dialog.show()
    }

    private fun showMultiScreenshotDialog() {
        val builder = AlertDialog.Builder(this, R.style.Theme_Material_Dialog_Alert)
        builder.setTitle("Set Screenshot Count")

        // Create a container layout for two inputs
        val layout = LinearLayout(this)
        layout.setOrientation(LinearLayout.VERTICAL)
        layout.setPadding(50, 40, 50, 10)

        // First input: Screenshots per row
        val label1 = TextView(this)
        label1.setText("Screenshots per row:")
        layout.addView(label1)

        val input1 = EditText(this)
        input1.setInputType(InputType.TYPE_CLASS_NUMBER)
        input1.setText(multiScreenshotCount.toString())
        input1.setSelection(input1.getText().length)
        layout.addView(input1)

        // Second input: Number of rows
        val label2 = TextView(this)
        label2.setText("Number of rows:")
        label2.setPadding(0, 20, 0, 0)
        layout.addView(label2)

        val input2 = EditText(this)
        input2.setInputType(InputType.TYPE_CLASS_NUMBER)
        input2.setText(multiScreenshotRows.toString())
        input2.setSelection(input2.getText().length)
        layout.addView(input2)

        builder.setView(layout)

        builder.setPositiveButton(
            "OK",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                try {
                    val screenshotsPerRow = input1.getText().toString().toInt()
                    val rows = input2.getText().toString().toInt()

                    if (screenshotsPerRow > 0 && screenshotsPerRow <= 100 && rows > 0 && rows <= 50) {
                        multiScreenshotCount = screenshotsPerRow
                        multiScreenshotRows = rows
                        val message =
                            "Next click: " + screenshotsPerRow + " screenshot" + (if (screenshotsPerRow > 1) "s" else "") +
                                    " × " + rows + " row" + (if (rows > 1) "s" else "")
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Screenshots: 1-100, Rows: 1-50", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show()
                }
            })

        builder.setNegativeButton(
            "Cancel",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.cancel() })

        val dialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow()!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.getWindow()!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        dialog.show()
    }

    private fun performMultipleRowsRecursive(currentRow: Int, totalRows: Int) {
        if (currentRow >= totalRows) {
            isMultiScreenshotInProgress = false
            val totalScreenshots = multiScreenshotCount * totalRows
            Toast.makeText(
                this,
                "Completed " + totalScreenshots + " screenshot" + (if (totalScreenshots > 1) "s" else "") +
                        " (" + totalRows + " row" + (if (totalRows > 1) "s" else "") + ")",
                Toast.LENGTH_LONG
            ).show()
            // Restore UI now that all rows are complete
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                setAllViewsVisible(true)
            }, 1100)
            return
        }

        // Perform one row of screenshots
        performMultipleScreenshotsRecursive(0, multiScreenshotCount, currentRow, totalRows)
    }

    private fun performMultipleScreenshotsRecursive(
        current: Int,
        total: Int,
        currentRow: Int,
        totalRows: Int
    ) {
        if (current >= total) {
            // Finished this row
            if (currentRow < totalRows - 1) {
                // Not the last row, need to go down one line
                // Mark next screenshot as line start for the 'z' suffix
                nextScreenshotIsLineStart = true

                // Calculate time needed for goDownOneLine
                // goDownOneLine does: initial wait (1100ms) + vertical scroll (500ms) + wait (SCROLL_DELAY_MS = 600ms) + horizontal scrolls back
                // Each horizontal scroll back takes 500ms + 600ms delay
                // Total time = 1100 + 500 + 600 + (screenshotCount * 1100ms)
                val goDownDelay: Int =
                    1100 + 500 + SCROLL_DELAY_MS + (screenshotCount * (500 + SCROLL_DELAY_MS))

                goDownOneLine(restoreUIAfter = false) // Don't restore UI between rows

                // Wait for goDownOneLine to complete, then start next row
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    performMultipleRowsRecursive(currentRow + 1, totalRows)
                }, (goDownDelay + 1000).toLong()) // Extra 1 second buffer for safety
            } else {
                // Last row finished
                performMultipleRowsRecursive(currentRow + 1, totalRows)
            }
            return
        }

        if (overlayView != null) {
            overlayView!!.triggerScreenshot()

            // Wait for this screenshot to complete before starting the next one
            // Total time per screenshot: 50ms + 150ms (capture) + 1100ms (wait) + 500ms (scroll) + 1100ms (settle) = 2900ms
            // Adding buffer for safety: 3100ms
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                performMultipleScreenshotsRecursive(current + 1, total, currentRow, totalRows)
            }, 3100)
        } else {
            isMultiScreenshotInProgress = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        if (screenshotService != null) {
            screenshotService!!.release()
            screenshotService = null
        }
    }

    companion object {
        private const val CHANNEL_ID = "SquareOverlayChannel"
        private const val NOTIFICATION_ID = 1

        // Default values
        private const val DEFAULT_SCROLL_DISTANCE = 1050
        private const val DEFAULT_VERTICAL_SCROLL_DISTANCE = 1038
        private const val DEFAULT_SQUARE_SIZE = 1000f
        private const val DEFAULT_SQUARE_Y = 400

        private const val MAX_SCROLL_DISTANCE =
            2000 // Maximum scroll distance to avoid off-screen gestures
        private const val SCROLL_DELAY_MS =
            600 // Delay between sequential scrolls to account for momentum
    }
}
