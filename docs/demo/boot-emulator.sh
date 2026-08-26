#!/bin/bash
# Creates (if needed) and boots the headless AVD used for the demo.
set -e
export JAVA_HOME=/home/hermes/toolchain/jdk17
export ANDROID_HOME=/home/hermes/toolchain/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export ANDROID_AVD_HOME=$HOME/.android/avd
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

AVD=habitlock
AVDMANAGER=$(find $ANDROID_HOME/cmdline-tools -name avdmanager -type f | head -1)

if [ ! -d "$ANDROID_AVD_HOME/$AVD.avd" ]; then
  echo "=== creating AVD $AVD ==="
  echo "no" | "$AVDMANAGER" create avd \
    -n "$AVD" \
    -k "system-images;android-34;google_apis;x86_64" \
    -d pixel_5 --force
  {
    echo "hw.lcd.density=440"
    echo "hw.lcd.width=1080"
    echo "hw.lcd.height=2340"
    echo "disk.dataPartition.size=6G"
  } >> "$ANDROID_AVD_HOME/$AVD.avd/config.ini"
fi

# 2 GB guest. The host has 15 GB but Gradle daemons and the JVM toolchain share it;
# an earlier 3 GB attempt was OOM-killed mid-demo.
sed -i 's/^hw.ramSize=.*/hw.ramSize=2048/' "$ANDROID_AVD_HOME/$AVD.avd/config.ini"
grep -q '^hw.ramSize' "$ANDROID_AVD_HOME/$AVD.avd/config.ini" || echo "hw.ramSize=2048" >> "$ANDROID_AVD_HOME/$AVD.avd/config.ini"

echo "=== booting $AVD headless ==="
exec emulator -avd "$AVD" \
  -no-window -no-audio -no-boot-anim -no-snapshot \
  -gpu guest \
  -memory 2048 \
  -accel on \
  -port 5554
