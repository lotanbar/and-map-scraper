package com.squareoverlay;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1000;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        
        TextView title = new TextView(this);
        title.setText("Square Overlay App");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);
        
        TextView instructions = new TextView(this);
        instructions.setText("⚠️ IMPORTANT: Enable accessibility service in Settings > Accessibility > Square Overlay\n\nRequired for auto-scroll after screenshots!");
        instructions.setTextSize(14);
        instructions.setPadding(0, 0, 0, 30);
        instructions.setTextColor(0xFFFF6600); // Orange color
        layout.addView(instructions);
        
        Button accessibilityButton = new Button(this);
        accessibilityButton.setText("Open Accessibility Settings");
        accessibilityButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        layout.addView(accessibilityButton);

        Button showButton = new Button(this);
        showButton.setText("Show Square");
        showButton.setOnClickListener(v -> {
            if (checkOverlayPermission()) {
                // Check if accessibility service is enabled
                if (!isAccessibilityServiceEnabled()) {
                    android.widget.Toast.makeText(this,
                        "⚠️ Accessibility service is NOT enabled!\nScroll won't work. Please enable it in Accessibility settings.",
                        android.widget.Toast.LENGTH_LONG).show();
                }
                requestScreenshotPermission();
            }
        });
        layout.addView(showButton);

        Button hideButton = new Button(this);
        hideButton.setText("Hide Square");
        hideButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OverlayService.class);
            intent.setAction("HIDE");
            startService(intent);
        });
        layout.addView(hideButton);
        
        setContentView(layout);
        
        // Check permission on start
        checkOverlayPermission();
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + ScrollAccessibilityService.class.getName();
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );
                if (settingValue != null) {
                    return settingValue.contains(service);
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
        return false;
    }

    private void requestScreenshotPermission() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // Permission not granted
                    TextView tv = new TextView(this);
                    tv.setText("Overlay permission is required for this app to work!");
                    setContentView(tv);
                }
            }
        } else if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                // Start overlay service
                Intent serviceIntent = new Intent(MainActivity.this, OverlayService.class);
                serviceIntent.setAction("SHOW");
                startService(serviceIntent);

                // Pass screenshot permission to service
                Intent projectionIntent = new Intent(MainActivity.this, OverlayService.class);
                projectionIntent.setAction("SET_PROJECTION");
                projectionIntent.putExtra("resultCode", resultCode);
                projectionIntent.putExtra("data", data);
                startService(projectionIntent);
            } else {
                TextView tv = new TextView(this);
                tv.setText("Screenshot permission is required to capture the square area!");
                tv.setPadding(40, 40, 40, 40);
                setContentView(tv);
            }
        }
    }
}
