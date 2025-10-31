package com.squareoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "SquareOverlayChannel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private SquareOverlayView overlayView;
    private ScreenshotButtonView screenshotButton;
    private AdjustButtonView minusButton;
    private AdjustButtonView plusButton;
    private AdjustButtonView resetButton;
    private CounterDisplayView counterDisplay;
    private ScrollIncrementInputView scrollIncrementInput;
    private NextLineButtonView nextLineButton;
    private ScreenshotService screenshotService;
    private int scrollDistance = 0; // Accumulated scroll distance in pixels
    private int scrollIncrement = 30; // Dynamic scroll increment, default 30 pixels
    private int screenshotCount = 0; // Track number of screenshots taken (number of horizontal scrolls)
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

        float initialSquareSize = screenWidth * 0.4f;
        int squareX = (int)((screenWidth - initialSquareSize) / 2);
        int squareY = (int)((screenHeight - initialSquareSize) / 2);

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
                // Hide overlay and buttons
                overlayView.setVisibility(android.view.View.INVISIBLE);
                if (screenshotButton != null) {
                    screenshotButton.setVisibility(android.view.View.INVISIBLE);
                }
                if (minusButton != null) {
                    minusButton.setVisibility(android.view.View.INVISIBLE);
                }
                if (plusButton != null) {
                    plusButton.setVisibility(android.view.View.INVISIBLE);
                }
                if (resetButton != null) {
                    resetButton.setVisibility(android.view.View.INVISIBLE);
                }
                if (counterDisplay != null) {
                    counterDisplay.setVisibility(android.view.View.INVISIBLE);
                }
                if (scrollIncrementInput != null) {
                    scrollIncrementInput.setVisibility(android.view.View.INVISIBLE);
                }
                if (nextLineButton != null) {
                    nextLineButton.setVisibility(android.view.View.INVISIBLE);
                }

                // Wait for UI to update, then capture
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    screenshotService.captureScreenshot(xPercent, yPercent, widthPercent, heightPercent);

                    // Increment screenshot counter
                    screenshotCount++;
                    android.util.Log.d("OverlayService", "Screenshot taken, count now: " + screenshotCount);

                    // Scroll horizontally by the width of the square after screenshot
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        scrollHorizontallyBySquareWidth();

                        // Show overlay and buttons again after scrolling
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            overlayView.setVisibility(android.view.View.VISIBLE);
                            if (screenshotButton != null) {
                                screenshotButton.setVisibility(android.view.View.VISIBLE);
                            }
                            if (minusButton != null) {
                                minusButton.setVisibility(android.view.View.VISIBLE);
                            }
                            if (plusButton != null) {
                                plusButton.setVisibility(android.view.View.VISIBLE);
                            }
                            if (resetButton != null) {
                                resetButton.setVisibility(android.view.View.VISIBLE);
                            }
                            if (counterDisplay != null) {
                                counterDisplay.setVisibility(android.view.View.VISIBLE);
                            }
                            if (scrollIncrementInput != null) {
                                scrollIncrementInput.setVisibility(android.view.View.VISIBLE);
                            }
                            if (nextLineButton != null) {
                                nextLineButton.setVisibility(android.view.View.VISIBLE);
                            }
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
        buttonParams.y = 350; // 350 pixels from bottom

        screenshotButton.setOnClickListener(() -> {
            if (overlayView != null) {
                overlayView.triggerScreenshot();
            }
        });

        windowManager.addView(screenshotButton, buttonParams);

        // Create minus button (left of screenshot button)
        minusButton = new AdjustButtonView(this, "-");

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
        minusParams.y = 550; // Above screenshot button
        minusParams.x = 240; // Fourth position: Minus button (200px wide, center at 240)

        minusButton.setOnClickListener(() -> {
            // Scroll left by scrollIncrement
            scrollDistance -= scrollIncrement;
            if (counterDisplay != null) {
                counterDisplay.setCounter(scrollDistance);
            }
            performSmallScroll(-scrollIncrement);
            android.util.Log.d("OverlayService", "Scrolled left, distance now: " + scrollDistance + "px");
        });

        windowManager.addView(minusButton, minusParams);

        // Create plus button (right of screenshot button)
        plusButton = new AdjustButtonView(this, "+");

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
        plusParams.y = 550; // Above screenshot button
        plusParams.x = 480; // Fifth position: Plus button (200px wide, center at 480)

        plusButton.setOnClickListener(() -> {
            // Scroll right by scrollIncrement
            if (scrollDistance + scrollIncrement <= MAX_SCROLL_DISTANCE) {
                scrollDistance += scrollIncrement;
                if (counterDisplay != null) {
                    counterDisplay.setCounter(scrollDistance);
                }
                performSmallScroll(scrollIncrement);
                android.util.Log.d("OverlayService", "Scrolled right, distance now: " + scrollDistance + "px");
            } else {
                Toast.makeText(this, "Maximum scroll distance reached: " + MAX_SCROLL_DISTANCE + "px", Toast.LENGTH_SHORT).show();
                android.util.Log.d("OverlayService", "Cannot scroll further, max reached: " + MAX_SCROLL_DISTANCE + "px");
            }
        });

        windowManager.addView(plusButton, plusParams);

        // Create reset button (above screenshot button)
        resetButton = new AdjustButtonView(this, "R");

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
        resetParams.y = 550; // Above screenshot button - same row as others
        resetParams.x = 0; // Third position: Reset button (200px wide, centered at 0)

        resetButton.setOnClickListener(() -> {
            scrollDistance = 0;
            if (counterDisplay != null) {
                counterDisplay.setCounter(scrollDistance);
            }
            android.util.Log.d("OverlayService", "Scroll distance reset to 0");
        });

        windowManager.addView(resetButton, resetParams);

        // Create scroll increment input (to the left of reset button)
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
        inputParams.y = 550; // Same row as other buttons
        inputParams.x = -530; // First position: Edit button (100px wide, center at -530)

        // Set up listener to update scrollIncrement when value changes
        scrollIncrementInput.setOnValueChangedListener(newValue -> {
            scrollIncrement = newValue;
            android.util.Log.d("OverlayService", "Scroll increment changed to: " + scrollIncrement + "px");
        });

        // Handle click to open dialog for editing
        scrollIncrementInput.setOnClickListener(() -> {
            android.util.Log.d("OverlayService", "ScrollIncrementInput clicked!");
            showScrollIncrementDialog();
        });

        windowManager.addView(scrollIncrementInput, inputParams);

        // Create counter display above the button row
        counterDisplay = new CounterDisplayView(this);

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
        counterParams.y = 550; // Same row as other buttons
        counterParams.x = -290; // Second position: Counter display (300px wide, center at -290)

        windowManager.addView(counterDisplay, counterParams);

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
        nextLineParams.y = 350; // Same row as screenshot button
        nextLineParams.x = 250; // To the right of screenshot button

        nextLineButton.setOnClickListener(() -> {
            goDownOneLine();
        });

        windowManager.addView(nextLineButton, nextLineParams);
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
    }

    private void scrollHorizontallyBySquareWidth() {
        android.util.Log.d("OverlayService", "scrollHorizontallyBySquareWidth called");

        if (overlayView == null || windowManager == null) {
            android.util.Log.e("OverlayService", "Cannot scroll: overlayView or windowManager is null");
            return;
        }

        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            android.util.Log.e("OverlayService", "Accessibility service not enabled. Please enable it in Settings > Accessibility");
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

            android.util.Log.d("OverlayService", "Performing scroll gesture with calibrated distance: " + scrollDistance + "px");
            android.util.Log.d("OverlayService", "Square actual position: (" + squareX + "," + squareY + ") size=" + squareSize);
            android.util.Log.d("OverlayService", "Swipe from (" + startX + "," + gestureY + ") to (" + endX + "," + gestureY + ")");

            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, duration);
        } catch (Exception e) {
            android.util.Log.e("OverlayService", "Failed to scroll: " + e.getMessage(), e);
        }
    }

    private void performSmallScroll(int distance) {
        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            android.util.Log.e("OverlayService", "Accessibility service not enabled");
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

            android.util.Log.d("OverlayService", "Small scroll OUTSIDE square: (" + startX + "," + gestureY + ") to (" + endX + "," + gestureY + ")");
            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, duration);
        } catch (Exception e) {
            android.util.Log.e("OverlayService", "Failed to perform small scroll: " + e.getMessage(), e);
        }
    }

    private void showScrollIncrementDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Set Scroll Increment (px)");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(scrollIncrement));
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                int newValue = Integer.parseInt(input.getText().toString());
                if (newValue > 0 && newValue <= 500) {
                    scrollIncrement = newValue;
                    if (scrollIncrementInput != null) {
                        scrollIncrementInput.setValue(scrollIncrement);
                    }
                    android.util.Log.d("OverlayService", "Scroll increment updated to: " + scrollIncrement + "px");
                }
            } catch (NumberFormatException e) {
                android.util.Log.e("OverlayService", "Invalid number entered");
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

    private void goDownOneLine() {
        android.util.Log.d("OverlayService", "goDownOneLine called, screenshotCount=" + screenshotCount);

        if (screenshotCount == 0) {
            Toast.makeText(this, "No screenshots taken yet", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollAccessibilityService accessibilityService = ScrollAccessibilityService.getInstance();
        if (accessibilityService == null) {
            android.util.Log.e("OverlayService", "Accessibility service not enabled");
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
            int verticalDistance = scrollDistance;
            int startY = centerY + (verticalDistance / 2);
            int endY = centerY - (verticalDistance / 2);

            android.util.Log.d("OverlayService", "Step 1: Scrolling down by " + verticalDistance + "px");
            android.util.Log.d("OverlayService", "Vertical swipe from (" + centerX + "," + startY + ") to (" + centerX + "," + endY + ")");

            accessibilityService.performVerticalScroll(centerX, startY, centerX, endY, 500);

            // Wait for vertical scroll to complete with momentum before starting horizontal scrolls
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                // Now scroll left, one scroll at a time with delays
                scrollLeftSequentially(0, screenshotCount);
            }, SCROLL_DELAY_MS);

        } catch (Exception e) {
            android.util.Log.e("OverlayService", "Failed to go down one line: " + e.getMessage(), e);
        }
    }

    private void scrollLeftSequentially(int currentScroll, int totalScrolls) {
        if (currentScroll >= totalScrolls) {
            // Done with all scrolls, reset screenshot counter
            android.util.Log.d("OverlayService", "All left scrolls completed, resetting screenshot counter");
            screenshotCount = 0;
            Toast.makeText(this, "Moved to next line", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("OverlayService", "Left scroll " + (currentScroll + 1) + " of " + totalScrolls);

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

            android.util.Log.d("OverlayService", "Left scroll from (" + startX + "," + centerY + ") to (" + endX + "," + centerY + ")");
            accessibilityService.performHorizontalScroll(startX, centerY, endX, centerY, 500);

            // Wait for scroll to complete with momentum, then do next scroll
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                scrollLeftSequentially(currentScroll + 1, totalScrolls);
            }, SCROLL_DELAY_MS);

        } catch (Exception e) {
            android.util.Log.e("OverlayService", "Failed to scroll left: " + e.getMessage(), e);
        }
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
