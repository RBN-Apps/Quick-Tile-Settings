import contextlib
import io
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
                summary = summarize([first, second], 'API 35 / Nexus 6')
            self.assertIn('| **Total** | **4** | **1** | **2** | **1** |', summary)
            for name in ['Dialog.back', 'Ime.focus']:
                self.assertIn(name, summary)
                self.assertIn('FAILED ' + name, log.getvalue())
            self.assertIn('at test:104', summary)
            self.assertIn('No focused window', log.getvalue())
            self.assertEqual(2, log.getvalue().count('::error::'))

    def test_no_report_warns_in_log_and_summary(self):
        log = io.StringIO()
        with contextlib.redirect_stdout(log):
            summary = summarize([], 'Nexus 10')
        self.assertIn('No instrumented test XML reports found', summary)
        self.assertIn('::warning::', log.getvalue())

    def test_failure_text_cannot_break_html_or_inject_annotations(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'TEST-escape.xml'
            report.write_text('''<testsuite><testcase classname="A" name="x|y">
                <failure>&lt;/pre&gt; &amp; 100%</failure></testcase></testsuite>''')
            log = io.StringIO()
            with contextlib.redirect_stdout(log):
                summary = summarize([report], 'API\n::notice::')
            self.assertIn('&lt;/pre&gt; &amp; 100%', summary)
            self.assertIn('x&#124;y', summary)
            self.assertIn('API%0A::notice::', log.getvalue().splitlines()[0])
            self.assertIn('100%25', log.getvalue().splitlines()[0])


if __name__ == '__main__':
    unittest.main()
