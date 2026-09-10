import contextlib
import io
import os
import subprocess
import sys
from pathlib import Path
import tempfile
import unittest

from summarize_instrumented_tests import summarize


class SummaryTest(unittest.TestCase):
    def test_all_reports_failures_errors_and_skips_are_visible(self):
        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory) / 'TEST-first.xml'
            second = Path(directory) / 'TEST-second.xml'
            first.write_text('''<testsuite><testcase classname="Dialog" name="cancel"/>
                <testcase classname="Dialog" name="back"><failure>Timeout\nat test:104</failure></testcase>
                </testsuite>''')
            second.write_text('''<testsuites><testsuite>
                <testcase classname="Ime" name="focus"><error message="No focused window"/></testcase>
                <testcase classname="Ime" name="ignored"><skipped/></testcase>
                </testsuite></testsuites>''')
            log = io.StringIO()
            with contextlib.redirect_stdout(log):
                summary, failed = summarize([first, second], 'API 35 / Nexus 6')
            self.assertTrue(failed)
            self.assertIn('| **Total** | **4** | **1** | **2** | **1** |', summary)
            for name in ['Dialog.back', 'Ime.focus']:
                self.assertIn(name, summary)
                self.assertIn('FAILED ' + name, log.getvalue())
            self.assertIn('at test:104', summary)
            self.assertIn('No focused window', log.getvalue())
            self.assertEqual(2, log.getvalue().count('::error::'))

    def test_no_report_errors_in_log_and_summary(self):
        log = io.StringIO()
        with contextlib.redirect_stdout(log):
            summary, failed = summarize([], 'Nexus 10')
        self.assertTrue(failed)
        self.assertIn('No instrumented test XML reports found', summary)
        self.assertIn('::error::', log.getvalue())

    def test_failure_text_cannot_break_html_or_inject_annotations(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'TEST-escape.xml'
            report.write_text('''<testsuite><testcase classname="A" name="x|y">
                <failure>&lt;/pre&gt; &amp; 100%</failure></testcase></testsuite>''')
            log = io.StringIO()
            with contextlib.redirect_stdout(log):
                summary, failed = summarize([report], 'API\n::notice::')
            self.assertTrue(failed)
            self.assertIn('&lt;/pre&gt; &amp; 100%', summary)
            self.assertIn('x&#124;y', summary)
            self.assertIn('API%0A::notice::', log.getvalue().splitlines()[0])
            self.assertIn('100%25', log.getvalue().splitlines()[0])

    def test_suite_error_without_testcase_trace_is_not_silently_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'TEST-incomplete.xml'
            report.write_text('<testsuite tests="1" errors="1"><testcase name="incomplete"/></testsuite>')
            with contextlib.redirect_stdout(io.StringIO()):
                summary, failed = summarize([report], 'CI fixture')
            self.assertTrue(failed)
            self.assertIn('suite declares failures/errors', summary)

    def test_cli_exit_status_and_summary_are_consistent(self):
        cases = [
            ('<testcase name="pass"/>', 0),
            ('<testcase name="ignored"><skipped/></testcase>', 0),
            ('<testcase name="fail"><failure>AssertionError</failure></testcase>', 1),
            ('<testcase name="error"><error>Crash</error></testcase>', 1),
            # AGP exports these as failure elements even when Gradle succeeds.
            ('<testcase name="assumption"><failure>org.junit.AssumptionViolatedException</failure></testcase>', 1),
            ('', 1),
            (None, 1),
            ('malformed XML', 1),
        ]
        for content, expected in cases:
            with self.subTest(content=content), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                if content is not None:
                    (root / 'TEST-result.xml').write_text('<testsuite>' + content + '</testsuite>')
                if content == 'malformed XML':
                    (root / 'TEST-result.xml').write_text('<testsuite>')
                summary = root / 'summary.md'
                result = subprocess.run([
                    sys.executable, str(Path(__file__).with_name('summarize_instrumented_tests.py')),
                    '--reports', directory, '--label', 'CI fixture',
                ], env=dict(os.environ, GITHUB_STEP_SUMMARY=str(summary)), capture_output=True, text=True)
                self.assertEqual(expected, result.returncode, result.stdout + result.stderr)
                if content != 'malformed XML':
                    self.assertIn('Instrumented Tests', summary.read_text())


if __name__ == '__main__':
    unittest.main()
