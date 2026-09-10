"""Publish every connected-test XML result, including method names and failure traces."""
import argparse
import html
import os
import sys
from pathlib import Path
import xml.etree.ElementTree as ET


def cell(value):
    return html.escape(str(value)).replace("|", "&#124;").replace("\n", "<br>")


def annotation(value):
    return str(value).replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def summarize(reports, label):
    lines = [f"## Instrumented Tests — {cell(label)}", "",
             "| Report | Total | Pass | Fail | Skip |",
             "| --- | ---: | ---: | ---: | ---: |"]
    details = []
    totals = [0, 0, 0, 0]
    report_errors = []
    for report in reports:
        root = ET.parse(report).getroot()
        tests = list(root.iter("testcase"))
        failed = [t for t in tests if t.find("failure") is not None or t.find("error") is not None]
        declared_failure = any(
            int(suite.get("failures", 0)) > 0 or int(suite.get("errors", 0)) > 0
            for suite in root.iter() if suite.tag in ("testsuite", "testsuites")
        )
        if declared_failure and not failed:
            message = f"{report.name}: suite declares failures/errors without testcase failure details."
            print(f"::error::{annotation(message)}")
            report_errors.append(message)
        skipped = sum(t.find("skipped") is not None for t in tests if t not in failed)
        counts = [len(tests), len(tests) - len(failed) - skipped, len(failed), skipped]
        totals = [a + b for a, b in zip(totals, counts)]
        lines.append(f"| {cell(report.name)} | " + " | ".join(map(str, counts)) + " |")
        for test in failed:
            name = f"{test.get('classname', '')}.{test.get('name', '')}"
            failures = [e for e in test if e.tag in ("failure", "error")]
            trace = "\n\n".join(((e.text or "").strip() or e.get("message") or e.get("type") or "No failure details")
                                  for e in failures)
            print(f"::error::{annotation(label + ': ' + name + ': ' + trace.splitlines()[0])}")
            print(f"FAILED {name}\n{trace}\n", flush=True)
            details.append(f"<details><summary>{cell(name)}</summary>\n\n<pre>{html.escape(trace)}</pre>\n\n</details>")
    lines.append("| **Total** | " + " | ".join(f"**{n}**" for n in totals) + " |")
    if not reports or totals[0] == 0:
        message = "No instrumented test XML reports found, or reports contain no tests. Check emulator startup and Gradle logs."
        print(f"::error::{message}")
        lines.extend(["", message])
    lines.extend(["", *(cell(message) for message in report_errors)] if report_errors else [])
    if details:
        lines.extend(["", "### Failed tests", "", *details])
    # GitHub annotations alone do not fail a step. Treat XML failures as authoritative,
    # including assumption failures exported by AGP despite a successful Gradle exit.
    return "\n".join(lines) + "\n", bool(report_errors) or totals[2] > 0 or totals[0] == 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, required=True)
    parser.add_argument("--label", required=True)
    args = parser.parse_args()
    report, failed = summarize(sorted(args.reports.rglob("TEST-*.xml")), args.label)
    print(report)
    if summary := os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(summary, "a", encoding="utf-8") as output:
            output.write(report)
    sys.exit(1 if failed else 0)
