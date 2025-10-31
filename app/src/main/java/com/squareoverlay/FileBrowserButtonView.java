package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

public class FileBrowserButtonView extends BaseButtonView {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 200;
    private static final int CORNER_RADIUS = 30;

    public FileBrowserButtonView(Context context) {
        super(context, BUTTON_WIDTH, BUTTON_HEIGHT, CORNER_RADIUS);
        buttonTextPaint.setTextSize(50);
    }

    @Override
    protected int getGradientStartColor() {
        return Color.rgb(156, 39, 176); // Light purple
    }

    @Override
    protected int getGradientEndColor() {
        return Color.rgb(123, 31, 162); // Darker purple
    }

    @Override
    protected int getPressedColor() {
        return Color.rgb(100, 50, 150); // Darker purple when pressed
    }

    @Override
    protected void drawButtonContent(Canvas canvas) {
        float textY = buttonRect.centerY() + (buttonTextPaint.getTextSize() / 2) - 10;
        canvas.drawText("📁", buttonRect.centerX(), textY, buttonTextPaint);
    }
}
