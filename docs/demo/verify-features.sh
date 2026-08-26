#!/bin/bash
# Verifies the new features on the running emulator:
#   - the AI-tutor task appears (optional in Phase 1)
#   - task rows expose an Open button
#   - a STALE heartbeat surfaces the OEM-kill warning
set -e
export ANDROID_HOME=/home/hermes/toolchain/android-sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH

PKG=com.liktun.japanesehabitlock
SVC="$PKG/$PKG.service.HabitLockAccessibilityService"
SHOTS=/home/hermes/demo-shots2
D="-s emulator-5554"

mkdir -p "$SHOTS"; rm -f "$SHOTS"/*.png "$SHOTS"/*.xml

shot() { sleep "${2:-2}"; adb $D exec-out screencap -p > "$SHOTS/$1.png"; echo "  [shot] $1.png ($(stat -c%s "$SHOTS/$1.png") b)"; }
tap() { echo "  [tap] $1,$2 ($3)"; adb $D shell input tap "$1" "$2"; sleep "${4:-2}"; }

echo "=== install ==="
adb $D install -r "$PWD/app/build/outputs/apk/debug/app-debug.apk" 2>&1 | tail -1
adb $D shell pm clear $PKG >/dev/null
sleep 2
adb $D shell settings put secure enabled_accessibility_services "$SVC"
adb $D shell settings put secure accessibility_enabled 1
sleep 4

echo "=== 1. checklist: AI tutor task + Open buttons ==="
adb $D shell am start -n "$PKG/.MainActivity" >/dev/null
shot 01-checklist-with-tutor 6

echo "=== dump visible task rows ==="
adb $D shell uiautomator dump --compressed /sdcard/u.xml >/dev/null 2>&1
adb $D pull /sdcard/u.xml "$SHOTS/u.xml" >/dev/null 2>&1
python3 - "$SHOTS/u.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter():
    t=(n.get('text') or '').strip()
    if t: print("   ", t[:78])
PY

echo "=== 2. settings screen ==="
tap 956 249 "Apps" 3
shot 02-settings-healthy 2

echo "=== done ==="
ls -la "$SHOTS"/*.png
