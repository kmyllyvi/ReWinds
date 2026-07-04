# ReWinds Agent Documentation Hub

Entry point for any session working on ReWinds. Read this, then the two files it points to.

## Session init (per ../CLAUDE.md)

On a new session, read in order:

1. `ARCHITECTURE-RULES.md` — the MV* patterns and platform constraints all code must respect.
2. `WORKFLOW.md` — how tracked work flows: the Linear lane (Backlog → Planned → In Progress →
   Completed), the labels (`spec-ready`, `in-review`, `needs-human`), the two human gates, and the
   per-agent handover contract.
3. `../CLAUDE.md` — build commands, config, current status.

Then ask Kimmo the goal for the session.

## How work flows

Tracked work runs through the agentic lane in `WORKFLOW.md` — status = whose turn, with Kimmo as the
human at the two gates (Gate 1 approves a spec before code is written; Gate 2 is the `needs-human`
escape hatch). The agents (`po`, `developer`, `code-reviewer`, plus manual `qa-test-agent` /
`ux-ui-reviewer`) live in `../.claude/agents/`. Ad-hoc one-off tasks can still be done directly without
the lane.

## Build commands (summary — full detail in ../CLAUDE.md)

```bash
# Android (the pilot build/test target)
./gradlew buildAndroidOnly
./gradlew :composeApp:testDebugUnitTest

# iOS — use Xcode, NOT Gradle. The Gradle iOS tasks are unreliable (see CLAUDE.md).
open ../../iosApp/iosApp.xcworkspace   # then cmd+R
```

iOS device builds have known OOM constraints — see `iOS_BUILD_GUIDE.md`.

## Key files

| File | Purpose |
|------|---------|
| `../CLAUDE.md` | Build commands, config, current status |
| `ARCHITECTURE-RULES.md` | MV* patterns and architectural conventions code must follow |
| `WORKFLOW.md` | The agentic lane, labels, gates, and handover contract |
| `iOS_BUILD_GUIDE.md` | iOS build detail and OOM constraints |
| `SESSION-INTERRUPTIONS.md` | What to do when a session is cut off by a credit limit (no auto-sweep is possible) |
| `agent instruction - doc.txt` | Post-merge documentation agent |
| `old dev logs/` | Historical dated session logs (archive) |
| `plans/` | Feature plans (`plans/DONE/` = completed) |
