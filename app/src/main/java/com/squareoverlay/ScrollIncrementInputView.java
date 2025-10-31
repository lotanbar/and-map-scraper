package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class ScrollIncrementInputView extends View {

    private Paint backgroundPaint;
    private Paint textPaint;
    private Paint iconPaint;
    private RectF backgroundRect;
    private int currentValue = 30;
    private boolean isPressed = false;

    private static final int WIDTH = 100;
    private static final int HEIGHT = 140;

    public interface OnClickListener {
        void onClick();
    }

    private OnClickListener onClickListener;

    public ScrollIncrementInputView(Context context) {
        super(context);
        setWillNotDraw(false);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.argb(220, 50, 50, 50));
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        iconPaint = new Paint();
        iconPaint.setColor(Color.WHITE);
        iconPaint.setTextSize(50);
        iconPaint.setAntiAlias(true);
        iconPaint.setTextAlign(Paint.Align.CENTER);

        backgroundRect = new RectF();
    }

    public int getValue() {
        return currentValue;
    }

    public void setValue(int value) {
        this.currentValue = value;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(WIDTH, HEIGHT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isPressed) {
            backgroundPaint.setColor(Color.argb(255, 70, 70, 70));
        } else {
            backgroundPaint.setColor(Color.argb(220, 50, 50, 50));
        }
        backgroundRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(backgroundRect, 15, 15, backgroundPaint);

        String icon = "✏";
        float iconY = getHeight() / 2f + 8;
        canvas.drawText(icon, getWidth() / 2f, iconY, iconPaint);

        String valueText = currentValue + "px";
        float textY = getHeight() - 15;
        canvas.drawText(valueText, getWidth() / 2f, textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                isPressed = false;
                invalidate();
                if (onClickListener != null) {
                    onClickListener.onClick();
                }
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }
}
