# Test run — (not yet run)

This file is overwritten by `run-full-tests.sh` on every run — a glance-at-it artifact showing what
ran, pass/fail, test count, and duration, even when everything is green. It's the lightweight
counterpart to the coverage report (`docs/coverage/`), which is heavier (bytecode instrumentation,
opt-in) and tracks trend history instead of just "what happened last time."

Run `./run-full-tests.sh` (or `/run-full-tests`) to generate the first real entry.
