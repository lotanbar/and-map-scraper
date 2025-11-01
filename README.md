# Square Overlay

Android screenshot tool with precise overlay positioning, automatic scrolling, and organized capture into zoom folders.

## Features

- **Draggable & resizable overlay** with real-time coordinate display
- **Sequential screenshot capture** with auto-scroll (00001.png, 00002.png...)
- **Zoom folder organization** - Create zoom1, zoom2, zoom3 folders for different zoom levels
- **Line break markers** - Mark new rows with 'z' suffix (00004z.png)
- **Scroll calibration** - Click counter displays to edit scroll distances
- **Built-in file browser** for quick access to captures

## Requirements

- Android 7.0+ (API 24)
- Overlay permission
- Screenshot permission
- Accessibility service (for auto-scrolling)

## Installation

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Enable accessibility service: Settings > Accessibility > Square Overlay

## Controls

**First Row (Left to Right):**
- **📁** File Browser
- **Z** Create new zoom folder (zoom1, zoom2...)
- **↓** Go down a line and scroll back
- **📷** Screenshot + auto-scroll
- **R** Reset to defaults

**Second Row:**
- Edit scroll increment, counter displays (clickable), calibration buttons

## Default Values

- Square: 1000x1000px, centered horizontally, Y=400
- Horizontal scroll: 1050px
- Vertical scroll: 1038px
- All delays: 1100ms

## Output Structure

```
/sdcard/Pictures/SquareOverlay/
├── 00001.png
├── 00002.png
├── 00003z.png  # Line break marker
├── zoom1/
│   ├── 00001.png
│   └── 00002.png
└── zoom2/
    └── ...
```
