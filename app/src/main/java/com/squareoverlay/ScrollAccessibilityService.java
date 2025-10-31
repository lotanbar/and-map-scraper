package com.squareoverlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class ScrollAccessibilityService extends AccessibilityService {

    private static final String TAG = "ScrollAccessibility";
    private static ScrollAccessibilityService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to handle accessibility events
    }

    @Override
    public void onInterrupt() {
        // Required override
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Accessibility service connected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Accessibility service destroyed");
    }

    public static ScrollAccessibilityService getInstance() {
        return instance;
    }

    public void performHorizontalScroll(int startX, int startY, int endX, int endY, int durationMs) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);

            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                path, 0, durationMs
            );

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);

            boolean dispatched = dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "Scroll gesture completed successfully");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.e(TAG, "Scroll gesture cancelled");
                }
            }, null);

            Log.d(TAG, "Scroll gesture dispatched: " + dispatched +
                " from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
        } else {
            Log.e(TAG, "Gesture dispatch requires Android N or higher");
        }
    }

    public void performVerticalScroll(int startX, int startY, int endX, int endY, int durationMs) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);

            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                path, 0, durationMs
            );

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);

            boolean dispatched = dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "Vertical scroll gesture completed successfully");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.e(TAG, "Vertical scroll gesture cancelled");
                }
            }, null);

            Log.d(TAG, "Vertical scroll gesture dispatched: " + dispatched +
                " from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
        } else {
            Log.e(TAG, "Gesture dispatch requires Android N or higher");
        }
    }
}
