# ReWinds — Agentic Workflow

How the agent team is orchestrated. **Kimmo is the CEO and the human in the loop.** Two points
require his approval; nothing crosses them automatically. This file is the single source of truth for
the workflow — the individual agent files own their _craft_, this file owns the _handover_.

Reusable machinery (this workflow, the gates) is mirrored in
`Dropbox/Agentic Development/Agentic Team Setup/`. Project-specific facts live in the codebase and
`docs/agent/ARCHITECTURE-RULES.md`.

> **Keep in sync:** `docs/human/sections/workflow.html` is the human-readable rendering of this file
> (pipeline diagram, roster cards, automation table). Whenever this file changes in a way that affects
> the lane, gates, roster, or automation state, update that HTML page in the same change.
>
> **Manual Test Checklist:** the async QA backlog lives at `docs/human/sections/test-checklist.html`
> (see Merge policy). It is a working document, not generated from this file — the developer appends
> rows to it directly.

---

## State model: native status + labels

The Linear workspace is on the free plan, so we **cannot create custom workflow statuses**. We use the
existing native statuses for the coarse lane position and **labels** for the agent sub-states. Labels
are free to create.

| Logical stage | How it's represented in Linear                  |
| ------------- | ----------------------------------------------- |
| Spec          | **Backlog** + label `spec-ready` (once specced) |
| Ready for Dev | **Todo** (native)                               |
| In Progress   | **In Progress** (native)                        |
| In Review     | **In Progress** + label `in-review`             |
| Needs Human   | label `needs-human` + assigned to Kimmo         |
| Completed     | **Completed** (native)                          |

Labels to create (one-time, in Linear): `spec-ready`, `in-review`, `needs-human`.

## Principle: position = whose turn it is

An orchestrator (you in a session, or later a scheduler) reads each issue's status + labels and
dispatches the matching agent. There is **no autonomous polling** — automation is deferred until the
manual loop is proven (see GitHub Action note at the end).

## Build lane

| Stage (status + label)    | Whose turn                               | Reads                                                        | Produces                                                              | Then moves to                                       |
| ------------------------- | ---------------------------------------- | ------------------------------------------------------------ | --------------------------------------------------------------------- | --------------------------------------------------- |
| Backlog (no label)        | — (you triage)                           | —                                                            | you point `po` at chosen items                                        | po works it                                         |
| Backlog → `spec-ready`    | **po** agent                             | issue title + your intent, ARCHITECTURE-RULES, relevant code | spec + acceptance criteria + DoD in the issue body; adds `spec-ready` | (stays Backlog — awaits Gate 1)                     |
| **GATE 1 — you**          | you                                      | the spec                                                     | approval                                                              | **Planned**                                         |
| Todo                      | — (dev queue)                            | —                                                            | orchestrator picks it up                                              | In Progress                                         |
| In Progress               | **developer** agent                      | spec + AC + DoD, repo, ARCHITECTURE-RULES                    | branch + commits + PR; handoff comment; adds `in-review`              | In Progress + `in-review`                           |
| In Progress + `in-review` | **code-reviewer** (auto on PR open)      | the diff/PR vs AC + DoD + MV\* rules                         | pass, or fail with specifics                                          | Completed / (remove `in-review`, stays In Progress) |
| `needs-human`             | **GATE 2 — you**                         | the blocker the agent hit                                    | a decision                                                            | back into the lane                                  |
| Completed                 | **doc-agent** (Phill — scheduled sweep)  | —                                                            | —                                                                     | —                                                   |

`code-reviewer` (Marcy) runs automatically on every PR open via GitHub Actions. `qa-test-agent` and `ux-ui-reviewer` are invoked manually when relevant.

## The two human gates

- **Gate 1 —** `spec-ready` **(in Backlog) → Todo.** You approve what gets built _before any code is
  written_. The `po` agent never moves an issue to Planned; it only adds `spec-ready`.
- **Gate 2 — `needs-human`.** Any agent that hits genuine ambiguity (unclear spec, architectural fork,
  scope question) adds `needs-human`, assigns you, and stops rather than guessing. You resolve it and
  move it back into the lane. This is the pressure-release valve that stops compounding error.

Direction and pivot decisions are yours. Not delegated.

## Merge policy

