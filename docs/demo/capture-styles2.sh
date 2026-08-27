#!/bin/bash
# Resumable capture: skips any screenshot that already exists, and reboots the
# emulator if it dies mid-run.
#
# Needed because heavy Canvas effects (radial-gradient embers, particle fields)
# can segfault the software-rendered headless emulator, and re-running the whole
# sequence from scratch each time never finishes.
export ANDROID_HOME=/home/hermes/toolchain/android-sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH

PKG=com.liktun.japanesehabitlock
ACT="$PKG/$PKG.ui.styles.StyleGalleryActivity"
SHOTS=/home/hermes/style-shots2
D="-s emulator-5554"
APK=/home/hermes/japanese-habit-lock/app/build/outputs/apk/debug/app-debug.apk

WANIKANI=wanikani_reviews
BUNPRO=bunpro_reviews
SHADOW=shadowing_session

mkdir -p "$SHOTS"

alive() { [ "$(adb $D shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; }

revive() {
  echo "  !! emulator down, rebooting"
  pkill -f "emulator -avd habitlock" 2>/dev/null || true
  sleep 5
  ( cd /home/hermes && nohup bash boot-emulator.sh > /home/hermes/emulator-boot.log 2>&1 & ) >/dev/null 2>&1
  for _ in $(seq 1 40); do alive && break; sleep 10; done
  alive || { echo "  !! could not revive"; return 1; }
  adb $D install -r "$APK" >/dev/null 2>&1
  return 0
}

# $1=style $2=label $3=done-csv $4=week $5=date
capture() {
  local out="$SHOTS/$(echo "$1" | tr 'A-Z_' 'a-z-')-$2.png"
  if [ -s "$out" ]; then echo "  [skip] $(basename "$out")"; return 0; fi
  alive || revive || return 1

  local done_arg="${3:-none}"; [ -z "$3" ] && done_arg="none"
  adb $D shell am force-stop $PKG >/dev/null 2>&1
  adb $D shell am start -n "$ACT" \
    --es style "$1" --es done "$done_arg" \
    --ei week "${4:-3}" --es date "${5:-2026-08-27}" >/dev/null 2>&1
  sleep 6
  if ! alive; then revive || return 1; return 1; fi
  adb $D exec-out screencap -p > "$out" 2>/dev/null
  if [ -s "$out" ]; then echo "  [shot] $(basename "$out")  $(stat -c%s "$out") b"
  else rm -f "$out"; echo "  [FAIL] $(basename "$out")"; fi
}

alive || revive
adb $D install -r "$APK" 2>&1 | tail -1

for STYLE in KINARI SUMIZOME KISETSU; do
  echo "=== $STYLE ==="
  capture "$STYLE" locked   ""                          3 2026-08-27
  capture "$STYLE" partial  "$WANIKANI"                 3 2026-08-27
  capture "$STYLE" unlocked "$WANIKANI,$BUNPRO,$SHADOW" 3 2026-08-27
done

echo "=== IMMERSION RAMP (Kinari) ==="
capture KINARI week01-english  "$WANIKANI" 1  2026-08-27
capture KINARI week04-labels   "$WANIKANI" 4  2026-08-27
capture KINARI week06-titles   "$WANIKANI" 6  2026-08-27
capture KINARI week10-furigana "$WANIKANI" 10 2026-08-27
capture KINARI week16-full     "$WANIKANI" 16 2026-08-27

echo "=== SEASONS (Kisetsu) ==="
capture KISETSU spring "$WANIKANI" 3 2026-04-10
capture KISETSU summer "$WANIKANI" 3 2026-07-10
capture KISETSU autumn "$WANIKANI" 3 2026-10-10
capture KISETSU winter "$WANIKANI" 3 2026-01-10

echo "=== captured: $(ls "$SHOTS"/*.png 2>/dev/null | wc -l) ==="
