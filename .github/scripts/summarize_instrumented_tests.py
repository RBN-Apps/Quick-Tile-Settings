"""Publish every connected-test XML result, including method names and failure traces."""
import argparse
import html
import os
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
    for report in reports:
        root = ET.parse(report).getroot()
        tests = list(root.iter("testcase"))
        failed = [t for t in tests if t.find("failure") is not None or t.find("error") is not None]
        skipped = sum(t.find("skipped") is not None for t in tests)
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
    if not reports:
        message = "No instrumented test XML reports found. Check emulator startup and Gradle logs."
        print(f"::warning::{message}")
        lines.extend(["", message])
    if details:
        lines.extend(["", "### Failed tests", "", *details])
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, required=True)
    parser.add_argument("--label", required=True)
    args = parser.parse_args()
    report = summarize(sorted(args.reports.rglob("TEST-*.xml")), args.label)
    print(report)
    if summary := os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(summary, "a", encoding="utf-8") as output:
            output.write(report)
