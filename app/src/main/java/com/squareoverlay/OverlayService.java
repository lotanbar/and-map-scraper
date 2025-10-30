package com.squareoverlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private SquareOverlayView overlayView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            
            if ("SHOW".equals(action)) {
                showOverlay();
            } else if ("HIDE".equals(action)) {
                hideOverlay();
            }
        }
        return START_STICKY;
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
    }
}
