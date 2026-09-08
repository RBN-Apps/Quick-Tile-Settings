#!/usr/bin/env bash
set -euo pipefail

# Only the emulator created by run_emulator_tests.sh is a test target.
export ANDROID_SERIAL=emulator-5554
artifacts=app/build/ci-device-diagnostics
mkdir -p "$artifacts"

capture_device() {
  local phase=$1
  adb shell dumpsys power > "$artifacts/$phase-power.txt" 2>&1
  adb shell dumpsys window > "$artifacts/$phase-window.txt" 2>&1
  adb shell dumpsys activity lastanr > "$artifacts/$phase-lastanr.txt" 2>&1
  adb shell dumpsys input > "$artifacts/$phase-input.txt" 2>&1
  adb exec-out screencap -p > "$artifacts/$phase.png"
}

# Keep system logs as well as Gradle's per-test app logs; ANRs can precede instrumentation.
adb logcat -b all -v threadtime > "$artifacts/system-logcat.txt" 2>&1 &
logcat_pid=$!
finish() {
  local result=$?
  trap - EXIT
  capture_device after-tests || true
  adb shell dumpsys dropbox --print system_app_anr > "$artifacts/system-app-anrs.txt" 2>&1 || true
  adb pull /sdcard/Download/qt-ci-diagnostics "$artifacts/tests" || true
  kill "$logcat_pid" 2>/dev/null || true
  wait "$logcat_pid" 2>/dev/null || true
  exit "$result"
}
trap finish EXIT

capture_device before-prepare || true
# Compose semantic actions can run behind system windows; real Back/IME events cannot.
adb shell svc power stayon true
adb shell settings put system screen_off_timeout 2147483647
# svc power stayon wakes the device without injecting input into a booting launcher.
adb shell wm dismiss-keyguard
adb shell settings put secure show_ime_with_hard_keyboard 1
# sys.boot_completed does not mean that the launcher/System UI has finished starting.
# Require consecutive focused launcher samples instead of sleeping a fixed duration.
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME
ready_samples=0
deadline=$((SECONDS + 180))
while (( SECONDS < deadline )); do
  windows=$(adb shell dumpsys window)
  if [[ "$windows" == *"Application Not Responding"* ]]; then
    echo "::error::Android system ANR before instrumentation; see ci-device-diagnostics."
    exit 1
  fi
  # A fresh image may initially resolve HOME to its setup activity.
  # Resolve again after provisioning instead of retaining that temporary component.
  setup_complete=$(adb shell settings get secure user_setup_complete | tr -d '\r')
  launcher=$(adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -n 1)
  launcher_package=${launcher%%/*}
  launcher_activity=${launcher#*/}
  if [[ "$launcher_activity" == .* ]]; then
    launcher_activity="$launcher_package$launcher_activity"
  fi
  launcher_window="$launcher_package/$launcher_activity"
  focus=$(printf '%s\n' "$windows" | grep 'mCurrentFocus=' || true)
  if [[ "$setup_complete" == 1 && "$launcher" == */* && "$focus" == *"$launcher_window"* ]]; then
    ready_samples=$((ready_samples + 1))
    if (( ready_samples >= 5 )); then
      break
    fi
  else
    ready_samples=0
  fi
  sleep 2
done
if (( ready_samples < 5 )); then
  echo "::error::Emulator launcher did not acquire stable input focus (setup=$setup_complete, launcher=$launcher, focus=$focus); see ci-device-diagnostics."
  exit 1
fi
capture_device prepared || true

timeout --preserve-status 25m ./gradlew connectedDebugAndroidTest --no-daemon --max-workers=2 \
  -Pkotlin.compiler.execution.strategy=in-process
