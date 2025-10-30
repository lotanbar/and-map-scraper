package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

public class AdjustButtonView extends View {

    private Paint buttonPaint;
    private Paint buttonTextPaint;
    private Paint shadowPaint;
    private RectF buttonRect;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 140;
    private static final int CORNER_RADIUS = 30;
    private static final int LONG_PRESS_DELAY = 400; // Start repeating after 400ms
    private static final int REPEAT_INTERVAL = 200; // Repeat every 200ms (safe for gesture completion)

    private boolean isPressed = false;
    private String label;
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private Runnable repeatRunnable;

    public interface OnClickListener {
        void onClick();
    }

    private OnClickListener clickListener;

    public AdjustButtonView(Context context, String label) {
        super(context);
        this.label = label;

        buttonPaint = new Paint();
        buttonPaint.setAntiAlias(true);
        buttonPaint.setStyle(Paint.Style.FILL);

        buttonTextPaint = new Paint();
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextSize(70);
        buttonTextPaint.setAntiAlias(true);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setFakeBoldText(true);

        shadowPaint = new Paint();
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Paint.Style.FILL);

        buttonRect = new RectF();

        // Setup long press behavior
        longPressRunnable = () -> {
            // Start repeating
            repeatRunnable = new Runnable() {
                @Override
                public void run() {
                    if (clickListener != null) {
                        clickListener.onClick();
                    }
                    longPressHandler.postDelayed(this, REPEAT_INTERVAL);
                }
            };
            longPressHandler.post(repeatRunnable);
        };
    }

    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw shadow
        RectF shadowRect = new RectF(
            centerX - BUTTON_WIDTH/2f + 4,
            centerY - BUTTON_HEIGHT/2f + 4,
            centerX + BUTTON_WIDTH/2f + 4,
            centerY + BUTTON_HEIGHT/2f + 4
        );
        canvas.drawRoundRect(shadowRect, CORNER_RADIUS, CORNER_RADIUS, shadowPaint);

        // Set up button rectangle
        buttonRect.set(
            centerX - BUTTON_WIDTH/2f,
            centerY - BUTTON_HEIGHT/2f,
            centerX + BUTTON_WIDTH/2f,
            centerY + BUTTON_HEIGHT/2f
        );

        // Create gradient effect
        if (isPressed) {
            buttonPaint.setColor(Color.rgb(200, 100, 0)); // Darker orange when pressed
        } else {
            LinearGradient gradient = new LinearGradient(
                buttonRect.left, buttonRect.top,
                buttonRect.left, buttonRect.bottom,
                Color.rgb(255, 152, 0),  // Light orange
                Color.rgb(230, 120, 0),  // Darker orange
                Shader.TileMode.CLAMP
            );
            buttonPaint.setShader(gradient);
        }

        // Draw button
        canvas.drawRoundRect(buttonRect, CORNER_RADIUS, CORNER_RADIUS, buttonPaint);

        // Reset shader for text
        buttonPaint.setShader(null);

        // Draw text
        float textY = buttonRect.centerY() + (buttonTextPaint.getTextSize() / 2) - 10;
        canvas.drawText(label, buttonRect.centerX(), textY, buttonTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        android.util.Log.d("AdjustButtonView", label + " button touch: action=" + event.getAction() + " at (" + touchX + "," + touchY + ")");

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                android.util.Log.d("AdjustButtonView", label + " ACTION_DOWN, buttonRect=" + buttonRect + " contains=" + buttonRect.contains(touchX, touchY));
                if (buttonRect.contains(touchX, touchY)) {
                    isPressed = true;
                    invalidate();

                    // Start long press timer
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
                    android.util.Log.d("AdjustButtonView", label + " button pressed, starting long press timer");
                    return true;
                }
                android.util.Log.d("AdjustButtonView", label + " touch outside button rect");
                return false;

            case MotionEvent.ACTION_UP:
                android.util.Log.d("AdjustButtonView", label + " ACTION_UP, isPressed=" + isPressed);
                if (isPressed) {
                    isPressed = false;
                    invalidate();

                    // Check if we started repeating
                    boolean wasLongPress = repeatRunnable != null;

                    // Cancel all pending actions
                    longPressHandler.removeCallbacks(longPressRunnable);
                    if (repeatRunnable != null) {
                        longPressHandler.removeCallbacks(repeatRunnable);
                        repeatRunnable = null;
                    }

                    // If it wasn't a long press and touch is still in bounds, fire single click
                    if (!wasLongPress && buttonRect.contains(touchX, touchY) && clickListener != null) {
                        android.util.Log.d("AdjustButtonView", label + " firing onClick");
                        clickListener.onClick();
                    } else {
                        android.util.Log.d("AdjustButtonView", label + " NOT firing onClick: wasLongPress=" + wasLongPress + " contains=" + buttonRect.contains(touchX, touchY) + " hasListener=" + (clickListener != null));
                    }
                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                android.util.Log.d("AdjustButtonView", label + " ACTION_CANCEL");
                isPressed = false;
                invalidate();

                // Cancel all pending actions
                longPressHandler.removeCallbacks(longPressRunnable);
                if (repeatRunnable != null) {
                    longPressHandler.removeCallbacks(repeatRunnable);
                    repeatRunnable = null;
                }
                return true;
        }

        return false;
    }
}
