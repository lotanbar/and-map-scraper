package com.squareoverlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class ScrollAccessibilityService extends AccessibilityService {

    private static ScrollAccessibilityService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public static ScrollAccessibilityService getInstance() {
        return instance;
    }

    public void performHorizontalScroll(int startX, int startY, int endX, int endY, int durationMs) {
        performScroll(startX, startY, endX, endY, durationMs);
    }

    public void performVerticalScroll(int startX, int startY, int endX, int endY, int durationMs) {
        performScroll(startX, startY, endX, endY, durationMs);
    }

    private void performScroll(int startX, int startY, int endX, int endY, int durationMs) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);

            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                path, 0, durationMs
            );

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);

            dispatchGesture(builder.build(), null, null);
        }
    }
}
