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
    private ScreenshotService screenshotService;
    private int scrollDistance = 0; // Accumulated scroll distance in pixels
    private static final int SCROLL_INCREMENT = 30; // Each +/- click scrolls 30 pixels
    private static final int MAX_SCROLL_DISTANCE = 1100; // Maximum scroll distance to avoid off-screen gestures

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

                // Wait for UI to update, then capture
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    screenshotService.captureScreenshot(xPercent, yPercent, widthPercent, heightPercent);

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
        minusParams.x = -30; // Second position (R, -, +, counter)

        minusButton.setOnClickListener(() -> {
            // Scroll left by SCROLL_INCREMENT
            scrollDistance -= SCROLL_INCREMENT;
            if (counterDisplay != null) {
                counterDisplay.setCounter(scrollDistance);
            }
            performSmallScroll(-SCROLL_INCREMENT);
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
        plusParams.x = 210; // Third position (R, -, +, counter)

        plusButton.setOnClickListener(() -> {
            // Scroll right by SCROLL_INCREMENT
            if (scrollDistance + SCROLL_INCREMENT <= MAX_SCROLL_DISTANCE) {
                scrollDistance += SCROLL_INCREMENT;
                if (counterDisplay != null) {
                    counterDisplay.setCounter(scrollDistance);
                }
                performSmallScroll(SCROLL_INCREMENT);
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
        resetParams.x = -250; // First position (R, -, +, counter)

        resetButton.setOnClickListener(() -> {
            scrollDistance = 0;
            if (counterDisplay != null) {
                counterDisplay.setCounter(scrollDistance);
            }
            android.util.Log.d("OverlayService", "Scroll distance reset to 0");
        });

        windowManager.addView(resetButton, resetParams);

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
        counterParams.y = 540; // Vertically centered with buttons (10px offset for alignment)
        counterParams.x = 500; // Fourth position (R, -, +, counter)

        windowManager.addView(counterDisplay, counterParams);
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

            // Quick gesture for small increments
            int duration = 100;

            android.util.Log.d("OverlayService", "Small scroll OUTSIDE square: (" + startX + "," + gestureY + ") to (" + endX + "," + gestureY + ")");
            accessibilityService.performHorizontalScroll(startX, gestureY, endX, gestureY, duration);
        } catch (Exception e) {
            android.util.Log.e("OverlayService", "Failed to perform small scroll: " + e.getMessage(), e);
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
