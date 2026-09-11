#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_HOME:?Android SDK required}"
: "${TEST_PROFILE:?AVD profile required}"
: "${TEST_API:?API level required}"
export ANDROID_SERIAL=emulator-5554
export ANDROID_AVD_HOME="${RUNNER_TEMP:?Temporary runner directory required}/qt-avds"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
mkdir -p "$ANDROID_AVD_HOME" app/build/ci-device-diagnostics

# Use the installed SDK tooling, but own startup: emulator-runner injects MENU as
# soon as sys.boot_completed flips, before the launcher necessarily owns a window.
# That input caused a Launcher ANR on the cold API-35 tablet before our preflight.
(set +o pipefail; yes | sdkmanager --licenses > /dev/null)
sdkmanager "system-images;android-$TEST_API;default;x86_64" "emulator"
printf 'no\n' | avdmanager create avd --force -n qt_ci \
  --package "system-images;android-$TEST_API;default;x86_64" --device "$TEST_PROFILE"
adb start-server
emulator_pid=
finish() {
  local result=$?
  trap - EXIT
  if [[ -n "$emulator_pid" ]]; then
    # Diagnostics are already captured; this disposable AVD has no snapshot to save.
    # QEMU can acknowledge `adb emu kill` yet hang during teardown and ignore TERM.
    # Kill only our own child before reaping it, preserving the test exit status.
    echo "Stopping CI emulator process $emulator_pid"
    kill -KILL "$emulator_pid" 2>/dev/null || true
    wait "$emulator_pid" 2>/dev/null || true
  fi
  exit "$result"
}
trap finish EXIT
"$ANDROID_HOME/emulator/emulator" -avd qt_ci -port 5554 -cores 2 -memory 2048 \
  -no-window -gpu swiftshader -no-snapshot -noaudio -no-boot-anim -no-metrics \
  > app/build/ci-device-diagnostics/emulator.log 2>&1 &
emulator_pid=$!
deadline=$((SECONDS + 600))
while [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != 1 ]]; do
  if ! kill -0 "$emulator_pid" 2>/dev/null || (( SECONDS >= deadline )); then
    echo "::error::Emulator failed to boot; see ci-device-diagnostics/emulator.log."
    exit 1
  fi
  sleep 2
done
# Preserve each profile's logical dp size, but render one quarter of the pixels.
# At native Nexus-10 resolution SwiftShader starved Launcher3's RenderThread on
# the two-core runner; its ANR window then stole focus from the whole test suite.
case "$TEST_PROFILE" in
  "Nexus 6") adb shell wm size 720x1280; adb shell wm density 280 ;;
  "Nexus 10") adb shell wm size 1280x800; adb shell wm density 160 ;;
  "7.6in Foldable") adb shell wm size 884x1104; adb shell wm density 210 ;;
esac
# No synthetic key events during boot. The driver waits for stable launcher focus.
adb shell settings put global window_animation_scale 0.0
adb shell settings put global transition_animation_scale 0.0
adb shell settings put global animator_duration_scale 0.0
bash .github/scripts/run_instrumented_tests.sh
