#!/bin/bash
# Captures each candidate front-end style on the emulator, in three states.
#
# Launches StyleGalleryActivity once per (style, state) pair with the state passed as
# an intent extra, so every screenshot is a clean full-screen render with no chrome.
set -e
export ANDROID_HOME=/home/hermes/toolchain/android-sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH

PKG=com.liktun.japanesehabitlock
ACT="$PKG/$PKG.ui.styles.StyleGalleryActivity"
SHOTS=/home/hermes/style-shots
D="-s emulator-5554"

WANIKANI=wanikani_reviews
BUNPRO=bunpro_reviews
SHADOW=shadowing_session

mkdir -p "$SHOTS"; rm -f "$SHOTS"/*.png

capture() { # $1=style $2=label $3=done-csv $4=settle-seconds
  adb $D shell am force-stop $PKG >/dev/null 2>&1 || true
  # `am start` rejects an empty --es value ("Argument expected after done"), so the
  # empty case passes a sentinel the activity treats as "nothing completed".
  local done_arg="${3:-none}"
  [ -z "$3" ] && done_arg="none"
  adb $D shell am start -n "$ACT" --es style "$1" --es done "$done_arg" >/dev/null 2>&1
  sleep "${4:-5}"
  local out="$SHOTS/$(echo "$1" | tr 'A-Z_' 'a-z-')-$2.png"
  adb $D exec-out screencap -p > "$out"
  echo "  [shot] $(basename "$out")  $(stat -c%s "$out") b"
}

echo "=== installing ==="
adb $D install -r "$PWD/app/build/outputs/apk/debug/app-debug.apk" 2>&1 | tail -1

for STYLE in SUMIE NEON WA_MODERN; do
  echo "=== $STYLE ==="
  capture "$STYLE" locked   ""                                 6
  capture "$STYLE" partial  "$WANIKANI"                        5
  capture "$STYLE" unlocked "$WANIKANI,$BUNPRO,$SHADOW"        6
done

echo "=== done ==="
ls -la "$SHOTS"/*.png
