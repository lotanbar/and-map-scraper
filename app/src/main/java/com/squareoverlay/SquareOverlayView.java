package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class SquareOverlayView extends View {

    private static final String TAG = "SquareOverlay";
    
    private Paint squarePaint;
    private Paint textPaint;
    private Paint textBackgroundPaint;
    
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    
    private int screenWidth;
    private int screenHeight;
    
    private float squareX;
    private float squareY;
    private float squareSize;
    
    private float absoluteScreenX;
    private float absoluteScreenY;
    
    private float initialTouchX;
    private float initialTouchY;
    private int initialWindowX;
    private int initialWindowY;
    private boolean isDragging = false;
    
    private static final int CORNER_SIZE = 160;
    private boolean isResizing = false;

    public interface ScreenshotCallback {
        void onScreenshotRequested(float xPercent, float yPercent, float widthPercent, float heightPercent, Runnable onHidden);
    }

    private ScreenshotCallback screenshotCallback;

    public SquareOverlayView(Context context, int screenWidth, int screenHeight, float initialSquareSize) {
        super(context);
        
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        squareSize = initialSquareSize;
        squareX = 0;
        squareY = 0;

        absoluteScreenX = (screenWidth - squareSize) / 2;
        absoluteScreenY = (screenHeight - squareSize) / 2;
        
        squarePaint = new Paint();
        squarePaint.setColor(Color.RED);
        squarePaint.setStyle(Paint.Style.STROKE);
        squarePaint.setStrokeWidth(5);
        squarePaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setAntiAlias(true);
        
        textBackgroundPaint = new Paint();
        textBackgroundPaint.setColor(Color.argb(200, 0, 0, 0));
        textBackgroundPaint.setStyle(Paint.Style.FILL);

        Log.d(TAG, "View created with square size: " + (int)squareSize);
    }

    public void setScreenshotCallback(ScreenshotCallback callback) {
        this.screenshotCallback = callback;
    }

    public void setWindowManager(WindowManager wm, WindowManager.LayoutParams params) {
        this.windowManager = wm;
        this.windowParams = params;
    }

    public void triggerScreenshot() {
        if (screenshotCallback != null) {
            float xPercent = (absoluteScreenX / screenWidth) * 100;
            float yPercent = (absoluteScreenY / screenHeight) * 100;
            float widthPercent = (squareSize / screenWidth) * 100;
            float heightPercent = (squareSize / screenHeight) * 100;

            screenshotCallback.onScreenshotRequested(xPercent, yPercent, widthPercent, heightPercent, () -> {
                // This runs after screenshot is hidden
            });
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // View size matches square size exactly
        int width = (int)squareSize;
        int height = (int)squareSize;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        canvas.drawRect(squareX, squareY, squareX + squareSize, squareY + squareSize, squarePaint);
        
        // Draw resize handle (reuse paint object)
        squarePaint.setStyle(Paint.Style.FILL);
        squarePaint.setColor(Color.BLUE);
        canvas.drawCircle(squareX + squareSize, squareY + squareSize, 55, squarePaint);
        squarePaint.setColor(Color.RED);
        squarePaint.setStyle(Paint.Style.STROKE);
        
        if (windowParams != null) {
            absoluteScreenX = windowParams.x + squareX;
            absoluteScreenY = windowParams.y + squareY;
        }
        
        float xPercent = (absoluteScreenX / screenWidth) * 100;
        float yPercent = (absoluteScreenY / screenHeight) * 100;
        float sizePercent = (squareSize / screenWidth) * 100;

        String line1 = String.format("Position: %d, %d", (int)absoluteScreenX, (int)absoluteScreenY);
        String line2 = String.format("Size: %d x %d", (int)squareSize, (int)squareSize);
        String line3 = String.format("Crop: %.1f%%, %.1f%%, %.1f%%", xPercent, yPercent, sizePercent);

        float textX = squareX + 20;
        float textY = squareY + 60;
        float lineHeight = 50;

        float textWidth = 380;
        float textHeight = 3 * lineHeight + 20;
        canvas.drawRect(textX - 10, textY - 45, textX + textWidth, textY + textHeight - 45, textBackgroundPaint);

        canvas.drawText(line1, textX, textY, textPaint);
        canvas.drawText(line2, textX, textY + lineHeight, textPaint);
        canvas.drawText(line3, textX, textY + lineHeight * 2, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (windowManager == null || windowParams == null) {
            return false;
        }
        
        float touchX = event.getX();
        float touchY = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float cornerX = squareX + squareSize;
                float cornerY = squareY + squareSize;
                float distanceToCorner = (float) Math.sqrt(
                    Math.pow(touchX - cornerX, 2) + Math.pow(touchY - cornerY, 2)
                );

                if (distanceToCorner < CORNER_SIZE) {
                    isResizing = true;
                    isDragging = false;
                    Log.d(TAG, "Started resizing");
                    return true;
                } else if (touchX >= squareX && touchX <= squareX + squareSize &&
                           touchY >= squareY && touchY <= squareY + squareSize) {
                    isDragging = true;
                    isResizing = false;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    initialWindowX = windowParams.x;
                    initialWindowY = windowParams.y;
                    Log.d(TAG, "Started dragging");
                    return true;
                }
                return false;
                
            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    float newSize = Math.max(200, touchX - squareX);
                    newSize = Math.min(newSize, Math.min(screenWidth * 0.9f, screenHeight * 0.9f));

                    if (newSize != squareSize) {
                        squareSize = newSize;

                        // Update window size to match new square size
                        windowParams.width = (int)squareSize;
                        windowParams.height = (int)squareSize;

                        windowManager.updateViewLayout(this, windowParams);

                        Log.d(TAG, "Resized: size=" + (int)squareSize);
                    }
                    return true;
                } else if (isDragging) {
                    float deltaX = event.getRawX() - initialTouchX;
                    float deltaY = event.getRawY() - initialTouchY;

                    windowParams.x = initialWindowX + (int)deltaX;
                    windowParams.y = initialWindowY + (int)deltaY;

                    // Keep square on screen - window position = square position
                    int minX = 0;
                    int minY = 0;
                    int maxX = screenWidth - (int)squareSize;
                    int maxY = screenHeight - (int)squareSize;

                    windowParams.x = Math.max(minX, Math.min(windowParams.x, maxX));
                    windowParams.y = Math.max(minY, Math.min(windowParams.y, maxY));

                    windowManager.updateViewLayout(this, windowParams);
                    invalidate();
                    return true;
                }
                return false;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging || isResizing) {
                    Log.d(TAG, "Final: x=" + (int)absoluteScreenX + ", y=" + (int)absoluteScreenY + ", size=" + (int)squareSize);
                }
                isDragging = false;
                isResizing = false;
                return true;
        }
        
        return false;
    }
}
