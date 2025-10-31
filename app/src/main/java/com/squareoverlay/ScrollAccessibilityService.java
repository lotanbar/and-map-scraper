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

    public void performRotation(int centerX, int centerY, boolean clockwise, int durationMs) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // Very gentle rotation
            // For a two-finger rotation gesture, we need two strokes that rotate around a center point
            int radius = 200; // Distance from center for the touch points (increased for better detection)

            // Calculate start and end angles for a gentle rotation
            // Use diagonal positions for more natural rotation recognition
            double startAngle1 = Math.toRadians(-45); // Upper right diagonal
            double startAngle2 = Math.toRadians(135); // Lower left diagonal

            // Gentle rotation: 15 degrees for better recognition
            // NOTE: Y-axis is inverted on Android (increases downward), so signs are flipped
            double rotationAmount = Math.toRadians(clockwise ? -15 : 15);

            double endAngle1 = startAngle1 + rotationAmount;
            double endAngle2 = startAngle2 + rotationAmount;

            // Calculate touch points for first finger
            float startX1 = (float)(centerX + radius * Math.cos(startAngle1));
            float startY1 = (float)(centerY + radius * Math.sin(startAngle1));
            float endX1 = (float)(centerX + radius * Math.cos(endAngle1));
            float endY1 = (float)(centerY + radius * Math.sin(endAngle1));

            // Calculate touch points for second finger
            float startX2 = (float)(centerX + radius * Math.cos(startAngle2));
            float startY2 = (float)(centerY + radius * Math.sin(startAngle2));
            float endX2 = (float)(centerX + radius * Math.cos(endAngle2));
            float endY2 = (float)(centerY + radius * Math.sin(endAngle2));

            Log.d(TAG, "Rotation center: (" + centerX + ", " + centerY + ")");
            Log.d(TAG, "Finger 1: (" + startX1 + "," + startY1 + ") -> (" + endX1 + "," + endY1 + ")");
            Log.d(TAG, "Finger 2: (" + startX2 + "," + startY2 + ") -> (" + endX2 + "," + endY2 + ")");

            // Create paths for both fingers using arcs for smoother rotation
            Path path1 = new Path();
            path1.moveTo(startX1, startY1);
            // Create a smooth curve by adding intermediate points
            for (int i = 1; i <= 3; i++) {
                double intermediateAngle = startAngle1 + (rotationAmount * i / 3.0);
                float intermediateX = (float)(centerX + radius * Math.cos(intermediateAngle));
                float intermediateY = (float)(centerY + radius * Math.sin(intermediateAngle));
                path1.lineTo(intermediateX, intermediateY);
            }

            Path path2 = new Path();
            path2.moveTo(startX2, startY2);
            // Create a smooth curve by adding intermediate points
            for (int i = 1; i <= 3; i++) {
                double intermediateAngle = startAngle2 + (rotationAmount * i / 3.0);
                float intermediateX = (float)(centerX + radius * Math.cos(intermediateAngle));
                float intermediateY = (float)(centerY + radius * Math.sin(intermediateAngle));
                path2.lineTo(intermediateX, intermediateY);
            }

            GestureDescription.StrokeDescription stroke1 = new GestureDescription.StrokeDescription(
                path1, 0, durationMs
            );

            GestureDescription.StrokeDescription stroke2 = new GestureDescription.StrokeDescription(
                path2, 0, durationMs
            );

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke1);
            builder.addStroke(stroke2);

            boolean dispatched = dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "Rotation gesture completed successfully");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.e(TAG, "Rotation gesture cancelled");
                }
            }, null);

            Log.d(TAG, "Rotation gesture dispatched: " + dispatched + " (" +
                (clockwise ? "clockwise" : "counter-clockwise") + ", 8 degrees)");
        } else {
            Log.e(TAG, "Gesture dispatch requires Android N or higher");
        }
    }
}
