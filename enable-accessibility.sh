#!/bin/bash
# Enable Square Overlay accessibility service

# Get the current enabled services
ENABLED=$(adb shell settings get secure enabled_accessibility_services)

# Add our service if not already there
SERVICE="com.squareoverlay/.ScrollAccessibilityService"

if [[ "$ENABLED" == *"$SERVICE"* ]]; then
    echo "Accessibility service already enabled"
else
    if [ "$ENABLED" == "null" ] || [ -z "$ENABLED" ]; then
        NEW_ENABLED="$SERVICE"
    else
        NEW_ENABLED="$ENABLED:$SERVICE"
    fi
    
    # Enable accessibility
    adb shell settings put secure enabled_accessibility_services "$NEW_ENABLED"
    adb shell settings put secure accessibility_enabled 1
    
    echo "Accessibility service enabled!"
fi

# Verify
adb shell settings get secure enabled_accessibility_services
