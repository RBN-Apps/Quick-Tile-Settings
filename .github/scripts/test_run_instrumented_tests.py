"""Exercise CI preflight and preserve failures without an attached Android device."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("run_instrumented_tests.sh").resolve()


class InstrumentedDriverTest(unittest.TestCase):
    def run_driver(self, test_exit=0, anr=False, first_boot=False):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            bin_dir = root / "bin"
            bin_dir.mkdir()
            commands = {
                "adb": '''#!/usr/bin/env bash
printf '%s\\n' "$*" >> adb-calls.txt
case "$*" in
  'shell settings get secure user_setup_complete')
    if [[ "$FIRST_BOOT" == 1 && ! -f setup-observed ]]; then
      echo 0
    else
      echo 1
    fi ;;
  'shell cmd package resolve-activity'*)
    if [[ "$FIRST_BOOT" == 1 && ! -f setup-observed ]]; then
      touch setup-observed
      echo com.example.setup/.Setup
    else
      echo com.example.launcher/.Home
    fi ;;
  'shell dumpsys window')
    if [[ "$SIMULATE_ANR" == 1 ]]; then
      echo 'mCurrentFocus=Window{Application Not Responding: com.android.systemui}'
    else
      echo 'mCurrentFocus=Window{com.example.launcher/com.example.launcher.Home}'
    fi ;;
  pull*) exit 1 ;;
  *) echo diagnostic ;;
esac
''',
                "sleep": "#!/usr/bin/env bash\nexit 0\n",
                "timeout": '#!/usr/bin/env bash\nshift 2\nexec "$@"\n',
            }
            for name, content in commands.items():
                path = bin_dir / name
                path.write_text(content)
                path.chmod(0o755)
            gradle = root / "gradlew"
            gradle.write_text('#!/usr/bin/env bash\ntouch gradle-invoked\nexit "$TEST_EXIT"\n')
            gradle.chmod(0o755)
            env = dict(os.environ, PATH=f"{bin_dir}:{os.environ['PATH']}",
                       TEST_EXIT=str(test_exit), SIMULATE_ANR=str(int(anr)),
                       FIRST_BOOT=str(int(first_boot)))
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, timeout=10)
            invoked = (root / "gradle-invoked").exists()
            calls = (root / "adb-calls.txt").read_text()
            diagnostics = (root / "app/build/ci-device-diagnostics/after-tests-lastanr.txt").exists()
            return result, invoked, calls, diagnostics

    def test_runs_suite_only_after_stable_launcher_focus(self):
        result, invoked, calls, diagnostics = self.run_driver()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(invoked)
        # Five readiness samples plus before/prepared/after diagnostic captures.
        self.assertEqual(8, calls.splitlines().count("shell dumpsys window"))
        self.assertTrue(diagnostics)

    def test_first_boot_resolves_launcher_again_after_setup(self):
        result, invoked, calls, diagnostics = self.run_driver(first_boot=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(invoked)
        self.assertEqual(9, calls.splitlines().count("shell dumpsys window"))
        self.assertTrue(diagnostics)

    def test_preserves_test_failure_even_if_diagnostics_pull_fails(self):
        for code in (1, 143):
            with self.subTest(code=code):
                result, invoked, _, diagnostics = self.run_driver(test_exit=code)
                self.assertEqual(code, result.returncode, result.stderr)
                self.assertTrue(invoked)
                self.assertTrue(diagnostics)

    def test_system_anr_fails_preflight_without_starting_tests(self):
        result, invoked, _, diagnostics = self.run_driver(anr=True)
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertFalse(invoked)
        self.assertIn("Android system ANR", result.stdout)
        self.assertTrue(diagnostics)


if __name__ == "__main__":
    unittest.main()
