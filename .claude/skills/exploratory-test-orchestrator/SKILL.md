---
name: exploratory-test-orchestrator
description: |
  Surveys recently-shipped commits on develop, derives a testable claim from each ("fixes X",
  "adds Y"), verifies the claim against an Android emulator (booting one headlessly if none is
  already running, and shutting down only the instance it started), and files a Linear ticket
  (with Randy dispatched on it) for anything that doesn't hold up. Use when asked to run
  exploratory testing, verify that recent commits actually work, check what shipped isn't broken,
  or invoked directly as "/exploratory-test-orchestrator". Adapted from solidmaint's
  exploratory-test-orchestrator skill (see KIM-331) for a mobile app with no staging server and no
  Slack — verification means driving a local emulator, output goes to Linear + a run-artifact file.
allowed-tools:
  - Read
  - Grep
  - Glob
  - Write
  - Agent
  - Bash(git log*)
  - Bash(git show*)
  - Bash(adb *)
  - Bash(emulator *)
  - Bash(maestro test*)
  - Bash(./gradlew :composeApp:connectedDebugAndroidTest*)
  - Bash(git add*)
  - Bash(git commit*)
  - Bash(git push*)
  - mcp__linear-server__list_issues
  - mcp__linear-server__get_issue
  - mcp__linear-server__save_issue
  - mcp__linear-server__list_issue_labels
---

# exploratory-test-orchestrator

Change-focused *verification* — not the Mon/Fri `rewinds-instrumented-ui-tests` sweep (which runs
the fixed regression suite). This derives a claim from what actually shipped in the last 2 days and
checks that specific claim against a locally-booted Android emulator, biased toward `can't-tell`
over false-positive `broken` findings.

**Read `system-prompt.md` in this directory for the full 7-step workflow** — survey, derive claim,
verify, judge, dedup, file, summarize. Do not re-derive the process from this file alone; the
frontmatter above is only the trigger description and the actually-enforced tool allowlist.

## At a glance

1. Survey 2 days of commits on `develop` (once — never re-run mid-session).
2. Derive a testable claim per user-facing change (max 4/run).
3. Ensure a device: reuse one already running, or boot one headlessly (~150s budget) if none is —
   this Skill has no CI backstop like the Mon/Fri sweep, so it manages emulator lifecycle itself
   rather than skipping. Reuse an existing Maestro/instrumented test where one covers the claim.
   Tear down only the instance it booted, always, even on a failed/partial run.
4. Judge `works` / `broken` / `can't-tell` (bias toward `can't-tell`).
5. Dedup against Linear (open + completed, keyed on commit SHA) before filing.
6. File confirmed-`broken` claims to Linear at **Todo** (cap 5/run, no `needs-human`), dispatch
   Randy on each directly.
7. Always overwrite `docs/human/test-runs/exploratory-latest.md`, even a clean run.

## Companion files (reference only — not parsed by Claude Code)

- `system-prompt.md` — the full workflow this skill follows.
- `manifest.json` — identity/metadata, mirroring solidmaint's shape for portability.
- `mcp-requirements.json` — which MCP servers this depends on.
- `allowed-tools.json` — human-readable mirror of the frontmatter `allowed-tools` above. **The
  frontmatter is the one Claude Code actually enforces** — if you change the restriction, change it
  there first and keep this file in sync, not the other way around.

## Scope (v1)

Android emulator only — no iOS/simulator verification. No Slack, no GitHub issues — Linear only.
Not yet on a schedule; invoked manually until a cadence is decided (see KIM-331).
