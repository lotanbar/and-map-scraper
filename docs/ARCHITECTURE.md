# Architecture

## Overview

The app uses a service-based overlay system to display a persistent, interactive square across all apps.

## Components

### 1. MainActivity (Entry Point)
- **Responsibility**: Permission management and user interface
- **Key Methods**:
  - `checkOverlayPermission()`: Requests SYSTEM_ALERT_WINDOW permission
  - `requestScreenshotPermission()`: Requests MediaProjection permission
  - Show/Hide button handlers: Start OverlayService with actions

### 2. OverlayService (Overlay Manager)
- **Responsibility**: Manage overlay window lifecycle and coordinate screenshot
- **Key Methods**:
  - `showOverlay()`: Create and display SquareOverlayView with ScreenshotService
  - `hideOverlay()`: Remove overlay from WindowManager
  - `onCreate()`: Start as foreground service with notification (required for MediaProjection)
- **Window Setup**:
  - Type: `TYPE_APPLICATION_OVERLAY` (Android 8+)
  - Flags: `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL` (allows interaction)
  - Size: Matches square size exactly (no padding)

### 3. ScreenshotService (Screenshot Capture)
- **Responsibility**: Handle MediaProjection API for screen capture
- **Key Methods**:
  - `startProjection()`: Initialize MediaProjection and create persistent VirtualDisplay
  - `captureScreenshot()`: Capture screen and crop to square coordinates
  - `cleanup()` / `release()`: Properly release resources
- **Android 14+ Compatibility**:
  - Registers callback on MediaProjection (required)
  - Uses persistent VirtualDisplay (cannot create multiple per projection)
  - Runs in foreground service with MEDIA_PROJECTION type

### 4. SquareOverlayView (Interactive UI)
- **Responsibility**: Render square and handle touch interactions
- **Key Features**:
  - Touch handling for drag/resize
  - Green camera button for screenshots
  - Real-time coordinate calculation
  - Text overlay with measurements

#### Touch Event Logic
```
ACTION_DOWN:
  - If touch near bottom-right corner (80px radius) → Start resize mode
  - If touch inside square → Start drag mode

ACTION_MOVE:
  - Resize mode: Adjust square size, update window dimensions
  - Drag mode: Move window, keep square on screen

ACTION_UP:
  - Reset drag/resize flags
```

## Data Flow

### Overlay Creation
```
User Tap → MainActivity → Request Screenshot Permission
                            ↓
                      Start OverlayService (foreground)
                            ↓
                      Create SquareOverlayView + ScreenshotService
                            ↓
                      Add to WindowManager
                            ↓
                      Touch Events → Update Window
```

### Screenshot Capture
```
User Taps Camera → SquareOverlayView callback
                            ↓
                      Hide Overlay (50ms delay)
                            ↓
                      ScreenshotService.captureScreenshot()
                            ↓
                      Reuse existing VirtualDisplay
                            ↓
                      Crop to square percentage coordinates
                            ↓
                      Save PNG to Pictures/SquareOverlay
                            ↓
                      Show Overlay (150ms delay)
```

## Coordinate System

- **Window coordinates**: Position of overlay window (matches square position exactly)
- **View coordinates**: Touch events relative to view (0,0 = top-left)
- **Absolute screen coordinates**: `windowParams.x + squareX`
- **Percentage coordinates**: Used for screenshots to work across different resolutions

No padding between window and square - they are the same size for pixel-perfect positioning.
