package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

public class AdjustButtonView extends BaseButtonView {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 140;
    private static final int CORNER_RADIUS = 30;

    private String label;

    public AdjustButtonView(Context context, String label) {
        super(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS, true);
        this.label = label;
        buttonTextPaint.setTextSize(70);
    }

    @Override
    protected int getGradientStartColor() {
        return Color.rgb(255, 152, 0); // Light orange
    }

    @Override
    protected int getGradientEndColor() {
        return Color.rgb(230, 120, 0); // Darker orange
    }

    @Override
    protected int getPressedColor() {
        return Color.rgb(200, 100, 0); // Darker orange when pressed
    }

    @Override
    protected void drawButtonContent(Canvas canvas) {
        float textY = buttonRect.centerY() + (buttonTextPaint.getTextSize() / 2) - 10;
        canvas.drawText(label, buttonRect.centerX(), textY, buttonTextPaint);
    }
}
