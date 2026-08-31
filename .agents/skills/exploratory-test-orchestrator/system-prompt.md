# exploratory-test-orchestrator — workflow

Autonomously verify recently-shipped changes actually work, and file a Linear ticket for anything
that doesn't match its commit's claim.

Adapted from solidmaint's `exploratory-test-orchestrator` skill for a mobile app with no staging
server and no Slack (see `KIM-331` for the design decisions). This is the change-focused
*verification* process — it is not the Mon/Fri `rewinds-instrumented-ui-tests` sweep (that runs the
fixed regression suite); this derives a claim from what actually shipped and checks that specific
claim.

## Usage

- Default (live run): files real Linear issues for confirmed-broken claims.
- `dry-run` argument: do everything except step 6 (filing); still writes the summary.

## Context budget — keep every step small

Long unbounded reads are what turn this into a runaway session. Keep to these habits throughout:

- Verify **at most 4 claims per run.** Log the rest as skipped-for-budget in the summary.
- **Never read a whole diff or file.** Use `git show <sha> --stat`, `git log -1 <sha>`, or
  `rg -n -C2 '<symbol>' <path>`.
- Truncate command output (`| head -40`, `| tail -30`).
- Fetch a Linear issue body only for a claim you've already decided to verify, one at a time.
- **Survey once.** Step 1's `git log` runs exactly once. If you're about to re-run it, stop —
  that urge is the loop. Take the claims you already have and move to step 6 (summary).

## 1. Survey what shipped since the last run

Survey a **2-day** window (yesterday + today) on `develop`. The 1-day overlap absorbs a skipped run;
dedup (step 5) is what keeps the overlap from re-filing the same finding.

```bash
git log --since='2 days ago' --no-merges --first-parent --pretty=format:'%h %s' develop | head -40
```

Keep only commits with a **user-observable** effect on the app (touches `composeApp/src`, changes
behavior or UI a user would notice). Skip pure refactor/build/CI/test/dep-bump/doc commits — no
user-observable claim means skip, never invent one.

## 2. Derive a testable claim from each change

Our commits reference `KIM-<n>: <summary>`. Use the commit subject as the primary claim source;
if it references a ticket, pull that Linear issue (`mcp__linear-server__get_issue`) for a richer
"expected behavior" — but only for a commit you've already decided is worth verifying, one at a
time. Claim shapes:
- *"adds / user can now X"* → X is reachable and works.
- *"fixes / Y no longer …"* → Y no longer reproduces.
- *"changes Z to …"* → the new Z behavior is present.

Every claim must cite its short SHA.

## 3. Ensure a target device, then verify each claim

Unlike the Mon/Fri `rewinds-instrumented-ui-tests` sweep — which deliberately skips if no emulator
is booted, because CI's `android-instrumented` job is a separate backstop for that suite — **this
Skill has no other verification path**. Skipping whenever nobody happens to have an emulator open
would make every unattended run degrade to all-`can't-tell`, which defeats the point of an
exploratory check. So this step manages the emulator's lifecycle itself, boot and shutdown, instead
of only opportunistically using one that's already running.

```bash
adb devices 2>/dev/null | grep -qE '\bdevice$' && echo "emulator present" || echo "no emulator"
```

- **Emulator already running** → use it. Do not boot a second one. Remember `booted_by_run=false` —
  you must not shut this one down at the end, since you didn't start it.
- **No emulator running** → boot one yourself, headless:
  ```bash
  emulator -avd "$(emulator -list-avds | head -1)" -no-window -no-audio -no-snapshot > /tmp/exploratory-emulator-boot.log 2>&1 &
  ```
  Then poll for boot completion (`adb wait-for-device`, then `adb shell getprop sys.boot_completed`
  until it reads `1`), with a **hard boot budget of ~150s**. Remember `booted_by_run=true`.
  - **Boot times out or fails** → kill whatever you launched, judge everything `can't-tell` for this
    run, note "emulator failed to boot within budget" in the summary, and proceed to step 6 — do not
    retry the boot, do not fall back to a different AVD.

