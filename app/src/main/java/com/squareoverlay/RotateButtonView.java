package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class RotateButtonView extends View {

    private Paint buttonPaint;
    private Paint arrowPaint;
    private Paint shadowPaint;
    private RectF buttonRect;

    private static final int BUTTON_SIZE = 120;
    private static final int CORNER_RADIUS = 60;

    private boolean isPressed = false;
    private boolean isClockwise;

    public interface OnClickListener {
        void onClick();
    }

    private OnClickListener clickListener;

    public RotateButtonView(Context context, boolean clockwise) {
        super(context);
        this.isClockwise = clockwise;

        buttonPaint = new Paint();
        buttonPaint.setAntiAlias(true);
        buttonPaint.setStyle(Paint.Style.FILL);

        arrowPaint = new Paint();
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setAntiAlias(true);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(8);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);

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
        setMeasuredDimension(BUTTON_SIZE, BUTTON_SIZE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw shadow
        RectF shadowRect = new RectF(
            centerX - BUTTON_SIZE/2f + 3,
            centerY - BUTTON_SIZE/2f + 3,
            centerX + BUTTON_SIZE/2f + 3,
            centerY + BUTTON_SIZE/2f + 3
        );
        canvas.drawRoundRect(shadowRect, CORNER_RADIUS, CORNER_RADIUS, shadowPaint);

        // Set up button rectangle
        buttonRect.set(
            centerX - BUTTON_SIZE/2f,
            centerY - BUTTON_SIZE/2f,
            centerX + BUTTON_SIZE/2f,
            centerY + BUTTON_SIZE/2f
        );

        // Set button color
        if (isPressed) {
            buttonPaint.setColor(Color.rgb(30, 100, 150)); // Darker blue when pressed
        } else {
            buttonPaint.setColor(Color.rgb(66, 133, 244)); // Nice blue
        }

        // Draw button
        canvas.drawRoundRect(buttonRect, CORNER_RADIUS, CORNER_RADIUS, buttonPaint);

        // Draw curved arrow for rotation
        drawRotationArrow(canvas, centerX, centerY);
    }

    private void drawRotationArrow(Canvas canvas, float centerX, float centerY) {
        float radius = 30;

        // Draw circular arc
        RectF arcRect = new RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        );

        Path arrowPath = new Path();

        if (isClockwise) {
            // Clockwise: arc from top going right
            arrowPath.arcTo(arcRect, -90, 270);

            // Add arrowhead at the end (pointing clockwise)
            float arrowEndX = centerX;
            float arrowEndY = centerY - radius;
            arrowPath.moveTo(arrowEndX, arrowEndY);
            arrowPath.lineTo(arrowEndX - 8, arrowEndY - 12);
            arrowPath.moveTo(arrowEndX, arrowEndY);
            arrowPath.lineTo(arrowEndX + 8, arrowEndY - 12);
        } else {
            // Counter-clockwise: arc from top going left
            arrowPath.arcTo(arcRect, -90, -270);

            // Add arrowhead at the end (pointing counter-clockwise)
            float arrowEndX = centerX;
            float arrowEndY = centerY - radius;
            arrowPath.moveTo(arrowEndX, arrowEndY);
            arrowPath.lineTo(arrowEndX - 8, arrowEndY - 12);
            arrowPath.moveTo(arrowEndX, arrowEndY);
            arrowPath.lineTo(arrowEndX + 8, arrowEndY - 12);
        }

        canvas.drawPath(arrowPath, arrowPaint);
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
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
                if (isPressed) {
                    isPressed = false;
                    invalidate();

                    if (buttonRect.contains(touchX, touchY) && clickListener != null) {
                        clickListener.onClick();
                    }
                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                invalidate();
                return true;
        }

        return false;
    }
}
