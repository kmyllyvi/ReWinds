#!/usr/bin/env bash
set -euo pipefail

# Run from the script's own directory so it works regardless of the caller's
# working directory or how Finder invokes it on double-click.
cd "$(dirname "${BASH_SOURCE[0]}")"

./gradlew coverageReport -PenableCoverage=true

echo
echo "Coverage report generated:"
echo "  docs/coverage/detailed.html   (HTML report with inline trend charts)"
echo "  docs/coverage/history.json    (trend data, last 50 runs)"