For each claim, once a device is available, look for an existing test that plausibly covers the
area:
- `rg -l '<ScreenOrViewModelName>' composeApp/src/androidInstrumentedTest .maestro/flows`
- If a Maestro flow already covers it, run just that flow: `maestro test .maestro/flows/<file>.yaml`.
- If a Compose semantic instrumented test covers it, run the full class via
  `./gradlew :composeApp:connectedDebugAndroidTest --tests "<TestClass>" -PincludeAllTargets=false --no-daemon`.
- If nothing covers it and the check is trivial (a couple of taps), author a small throwaway
  Maestro flow under `.maestro/flows/tmp/` for this run only — do not commit it, do not build new
  heavyweight harnesses.
- Given the near-zero UI-test baseline (KIM-289), expect most claims to land `can't-tell` in v1 —
  that's an honest result, not something to work around by inventing tests on the fly.

One targeted attempt per claim. If it doesn't clearly show works/broken, judge `can't-tell`
immediately and move on.

**Teardown — always, regardless of outcome.** If `booted_by_run=true`, shut down the instance you
booted before moving to step 6 (`adb -s <serial> emu kill`, or `kill` the process you launched if
`emu kill` doesn't respond). This must happen even if a claim's verification errored out partway —
don't leave a booted emulator behind just because a step above it failed. Never shut down an
emulator you found already running (`booted_by_run=false`).

## 4. Judge: `works` / `broken` / `can't-tell`

- **`broken`** (fileable) — ALL of: the claimed behavior demonstrably fails, reproduced ≥2x (rules
  out flake), the commit is confirmed present in the working tree you're testing, and the failure
  is the claim itself (not an unrelated build/env/auth issue).
- **`can't-tell`** (never filed) — no emulator, no deterministic repro, no test coverage for the
  area, or too-vague a claim.
- **`works`** — no action.

When torn, choose `can't-tell` — a false positive costs more trust than a missed bug.

## 5. Dedup, then file (skip filing in dry-run; still summarize)

For each confirmed `broken` claim, search Linear across **all** statuses (open and completed both
count — a closed/wontfix match must suppress re-filing):

```
mcp__linear-server__list_issues with team=Kimmo_m, project="ReWinds app", query="<short-sha>"
```

- **Match, any status** → add a comment ("still reproduces on `<sha>`, run `<date>`") instead of
  filing a new issue. A completed match especially must not be re-opened or re-filed — log it as
  `deduped (#<id>)` in the summary.
- **No match** → file (dry-run: just record the finding for the summary instead of calling
  `save_issue`).

To file: `mcp__linear-server__save_issue` (new issue) — team `Kimmo_m`, project `ReWinds app`,
status **Todo**, label `exploratory-verify`, unassigned. Body: commit SHA + link; **Claimed
behavior** (quoted); **Observed**; **Repro** (deterministic steps/commands); **Confidence note**
(why `broken` not `can't-tell`). Then dispatch **Randy (developer)** on the new issue directly
(same as the Mon/Fri instrumented-ui-tests sweep) — no `needs-human`, it goes straight into the
normal Randy → Marcy lane.

**Per-run cap: 5.** If more than 5 confirmed `broken` claims, file the top 5 and note what was
dropped in the summary — never silently truncate.

## 6. Write the summary — always, every run

Overwrite `docs/human/test-runs/exploratory-latest.md` (mirrors the "latest state, not a history
log" pattern `run-full-tests.sh` uses for `latest.md`) — including a clean 0-broken or 0-claims run,
since a silent run is indistinguishable from a broken one:

```markdown
# Exploratory test run — <timestamp>

<N> checked · <B> broken · <F> filed · <mode: live|dry-run>

- ✅/🐛/❔ `<short-sha>` — <claim, ≤10 words> → works|broken|can't-tell (<terse why>)
  ...

<if F>0: Filed: <links>>
<if capped: note what was dropped>
```

Commit and push this one file to `develop` (same as the instrumented-ui-tests sweep commits
`latest.md`).

## 7. Report back

If run interactively, report the summary table and point to the file + any filed issue links. If
run as part of a scheduled task, follow that task's own reporting convention instead of assuming an
interactive user is watching.

## Forbidden

- No cross-repo, no GitHub issues — Linear only.
- No Slack — the summary artifact is the only human-facing output.
- No speculative claims — only verify claims traceable to a real recent commit.
- No destructive/high-side-effect app actions — read/verify journeys only.
- Android only in v1 — no iOS/simulator verification.
- Never re-survey mid-run (see context-budget note above).
