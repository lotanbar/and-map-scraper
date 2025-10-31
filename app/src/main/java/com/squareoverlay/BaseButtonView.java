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

public abstract class BaseButtonView extends View {

    protected Paint buttonPaint;
    protected Paint buttonTextPaint;
    protected Paint shadowPaint;
    protected RectF buttonRect;

    protected int buttonWidth;
    protected int buttonHeight;
    protected int cornerRadius;
    protected boolean isPressed = false;

    private static final int LONG_PRESS_DELAY = 400;
    private static final int REPEAT_INTERVAL = 200;

    private boolean enableLongPress = false;
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private Runnable repeatRunnable;

    public interface OnClickListener {
        void onClick();
    }

    protected OnClickListener clickListener;

    public BaseButtonView(Context context, int width, int height, int cornerRadius) {
        this(context, width, height, cornerRadius, false);
    }

    public BaseButtonView(Context context, int width, int height, int cornerRadius, boolean enableLongPress) {
        super(context);
        this.buttonWidth = width;
        this.buttonHeight = height;
        this.cornerRadius = cornerRadius;
        this.enableLongPress = enableLongPress;

        buttonPaint = new Paint();
        buttonPaint.setAntiAlias(true);
        buttonPaint.setStyle(Paint.Style.FILL);

        buttonTextPaint = new Paint();
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setAntiAlias(true);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setFakeBoldText(true);

        shadowPaint = new Paint();
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Paint.Style.FILL);

        buttonRect = new RectF();

        if (enableLongPress) {
            longPressRunnable = () -> {
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
    }

    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(buttonWidth, buttonHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        RectF shadowRect = new RectF(
            centerX - buttonWidth/2f + 4,
            centerY - buttonHeight/2f + 4,
            centerX + buttonWidth/2f + 4,
            centerY + buttonHeight/2f + 4
        );
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint);

        buttonRect.set(
            centerX - buttonWidth/2f,
            centerY - buttonHeight/2f,
            centerX + buttonWidth/2f,
            centerY + buttonHeight/2f
        );

        if (isPressed) {
            buttonPaint.setColor(getPressedColor());
        } else {
            LinearGradient gradient = new LinearGradient(
                buttonRect.left, buttonRect.top,
                buttonRect.left, buttonRect.bottom,
                getGradientStartColor(),
                getGradientEndColor(),
                Shader.TileMode.CLAMP
            );
            buttonPaint.setShader(gradient);
        }

        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, buttonPaint);
        buttonPaint.setShader(null);

        drawButtonContent(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (buttonRect.contains(touchX, touchY)) {
                    isPressed = true;
                    invalidate();

                    if (enableLongPress) {
                        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
                    }

                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
                if (isPressed) {
                    isPressed = false;
                    invalidate();

                    if (enableLongPress) {
                        boolean wasLongPress = repeatRunnable != null;
                        longPressHandler.removeCallbacks(longPressRunnable);
                        if (repeatRunnable != null) {
                            longPressHandler.removeCallbacks(repeatRunnable);
                            repeatRunnable = null;
                        }

                        if (!wasLongPress && buttonRect.contains(touchX, touchY) && clickListener != null) {
                            clickListener.onClick();
                        }
                    } else {
                        if (buttonRect.contains(touchX, touchY) && clickListener != null) {
                            clickListener.onClick();
                        }
                    }

                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                invalidate();

                if (enableLongPress) {
                    longPressHandler.removeCallbacks(longPressRunnable);
                    if (repeatRunnable != null) {
                        longPressHandler.removeCallbacks(repeatRunnable);
                        repeatRunnable = null;
                    }
                }

                return true;
        }

        return false;
    }

    protected abstract int getGradientStartColor();
    protected abstract int getGradientEndColor();
    protected abstract int getPressedColor();
    protected abstract void drawButtonContent(Canvas canvas);
}