**Manual testing no longer gates a merge.** A PR merges as soon as **both** of these hold:

1. **Code review passed** — `code-reviewer` (Marcy) approved with no Critical/Major findings.
2. **CI is green** — all required checks pass, including the coverage floor and patch-coverage gate.

The review + CI are the gate, not a human's final click. This applies **even when the change has
behaviour that wants a device/simulator run or a visual/UX eye** — that verification is decoupled from
the merge and logged instead (see below). The only thing that still holds a PR open is a `needs-human`
flag (Gate 2), i.e. a genuine decision the agent couldn't make — not routine verification.

**Manual verification is async, via a logged checklist.** When a change has behaviour a reviewer agent
can't verify itself (real device/simulator interaction, visual/UX correctness, exploratory feel), the
developer appends a row to the **Manual Test Checklist**
(`docs/human/sections/test-checklist.html`) describing the case: steps → expected, and whether it
should become an automated test. Kimmo verifies those cases in batches on device, on his own schedule,
and ticks them off (git-tracked). A failed manual case becomes a new bug ticket — it does not block the
already-merged PR.

`qa-test-agent` (Seppo) and `ux-ui-reviewer` (Mr.T) are still dispatched manually when a change warrants
a deeper pass, but they too are off the merge path — their findings become tickets or checklist rows,
not merge blockers.

Branch protection on `develop` (required status checks) is the backstop — even auto-merge cannot land a
red PR once it is enabled.

---

## Linear handover protocol (every agent follows this)

1. **On dispatch**, read the target issue: its status, labels, body (spec/AC/DoD), and comments.
   Confirm it matches your role (table above). If it doesn't, stop and say so — don't act out of turn.
2. **Do your craft** per your own agent file.
3. **On completion**, post a comment summarising what you did, then **update status/labels** to the
   "Then moves to" value above.
4. **If blocked** (ambiguity, architectural decision, missing context): comment the specific blocker,
   add `needs-human`, assign Kimmo, and stop. Do not guess.

Linear tools: `mcp__linear-server__get_issue`, `list_issues`, `save_issue`. Team **KIM**, project
**ReWinds**. (`save_issue` sets status, labels, and assignee.)

### Spec artifact shape (po agent writes this into the issue body)

```
## Spec
<2–5 sentences: what we're building and why>

## Acceptance criteria
- [ ] <observable, testable, one behaviour per line — code-reviewer ticks these>
- [ ] <if this ticket is exempt from the standard "new tests required" DoD item, say so here with
      the reason, e.g. "No new tests required — pure annotation of existing Composables, no new
      logic">

## Definition of done
- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] Passes the CI coverage gates: the enforced global floor (`jacocoTestCoverageVerification`) and the patch-coverage gate (`diff-cover` ≥ 70% on new/changed lines)

## Notes
Priority set by po (Kimmo reranks freely); size proposed — Kimmo decides scope at Gate 1.
Reviewers needed: code-reviewer [+ qa-test-agent if logic-heavy] [+ ux-ui-reviewer if UI]
```

