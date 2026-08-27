#!/bin/bash
# Proves the theme picker works end to end in the REAL app (not the gallery):
# open settings -> theme -> pick each theme -> confirm the checklist repaints,
# then relaunch to confirm the choice persisted.
export ANDROID_HOME=/home/hermes/toolchain/android-sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH

PKG=com.liktun.japanesehabitlock
SHOTS=/home/hermes/theme-shots
D="-s emulator-5554"
APK=/home/hermes/japanese-habit-lock/app/build/outputs/apk/debug/app-debug.apk

mkdir -p "$SHOTS"; rm -f "$SHOTS"/*.png "$SHOTS"/*.xml

alive() { [ "$(adb $D shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; }
shot() { sleep "${2:-3}"; adb $D exec-out screencap -p > "$SHOTS/$1.png"; echo "  [shot] $1.png $(stat -c%s "$SHOTS/$1.png" 2>/dev/null) b"; }

# Taps the first node whose text contains $1 (excluding EditText, which also "matches"
# a typed query and was the cause of an earlier mis-tap).
tap_text() {
  adb $D shell uiautomator dump --compressed /sdcard/u.xml >/dev/null 2>&1
  adb $D pull /sdcard/u.xml "$SHOTS/u.xml" >/dev/null 2>&1
  local c
  c=$(python3 - "$SHOTS/u.xml" "$1" <<'PY'
import re,sys,xml.etree.ElementTree as ET
try: tree=ET.parse(sys.argv[1])
except Exception: sys.exit(0)
want=sys.argv[2].lower()
for n in tree.iter():
    t=(n.get('text') or '')+' '+(n.get('content-desc') or '')
    if want in t.lower() and 'EditText' not in (n.get('class') or ''):
        m=re.findall(r'-?\d+', n.get('bounds',''))
        if len(m)==4:
            print((int(m[0])+int(m[2]))//2,(int(m[1])+int(m[3]))//2); break
PY
)
  [ -z "$c" ] && { echo "  !! not found: $1"; return 1; }
  echo "  [tap] '$1' at $c"
  adb $D shell input tap $c
  sleep 2
}

echo "=== install + reset ==="
adb $D install -r "$APK" 2>&1 | tail -1
adb $D shell pm clear $PKG >/dev/null
sleep 2

echo "=== 1. default theme on first launch ==="
adb $D shell am start -n "$PKG/.MainActivity" >/dev/null
shot 01-default 6

echo "=== 2. into settings -> theme picker ==="
tap_text "⚙" || true
shot 02-settings 3
tap_text "Theme" || true
shot 03-picker 3

echo "=== 3. pick each theme and confirm the checklist repaints ==="
for T in "Sumi-e" "Neon" "Kinari" "Kisetsu" "Wa-Modern"; do
  alive || { echo "  !! device gone"; break; }
  tap_text "$T" || continue
  # Back out of the picker to the checklist to see the theme applied.
  adb $D shell input keyevent KEYCODE_BACK; sleep 1
  adb $D shell input keyevent KEYCODE_BACK; sleep 2
  shot "04-applied-$(echo "$T" | tr 'A-Z ' 'a-z-')" 4
  # Return to the picker for the next pick.
  tap_text "⚙" >/dev/null 2>&1 || true
  tap_text "Theme" >/dev/null 2>&1 || true
done

echo "=== 4. persistence: kill and relaunch ==="
adb $D shell am force-stop $PKG
sleep 2
adb $D shell am start -n "$PKG/.MainActivity" >/dev/null
shot 05-persisted 6

echo "=== done: $(ls "$SHOTS"/*.png 2>/dev/null | wc -l) shots ==="
