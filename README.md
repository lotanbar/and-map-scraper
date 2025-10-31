# Square Overlay

Advanced Android screenshot tool with precise overlay positioning, automatic scrolling, and intelligent image stitching for capturing large content areas.

## ⚙️ Carefully Calibrated Defaults

These values have been meticulously tuned for optimal performance:

- **Square Size**: `695px` - Perfect balance for capturing content tiles
- **Square Position**: `(338, 117)` - Optimal starting position on screen
- **Scroll Jump**: `773px` - Precisely calibrated for seamless horizontal scrolling
- **Scroll Increment**: `30px` - Fine-tuning adjustment step

> **Note**: These defaults are the result of extensive calibration. The Reset (R) button restores all values to these optimal settings.

## Features

### Core Functionality
- **Draggable & Resizable Overlay**: Red square with blue corner handle for resizing
- **Real-time Coordinate Display**: Shows position (pixels), size, and percentages
- **Sequential Screenshot Capture**: Auto-numbered files (1.png, 2.png, 3.png...)
- **Automatic Horizontal Scrolling**: Scrolls right by square width after each capture
- **Works Across All Apps**: Overlay persists over any application

### Advanced Features
- **Scroll Calibration**: Fine-tune scroll distance with +/- buttons and counter display
- **Line Break Markers**: Mark new rows with 'z' suffix (4z.png, 7z.png) for stitching
- **Multi-Level Zoom Stitching**: Combine tiles into larger images with automatic row detection
- **Built-in File Browser**: Quick access to captured screenshots
- **Next Line Navigation**: Scroll down and reset to start of next row

## Requirements

- Android 7.0+ (API 24)
- Overlay permission (granted on first launch)
- Screenshot permission (granted when showing overlay)
- Accessibility service permission (for auto-scrolling)

## Installation

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

### Initial Setup
1. Launch app and grant overlay permission
2. Tap "Show Square" and grant screenshot permission
3. Enable accessibility service in Settings > Accessibility > Square Overlay

### Basic Screenshot Workflow
1. Position the square over desired content
2. Tap camera button to capture (saves as `1.png`)
3. App automatically scrolls right by square width
4. Repeat for additional tiles (2.png, 3.png...)

### Multi-Row Capture Workflow
1. Capture first row of tiles (1.png, 2.png, 3.png...)
2. Tap **↓ (Next Line)** button to mark line break
3. Next capture will be numbered with 'z' suffix (e.g., 4z.png)
4. Continue capturing next row (5.png, 6.png...)
5. Repeat for additional rows

### Zoom Level Stitching
1. After capturing all tiles, tap **Z (Next Zoom Level)** button
2. All tiles are moved to `zoom1/` folder
3. Images are automatically stitched:
   - Horizontal: tiles in numerical order
   - Vertical: new rows start at files with 'z' suffix
4. Final stitched image saved as `zoom1.png`
5. Counters reset - ready for next zoom level

## 🎮 Control Panel

### Bottom Row (Main Controls)
- **📁 (File Browser)**: Opens file manager to view captures
- **↓ (Next Line)**: Marks next capture as new row start
- **📷 (Camera)**: Captures screenshot and auto-scrolls
- **Z (Zoom Level)**: Stitches all tiles and resets counters

### Top Row (Calibration)
- **Edit Input**: Set custom scroll increment (tap to edit)
- **Counter Display**: Shows current scroll distance in pixels
- **R (Reset)**: Restore all defaults (size, position, scroll distance)
- **- (Minus)**: Scroll left by increment value
- **+ (Plus)**: Scroll right by increment value

## 📂 Output Structure

```
/sdcard/Pictures/SquareOverlay/
├── 1.png              # First tile
├── 2.png              # Second tile
├── 3.png              # Third tile
├── 4z.png             # New row marker
├── 5.png              # Second row continues
├── zoom1/             # After clicking Z button
│   ├── 1.png
│   ├── 2.png
│   └── ...
├── zoom1.png          # Stitched result
└── zoom2.png          # Next zoom level
```

## Technical Details

- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Build Tool**: Gradle 8.3.2
- **Java Version**: 17
- **Image Stitching**: Native Bitmap Canvas (no external dependencies)

## Project Structure

```
square-overlay/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/squareoverlay/
│           │   ├── MainActivity.java              # Entry point, permission handling
│           │   ├── OverlayService.java            # Service managing overlay & controls
│           │   ├── ScreenshotService.java         # MediaProjection capture
│           │   ├── SquareOverlayView.java         # Draggable square overlay
│           │   ├── ScreenshotButtonView.java      # Camera button
│           │   ├── NextLineButtonView.java        # Line break button
│           │   ├── NextZoomLevelButtonView.java   # Zoom stitching button
│           │   ├── FileBrowserButtonView.java     # File manager button
│           │   ├── AdjustButtonView.java          # Calibration buttons
│           │   ├── CounterDisplayView.java        # Scroll distance display
│           │   ├── ScrollIncrementInputView.java  # Increment editor
│           │   └── ScrollAccessibilityService.java # Auto-scroll gestures
│           └── AndroidManifest.xml
├── build.gradle
└── README.md
```

## Components

### OverlayService
Core service managing:
- Overlay window lifecycle
- All control buttons and positioning
- Screenshot workflow with auto-scroll
- Scroll calibration system
- Zoom level processing and image stitching

### ScreenshotService
MediaProjection API handler:
- Persistent VirtualDisplay for rapid captures
- Precise square region cropping
- Sequential file naming with line markers

### ScrollAccessibilityService
Accessibility service for gesture automation:
- Horizontal/vertical scroll gestures
- Calibrated swipe distances
- Multi-step sequential scrolling

### SquareOverlayView
Custom draggable overlay:
- Touch-based positioning
- Corner handle resizing
- Real-time coordinate display
- Screenshot region preview
