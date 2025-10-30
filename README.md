# Square Overlay

Android app that displays a draggable, resizable square overlay on the screen with coordinate tracking.

## Features

- Draggable red square overlay
- Resizable via blue corner handle
- Real-time coordinate display (absolute pixels and percentages)
- Works across all apps (requires overlay permission)

## Requirements

- Android 7.0+ (API 24)
- Overlay permission (granted on first launch)

## Installation

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Launch app and grant overlay permission when prompted
2. Tap "Show Square" to display overlay
3. Drag square by touching inside it
4. Resize by dragging the blue circle in bottom-right corner
5. Tap "Hide Square" to remove overlay

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
│           │   ├── OverlayService.java       # Service managing overlay lifecycle
│           │   └── SquareOverlayView.java    # Custom view with drag/resize
│           └── AndroidManifest.xml
├── build.gradle
└── gradle.properties
```

## Components

### MainActivity
Entry point that handles overlay permission requests and controls service.

### OverlayService
Manages overlay window lifecycle (show/hide).

### SquareOverlayView
Custom view implementing:
- Touch event handling for drag/resize
- Real-time coordinate calculation
- Dynamic text overlay with measurements
