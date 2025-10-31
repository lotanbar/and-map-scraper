package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class CounterDisplayView extends View {

    private Paint backgroundPaint;
    private Paint textPaint;
    private RectF backgroundRect;
    private int counterValue = 0;

    private static final int WIDTH = 300;
    private static final int HEIGHT = 140; // Match other buttons

    public CounterDisplayView(Context context) {
        super(context);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.argb(220, 0, 0, 0));
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        backgroundRect = new RectF();
    }

    public void setCounter(int value) {
        android.util.Log.d("CounterDisplayView", "setCounter called with value: " + value + " (old: " + this.counterValue + ")");
        this.counterValue = value;
        invalidate();
        android.util.Log.d("CounterDisplayView", "invalidate() called, requesting redraw");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(WIDTH, HEIGHT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        android.util.Log.d("CounterDisplayView", "onDraw called, counterValue=" + counterValue);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw background
        backgroundRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(backgroundRect, 20, 20, backgroundPaint);

        // Draw counter text
        String text = counterValue + "px";
        float textY = centerY + (textPaint.getTextSize() / 2) - 5;
        canvas.drawText(text, centerX, textY, textPaint);

        android.util.Log.d("CounterDisplayView", "onDraw completed, drew text: " + text);
    }
}
