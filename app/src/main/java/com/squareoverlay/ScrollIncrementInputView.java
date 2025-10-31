package com.squareoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

public class ScrollIncrementInputView extends FrameLayout {

    private Paint backgroundPaint;
    private Paint labelPaint;
    private RectF backgroundRect;
    private EditText editText;
    private int currentValue = 30;

    private static final int WIDTH = 300;
    private static final int HEIGHT = 120;

    public interface OnValueChangedListener {
        void onValueChanged(int newValue);
    }

    private OnValueChangedListener valueChangedListener;

    public ScrollIncrementInputView(Context context) {
        super(context);
        setWillNotDraw(false);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.argb(220, 50, 50, 50));
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        labelPaint = new Paint();
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(40);
        labelPaint.setAntiAlias(true);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        backgroundRect = new RectF();

        // Create EditText for numeric input
        editText = new EditText(context);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setText(String.valueOf(currentValue));
        editText.setTextSize(30);
        editText.setTextColor(Color.WHITE);
        editText.setBackgroundColor(Color.argb(100, 255, 255, 255));
        editText.setGravity(Gravity.CENTER);
        editText.setPadding(20, 10, 20, 10);
        editText.setSingleLine(true);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // Handle "Done" button on keyboard
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                try {
                    int newValue = Integer.parseInt(editText.getText().toString());
                    if (newValue > 0 && newValue <= 500) {
                        currentValue = newValue;
                        if (valueChangedListener != null) {
                            valueChangedListener.onValueChanged(currentValue);
                        }
                        hideKeyboard();
                        return true;
                    } else {
                        // Reset to previous value if invalid
                        editText.setText(String.valueOf(currentValue));
                        hideKeyboard();
                    }
                } catch (NumberFormatException e) {
                    // Reset to previous value if invalid
                    editText.setText(String.valueOf(currentValue));
                    hideKeyboard();
                }
            }
            return false;
        });

        // Layout params for EditText - centered in the view
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            200,
            80
        );
        params.gravity = Gravity.CENTER;
        addView(editText, params);
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.valueChangedListener = listener;
    }

    public int getValue() {
        return currentValue;
    }

    public void setValue(int value) {
        this.currentValue = value;
        editText.setText(String.valueOf(value));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(WIDTH, HEIGHT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        backgroundRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(backgroundRect, 20, 20, backgroundPaint);

        // Draw label above the input
        String label = "px/step";
        float labelY = 35;
        canvas.drawText(label, getWidth() / 2f, labelY, labelPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Request focus and show keyboard
            editText.requestFocus();
            showKeyboard();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        editText.clearFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }
}
