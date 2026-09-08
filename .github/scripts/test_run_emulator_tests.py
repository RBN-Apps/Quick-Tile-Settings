"""Check cold-boot ordering and exit propagation without downloading an emulator."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("run_emulator_tests.sh").resolve()


class EmulatorStartupTest(unittest.TestCase):
    def run_startup(self, test_exit=0, boot_failure=False):
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
                       TEST_PROFILE="Nexus 10", TEST_API="35", TEST_EXIT=str(test_exit),
                       BOOT_FAILURE=str(int(boot_failure)))
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, timeout=15)
            return result, (root / "tests-started").exists(), (root / "calls").read_text()

    def test_waits_for_boot_without_injecting_input_and_cleans_up(self):
        result, started, calls = self.run_startup()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(started)
        self.assertNotIn("input keyevent", calls)
        self.assertIn("emu kill", calls)

    def test_preserves_test_failure(self):
        result, started, calls = self.run_startup(test_exit=7)
        self.assertEqual(7, result.returncode, result.stderr)
        self.assertTrue(started)
        self.assertIn("emu kill", calls)

    def test_dead_emulator_never_starts_tests(self):
        result, started, calls = self.run_startup(boot_failure=True)
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertFalse(started)
        self.assertIn("Emulator failed to boot", result.stdout)
        self.assertIn("emu kill", calls)
