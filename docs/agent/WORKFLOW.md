# ReWinds — Agentic Workflow

How the agent team is orchestrated. **Kimmo is the CEO and the human in the loop.** As of
2026-07-10, only genuine ambiguity requires his approval — everything else is designed to cross
automatically. This file is the single source of truth for the workflow — the individual agent
files own their _craft_, this file owns the _handover_.

**Why so few gates:** ReWinds is a concept project, not production — a broken `develop` costs
nothing. Anything that actually reaches an app store already passes through Kimmo's hands at that
separate, manual step, so gating routine dev work doesn't reduce his real risk exposure. Default to
autonomy for clear-cut tickets; escalate (`needs-human`) only when an agent hits something it
genuinely can't decide for itself.

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
| Backlog → `spec-ready`    | **po** agent                             | issue title + your intent, ARCHITECTURE-RULES, relevant code | spec + acceptance criteria + DoD in the issue body; adds `spec-ready`; **moves status to Todo itself, no approval step** | Todo |
| Todo                      | — (dev queue)                            | —                                                            | orchestrator picks it up                                              | In Progress                                         |
| In Progress               | **developer** agent                      | spec + AC + DoD, repo, ARCHITECTURE-RULES                    | branch + commits + PR; handoff comment; adds `in-review`              | In Progress + `in-review`                           |
| In Progress + `in-review` | **code-reviewer** (auto on PR open)      | the diff/PR vs AC + DoD + MV\* rules                         | pass + **merges the PR herself** (`gh pr merge`), or fail with specifics | Completed (merged) / (remove `in-review`, stays In Progress) |
| `needs-human`             | **GATE — you**                           | the blocker the agent hit                                    | a decision                                                            | back into the lane                                  |
| Completed                 | **doc-agent** (Phill — scheduled sweep)  | —                                                            | —                                                                     | —                                                   |

`code-reviewer` (Marcy) runs automatically on every PR open via GitHub Actions. `qa-test-agent` and `ux-ui-reviewer` are invoked manually when relevant.

## The human gate

- **`needs-human`.** Any agent that hits genuine ambiguity (unclear spec, architectural fork, scope
  question) adds `needs-human`, assigns you, and stops rather than guessing. You resolve it and move
  it back into the lane. This is the only remaining checkpoint — a pressure-release valve for
  compounding error, not a routine approval step.

Spec approval before code (formerly "Gate 1") was removed 2026-07-10: `po` now moves an issue
straight from `spec-ready` to **Todo** itself once the spec is written — no sign-off required.
Direction and pivot decisions on genuinely ambiguous scope are still yours; that's what
`needs-human` is for. Everything else is designed to run without you.

## Merge policy

**Manual testing no longer gates a merge.** A PR merges as soon as **both** of these hold:

1. **Code review passed** — `code-reviewer` (Marcy) approved with no Critical/Major findings.
2. **CI is green** — all required checks pass, including the coverage floor and patch-coverage gate.

The review + CI are the gate, not a human's final click. This applies **even when the change has
behaviour that wants a device/simulator run or a visual/UX eye** — that verification is decoupled from
the merge and logged instead (see below). The only thing that still holds a PR open is a `needs-human`
flag, i.e. a genuine decision the agent couldn't make — not routine verification.

