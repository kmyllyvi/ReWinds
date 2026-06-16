# Tooling — Slash Commands

**Last updated:** 2026-06-16 (PR d6b37f0 — "Merge branch 'agent-updates' into develop")
**Status:** Active

## Purpose

Reusable slash commands stored under `.claude/commands/` that any agent or human operator can invoke in a Claude Code session. Each file becomes a `/command-name` shortcut that executes its described steps with the current session context.

## Responsibilities

- Encapsulate repeatable multi-step workflows so they can be triggered with a single command.
- Surface the correct agent or toolchain for a task without requiring the caller to know implementation details.

## Registered commands

### `/run-ui-tests`

**File:** `.claude/commands/run-ui-tests.md`

Runs the Maestro E2E flow suite locally against a connected Android emulator or iOS simulator.

**Usage:**
- `/run-ui-tests` — runs all flows in `.maestro/`
- `/run-ui-tests j6` — runs a specific flow by name prefix (e.g. `j6_ask_ai_chat_question.yaml`)

**Steps performed:**
1. Checks that `maestro` is installed; if not, tells the user to install via `curl -fsSL https://get.maestro.mobile.dev | bash`.
2. If an argument is provided, locates the matching flow file under `.maestro/flows/` and runs only that file. Otherwise runs the full suite with `maestro test .maestro/`.
3. Reports pass/fail per flow. Shows Maestro error output on failure.

**Prerequisites:**
- Android: a running emulator with the debug APK already installed (`./gradlew :composeApp:assembleDebug`, then `adb install -r ...apk`). App ID: `com.km.rewinds`.
- iOS: a booted simulator with the app installed via Xcode.
- Flow J6 (`j6_ask_ai_chat_question.yaml`) hits the live Anthropic API — requires `MAESTRO_APP_ANTHROPIC_KEY` set in the environment.
- The command does not build the app — the caller must build and install first.

**Relationship to testing strategy:** This command is the local entry point for Layer 2 (Maestro E2E smoke suite) as defined in `docs/agent/testing/TESTING-STRATEGY.md §5.1`.

---

### `/update-docs`

**File:** `.claude/commands/update-docs.md`

Delegates a documentation update task to the **Phill (doc-agent)** agent.

**Usage:**
- `/update-docs` — updates docs based on the latest merged commit (`git log -1`)
- `/update-docs PR#42` — updates docs based on a specific PR number
- `/update-docs KIM-123` — updates docs based on a Linear ticket

**Steps performed (by Phill):**
- If a PR number is given: fetch the PR diff and linked Linear ticket, then update `/docs/agent/`.
- If a Linear ticket ID is given: read the ticket and the latest commit on its branch, then update docs.
- If no argument: identify the latest commit and update docs based on that change.

**Scope:** Phill updates `/docs/agent/` only. `CLAUDE.md` and agent files under `.claude/agents/` are not touched.

## Dependencies

- **Maestro** CLI — installed separately, not a Gradle or Xcode dependency.
- **Anthropic API key** — required only for flow J6 (`MAESTRO_APP_ANTHROPIC_KEY`).
- **Phill (doc-agent)** — must be registered in `.claude/agents/` for `/update-docs` to dispatch correctly.

## Known constraints

- Slash commands are invoked inside a Claude Code session; they cannot be run as standalone shell scripts.
- `/run-ui-tests` requires the app to be pre-built and pre-installed; it does not trigger a build.
- `/update-docs` is only as reliable as the Phill agent — if Phill's instructions are out of date, the docs update may be incomplete.
