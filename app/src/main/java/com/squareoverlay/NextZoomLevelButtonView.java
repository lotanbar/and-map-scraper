package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

public class NextZoomLevelButtonView extends BaseButtonView {

    private static final int BUTTON_WIDTH = 280;
    private static final int BUTTON_HEIGHT = 200;
    private static final int CORNER_RADIUS = 30;

    public NextZoomLevelButtonView(Context context) {
        super(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS);
        buttonTextPaint.setTextSize(50);
    }

    @Override
    protected int getGradientStartColor() {
        return Color.rgb(180, 50, 200); // Light purple
    }

    @Override
    protected int getGradientEndColor() {
        return Color.rgb(140, 20, 160); // Darker purple
    }

    @Override
    protected int getPressedColor() {
        return Color.rgb(120, 0, 120); // Darker purple when pressed
    }

    @Override
    protected void drawButtonContent(Canvas canvas) {
        float textY = buttonRect.centerY() + (buttonTextPaint.getTextSize() / 2) - 10;
        canvas.drawText("Z", buttonRect.centerX(), textY, buttonTextPaint);
    }
}