**Who actually presses merge:** there is no GitHub-native auto-merge or branch protection wired up (this
repo is private on the free plan — branch protection needs GitHub Pro or a public repo, so it isn't the
backstop it might look like). Since nothing merges the PR on its own, **Marcy merges it herself**,
in-session, the moment she confirms both conditions above: `gh pr merge <n> --merge` (regular merge
commit into `develop`, matching existing history — not squash, not rebase; branch is left in place, not
auto-deleted). This happens as part of the same review pass that sets the issue to **Completed** — it is
not a separate step and does not wait for Kimmo. If CI shows red or is still pending when she checks, she
does not merge — she reports the block instead (that's a Randy problem to fix, not hers to wait out).

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

There is no branch-protection backstop today (see above) — the discipline is Marcy only ever running
`gh pr merge` after she's personally confirmed both conditions, never before. If this repo is ever upgraded to
GitHub Pro or made public, turn on branch protection with required status checks on `develop` as a second
backstop.

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
Priority and size set by po (Kimmo reranks/rescopes freely, any time — not a pre-build gate).
If scope is genuinely ambiguous, po flags `needs-human` instead of guessing.
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

- **Backlog**, no `spec-ready` → dispatch `po`. It specs the issue and moves it straight to **Todo**
  itself — no approval step in between.
- **Todo** → dispatch `developer` (it moves the issue to In Progress and works).
- **In Progress** + `in-review` → dispatch `code-reviewer`.
- **Any** + `needs-human` → it's yours (the one remaining gate).
- **Completed** → Phill (doc-agent) picks it up automatically on the next morning sweep (Tue–Sat 09:07, Claude Code cron, Pro subscription).

Two agents may run in parallel **only if they touch different issues and different files** (e.g. `po`
drafting a new ticket while `developer` codes one already in Todo).

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
agent via the Task tool (issue number + branch + PR link). Marcy reviews against AC/DoD/MV* rules. If she
passes it (no Critical/Major, CI confirmed green), she **merges the PR herself** (`gh pr merge <n>
--merge`) and sets the Linear status/labels to Completed in the same pass — see **Merge policy** above for
why this has to be an explicit in-session action rather than an assumption. If she fails it, she removes
`in-review` and comments the specifics instead; nothing merges. Randy relays her verdict verbatim. Runs
inside Claude Code (Pro subscription, no API token cost).

The GitHub Actions code-review workflow (`.github/workflows/code-review.yml`) is retained for manual
`workflow_dispatch` runs but no longer triggers on PR open. This was a **cost decision** (free-plan
Actions minutes), not a quality one — restore the `pull_request` trigger to re-enable CI review if the
budget ever allows.

### Layer 1 UI-test sweep — Claude Code scheduled task, Mon/Fri 09:07
The `android-instrumented` CI job (Layer 1 Compose semantic UI tests, `connectedDebugAndroidTest` on an
emulator) no longer runs on every push/PR — that was the real per-PR cost driver among the UI-test
layers (the Maestro E2E suite was already push-to-master/manual-only since KIM-294). The GitHub Actions
job is parked at `workflow_dispatch` only in `.github/workflows/ci.yml`, same cost-decision pattern as
`code-review.yml`/`doc-agent.yml`.

In its place, a Claude Code scheduled task (`rewinds-instrumented-ui-tests`, Mon/Fri 09:07 local) runs
`./run-full-tests.sh` against a locally-booted Android emulator — Compose instrumented tests need a
real emulator to execute, so unlike the doc-sweep/code-review moves this genuinely runs on Kimmo's
machine, not a GitHub-hosted runner, and only fires anything useful if an emulator happens to be
available at run time (it skips cleanly, not as a failure, if none is booted). On a test failure it
opens/updates a Linear ticket at **Todo** (no `needs-human`, no Kimmo assignment — this sweep bypasses
the human gate by design, per the 2026-07-10 minimize-gates decision above) and immediately dispatches
`developer` (Randy) to diagnose, fix, verify, commit, and open a PR through the normal
Randy → Marcy → merge path. Runs inside Claude Code (Pro subscription, no API token cost). Restore the
`pull_request` trigger in `ci.yml` to go back to unattended per-PR coverage if the budget ever allows.

**Persisted run artifact, even when green.** Before this, the only trace of a test run was a Linear
ticket on failure — no record existed of "this ran, it passed, here's how long it took." `run-full-tests.sh`
(repo root; also available as `/run-full-tests`) always writes a timestamped summary (suite, pass/fail,
test count, duration) to `docs/human/test-runs/latest.md`, overwriting it on every run — a glanceable
artifact without waiting for a failure, deliberately cheap: no bytecode instrumentation, no HTML, no
trend history (that heavier job stays the separate, opt-in `/coverage` command and its
`docs/coverage/history.json` trend log). The scheduled sweep commits and pushes this one file to
`develop` after each run.

### Doc sweep (Phill) — Claude Code cron, Tue–Sat 09:07
Runs inside Claude Code (Pro subscription, no API token cost). Checks Linear for tickets that moved
to Done in the last 24h, finds the corresponding merge commit on `develop`, and opens a docs PR
targeting `develop`. If nothing completed, exits cleanly. The GitHub Actions doc-agent workflow
(`.github/workflows/doc-agent.yml`) is retained for manual `workflow_dispatch` runs but no longer
triggers automatically.

### CI auto-fix (Randy) — in-session, blocking watch
After opening the PR, Randy blocks on the triggered `CI` run (`gh run watch --exit-status`). On a red
build he pulls the failing logs, **reproduces locally** (`./gradlew buildAndroidOnly` for compile/lint
failures, plus `./gradlew :composeApp:testDebugUnitTest` for test failures) so the fix is verified before
it ever goes back to GitHub, fixes, commits with an `[auto-fix]` marker, pushes, and re-watches — bounded
to **2 attempts**, then `needs-human` + Kimmo. Reproducing locally first is the point: it turns "push and
hope" into "push once, already green," which is what keeps this off the Actions-minutes budget. He does
this before the Marcy handoff (no point reviewing a red build). Runs inside Claude Code (Pro
subscription, no API token cost), but only while a session is active and only for the run Randy just
triggered — it is not ambient coverage for overnight or third-party failures. The GitHub Actions auto-fix
workflow (`.github/workflows/ci-failure.yml`) is parked at `workflow_dispatch` only (was pay-per-token on
every failure); restore its `workflow_run` trigger to re-enable unattended CI-side auto-fix.
