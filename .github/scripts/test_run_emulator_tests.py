"""Check cold-boot ordering and exit propagation without downloading an emulator."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("run_emulator_tests.sh").resolve()


class EmulatorStartupTest(unittest.TestCase):
    def run_startup(self, test_exit=0, boot_failure=False, profile="Nexus 10", stubborn_emulator=False):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            sdk = root / "sdk"
            tools = sdk / "cmdline-tools/latest/bin"
            tools.mkdir(parents=True)
            (sdk / "platform-tools").mkdir()
            (sdk / "emulator").mkdir()
            scripts = {
                tools / "sdkmanager": '#!/bin/bash\nexit 0\n',
                tools / "avdmanager": '#!/bin/bash\ncat >/dev/null\n',
                sdk / "emulator/emulator": '''#!/bin/bash
if [[ "$BOOT_FAILURE" == 1 ]]; then exit 1; fi
if [[ "$STUBBORN_EMULATOR" == 1 ]]; then trap '' TERM; fi
echo $$ > emulator-pid
touch emulator-started
exec /bin/sleep 60
''',
                sdk / "platform-tools/adb": '''#!/bin/bash
echo "$*" >> calls
case "$*" in
 'shell getprop sys.boot_completed')
   if [[ -f boot-polled && -f emulator-started ]]; then echo 1; else touch boot-polled; fi ;;
 'shell input keyevent'*) exit 99 ;;
esac
''',
            }
            driver = root / ".github/scripts/run_instrumented_tests.sh"
            driver.parent.mkdir(parents=True)
            scripts[driver] = '''#!/bin/bash
test -f boot-polled && test -f emulator-started || exit 98
touch tests-started
exit "$TEST_EXIT"
'''
            for path, content in scripts.items():
                path.write_text(content)
                path.chmod(0o755)
            env = dict(os.environ, ANDROID_HOME=str(sdk), RUNNER_TEMP=str(root),
                       TEST_PROFILE=profile, TEST_API="35", TEST_EXIT=str(test_exit),
                       BOOT_FAILURE=str(int(boot_failure)),
                       STUBBORN_EMULATOR=str(int(stubborn_emulator)))
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, timeout=15)
            if (root / "emulator-pid").exists():
                with self.assertRaises(ProcessLookupError):
                    os.kill(int((root / "emulator-pid").read_text()), 0)
            return result, (root / "tests-started").exists(), (root / "calls").read_text()

    def test_waits_for_boot_without_injecting_input_and_cleans_up(self):
        result, started, calls = self.run_startup()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(started)
        self.assertNotIn("input keyevent", calls)

    def test_profiles_reduce_rendering_work_without_changing_dp_geometry(self):
        for profile, native_width, native_height, native_density in [
            ("Nexus 6", 1440, 2560, 560),
            ("Nexus 10", 2560, 1600, 320),
            ("7.6in Foldable", 1768, 2208, 420),
        ]:
            with self.subTest(profile=profile):
                result, started, calls = self.run_startup(profile=profile)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertTrue(started)
                size = next(c.removeprefix("shell wm size ") for c in calls.splitlines()
                            if c.startswith("shell wm size "))
                density = int(next(c.removeprefix("shell wm density ") for c in calls.splitlines()
                                   if c.startswith("shell wm density ")))
                width, height = map(int, size.split("x"))
                self.assertEqual(native_width / native_density, width / density)
                self.assertEqual(native_height / native_density, height / density)
                self.assertLess(width * height, native_width * native_height)

    def test_preserves_test_failure(self):
        result, started, calls = self.run_startup(test_exit=7)
        self.assertEqual(7, result.returncode, result.stderr)
        self.assertTrue(started)

    def test_dead_emulator_never_starts_tests(self):
        result, started, calls = self.run_startup(boot_failure=True)
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertFalse(started)
        self.assertIn("Emulator failed to boot", result.stdout)

    def test_stubborn_emulator_is_reaped_without_losing_test_result(self):
        for exit_code in (0, 7):
            with self.subTest(exit_code=exit_code):
                result, started, _ = self.run_startup(
                    test_exit=exit_code, stubborn_emulator=True)
                self.assertTrue(started)
                self.assertEqual(exit_code, result.returncode, result.stderr)
