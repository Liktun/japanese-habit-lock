#!/bin/bash
# End-to-end demo of the block gate on a real emulator.
#
# Proves:
#   LOCKED   + open Clock -> BlockerActivity intercepts it   <-- the claim
#   UNLOCKED + open Clock -> Clock opens normally            <-- the control
#
# Coordinates are hardcoded from a one-time `uiautomator dump` of this exact AVD
# (1080x2340 @440dpi). Calling uiautomator repeatedly segfaulted the headless
# emulator, so the layout is pinned instead of re-queried each run.
set -e
export ANDROID_HOME=/home/hermes/toolchain/android-sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH

PKG=com.liktun.japanesehabitlock
SVC="$PKG/$PKG.service.HabitLockAccessibilityService"
VICTIM=com.google.android.deskclock
SHOTS=/home/hermes/demo-shots
D="-s emulator-5554"

mkdir -p "$SHOTS"; rm -f "$SHOTS"/*.png

shot() { sleep "${2:-2}"; adb $D exec-out screencap -p > "$SHOTS/$1.png"; echo "  [shot] $1.png ($(stat -c%s "$SHOTS/$1.png") b)"; }
tap() { echo "  [tap] $1,$2  ($3)"; adb $D shell input tap "$1" "$2"; sleep "${4:-2}"; }

top() {
  adb $D shell dumpsys activity activities 2>/dev/null \
    | grep -m1 "topResumedActivity" \
    | grep -oE '[a-zA-Z0-9_.]+/[a-zA-Z0-9_.$]+' | head -1
}

open_victim() {
  adb $D shell monkey -p $VICTIM -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  sleep 5
}

echo "=== 0. clean slate + enable service ==="
adb $D shell pm clear $PKG >/dev/null
sleep 2
adb $D shell settings put secure enabled_accessibility_services "$SVC"
adb $D shell settings put secure accessibility_enabled 1
sleep 4
echo "  registered: $(adb $D shell settings get secure enabled_accessibility_services | tr -d '\r')"
echo "  bound in dumpsys: $(adb $D shell dumpsys accessibility 2>/dev/null | grep -c HabitLockAccessibilityService)"

echo "=== 1. launch app (expect LOCKED) ==="
adb $D shell am start -n "$PKG/.MainActivity" >/dev/null
shot 01-locked 6

echo "=== 2. open the app picker ==="
tap 956 249 "Apps button" 3
shot 02-picker 2

echo "=== 3. tick the Clock checkbox ==="
tap 133 2016 "Clock checkbox" 2
shot 03-clock-blocked 2

echo "=== 4. back to the checklist ==="
adb $D shell input keyevent KEYCODE_BACK
sleep 3

echo "=== 5. THE TEST: open Clock while LOCKED ==="
open_victim
BLOCKED_TOP=$(top)
echo "  top activity: $BLOCKED_TOP"
shot 05-BLOCKED 2

echo "=== 6. complete the 3 blocking tasks ==="
adb $D shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 4
tap 100 1046 "WaniKani checkbox" 1
tap 100 1219 "Bunpro checkbox" 1
tap 100 1392 "Shadowing checkbox" 1
shot 06-unlocked 3

echo "=== 7. CONTROL: open Clock while UNLOCKED ==="
open_victim
ALLOWED_TOP=$(top)
echo "  top activity: $ALLOWED_TOP"
shot 07-allowed 2

echo
echo "================= VERDICT ================="
echo "while LOCKED   top was: $BLOCKED_TOP"
echo "while UNLOCKED top was: $ALLOWED_TOP"
PASS=1
case "$BLOCKED_TOP" in
  *BlockerActivity*) echo "PASS: blocker intercepted the blocked app" ;;
  *) echo "FAIL: expected BlockerActivity while locked"; PASS=0 ;;
esac
case "$ALLOWED_TOP" in
  *deskclock*) echo "PASS: app opened normally once unlocked" ;;
  *) echo "FAIL: expected the app to open while unlocked"; PASS=0 ;;
esac
echo "OVERALL: $([ $PASS -eq 1 ] && echo PASS || echo FAIL)"
ls -la "$SHOTS"/*.png