The six DoD bullets above are **generic and standard for every ticket** — po should not rewrite or
restate them per ticket (they're project-wide policy, not per-issue content). Anything
ticket-specific (including exemptions to the "new tests required" rule) belongs in
**Acceptance criteria**, not DoD.

### Dev → Review handover convention

- Branch: `kimmomyllyviita/kim-<issue-number>-<short-slug>`.
- Build/test for the pilot is **Android only** (`buildAndroidOnly`, `testDebugUnitTest`). iOS is
  verified manually in Xcode — the developer agent does not attempt Gradle iOS builds.
- Open a PR using `.github/pull_request_template.md`, with `Closes KIM-<n>` in the Linear section.
- The developer's closing comment must contain: `Branch: …`, `PR: <link>`, `Build: pass/fail`,
  `Tests: pass/fail`, `New tests written: <yes — list files> | <no — reason>`, `Summary`, `Deviations`. Then add the `in-review` label.
- **Manual test cases:** if the change has behaviour that needs an on-device / visual / UX check the
  reviewer can't perform, the developer appends a row per case to
  `docs/human/sections/test-checklist.html` (steps → expected, and an "Automate?" recommendation) in
  the same PR. This does **not** hold the merge — it's the async QA backlog. If there's nothing manual
  to verify (pure logic/config/docs, fully unit-tested), no row is needed; note `Manual test cases: none`
  in the closing comment.

---

## Orchestrator dispatch (manual, for the pilot)

To advance the board, read the issues and act:

- **Backlog**, no `spec-ready` → dispatch `po`.
- **Backlog** + `spec-ready` → it's at Gate 1; **you** review and, if approved, move to **Planned**.
- **Planned** → dispatch `developer` (it moves the issue to In Progress and works).
- **In Progress** + `in-review` → dispatch `code-reviewer`.
- **Any** + `needs-human` → it's yours (Gate 2).
- **Completed** → Phill (doc-agent) picks it up automatically on the next morning sweep (Tue–Sat 09:07, Claude Code cron, Pro subscription).

Two agents may run in parallel **only if they touch different issues and different files** (e.g. `po`
drafting a new ticket while `developer` codes an approved one). New po tickets stay in Backlog and wait
at Gate 1 — parallelism never bypasses a gate.

## Roster

| Agent                            | Persona  | Role in lane           | Model  |
| -------------------------------- | -------- | ---------------------- | ------ |
| `po`                             | Shirley  | Spec                   | sonnet |
| `developer`                      | Randy    | In Progress            | opus   |
| `code-reviewer`                  | Marcy    | In Review (pilot)      | opus   |
| `qa-test-agent`                  | Seppo    | In Review (manual)     | opus   |
| `ux-ui-reviewer`                 | Mr.T     | In Review (manual, UI) | sonnet |
| `codebase-architect`             | Armin    | advisory, off-lane     | sonnet |
| `doc-agent`                      | Phill    | post-merge docs        | sonnet |

## One-time setup (Kimmo, in the Linear UI)

Free plan blocks custom workflow statuses, so **no status changes are needed**. Just create three
labels under Team KIM: `spec-ready`, `in-review`, `needs-human`. Optionally `agent:po`, `agent:dev`,
`agent:review` if you want to see which agent last touched an issue.

---

## Automation in place

### Code review (Marcy) — in-session Randy→Marcy handover
Trigger: when Randy opens the PR and adds `in-review`, his final step is to invoke the `code-reviewer`
agent via the Task tool (issue number + branch + PR link). Marcy reviews against AC/DoD/MV* rules,
updates the Linear labels/status herself (Linear MCP is available locally), and Randy relays her verdict
verbatim. Runs inside Claude Code (Pro subscription, no API token cost). On merge, see the **Merge
policy** above — review-approved, CI-green PRs merge without waiting for a human; any manual verification
is logged to the Test Checklist rather than gating the merge.

The GitHub Actions code-review workflow (`.github/workflows/code-review.yml`) is retained for manual
`workflow_dispatch` runs but no longer triggers on PR open. This was a **cost decision** (free-plan
Actions minutes), not a quality one — restore the `pull_request` trigger to re-enable CI review if the
budget ever allows.

### Doc sweep (Phill) — Claude Code cron, Tue–Sat 09:07
Runs inside Claude Code (Pro subscription, no API token cost). Checks Linear for tickets that moved
to Done in the last 24h, finds the corresponding merge commit on `develop`, and opens a docs PR
targeting `develop`. If nothing completed, exits cleanly. The GitHub Actions doc-agent workflow
(`.github/workflows/doc-agent.yml`) is retained for manual `workflow_dispatch` runs but no longer
triggers automatically.

### CI auto-fix (Randy) — in-session, blocking watch
After opening the PR, Randy blocks on the triggered `CI` run (`gh run watch --exit-status`). On a red
build he pulls the failing logs, fixes, commits with an `[auto-fix]` marker, pushes, and re-watches —
bounded to **2 attempts**, then `needs-human` + Kimmo. He does this before the Marcy handoff (no point
reviewing a red build). Runs inside Claude Code (Pro subscription, no API token cost), but only while a
session is active and only for the run Randy just triggered — it is not ambient coverage for overnight or
third-party failures. The GitHub Actions auto-fix workflow (`.github/workflows/ci-failure.yml`) is parked
at `workflow_dispatch` only (was pay-per-token on every failure); restore its `workflow_run` trigger to
re-enable unattended CI-side auto-fix.
