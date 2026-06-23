#!/usr/bin/env python3
"""
Generate a code-coverage HTML report from real JaCoCo XML output.

This parses the JaCoCo XML report (produced by the `jacocoTestReport` Gradle task) and computes
line/branch coverage from its <counter> elements — the root <report> counters give the totals,
and per-<package>/<sourcefile> counters give the file-by-file breakdown. No numbers are hardcoded.

Each successful run also appends its metrics to docs/coverage/history.json (capped at the last 50
runs) and renders inline SVG trend charts (line-coverage % and total lines over time) into the HTML
report. The charts are hand-rolled <polyline> SVG — no JS, no CDN — so the report renders offline.

Usage:
    python3 generate_coverage_metrics.py [path/to/jacocoTestReport.xml]

If no path is given it falls back to the default Gradle output location. Run the report first:
    ./gradlew :composeApp:coverageReport -PenableCoverage=true
"""

import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime

DEFAULT_XML = Path("composeApp/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
REPORT_DIR = Path("docs/coverage")
HISTORY_FILE = REPORT_DIR / "history.json"

# Cap the on-disk history so the file (and the rendered chart) stay readable over time. 50 runs is
# plenty to spot a trend without the SVG turning into an unreadable thicket of points; older entries
# are dropped oldest-first. Bump this if you ever want a longer window.
HISTORY_LIMIT = 50


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


def update_history(line_covered, line_total, line_pct, branch_pct):
    """Append this run's metrics to docs/coverage/history.json and return the full (capped) history.

    The file is a JSON array of entries; we read what's there, append one, then keep only the most
    recent HISTORY_LIMIT entries. A corrupt/empty file is treated as no history rather than crashing
    the report.
    """
    history = []
    if HISTORY_FILE.exists():
        try:
            loaded = json.loads(HISTORY_FILE.read_text())
            if isinstance(loaded, list):
                history = loaded
        except (json.JSONDecodeError, ValueError):
            print(f"⚠ {HISTORY_FILE} was unreadable — starting a fresh history.")

    history.append({
        "timestamp": datetime.now().isoformat(timespec="seconds"),
        "total_lines": line_total,
        "covered_lines": line_covered,
        "line_coverage_pct": round(line_pct, 2),
        "branch_coverage_pct": round(branch_pct, 2),
    })
    history = history[-HISTORY_LIMIT:]

    HISTORY_FILE.write_text(json.dumps(history, indent=2))
    return history


def _polyline_points(values, width, height, pad, vmin, vmax):
    """Map a series of values to SVG coordinate points within the padded plot area."""
    n = len(values)
    span = (vmax - vmin) or 1  # avoid divide-by-zero when all values are equal
    plot_w = width - 2 * pad
    plot_h = height - 2 * pad
    points = []
    for i, v in enumerate(values):
        x = pad + (plot_w * i / (n - 1) if n > 1 else plot_w / 2)
        y = pad + plot_h * (1 - (v - vmin) / span)
        points.append((x, y))
    return points


def _line_chart_svg(title, values, color, unit, value_fmt):
    """Hand-rolled SVG <polyline> chart for a single metric series — no JS, no external assets."""
    width, height, pad = 720, 220, 40
    if not values:
        return f'<p style="color:#999;">No history yet for {title}.</p>'

    vmin, vmax = min(values), max(values)
    # Pad the value range a touch so a flat or near-flat line isn't pinned to the axis edges.
    if vmin == vmax:
        vmin, vmax = vmin - 1, vmax + 1
    else:
        margin = (vmax - vmin) * 0.1
        vmin, vmax = vmin - margin, vmax + margin

    points = _polyline_points(values, width, height, pad, vmin, vmax)
    polyline = " ".join(f"{x:.1f},{y:.1f}" for x, y in points)
    dots = "".join(
        f'<circle cx="{x:.1f}" cy="{y:.1f}" r="3" fill="{color}" />' for x, y in points
    )

    # Three horizontal gridlines + axis labels (top, mid, bottom of the value range).
    gridlines = ""
    for frac in (0.0, 0.5, 1.0):
        y = pad + (height - 2 * pad) * frac
        label_val = vmax - (vmax - vmin) * frac
        gridlines += (
            f'<line x1="{pad}" y1="{y:.1f}" x2="{width - pad}" y2="{y:.1f}" '
            f'stroke="#eee" stroke-width="1" />'
            f'<text x="{pad - 6:.1f}" y="{y + 4:.1f}" text-anchor="end" '
            f'font-size="10" fill="#999">{value_fmt(label_val)}{unit}</text>'
        )

    first_val, last_val = values[0], values[-1]
    delta = last_val - first_val
    arrow = "▲" if delta > 0 else ("▼" if delta < 0 else "▬")
    delta_color = "#10b981" if delta > 0 else ("#ef4444" if delta < 0 else "#999")
    delta_label = (
        f'{arrow} {value_fmt(abs(delta))}{unit} since first run'
        if len(values) > 1 else "single run"
    )

    return f"""
    <div class="chart-block">
        <div class="chart-head">
            <h3>{title}</h3>
            <span style="color:{delta_color}; font-weight:600;">{delta_label}</span>
        </div>
        <svg viewBox="0 0 {width} {height}" width="100%" role="img" aria-label="{title} trend">
            {gridlines}
            <polyline points="{polyline}" fill="none" stroke="{color}" stroke-width="2" />
            {dots}
            <text x="{points[-1][0]:.1f}" y="{points[-1][1] - 8:.1f}" text-anchor="end"
                  font-size="11" font-weight="600" fill="{color}">{value_fmt(last_val)}{unit}</text>
        </svg>
    </div>"""


def build_history_section(history):
    """Render the two trend charts (line-coverage % and total lines) from the history entries."""
    line_pcts = [h.get("line_coverage_pct", 0) for h in history]
    total_lines = [h.get("total_lines", 0) for h in history]

    coverage_chart = _line_chart_svg(
        "Line Coverage Trend", line_pcts, "#667eea", "%", lambda v: f"{v:.1f}"
    )
    lines_chart = _line_chart_svg(
        "Total Lines Trend", total_lines, "#764ba2", "", lambda v: f"{v:,.0f}"
    )

    return f"""
        <div class="charts">
            <h2>📉 Coverage Over Time ({len(history)} run{'s' if len(history) != 1 else ''})</h2>
            {coverage_chart}
            {lines_chart}
        </div>"""


def build_html(line_covered, line_total, branch_covered, branch_total, files, history):
    line_pct = (line_covered / line_total * 100) if line_total else 0
    branch_pct = (branch_covered / branch_total * 100) if branch_total else 0
    overall_grade, _ = grade_for(line_pct)
    history_section = build_history_section(history)

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
        .charts {{
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            padding: 30px;
            margin-bottom: 30px;
        }}
        .charts h2 {{ color: #333; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 2px solid #667eea; }}
        .chart-block {{ margin-bottom: 30px; }}
        .chart-block:last-child {{ margin-bottom: 0; }}
        .chart-head {{ display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px; }}
        .chart-head h3 {{ color: #333; font-size: 1.05em; }}
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

        {history_section}

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
    history = update_history(line_covered, line_total, line_pct, branch_pct)
    html = build_html(line_covered, line_total, branch_covered, branch_total, files, history)
    report_file = REPORT_DIR / "detailed.html"
    report_file.write_text(html)

    print("\n✅ Coverage report generated from real JaCoCo data!")
    print(f"   Line Coverage:   {line_pct:.1f}% ({line_covered}/{line_total})")
    print(f"   Branch Coverage: {branch_pct:.1f}% ({branch_covered}/{branch_total})")
    print(f"   Files reported:  {len(files)}")
    print(f"   History entries: {len(history)} (in {HISTORY_FILE})")
    print(f"\n📊 Open report: open {report_file}")


if __name__ == "__main__":
    main()
