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
    private ScreenshotService screenshotService;

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
                // Hide overlay
                overlayView.setVisibility(android.view.View.INVISIBLE);

                // Wait for UI to update, then capture
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    screenshotService.captureScreenshot(xPercent, yPercent, widthPercent, heightPercent);

                    // Show overlay again after capture
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        overlayView.setVisibility(android.view.View.VISIBLE);
                    }, 150);
                }, 50);
            } else {
                Toast.makeText(this, "Screenshot not initialized", Toast.LENGTH_SHORT).show();
            }
        });

        windowManager.addView(overlayView, params);
    }

    private void hideOverlay() {
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
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
