package com.squareoverlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No action needed
    }

    override fun onInterrupt() {
        // No action needed
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun performHorizontalScroll(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Int) {
        performScroll(startX, startY, endX, endY, durationMs)
    }

    fun performVerticalScroll(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Int) {
        performScroll(startX, startY, endX, endY, durationMs)
    }

    private fun performScroll(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(startX.toFloat(), startY.toFloat())
                lineTo(endX.toFloat(), endY.toFloat())
            }

            val stroke = StrokeDescription(path, 0, durationMs.toLong())
            val gesture = GestureDescription.Builder()
                .addStroke(stroke)
                .build()

            dispatchGesture(gesture, null, null)
        }
    }

    companion object {
        @JvmStatic
        var instance: ScrollAccessibilityService? = null
            private set
    }
}
