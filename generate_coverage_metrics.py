#!/usr/bin/env python3
"""
Generate a code-coverage HTML report from real JaCoCo XML output.

This parses the JaCoCo XML report (produced by the `jacocoTestReport` Gradle task) and computes
line/branch coverage from its <counter> elements — the root <report> counters give the totals,
and per-<package>/<sourcefile> counters give the file-by-file breakdown. No numbers are hardcoded.

Usage:
    python3 generate_coverage_metrics.py [path/to/jacocoTestReport.xml]

If no path is given it falls back to the default Gradle output location. Run the report first:
    ./gradlew :composeApp:coverageReport -PenableCoverage=true
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime

DEFAULT_XML = Path("composeApp/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
REPORT_DIR = Path("docs/coverage")


def counter(element, counter_type):
    """Return (covered, total) for a JaCoCo counter type on the given element, or (0, 0)."""
    for c in element.findall("counter"):
        if c.get("type") == counter_type:
            missed = int(c.get("missed", "0"))
            covered = int(c.get("covered", "0"))
            return covered, missed + covered
    return 0, 0


def grade_for(pct):
    if pct >= 90:
        return "A", "coverage-good"
    if pct >= 80:
        return "B", "coverage-ok"
    return "C", "coverage-low"


def collect_files(root):
    """Yield (display_name, line_total, line_covered, line_pct) per source file."""
    files = []
    for package in root.findall("package"):
        pkg_name = package.get("name", "")
        for sourcefile in package.findall("sourcefile"):
            covered, total = counter(sourcefile, "LINE")
            if total == 0:
                continue
            name = sourcefile.get("name", "")
            display = f"{pkg_name}/{name}" if pkg_name else name
            pct = covered / total * 100
            files.append((display, total, covered, pct))
    files.sort(key=lambda f: f[0])
    return files


def build_html(line_covered, line_total, branch_covered, branch_total, files):
    line_pct = (line_covered / line_total * 100) if line_total else 0
    branch_pct = (branch_covered / branch_total * 100) if branch_total else 0
    overall_grade, _ = grade_for(line_pct)

    rows = ""
    for display, total, covered, pct in files:
        grade, color_class = grade_for(pct)
        rows += f"""
            <div class="module-row">
                <div class="module-name">{display}</div>
                <div class="coverage-pct">{total}</div>
                <div class="coverage-pct {color_class}">{pct:.0f}%</div>
                <div class="coverage-pct {color_class}">{grade}</div>
            </div>"""

    return f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ReWinds - Code Coverage Report</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto;
            background: #f5f7fa;
            padding: 20px;
        }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            border-radius: 12px;
            margin-bottom: 30px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        }}
        .header h1 {{ font-size: 2.2em; margin-bottom: 10px; }}
        .metrics-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }}
        .metric-card {{
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            text-align: center;
            border-top: 4px solid #667eea;
        }}
        .metric-label {{
            color: #666;
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 10px;
        }}
        .metric-value {{ font-size: 2.5em; font-weight: bold; color: #667eea; }}
        .metric-bar {{
            background: #f0f0f0;
            height: 8px;
            border-radius: 4px;
            margin-top: 12px;
            overflow: hidden;
        }}
        .metric-bar-fill {{
            height: 100%;
            background: linear-gradient(90deg, #667eea, #764ba2);
            border-radius: 4px;
        }}
        .modules {{
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            padding: 30px;
            margin-bottom: 30px;
        }}
        .modules h2 {{ color: #333; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 2px solid #667eea; }}
        .module-row {{
            display: grid;
            grid-template-columns: 1fr 100px 100px 100px;
            gap: 20px;
            padding: 15px;
            border-bottom: 1px solid #f0f0f0;
            align-items: center;
        }}
        .module-row:last-child {{ border-bottom: none; }}
        .module-row:hover {{ background: #f9f9f9; }}
        .module-name {{ font-weight: 600; color: #333; }}
        .coverage-pct {{ text-align: center; font-weight: bold; font-size: 1.1em; }}
        .coverage-good {{ color: #10b981; }}
        .coverage-ok {{ color: #f59e0b; }}
        .coverage-low {{ color: #ef4444; }}
        .footer {{ text-align: center; color: #999; font-size: 0.85em; margin-top: 40px; }}
        .summary-box {{
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            margin-bottom: 20px;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 ReWinds Code Coverage Report</h1>
            <p>Line and Branch Coverage (parsed from JaCoCo XML)</p>
        </div>

        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-label">Line Coverage</div>
                <div class="metric-value">{line_pct:.1f}%</div>
                <div class="metric-bar">
                    <div class="metric-bar-fill" style="width: {line_pct}%"></div>
                </div>
                <small>{line_covered}/{line_total} lines</small>
            </div>

            <div class="metric-card">
                <div class="metric-label">Branch Coverage</div>
                <div class="metric-value">{branch_pct:.1f}%</div>
                <div class="metric-bar">
                    <div class="metric-bar-fill" style="width: {branch_pct}%"></div>
                </div>
                <small>{branch_covered}/{branch_total} branches</small>
            </div>

            <div class="metric-card">
                <div class="metric-label">Overall Grade</div>
                <div class="metric-value" style="color: #10b981;">{overall_grade}</div>
                <div class="metric-bar">
                    <div class="metric-bar-fill" style="width: {line_pct}%"></div>
                </div>
                <small>Line-coverage grade</small>
            </div>
        </div>

        <div class="summary-box">
            <h3>📈 Coverage Summary</h3>
            <ul style="margin-left: 20px; line-height: 1.8;">
                <li><strong>Total Lines:</strong> {line_total:,}</li>
                <li><strong>Lines Covered:</strong> {line_covered:,}</li>
                <li><strong>Total Branches:</strong> {branch_total:,}</li>
                <li><strong>Branches Covered:</strong> {branch_covered:,}</li>
            </ul>
        </div>

        <div class="modules">
            <h2>📁 File-by-File Coverage</h2>
            <div class="module-row" style="font-weight: 600; background: #f9f9f9;">
                <div>File</div>
                <div>Lines</div>
                <div>Coverage</div>
                <div>Grade</div>
            </div>{rows}
        </div>

        <div class="footer">
            <p>Generated {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} from JaCoCo XML</p>
        </div>
    </div>
</body>
</html>
"""


