# Square Overlay

Android app that displays a draggable, resizable square overlay on the screen with coordinate tracking.

## Features

- Draggable red square overlay
- Resizable via blue corner handle
- Real-time coordinate display (absolute pixels and percentages)
- Screenshot capture of exact square area with green camera button
- Screenshots saved to Pictures/SquareOverlay with timestamp
- Works across all apps (requires overlay permission)

## Requirements

- Android 7.0+ (API 24)
- Overlay permission (granted on first launch)
- Screenshot permission (granted when showing overlay)

## Installation

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Launch app and grant overlay permission when prompted
2. Tap "Show Square" and grant screenshot permission
3. Drag square by touching inside it
4. Resize by dragging the blue circle in bottom-right corner
5. Tap green camera button to capture screenshot of square area
6. Screenshots saved to `/sdcard/Pictures/SquareOverlay/square_YYYYMMDD_HHmmss.png`
7. Tap "Hide Square" to remove overlay

## Technical Details

- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Build Tool**: Gradle 8.3.2
- **Java Version**: 17

## Project Structure

```
square-overlay/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/squareoverlay/
│           │   ├── MainActivity.java         # Entry point, permission handling
│           │   ├── OverlayService.java       # Foreground service managing overlay
│           │   ├── ScreenshotService.java    # MediaProjection screenshot capture
│           │   └── SquareOverlayView.java    # Custom view with drag/resize/screenshot
│           └── AndroidManifest.xml
├── build.gradle
└── gradle.properties
```

## Components

### MainActivity
Entry point that handles overlay permission requests and controls service.

### OverlayService
Foreground service managing overlay window lifecycle with MediaProjection support.

### ScreenshotService
Handles MediaProjection API for screen capture:
- Creates persistent VirtualDisplay for multiple captures
- Crops screenshots to exact square coordinates using percentages
- Saves to Pictures/SquareOverlay

### SquareOverlayView
Custom view implementing:
- Touch event handling for drag/resize
- Screenshot button with callback
- Real-time coordinate calculation
- Dynamic text overlay with measurements
