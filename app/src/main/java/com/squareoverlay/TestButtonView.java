package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

public class TestButtonView extends BaseButtonView {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 140;
    private static final int CORNER_RADIUS = 30;

    public TestButtonView(Context context) {
        super(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS, true);
        buttonTextPaint.setTextSize(60);
    }

    @Override
    protected int getGradientStartColor() {
        return Color.rgb(76, 175, 80); // Light green
    }

    @Override
    protected int getGradientEndColor() {
        return Color.rgb(56, 142, 60); // Darker green
    }

    @Override
    protected int getPressedColor() {
        return Color.rgb(34, 139, 34); // Darker green when pressed
    }

    @Override
    protected void drawButtonContent(Canvas canvas) {
        float textY = buttonRect.centerY() + (buttonTextPaint.getTextSize() / 2) - 10;
        canvas.drawText("T", buttonRect.centerX(), textY, buttonTextPaint);
    }
}
