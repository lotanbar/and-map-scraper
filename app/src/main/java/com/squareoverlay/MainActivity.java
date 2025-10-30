package com.squareoverlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        
        TextView title = new TextView(this);
        title.setText("Square Overlay App");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);
        
        TextView instructions = new TextView(this);
        instructions.setText("This app will display a draggable square overlay on your screen.\\n\\n" +
                "1. Grant overlay permission\\n" +
                "2. Press 'Show Square' button\\n" +
                "3. Drag and resize the square\\n" +
                "4. The coordinates will be displayed on the square");
        instructions.setPadding(0, 0, 0, 40);
        layout.addView(instructions);
        
        Button showButton = new Button(this);
        showButton.setText("Show Square");
        showButton.setOnClickListener(v -> {
            if (checkOverlayPermission()) {
                Intent intent = new Intent(MainActivity.this, OverlayService.class);
                intent.setAction("SHOW");
                startService(intent);
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
        }
    }
}
