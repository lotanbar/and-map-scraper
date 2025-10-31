package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

public class NextLineButtonView extends BaseButtonView {

    private static final int BUTTON_WIDTH = 280;
    private static final int BUTTON_HEIGHT = 200;
    private static final int CORNER_RADIUS = 30;

    public NextLineButtonView(Context context) {
        super(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS);
        buttonTextPaint.setTextSize(50);
    }

    @Override
    protected int getGradientStartColor() {
        return Color.rgb(33, 150, 243); // Light blue
    }

    @Override
    protected int getGradientEndColor() {
        return Color.rgb(0, 120, 200); // Darker blue
    }

    @Override
    protected int getPressedColor() {
        return Color.rgb(0, 100, 150); // Darker blue when pressed
    }

    @Override
    protected void drawButtonContent(Canvas canvas) {
        float textY = buttonRect.centerY() + (buttonTextPaint.getTextSize() / 2) - 10;
        canvas.drawText("↓", buttonRect.centerX(), textY, buttonTextPaint);
    }
}
