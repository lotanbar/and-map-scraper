package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

public class FileBrowserButtonView extends View {

    private Paint buttonPaint;
    private Paint buttonTextPaint;
    private Paint shadowPaint;
    private RectF buttonRect;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 200;
    private static final int CORNER_RADIUS = 30;

    private boolean isPressed = false;

    public interface OnClickListener {
        void onClick();
    }

    private OnClickListener clickListener;

    public FileBrowserButtonView(Context context) {
        super(context);

        buttonPaint = new Paint();
        buttonPaint.setAntiAlias(true);
        buttonPaint.setStyle(Paint.Style.FILL);

        buttonTextPaint = new Paint();
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextSize(50);
        buttonTextPaint.setAntiAlias(true);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setFakeBoldText(true);

        shadowPaint = new Paint();
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Paint.Style.FILL);

        buttonRect = new RectF();
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

        // Create gradient effect - using purple/violet colors
        if (isPressed) {
            buttonPaint.setColor(Color.rgb(100, 50, 150)); // Darker purple when pressed
        } else {
            LinearGradient gradient = new LinearGradient(
                buttonRect.left, buttonRect.top,
                buttonRect.left, buttonRect.bottom,
                Color.rgb(156, 39, 176),  // Light purple
                Color.rgb(123, 31, 162),  // Darker purple
                Shader.TileMode.CLAMP
            );
            buttonPaint.setShader(gradient);
        }

        // Draw button
        canvas.drawRoundRect(buttonRect, CORNER_RADIUS, CORNER_RADIUS, buttonPaint);

        // Reset shader for text
        buttonPaint.setShader(null);

        // Draw folder icon
        float textY = buttonRect.centerY() + (buttonTextPaint.getTextSize() / 2) - 10;
        canvas.drawText("📁", buttonRect.centerX(), textY, buttonTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        android.util.Log.d("FileBrowserButtonView", "File browser button touch: action=" + event.getAction() + " at (" + touchX + "," + touchY + ")");

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                android.util.Log.d("FileBrowserButtonView", "ACTION_DOWN, buttonRect=" + buttonRect + " contains=" + buttonRect.contains(touchX, touchY));
                if (buttonRect.contains(touchX, touchY)) {
                    isPressed = true;
                    invalidate();
                    android.util.Log.d("FileBrowserButtonView", "File browser button pressed");
                    return true;
                }
                android.util.Log.d("FileBrowserButtonView", "Touch outside button rect");
                return false;

            case MotionEvent.ACTION_UP:
                android.util.Log.d("FileBrowserButtonView", "ACTION_UP, isPressed=" + isPressed);
                if (isPressed) {
                    isPressed = false;
                    invalidate();

                    // If touch is still in bounds, fire click
                    if (buttonRect.contains(touchX, touchY) && clickListener != null) {
                        android.util.Log.d("FileBrowserButtonView", "Firing onClick");
                        clickListener.onClick();
                    }
                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                android.util.Log.d("FileBrowserButtonView", "ACTION_CANCEL");
                isPressed = false;
                invalidate();
                return true;
        }

        return false;
    }
}
