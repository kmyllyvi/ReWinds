# Agentic exploratory testing runbook (Layer 3)

**Layer 3** of the [UI testing strategy](./TESTING-STRATEGY.md#layered-ui-testing-strategy)
(EPIC KIM-289). This is the direct answer to **KIM-288**'s question — _"can AI
just click around the app like a human without scripting every journey, does it
work in a native env, and what does it cost?"_

A periodic, **human-triggered** session where a computer-use / vision agent
(Claude in Chrome, or a computer-use agent driving an Android emulator / iOS
simulator) exercises the **running** ReWinds app _without a scripted journey_, to
surface issues the deterministic suites miss:

- **Layer 1 — KIM-293** Compose semantic UI tests (Android, every PR; in
  `composeApp/src/androidUnitTest/`).
- **Layer 2 — KIM-294** Maestro E2E smoke suite (Android + iOS, manual / on
  release to master; in `.maestro/flows/`).

Layers 1 and 2 prove that **journeys we already know about** still work. Layer 3
is for finding the journeys — and the broken states — **we haven't thought of
yet**: weird input, unusual navigation order, rotation, empty/error states,
content overflow, and visual glitches that an assertion-based test never looks at.

> **This process is NOT CI-gating.** It never blocks a PR or a release. It runs
> manually, on a cadence, and its only output is _notes + new tickets_. This is
> the key distinction from Layer 1 (per-PR gate) and Layer 2 (release-branch
> gate). See [Non-CI-gating](#non-ci-gating) below.

---

## What "agent" means here (scope)

For this runbook, **"agent" = a human-triggered Claude session** using
computer-use / Claude-in-Chrome tooling against a **locally running**
emulator or simulator. Kimmo starts the session, watches it, and curates the
findings.

It is **not** an automated, scheduled, headless service. Building a standing
agentic-testing runner would be a separate, larger ticket — scoped only _after_
this manual process has proven itself useful. Keep that boundary: the value of
Layer 3 is cheap, occasional, human-in-the-loop exploration, not another piece
of always-on infrastructure to maintain.

---

## How to run a session

### 1. Launch the app

**Android emulator (primary — cheapest, fastest to drive):**

```bash
# Start an emulator (any recent Pixel AVD), then build + install the debug app
./gradlew :composeApp:installDebug -PincludeAllTargets=false
```

The emulator window is a normal macOS window, so a desktop computer-use agent
can screenshot and click it directly.

**iOS simulator (secondary — run when a finding looks platform-specific):**

```bash
# Build + install via Xcode (Cmd+R once on iosApp.xcworkspace), or headless:
xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp \
  -configuration Debug -sdk iphonesimulator -derivedDataPath build
xcrun simctl install booted \
  build/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch booted "$(xcrun simctl listapps booted \
  | grep -i rewinds | head -1 | sed -E 's/.*"([^"]*rewinds[^"]*)".*/\1/')"
```

> iOS is built via **Xcode / `iosApp.xcworkspace`**, never Gradle iOS tasks (see
> `CLAUDE.md`). The agent only drives the already-installed app.

You do **not** need a fully clean device. But note the starting state (fresh
install vs. existing places vs. configured keys) in your notes — it changes what
the agent can reach.

### 2. Provision preconditions

To explore deep screens the agent needs some app state. Mirror Layer 2's
preconditions (see `.maestro/README.md` → _Test data / keys_):

- **Visual Crossing key** configured (Settings) so place search resolves and
  weather data downloads.
- **Anthropic key** configured if you want the agent to exercise AI chat end to
  end (this hits the **live** API — see [Cost](#rough-costtime-budget)).
- At least one saved place with a **FULL** month and one place with a
  **NO_DATA** month, to reach statistics and the download dialog.
- Device **locale = English** (some screens are English-only today).

Or deliberately run with _nothing_ provisioned — that is exactly how you test the
empty / first-launch / blocked-feature states (seed goal G2 below).

### 3. Point the agent at it and give it a seed goal

Pick **one seed goal** from the catalogue below per mini-session (don't hand the
agent all of them at once — a focused 10–30-step session produces better notes
than an unbounded wander). Give it the goal plus this standing instruction:

> "Explore the ReWinds app on this emulator toward the goal below. Click, scroll,
> type, and navigate like a curious human. After each meaningful screen, take a
> screenshot. If anything looks wrong — a crash, a blank screen, overlapping or
> clipped text, a frozen spinner, a control that does nothing, a confusing
> message — **stop, screenshot it, and describe what you expected vs. what you
> saw**. Don't try to fix anything; just report."

### 4. Capture findings

Every session produces a short written log. The minimum a finding needs to be
actionable:

- **Screenshot(s)** of the bad state (and the step before it).
- **Repro steps**: the click/scroll/type sequence that got there.
- **Expected vs. actual**, in one or two sentences.
- **Platform + state**: Android/iOS, and what was provisioned (keys, places).

Keep raw session logs out of the repo (they're large and transient). The durable
artifacts are the **Linear tickets** the session produces (see
[Feedback loop](#feedback-loop)).

---

## Seed exploration goals

A catalogue of concrete starting goals, so exploration is _seeded_, not fully
open-ended. Rotate through them across sessions; add new ones as the app grows.
These deliberately overlap the J1–J8 journeys (KIM-291 / KIM-294) so the agent
probes the **edges around** known-good paths.

| #   | Seed goal                                  | What it stresses / what to watch for                                               |
| --- | ------------------------------------------ | ---------------------------------------------------------------------------------- |
| G1  | **Open every tab and scroll to the end**   | Content overflow, clipped/overlapping text, lazy-list jank, missing empty states.  |
| G2  | **Trigger empty / first-launch states**    | Clear app data, then Home, Place summary, Chat with nothing provisioned. Blank vs. helpful empty UI; the VC-key nudge banner; chat-blocked-no-key dialog. |
| G3  | **Trigger error states**                   | Search with airplane mode on; download a month with no network; bad/expired key. Does it show a real error or a frozen spinner / silent failure? |
| G4  | **Rotate the device mid-flow**             | Portrait↔landscape on Home, Place summary, month grid, Chat. State loss, relayout breakage, scroll-position jumps. |
| G5  | **AI chat with an unusual query**          | Empty message, emoji-only, a 2000-char paragraph, a query about a place not saved, a non-weather question. Crashes, truncation, unhandled tool errors, runaway cost. |
| G6  | **Add / open / delete places rapidly**     | Add several places fast, open summaries, delete the one being viewed, delete all. Stale screens, navigation to a deleted place, list not refreshing. |
| G7  | **Navigate in an unexpected order**        | Deep-link around: Place → Chat → Settings → back-back-back; system back from every screen. Broken back stack, double screens, lost state. |
| G8  | **Month grid edge months**                 | January / December, a month with partial data, a future month, leap-day Feb. Off-by-one in date filtering, NO_DATA vs. FULL rendering, empty statistics. |
| G9  | **Settings round-trip**                    | Enter a key, leave, return; clear a key; enter a malformed key. Persistence across restart, validation messaging, secure-field behaviour. |
| G10 | **Tiny / huge font + display scale**       | System font size at max, display zoom up. Truncation, button text wrapping, tap targets overlapping. |

> Keep this table in sync with the critical-journey list (KIM-291) as journeys
> are added or change. A seed goal that consistently finds nothing can be retired.

---

## Cadence and ownership

| Aspect       | Decision                                                                                 |
| ------------ | ---------------------------------------------------------------------------------------- |
| **Cadence**  | **Roughly weekly**, and **always once before a release to master**.                      |
| **Trigger**  | **Manual** — Kimmo starts a session. There is no cron, no CI hook, no scheduled runner.  |
| **Owner**    | **Kimmo** owns the cadence and curates findings into tickets. He may delegate a session but owns the triage. |
| **Duration** | Time-box to **one or two focused mini-sessions** (~15–30 min of agent driving) per run; one seed goal each. |
| **Scope**    | Android first. Escalate to the iOS simulator only when a finding looks platform-specific. |

The cadence is intentionally loose: this layer is a safety net, not a gate. A
missed week costs nothing; the deterministic layers still run on every PR and
release.

---

## Feedback loop

The whole point of Layer 3 is to **feed the deterministic layers**. A wander that
produces no tickets and no new tests was, at best, reassurance.

```
agentic session
      │  finds something off
      ▼
reproduce manually ──no──▶ discard (note it as a false positive)
      │ yes
      ▼
file a Linear bug  (KIM / ReWinds project, label the area)
      │
      ├── recurring, or on a critical journey? ──▶ promote to a deterministic test:
      │        • E2E / cross-platform critical path ─▶ Maestro flow  (KIM-294, .maestro/flows/)
      │        • single-screen / logic / state       ─▶ Compose semantic test (KIM-293)
      │
      └── one-off / cosmetic ──▶ fix via the bug ticket; no new automated test
```

Rules:

1. **A confirmed finding becomes a Linear bug ticket** in the KIM / ReWinds
   project — with screenshot, repro steps, expected vs. actual, and platform.
   Follow the normal spec → Gate 1 → dev flow (`docs/agent/WORKFLOW.md`); Layer 3
   does not get to bypass it.
2. **A finding that recurs, or that sits on a critical journey, gets promoted
   into a regression test** so it can never silently come back:
   - If it's an **end-to-end / cross-platform critical path**, add or extend a
     **Maestro flow** (KIM-294, `.maestro/flows/`, using `TestTags` `id:`
     selectors from `core/TestTags.kt`).
   - If it's a **single-screen behaviour, state transition, or piece of logic**,
     add a **Compose semantic test** (KIM-293) or a unit test on the ViewModel.
   - If it needs a new `TestTags` constant to be selectable, note that on the
     ticket (KIM-290 territory) rather than inventing an ad-hoc selector.
3. **One-off cosmetic issues** are just fixed via the bug ticket — don't burden
   the suites with a test for something that can't regress meaningfully.

This is how exploratory coverage **ratchets into** deterministic coverage: Layer
3 discovers, Layers 1–2 lock it down.

---

## Non-CI-gating

This process **never blocks a PR or a release.** It is explicitly the
non-gating layer of the strategy:

| Layer | Suite                          | When it runs                  | Gates?                                |
| ----- | ------------------------------ | ----------------------------- | ------------------------------------- |
| 1     | Compose semantic (KIM-293)     | **Every PR**                  | **Yes** — red blocks merge            |
| 2     | Maestro E2E (KIM-294)          | **Manual / release to master**| **Yes for the release** — red blocks  |
| **3** | **Agentic exploratory (this)** | **Periodic, manual**          | **No — never blocks anything**        |

Because the agent is non-deterministic and vision-driven, wiring it into a gate
would produce flaky, unattributable failures and unbounded cost. Its findings
influence CI only **indirectly**, by becoming Layer 1/2 tests via the feedback
loop above.

---

## Rough cost/time budget

This is what KIM-288 actually asked about, so budget before you run.

The agent is **vision-LLM-driven**: each step is roughly _screenshot → model
reasons → emits an action_. A useful exploratory journey is **~10–30+ steps**,
and each step sends an image plus accumulated context to the model.

| Cost driver                | Note                                                                                         |
| -------------------------- | -------------------------------------------------------------------------------------------- |
| **Per step**               | One screenshot (image tokens) + growing transcript. Image + context tokens dominate.         |
| **Per journey**            | ~10–30+ steps → **time-box each mini-session** rather than letting it run open-ended.         |
| **AI-chat seed goals (G5)**| Hit the **live Anthropic API** _inside the app under test_ — that's a second, separate spend on top of the driving agent. Use a cheap model / short prompts when exercising chat just to reach UI states. |
| **Human time**            | A session needs a human watching and curating (~15–30 min). That, not tokens, is often the real cost. |

**Budgeting guidance:**

- **Cost is bounded by step count, so time-box.** Cap a mini-session (e.g. ~20–30
  steps or ~15 minutes) and stop, rather than chasing an open-ended wander.
- **One seed goal per mini-session.** Focused sessions are cheaper _and_ produce
  better notes than one giant unbounded run.
- **Prefer the Android emulator** for routine runs — it's the cheapest to drive
  and reach. Escalate to the iOS simulator only to confirm a platform-specific
  finding.
- **Keep AI-chat (G5) runs short** so the in-app live API spend stays small.
- **Don't automate it (yet).** An always-on scheduled agentic runner multiplies
  every cost above by its frequency. Prove value manually first (KIM-288 / this
  runbook's scope); a scheduled runner is a separate, later decision.

Net: this is a **deliberately cheap, occasional** activity. If a run starts
feeling expensive, shrink the seed goal and the step cap — don't widen the wander.

---

## References

- **KIM-288** — original question: AI "click around like a human" testing,
  native feasibility, cost. **This runbook is the answer.**
- **KIM-289** — EPIC: UI Testing Strategy & Automation (the 3-layer plan).
- **KIM-291** — the 5–10 critical user journeys (J1–J11); seed goals here probe
  the edges around them.
- **KIM-293** — Layer 1: Compose semantic UI tests (per-PR gate). Promotion
  target for single-screen / logic regressions.
- **KIM-294** — Layer 2: Maestro E2E smoke suite (`.maestro/flows/`). Promotion
  target for end-to-end / cross-platform regressions.
- `docs/agent/testing/TESTING-STRATEGY.md` — overall strategy; this doc is its
  Layer 3.
- `.maestro/README.md` — Layer 2 run instructions, preconditions, and app ids
  reused by this runbook's setup.
- `docs/agent/WORKFLOW.md` — the ticket lifecycle a Layer 3 finding follows once
  it becomes a bug.
