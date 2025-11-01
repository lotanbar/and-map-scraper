package com.squareoverlay

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Square Overlay App"
            textSize = 24f
            setPadding(0, 0, 0, 40)
        }
        layout.addView(title)

        val instructions = TextView(this).apply {
            text = "⚠️ IMPORTANT: Enable accessibility service in Settings > Accessibility > Square Overlay\n\nRequired for auto-scroll after screenshots!"
            textSize = 14f
            setPadding(0, 0, 0, 30)
            setTextColor(0xFFFF6600.toInt()) // Orange color
        }
        layout.addView(instructions)

        val accessibilityButton = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(accessibilityButton)

        val showButton = Button(this).apply {
            text = "Show Square"
            setOnClickListener {
                if (checkOverlayPermission()) {
                    // Check if accessibility service is enabled
                    if (!isAccessibilityServiceEnabled) {
                        Toast.makeText(
                            this@MainActivity,
                            "⚠️ Accessibility service is NOT enabled!\nScroll won't work. Please enable it in Accessibility settings.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    requestScreenshotPermission()
                }
            }
        }
        layout.addView(showButton)

        val hideButton = Button(this).apply {
            text = "Hide Square"
            setOnClickListener {
                Intent(this@MainActivity, OverlayService::class.java).apply {
                    action = "HIDE"
                    startService(this)
                }
            }
        }
        layout.addView(hideButton)

        setContentView(layout)

        // Check permission on start
        checkOverlayPermission()
    }

    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
                return false
            }
        }
        return true
    }

    private val isAccessibilityServiceEnabled: Boolean
        get() {
            val service = "$packageName/${ScrollAccessibilityService::class.java.name}"
            try {
                val accessibilityEnabled = Settings.Secure.getInt(
                    contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
                if (accessibilityEnabled == 1) {
                    val settingValue = Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    )
                    if (settingValue != null) {
                        return settingValue.contains(service)
                    }
                }
            } catch (e: SettingNotFoundException) {
                return false
            }
            return false
        }

    private fun requestScreenshotPermission() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_CODE_SCREEN_CAPTURE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        // Permission not granted
                        setContentView(TextView(this).apply {
                            text = "Overlay permission is required for this app to work!"
                        })
                    }
                }
            }
            REQUEST_CODE_SCREEN_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    // Start overlay service
                    Intent(this, OverlayService::class.java).apply {
                        action = "SHOW"
                        startService(this)
                    }

                    // Pass screenshot permission to service
                    Intent(this, OverlayService::class.java).apply {
                        action = "SET_PROJECTION"
                        putExtra("resultCode", resultCode)
                        putExtra("data", data)
                        startService(this)
                    }
                } else {
                    setContentView(TextView(this).apply {
                        text = "Screenshot permission is required to capture the square area!"
                        setPadding(40, 40, 40, 40)
                    })
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }
}
