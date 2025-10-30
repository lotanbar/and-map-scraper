# Architecture

## Overview

The app uses a service-based overlay system to display a persistent, interactive square across all apps.

## Components

### 1. MainActivity (Entry Point)
- **Responsibility**: Permission management and user interface
- **Key Methods**:
  - `checkOverlayPermission()`: Requests SYSTEM_ALERT_WINDOW permission
  - Show/Hide button handlers: Start OverlayService with actions

### 2. OverlayService (Overlay Manager)
- **Responsibility**: Manage overlay window lifecycle
- **Key Methods**:
  - `showOverlay()`: Create and display SquareOverlayView
  - `hideOverlay()`: Remove overlay from WindowManager
- **Window Setup**:
  - Type: `TYPE_APPLICATION_OVERLAY` (Android 8+)
  - Flags: `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL` (allows interaction)
  - Size: Square size + 100px padding on each side

### 3. SquareOverlayView (Interactive UI)
- **Responsibility**: Render square and handle touch interactions
- **Key Features**:
  - Touch handling for drag/resize
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

```
User Tap → MainActivity → Start OverlayService
                            ↓
                      Create SquareOverlayView
                            ↓
                      Add to WindowManager
                            ↓
                      Touch Events → Update Window
```

## Coordinate System

- **Window coordinates**: Position of overlay window (can be negative)
- **View coordinates**: Touch events relative to view (0,0 = top-left)
- **Absolute screen coordinates**: `windowParams.x + squareX`

The overlay uses 100px padding to allow smooth dragging to screen edges without square going off-screen.