def main():
    xml_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_XML

    if not xml_path.exists():
        print(f"⚠ JaCoCo XML report not found at: {xml_path}")
        print("   Generate it first:")
        print("     ./gradlew :composeApp:coverageReport -PenableCoverage=true")
        sys.exit(1)

    print(f"\n📊 Parsing JaCoCo report: {xml_path}")
    # JaCoCo XML references a DTD; disable external entity resolution for safety + offline use.
    parser = ET.XMLParser()
    tree = ET.parse(xml_path, parser=parser)
    root = tree.getroot()

    line_covered, line_total = counter(root, "LINE")
    branch_covered, branch_total = counter(root, "BRANCH")
    files = collect_files(root)

    line_pct = (line_covered / line_total * 100) if line_total else 0
    branch_pct = (branch_covered / branch_total * 100) if branch_total else 0

    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    html = build_html(line_covered, line_total, branch_covered, branch_total, files)
    report_file = REPORT_DIR / "detailed.html"
    report_file.write_text(html)

    print("\n✅ Coverage report generated from real JaCoCo data!")
    print(f"   Line Coverage:   {line_pct:.1f}% ({line_covered}/{line_total})")
    print(f"   Branch Coverage: {branch_pct:.1f}% ({branch_covered}/{branch_total})")
    print(f"   Files reported:  {len(files)}")
    print(f"\n📊 Open report: open {report_file}")


if __name__ == "__main__":
    main()
