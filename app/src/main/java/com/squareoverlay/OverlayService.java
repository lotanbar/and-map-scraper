package com.squareoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "SquareOverlayChannel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private SquareOverlayView overlayView;
    private ScreenshotButtonView screenshotButton;
    private AdjustButtonView minusButton;
    private AdjustButtonView plusButton;
    private AdjustButtonView vMinusButton;
    private AdjustButtonView vPlusButton;
    private ResetButtonView resetButton;
    private TestButtonView hTestButton;
    private TestButtonView vTestButton;
    private CounterDisplayView counterDisplay;
    private CounterDisplayView vCounterDisplay;
    private ScrollIncrementInputView scrollIncrementInput;
    private ScrollIncrementInputView vScrollIncrementInput;
    private NextLineButtonView nextLineButton;
    private FileBrowserButtonView fileBrowserButton;
    private NextZoomLevelButtonView nextZoomLevelButton;
    private ScreenshotService screenshotService;

    // Default values
    private static final int DEFAULT_SCROLL_DISTANCE = 773;
    private static final int DEFAULT_VERTICAL_SCROLL_DISTANCE = 773;
    private static final float DEFAULT_SQUARE_SIZE = 695f;
    private static final int DEFAULT_SQUARE_X = 346;
    private static final int DEFAULT_SQUARE_Y = 346;

    private int scrollDistance = DEFAULT_SCROLL_DISTANCE; // Horizontal scroll distance in pixels
    private int verticalScrollDistance = DEFAULT_VERTICAL_SCROLL_DISTANCE; // Vertical scroll distance in pixels
    private int scrollIncrement = 30; // Horizontal scroll increment, default 30 pixels
    private int verticalScrollIncrement = 30; // Vertical scroll increment, default 30 pixels
    private int screenshotCount = 0; // Track number of screenshots taken (number of horizontal scrolls)
    private int screenshotNumber = 1; // Sequential number for screenshot filenames (1, 2, 3...)
    private boolean nextScreenshotIsLineStart = false; // Flag to mark next screenshot with 'z' suffix
    private static final int MAX_SCROLL_DISTANCE = 1100; // Maximum scroll distance to avoid off-screen gestures
    private static final int SCROLL_DELAY_MS = 600; // Delay between sequential scrolls to account for momentum

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            if ("SHOW".equals(action)) {
                showOverlay();
            } else if ("HIDE".equals(action)) {
                hideOverlay();
            } else if ("SET_PROJECTION".equals(action)) {
                int resultCode = intent.getIntExtra("resultCode", 0);
                Intent data = intent.getParcelableExtra("data");
                if (screenshotService != null) {
                    screenshotService.startProjection(resultCode, data);
                }
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Square Overlay Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Running square overlay with screenshot capability");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForeground() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Square Overlay Active")
                .setContentText("Tap screenshot button to capture")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void showOverlay() {
        if (overlayView != null) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // Default square size and position
        float initialSquareSize = DEFAULT_SQUARE_SIZE;
        int squareX = DEFAULT_SQUARE_X;
        int squareY = DEFAULT_SQUARE_Y;

        int overlayWidth = (int)initialSquareSize;
        int overlayHeight = (int)initialSquareSize;

        overlayView = new SquareOverlayView(this, screenWidth, screenHeight, initialSquareSize);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                overlayWidth,
                overlayHeight,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = squareX;
        params.y = squareY;

        overlayView.setWindowManager(windowManager, params);

        screenshotService = new ScreenshotService(this);

        overlayView.setScreenshotCallback((xPercent, yPercent, widthPercent, heightPercent, onHidden) -> {
            if (screenshotService != null) {
                setAllViewsVisible(false);

                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    screenshotService.captureScreenshot(xPercent, yPercent, widthPercent, heightPercent, screenshotNumber, nextScreenshotIsLineStart);

                    nextScreenshotIsLineStart = false;
                    screenshotCount++;
                    screenshotNumber++;

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        scrollHorizontallyBySquareWidth();

                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            setAllViewsVisible(true);
                        }, 150);
                    }, 150);
                }, 50);
            } else {
                Toast.makeText(this, "Screenshot not initialized", Toast.LENGTH_SHORT).show();
            }
        });

        windowManager.addView(overlayView, params);

        // Create screenshot button at bottom center
        screenshotButton = new ScreenshotButtonView(this);

        int buttonLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buttonLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            buttonLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                buttonLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        buttonParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        buttonParams.y = 200; // Larger gap - closer to bottom
        buttonParams.x = 270; // Screenshot center in centered group

        screenshotButton.setOnClickListener(() -> {
            if (overlayView != null) {
                overlayView.triggerScreenshot();
            }
        });

        windowManager.addView(screenshotButton, buttonParams);

        // Create minus button (left of screenshot button)
        minusButton = new AdjustButtonView(this, "H-");

        int minusLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            minusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            minusLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams minusParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                minusLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        minusParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        minusParams.y = 450; // Horizontal calibration row (middle row)
        minusParams.x = 240; // Fourth position: Minus button (200px wide, center at 240)

        minusButton.setOnClickListener(() -> {
            // Scroll left by scrollIncrement
            scrollDistance -= scrollIncrement;
            if (counterDisplay != null) {
                counterDisplay.setCounter(scrollDistance);
            }
            performSmallScroll(-scrollIncrement);
        });

        windowManager.addView(minusButton, minusParams);

        // Create plus button (right of screenshot button)
        plusButton = new AdjustButtonView(this, "H+");

        int plusLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            plusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            plusLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams plusParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                plusLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        plusParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        plusParams.y = 450; // Horizontal calibration row (middle row)
        plusParams.x = 480; // Fifth position: Plus button (200px wide, center at 480)

        plusButton.setOnClickListener(() -> {
            // Scroll right by scrollIncrement
            if (scrollDistance + scrollIncrement <= MAX_SCROLL_DISTANCE) {
                scrollDistance += scrollIncrement;
                if (counterDisplay != null) {
                    counterDisplay.setCounter(scrollDistance);
                }
                performSmallScroll(scrollIncrement);
            } else {
                Toast.makeText(this, "Maximum scroll distance reached: " + MAX_SCROLL_DISTANCE + "px", Toast.LENGTH_SHORT).show();
            }
        });

        windowManager.addView(plusButton, plusParams);

        // Create V- button (vertical calibration minus)
        vMinusButton = new AdjustButtonView(this, "V-");

        int vMinusLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vMinusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            vMinusLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams vMinusParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                vMinusLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        vMinusParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        vMinusParams.y = 650; // Vertical calibration row (top row)
        vMinusParams.x = 240; // Same horizontal position as minus button

        vMinusButton.setOnClickListener(() -> {
            // Decrease vertical scroll distance by verticalScrollIncrement AND scroll up
            verticalScrollDistance -= verticalScrollIncrement;
            if (vCounterDisplay != null) {
                vCounterDisplay.setCounter(verticalScrollDistance);
            }
            performSmallVerticalScroll(-verticalScrollIncrement); // Negative = scroll UP
        });

        windowManager.addView(vMinusButton, vMinusParams);

        // Create V+ button (vertical calibration plus)
        vPlusButton = new AdjustButtonView(this, "V+");

        int vPlusLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vPlusLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            vPlusLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams vPlusParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                vPlusLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        vPlusParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        vPlusParams.y = 650; // Vertical calibration row (same as V-)
        vPlusParams.x = 480; // Same horizontal position as plus button

        vPlusButton.setOnClickListener(() -> {
            // Increase vertical scroll distance by verticalScrollIncrement AND scroll down
            if (verticalScrollDistance + verticalScrollIncrement <= MAX_SCROLL_DISTANCE) {
                verticalScrollDistance += verticalScrollIncrement;
                if (vCounterDisplay != null) {
                    vCounterDisplay.setCounter(verticalScrollDistance);
                }
                performSmallVerticalScroll(verticalScrollIncrement); // Positive = scroll DOWN
            } else {
                Toast.makeText(this, "Maximum vertical scroll distance reached: " + MAX_SCROLL_DISTANCE + "px", Toast.LENGTH_SHORT).show();
            }
        });

        windowManager.addView(vPlusButton, vPlusParams);

        // Create vertical scroll increment input
        vScrollIncrementInput = new ScrollIncrementInputView(this);
        vScrollIncrementInput.setValue(verticalScrollIncrement); // Set default value

        int vInputLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vInputLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            vInputLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams vInputParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                vInputLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        vInputParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        vInputParams.y = 650; // Vertical calibration row
        vInputParams.x = -530; // Same horizontal position as scroll increment input

        // Set up listener to update verticalScrollIncrement when value changes

        // Handle click to open dialog for editing
        vScrollIncrementInput.setOnClickListener(() -> {
            showScrollIncrementDialog(true);
        });

        windowManager.addView(vScrollIncrementInput, vInputParams);

        // Create vertical test button
        vTestButton = new TestButtonView(this);

        int vTestLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vTestLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            vTestLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams vTestParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                vTestLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        vTestParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        vTestParams.y = 650; // Vertical calibration row (top row)
        vTestParams.x = 0; // Same horizontal position as horizontal test button

        vTestButton.setOnClickListener(() -> {
            performVerticalTest();
        });

        windowManager.addView(vTestButton, vTestParams);

        // Create scroll increment input (to the left of minus button)
        scrollIncrementInput = new ScrollIncrementInputView(this);
        scrollIncrementInput.setValue(scrollIncrement); // Set default value

        int inputLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            inputLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            inputLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams inputParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                inputLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        inputParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        inputParams.y = 450; // Horizontal calibration row (middle row)
        inputParams.x = -530; // First position: Edit button (100px wide, center at -530)

        // Set up listener to update scrollIncrement when value changes

        // Handle click to open dialog for editing
        scrollIncrementInput.setOnClickListener(() -> {
            showScrollIncrementDialog(false);
        });

        windowManager.addView(scrollIncrementInput, inputParams);

        // Create horizontal test button
        hTestButton = new TestButtonView(this);

        int hTestLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hTestLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            hTestLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams hTestParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                hTestLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        hTestParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        hTestParams.y = 450; // Horizontal calibration row (middle row)
        hTestParams.x = 0; // Third position: Test button (200px wide, centered at 0)

        hTestButton.setOnClickListener(() -> {
            performHorizontalTest();
        });

        windowManager.addView(hTestButton, hTestParams);

        // Create counter display above the button row
        counterDisplay = new CounterDisplayView(this);
        counterDisplay.setCounter(scrollDistance); // Set initial value to default scrollDistance

        int counterLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            counterLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            counterLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams counterParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                counterLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        counterParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        counterParams.y = 450; // Horizontal calibration row (middle row)
        counterParams.x = -290; // Second position: Counter display (300px wide, center at -290)

        windowManager.addView(counterDisplay, counterParams);

        // Create vertical counter display (shows vertical scroll distance)
        vCounterDisplay = new CounterDisplayView(this);
        vCounterDisplay.setCounter(verticalScrollDistance); // Set initial value to default vertical scrollDistance

        int vCounterLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vCounterLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            vCounterLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams vCounterParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                vCounterLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        vCounterParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        vCounterParams.y = 650; // Vertical calibration row
        vCounterParams.x = -290; // Same horizontal position as regular counter display

        windowManager.addView(vCounterDisplay, vCounterParams);

        // Create next line button (to the right of plus button)
        nextLineButton = new NextLineButtonView(this);

        int nextLineLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nextLineLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            nextLineLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams nextLineParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                nextLineLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        nextLineParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        nextLineParams.y = 200; // Same row as screenshot button
        nextLineParams.x = -40; // Go down center in centered group

        nextLineButton.setOnClickListener(() -> {
            // Mark next screenshot to have 'z' suffix
            nextScreenshotIsLineStart = true;
            goDownOneLine();
        });

        windowManager.addView(nextLineButton, nextLineParams);

        // Create reset button (to the left of file browser button)
        resetButton = new ResetButtonView(this);

        int resetLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            resetLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            resetLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams resetParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                resetLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        resetParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        resetParams.y = 200; // Same row as screenshot button
        resetParams.x = -540; // Reset center in centered group

        resetButton.setOnClickListener(() -> {
            // Reset scroll distances to defaults
            scrollDistance = DEFAULT_SCROLL_DISTANCE;
            verticalScrollDistance = DEFAULT_VERTICAL_SCROLL_DISTANCE;
            if (counterDisplay != null) {
                counterDisplay.setCounter(scrollDistance);
            }
            if (vCounterDisplay != null) {
                vCounterDisplay.setCounter(verticalScrollDistance);
            }

            // Reset scroll increments to defaults
            scrollIncrement = 30;
            verticalScrollIncrement = 30;
            if (scrollIncrementInput != null) {
                scrollIncrementInput.setValue(scrollIncrement);
            }
            if (vScrollIncrementInput != null) {
                vScrollIncrementInput.setValue(verticalScrollIncrement);
            }

            // Reset square position and size to defaults
            if (overlayView != null && windowManager != null) {
                WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) overlayView.getLayoutParams();
                if (overlayParams != null) {
                    // Reset position
                    overlayParams.x = DEFAULT_SQUARE_X;
                    overlayParams.y = DEFAULT_SQUARE_Y;

                    // Reset size
                    overlayParams.width = (int) DEFAULT_SQUARE_SIZE;
                    overlayParams.height = (int) DEFAULT_SQUARE_SIZE;

                    // Apply changes
                    windowManager.updateViewLayout(overlayView, overlayParams);

                    // Reset the overlay view's internal size
                    overlayView.setSquareSize(DEFAULT_SQUARE_SIZE);
                }
            }
        });

        windowManager.addView(resetButton, resetParams);

        // Create file browser button (to the right of reset button)
        fileBrowserButton = new FileBrowserButtonView(this);

        int fileBrowserLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fileBrowserLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            fileBrowserLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams fileBrowserParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                fileBrowserLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        fileBrowserParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        fileBrowserParams.y = 200; // Same row as other buttons
        fileBrowserParams.x = -310; // Gallery center in centered group

        fileBrowserButton.setOnClickListener(() -> {
            openScreenshotFolder();
        });

        windowManager.addView(fileBrowserButton, fileBrowserParams);

        // Create next zoom level button (to the right of screenshot button)
        nextZoomLevelButton = new NextZoomLevelButtonView(this);

        int nextZoomLevelLayoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nextZoomLevelLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            nextZoomLevelLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams nextZoomLevelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                nextZoomLevelLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        nextZoomLevelParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        nextZoomLevelParams.y = 200; // Same row as screenshot button
        nextZoomLevelParams.x = 540; // Zoom center in centered group

        nextZoomLevelButton.setOnClickListener(() -> {
            processNextZoomLevel();
        });

        windowManager.addView(nextZoomLevelButton, nextZoomLevelParams);
    }

    private void hideOverlay() {
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
        if (screenshotButton != null && windowManager != null) {
            windowManager.removeView(screenshotButton);
            screenshotButton = null;
        }
        if (minusButton != null && windowManager != null) {
            windowManager.removeView(minusButton);
            minusButton = null;
        }
        if (plusButton != null && windowManager != null) {
            windowManager.removeView(plusButton);
            plusButton = null;
        }
        if (resetButton != null && windowManager != null) {
            windowManager.removeView(resetButton);
            resetButton = null;
        }
        if (counterDisplay != null && windowManager != null) {
            windowManager.removeView(counterDisplay);
            counterDisplay = null;
        }
        if (scrollIncrementInput != null && windowManager != null) {
            windowManager.removeView(scrollIncrementInput);
            scrollIncrementInput = null;
        }
        if (nextLineButton != null && windowManager != null) {
            windowManager.removeView(nextLineButton);
            nextLineButton = null;
        }
        if (fileBrowserButton != null && windowManager != null) {
            windowManager.removeView(fileBrowserButton);
            fileBrowserButton = null;
        }
        if (nextZoomLevelButton != null && windowManager != null) {
            windowManager.removeView(nextZoomLevelButton);
            nextZoomLevelButton = null;
        }
        if (vMinusButton != null && windowManager != null) {
            windowManager.removeView(vMinusButton);
            vMinusButton = null;
        }
        if (vPlusButton != null && windowManager != null) {
            windowManager.removeView(vPlusButton);
            vPlusButton = null;
        }
        if (vCounterDisplay != null && windowManager != null) {
            windowManager.removeView(vCounterDisplay);
            vCounterDisplay = null;
        }
        if (vScrollIncrementInput != null && windowManager != null) {
            windowManager.removeView(vScrollIncrementInput);
            vScrollIncrementInput = null;
        }
        if (hTestButton != null && windowManager != null) {
            windowManager.removeView(hTestButton);
            hTestButton = null;
        }
        if (vTestButton != null && windowManager != null) {
            windowManager.removeView(vTestButton);
            vTestButton = null;
        }
    }

    private void scrollHorizontallyBySquareWidth() {

        if (overlayView == null || windowManager == null) {
            return;
        }

        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            Toast.makeText(this, "Please enable Square Overlay accessibility service in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            // Get the square's ACTUAL position on screen
            int[] location = new int[2];
            overlayView.getLocationOnScreen(location);
            int squareX = location[0];
            int squareY = location[1];
            int squareSize = overlayView.getWidth();

            // Perform gesture OUTSIDE the square, to the right of it, at same Y level
            // This way we touch the actual content, not the overlay window
            int gestureY = squareY + squareSize / 2;
            int gestureStartX = squareX + squareSize + 50; // 50px to the right of square
            int swipeDistance = scrollDistance; // Use the accumulated calibration value
            int startX = gestureStartX;
            int endX = startX - swipeDistance;

            // Use slower duration for more precise scrolling
            int duration = 500;


            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, duration);
        } catch (Exception e) {
        }
    }

    private void performSmallScroll(int distance) {
        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            return;
        }

        if (windowManager == null || overlayView == null) {
            return;
        }

        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);

            // Get the square's ACTUAL position on screen
            int[] location = new int[2];
            overlayView.getLocationOnScreen(location);
            int squareX = location[0];
            int squareY = location[1];
            int squareSize = overlayView.getWidth();

            // Perform gesture OUTSIDE the square, to the right of it, at same Y level
            // This way we touch the actual content, not the overlay window
            int gestureY = squareY + squareSize / 2;
            int gestureStartX = squareX + squareSize + 50; // 50px to the right of square
            int startX = gestureStartX;
            int endX = startX - distance; // distance can be positive or negative

            // Slower gesture for better scroll recognition (300ms is typical for scroll gestures)
            int duration = 300;

            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, duration);
        } catch (Exception e) {
        }
    }

    private void performSmallVerticalScroll(int distance) {
        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            return;
        }

        if (windowManager == null) {
            return;
        }

        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            // Perform gesture at center of screen
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            // distance > 0 = scroll DOWN (swipe UP)
            // distance < 0 = scroll UP (swipe DOWN)
            int startY = centerY + (distance / 2);
            int endY = centerY - (distance / 2);

            // Slower gesture for better scroll recognition
            int duration = 300;

            accessibilityService.performVerticalScroll(centerX, startY, centerX, endY, duration);
        } catch (Exception e) {
        }
    }

    private void openScreenshotFolder() {

        try {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File appDir = new File(picturesDir, "SquareOverlay");

            // Create directory if it doesn't exist
            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            // Simple approach: Just open any file manager and let user navigate
            // Use ACTION_OPEN_DOCUMENT_TREE or ACTION_VIEW with the Pictures directory
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3APictures%2FSquareOverlay"),
                                  "vnd.android.document/directory");
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } catch (Exception e) {

            // Fallback: Launch OnePlus File Manager (what was working before)
            try {
                android.content.Intent intent = new android.content.Intent();
                intent.setClassName("com.oneplus.filemanager", "com.oplus.filemanager.main.ui.MainActivity");
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open file manager", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goDownOneLine() {

        if (screenshotCount == 0) {
            Toast.makeText(this, "No screenshots taken yet", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            Toast.makeText(this, "Please enable Square Overlay accessibility service in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        if (overlayView == null || windowManager == null) {
            return;
        }

        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            // Perform gestures in the CENTER of the screen for consistent scroll distance
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            // First, scroll down by the square size (one line)
            // Swipe UP to scroll DOWN content
            int verticalDistance = verticalScrollDistance; // Use separate vertical calibration
            int startY = centerY + (verticalDistance / 2);
            int endY = centerY - (verticalDistance / 2);


            accessibilityService.performVerticalScroll(centerX, startY, centerX, endY, 500);

            // Wait for vertical scroll to complete with momentum before starting horizontal scrolls
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                // Now scroll left, one scroll at a time with delays
                scrollLeftSequentially(0, screenshotCount);
            }, SCROLL_DELAY_MS);

        } catch (Exception e) {
        }
    }

    private void scrollLeftSequentially(int currentScroll, int totalScrolls) {
        if (currentScroll >= totalScrolls) {
            // Done with all scrolls, reset screenshot counter
            screenshotCount = 0;
            Toast.makeText(this, "Moved to next line", Toast.LENGTH_SHORT).show();
            return;
        }


        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null || windowManager == null) {
            return;
        }

        try {
            // Perform gestures in the CENTER of the screen for consistent scroll distance
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            // Swipe RIGHT to scroll LEFT (back to beginning of line)
            int startX = centerX - (scrollDistance / 2);
            int endX = centerX + (scrollDistance / 2);

            accessibilityService.performHorizontalScroll(startX, centerY, endX, centerY, 500);

            // Wait for scroll to complete with momentum, then do next scroll
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                scrollLeftSequentially(currentScroll + 1, totalScrolls);
            }, SCROLL_DELAY_MS);

        } catch (Exception e) {
        }
    }

    private void processNextZoomLevel() {

        try {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File appDir = new File(picturesDir, "SquareOverlay");

            if (!appDir.exists() || !appDir.isDirectory()) {
                Toast.makeText(this, "Output directory not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Find all PNG files in the output directory
            File[] pngFiles = appDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") && !name.startsWith("zoom"));

            if (pngFiles == null || pngFiles.length == 0) {
                Toast.makeText(this, "No images found to stitch", Toast.LENGTH_SHORT).show();
                return;
            }

            // Find the next zoom level number
            int nextZoomLevel = 1;
            while (new File(appDir, "zoom" + nextZoomLevel).exists()) {
                nextZoomLevel++;
            }


            // Create the zoom folder
            File zoomFolder = new File(appDir, "zoom" + nextZoomLevel);
            if (!zoomFolder.mkdirs()) {
                Toast.makeText(this, "Failed to create zoom folder", Toast.LENGTH_SHORT).show();
                return;
            }

            // Move all PNG files to the zoom folder
            for (File file : pngFiles) {
                File dest = new File(zoomFolder, file.getName());
                if (!file.renameTo(dest)) {
                }
            }

            // Reset counters after successfully moving files
            screenshotCount = 0;
            screenshotNumber = 1;

            // Now stitch the images using ImageMagick
            stitchImages(zoomFolder, new File(appDir, "zoom" + nextZoomLevel + ".png"));

            Toast.makeText(this, "Zoom level " + nextZoomLevel + " created", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to create zoom level", Toast.LENGTH_SHORT).show();
        }
    }

    private void stitchImages(File sourceFolder, File outputFile) {
        new Thread(() -> {
            try {

                // Get all image files and sort them
                File[] imageFiles = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

                if (imageFiles == null || imageFiles.length == 0) {
                    return;
                }

                // Sort files numerically (extract number from filename)
                java.util.Arrays.sort(imageFiles, (f1, f2) -> {
                    String name1 = f1.getName().replaceAll("[^0-9]", "");
                    String name2 = f2.getName().replaceAll("[^0-9]", "");
                    int num1 = name1.isEmpty() ? 0 : Integer.parseInt(name1);
                    int num2 = name2.isEmpty() ? 0 : Integer.parseInt(name2);
                    return Integer.compare(num1, num2);
                });

                // Group files into rows (files with 'z' suffix start new rows)
                java.util.List<java.util.List<File>> rows = new java.util.ArrayList<>();
                java.util.List<File> currentRow = new java.util.ArrayList<>();

                for (File file : imageFiles) {
                    String name = file.getName();

                    // Check if this file starts a new row (has 'z' before .png)
                    if (name.contains("z.png") && !currentRow.isEmpty()) {
                        rows.add(currentRow);
                        currentRow = new java.util.ArrayList<>();
                    }

                    currentRow.add(file);
                }

                // Add the last row
                if (!currentRow.isEmpty()) {
                    rows.add(currentRow);
                }


                // Create row bitmaps
                java.util.List<android.graphics.Bitmap> rowBitmaps = new java.util.ArrayList<>();

                for (int i = 0; i < rows.size(); i++) {
                    java.util.List<File> row = rows.get(i);

                    // Load all bitmaps for this row
                    java.util.List<android.graphics.Bitmap> bitmapsInRow = new java.util.ArrayList<>();
                    int totalWidth = 0;
                    int maxHeight = 0;

                    for (File file : row) {
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bitmap != null) {
                            bitmapsInRow.add(bitmap);
                            totalWidth += bitmap.getWidth();
                            maxHeight = Math.max(maxHeight, bitmap.getHeight());
                        } else {
                        }
                    }

                    // Stitch this row horizontally
                    if (!bitmapsInRow.isEmpty()) {
                        android.graphics.Bitmap rowBitmap = android.graphics.Bitmap.createBitmap(
                            totalWidth, maxHeight, android.graphics.Bitmap.Config.ARGB_8888);
                        android.graphics.Canvas canvas = new android.graphics.Canvas(rowBitmap);

                        int xOffset = 0;
                        for (android.graphics.Bitmap bitmap : bitmapsInRow) {
                            canvas.drawBitmap(bitmap, xOffset, 0, null);
                            xOffset += bitmap.getWidth();
                            bitmap.recycle(); // Free memory
                        }

                        rowBitmaps.add(rowBitmap);
                    }
                }

                // Now stitch all rows vertically
                if (!rowBitmaps.isEmpty()) {
                    int maxWidth = 0;
                    int totalHeight = 0;

                    for (android.graphics.Bitmap rowBitmap : rowBitmaps) {
                        maxWidth = Math.max(maxWidth, rowBitmap.getWidth());
                        totalHeight += rowBitmap.getHeight();
                    }


                    android.graphics.Bitmap finalBitmap = android.graphics.Bitmap.createBitmap(
                        maxWidth, totalHeight, android.graphics.Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas finalCanvas = new android.graphics.Canvas(finalBitmap);

                    int yOffset = 0;
                    for (android.graphics.Bitmap rowBitmap : rowBitmaps) {
                        finalCanvas.drawBitmap(rowBitmap, 0, yOffset, null);
                        yOffset += rowBitmap.getHeight();
                        rowBitmap.recycle(); // Free memory
                    }

                    // Save the final bitmap
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
                    finalBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                    finalBitmap.recycle();


                    // Show success message on UI thread
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        Toast.makeText(OverlayService.this, "Zoom level stitched successfully!", Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(OverlayService.this, "Stitching failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void performHorizontalTest() {
        if (overlayView == null || screenshotService == null) {
            Toast.makeText(this, "Not ready for test", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide all UI elements for clean screenshot
        setAllViewsVisible(false);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Get square coordinates
            float xPercent = overlayView.getSquareXPercent();
            float yPercent = overlayView.getSquareYPercent();
            float widthPercent = overlayView.getSquareWidthPercent();
            float heightPercent = overlayView.getSquareHeightPercent();

            // Take first screenshot
            screenshotService.captureTestScreenshot(xPercent, yPercent, widthPercent, heightPercent,
                                                    "horizontal test", "before.png");

            // Wait for screenshot to complete, then scroll
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                // Perform horizontal scroll using the SAME method as screenshot button
                scrollHorizontallyBySquareWidth();

                // Wait for scroll to complete (500ms scroll duration + 600ms for momentum/settling)
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    screenshotService.captureTestScreenshot(xPercent, yPercent, widthPercent, heightPercent,
                                                            "horizontal test", "after.png");

                    // Wait for second screenshot to complete, then stitch and rotate
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        stitchAndRotateTestImages();
                        setAllViewsVisible(true);
                    }, 150);
                }, 1100);
            }, 150);
        }, 50);
    }

    private void performVerticalTest() {
        if (overlayView == null || screenshotService == null) {
            Toast.makeText(this, "Not ready for test", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            Toast.makeText(this, "Please enable Square Overlay accessibility service in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        // Hide all UI elements for clean screenshot
        setAllViewsVisible(false);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Get square coordinates
            float xPercent = overlayView.getSquareXPercent();
            float yPercent = overlayView.getSquareYPercent();
            float widthPercent = overlayView.getSquareWidthPercent();
            float heightPercent = overlayView.getSquareHeightPercent();

            // Take first screenshot
            screenshotService.captureTestScreenshot(xPercent, yPercent, widthPercent, heightPercent,
                                                    "vertical test", "before.png");

            // Wait for screenshot to complete, then scroll
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                // Perform vertical scroll using the same method as goDownOneLine
                try {
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                    int screenWidth = displayMetrics.widthPixels;
                    int screenHeight = displayMetrics.heightPixels;

                    int centerX = screenWidth / 2;
                    int centerY = screenHeight / 2;

                    // Scroll down by verticalScrollDistance
                    int verticalDistance = verticalScrollDistance;
                    int startY = centerY + (verticalDistance / 2);
                    int endY = centerY - (verticalDistance / 2);

                    accessibilityService.performVerticalScroll(centerX, startY, centerX, endY, 500);
                } catch (Exception e) {
                }

                // Wait for scroll to complete (500ms scroll duration + 600ms for momentum/settling)
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    screenshotService.captureTestScreenshot(xPercent, yPercent, widthPercent, heightPercent,
                                                            "vertical test", "after.png");

                    // Wait for second screenshot to complete, then stitch
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        stitchTestImages("vertical test");
                        setAllViewsVisible(true);
                    }, 150);
                }, 1100);
            }, 150);
        }, 50);
    }

    private void stitchAndRotateTestImages() {
        stitchTestImages("horizontal test", true);
    }

    private void stitchTestImages(String testFolder) {
        stitchTestImages(testFolder, false);
    }

    private void stitchTestImages(String testFolder, boolean stitchHorizontally) {
        new Thread(() -> {
            try {
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File appDir = new File(picturesDir, "SquareOverlay");
                File testDir = new File(appDir, testFolder);

                File beforeFile = new File(testDir, "before.png");
                File afterFile = new File(testDir, "after.png");

                if (!beforeFile.exists() || !afterFile.exists()) {
                    return;
                }

                // Load the two images
                android.graphics.Bitmap beforeBitmap = android.graphics.BitmapFactory.decodeFile(beforeFile.getAbsolutePath());
                android.graphics.Bitmap afterBitmap = android.graphics.BitmapFactory.decodeFile(afterFile.getAbsolutePath());

                if (beforeBitmap == null || afterBitmap == null) {
                    return;
                }

                android.graphics.Bitmap stitchedBitmap;
                android.graphics.Canvas canvas;

                if (stitchHorizontally) {
                    // Stitch horizontally (side by side) - for horizontal test
                    int totalWidth = beforeBitmap.getWidth() + afterBitmap.getWidth();
                    int maxHeight = Math.max(beforeBitmap.getHeight(), afterBitmap.getHeight());

                    stitchedBitmap = android.graphics.Bitmap.createBitmap(
                        totalWidth, maxHeight, android.graphics.Bitmap.Config.ARGB_8888);
                    canvas = new android.graphics.Canvas(stitchedBitmap);

                    // Draw before image on the left
                    canvas.drawBitmap(beforeBitmap, 0, 0, null);
                    // Draw after image on the right
                    canvas.drawBitmap(afterBitmap, beforeBitmap.getWidth(), 0, null);
                } else {
                    // Stitch vertically (before above, after below) - for vertical test
                    int maxWidth = Math.max(beforeBitmap.getWidth(), afterBitmap.getWidth());
                    int totalHeight = beforeBitmap.getHeight() + afterBitmap.getHeight();

                    stitchedBitmap = android.graphics.Bitmap.createBitmap(
                        maxWidth, totalHeight, android.graphics.Bitmap.Config.ARGB_8888);
                    canvas = new android.graphics.Canvas(stitchedBitmap);

                    // Draw before image on top
                    canvas.drawBitmap(beforeBitmap, 0, 0, null);
                    // Draw after image on bottom
                    canvas.drawBitmap(afterBitmap, 0, beforeBitmap.getHeight(), null);
                }

                // Save stitched image
                File stitchedFile = new File(testDir, "stitched.png");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(stitchedFile);
                stitchedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                // Clean up
                beforeBitmap.recycle();
                afterBitmap.recycle();
                stitchedBitmap.recycle();

                // Show success message and open the image on UI thread
                String testName = testFolder.substring(0, 1).toUpperCase() + testFolder.substring(1);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(OverlayService.this, testName + " complete", Toast.LENGTH_SHORT).show();
                    openImageFile(stitchedFile);
                });

            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(OverlayService.this, "Stitching failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void openImageFile(File imageFile) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            android.net.Uri uri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android N and above
                uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
                );
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = android.net.Uri.fromFile(imageFile);
            }

            intent.setDataAndType(uri, "image/*");
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setAllViewsVisible(boolean visible) {
        int visibility = visible ? android.view.View.VISIBLE : android.view.View.INVISIBLE;

        if (overlayView != null) overlayView.setVisibility(visibility);
        if (screenshotButton != null) screenshotButton.setVisibility(visibility);
        if (minusButton != null) minusButton.setVisibility(visibility);
        if (plusButton != null) plusButton.setVisibility(visibility);
        if (resetButton != null) resetButton.setVisibility(visibility);
        if (counterDisplay != null) counterDisplay.setVisibility(visibility);
        if (scrollIncrementInput != null) scrollIncrementInput.setVisibility(visibility);
        if (nextLineButton != null) nextLineButton.setVisibility(visibility);
        if (fileBrowserButton != null) fileBrowserButton.setVisibility(visibility);
        if (nextZoomLevelButton != null) nextZoomLevelButton.setVisibility(visibility);
        if (vMinusButton != null) vMinusButton.setVisibility(visibility);
        if (vPlusButton != null) vPlusButton.setVisibility(visibility);
        if (vCounterDisplay != null) vCounterDisplay.setVisibility(visibility);
        if (vScrollIncrementInput != null) vScrollIncrementInput.setVisibility(visibility);
        if (hTestButton != null) hTestButton.setVisibility(visibility);
        if (vTestButton != null) vTestButton.setVisibility(visibility);
    }

    private void showScrollIncrementDialog(boolean isVertical) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Set " + (isVertical ? "Vertical " : "") + "Scroll Increment (px)");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(isVertical ? verticalScrollIncrement : scrollIncrement));
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                int newValue = Integer.parseInt(input.getText().toString());
                if (newValue > 0 && newValue <= 500) {
                    if (isVertical) {
                        verticalScrollIncrement = newValue;
                        if (vScrollIncrementInput != null) {
                            vScrollIncrementInput.setValue(verticalScrollIncrement);
                        }
                    } else {
                        scrollIncrement = newValue;
                        if (scrollIncrementInput != null) {
                            scrollIncrementInput.setValue(scrollIncrement);
                        }
                    }
                }
            } catch (NumberFormatException e) {
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        android.app.AlertDialog dialog = builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
        if (screenshotService != null) {
            screenshotService.release();
            screenshotService = null;
        }
    }
}
