# KIM-327: Month card missing loading state

**Status:** Done · **Priority:** No priority · **Labels:** Bug
**Created:** 2026-07-01T06:57:31.024Z · **Completed:** 2026-07-01T07:42:26.580Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-327/month-card-missing-loading-state
**Related:** related: KIM-278 — performance: Loading times are generally too long / some loading indicators missing

## Description

The month chip/card has no loading state. When it starts loading it shows nothing (blank) until data arrives.

**Expected:** Show a loading indicator/placeholder on the month card while data is being fetched, instead of an empty card.

*Source: voice memo bug report ("Loading state").*

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-01T07:42:36.266Z

**Marcy re-review — Major resolved, Completed.**

Confirmed commit 400ef0a fixes the testTag collision:
- `TestTags.MONTH_STAT_CARD_GRID_SKELETON = "month_stat_card_grid_skeleton"` added, distinct from `MONTH_STAT_CARD_GRID = "month_stat_card_grid"` — no string collision.
- `StatCardGridSkeleton` now tags the skeleton; loaded `StatCardGrid` keeps `MONTH_STAT_CARD_GRID` exclusively. The two are mutually exclusive branches (`currentStats == null` vs `else`), so both tags never coexist.
- J4 (`.maestro/flows/j4_monthly_statistics_full_month.yaml`) gates on `month_stat_card_grid` at line 31 — that wait can no longer be satisfied by the skeleton, so the loaded-state assertion is no longer masked.

buildAndroidOnly + testDebugUnitTest green on the new commit; CI green.

Everything else in the prior review was Passing. The "month card" vs "month stat cards" interpretation question is Kimmo's Gate 2 call, not a review blocker.

Verdict: Ready to commit / merge.

### kimmo.myllyviita@gmail.com — 2026-07-01T07:29:24.248Z

**Code review (Marcy) — Needs changes**

Verdict: fail. One Major issue blocks merge; everything else is clean.

**Major — duplicate testTag breaks an existing UI test's signal**
- `StatCardGridSkeleton` is tagged `TestTags.MONTH_STAT_CARD_GRID` (MonthlyStatisticsView.kt:328), the same tag already carried by the real `StatCardGrid` (line 283).
- Maestro flow `.maestro/flows/j4_monthly_statistics_full_month.yaml:29-32` uses `month_stat_card_grid` as its gate for "loaded stats rendered." With this change the loading skeleton now satisfies that wait *before* real stats arrive, so J4 can pass on the skeleton and never confirm the loaded state. That is a real loss of test coverage from a production change.
- Fix: give the skeleton its own tag (e.g. `MONTH_STAT_CARD_GRID_SKELETON`) and keep `MONTH_STAT_CARD_GRID` exclusive to the loaded grid. Randy's call, not mine to edit.

**Passing checks**
- MV* compliance: clean — both new composables are pure render (theme-driven muted Boxes), no `remember { mutableStateOf }`, no logic. The `stats == null` branch pre-existed; only spinner→skeleton swapped.
- Skeleton shape parity with `MonthStatCard` (surface bg, 11.dp corner, 11/10 padding) is faithful.
- Expected behaviour ("placeholder instead of blank/spinner while loading") is met for the chosen target.
- DoD test exemption (pure UI styling, no logic): acceptable per WORKFLOW.md.

**Gate 2 — not a blocker, Kimmo's call**
- Interpretation of "month card" is genuinely ambiguous and Randy flagged it correctly. His reading (the `MonthStatCard` stat grid, where a real blank/spinner gap existed) is reasonable and defensible. The alternative (`MonthCell` per-cell indicator during on-demand download, where `isDownloadingMonth` is tracked but never rendered) is a separate concern. This is a scope decision for Kimmo, not something to resolve in review.

Honest limit: I share Randy's model family, so treat this as spec-drift/defect-level review, not deep design. The testTag regression is concrete; a human should still eyeball the visual result before merge.

`in-review` removed; issue stays In Progress. Back to Randy for the testTag fix.

### kimmo.myllyviita@gmail.com — 2026-07-01T07:22:21.458Z

**Dev handoff (Randy)**

- **Branch:** `kimmomyllyviita/kim-327-month-card-loading-state`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/62
- **Build:** pass (`./gradlew buildAndroidOnly`)
- **Tests:** pass (`./gradlew :composeApp:testDebugUnitTest`)
- **New tests written:** no — pure UI placeholder change, no new logic (DoD test exemption: UI styling).

**Summary:** While a month's stats were being computed, the stat-card area showed a bare spinner (and could flash blank). It now shows skeleton stat cards. Added `MonthStatCardSkeleton` (muted placeholder shaped like `MonthStatCard`, matching the KIM-278 skeleton approach); `MonthlyStatisticsView` renders a 2×2 `StatCardGridSkeleton` while `stats == null`.

**Open question / ambiguity (KIM-327 wording).** "Month chip/card shows nothing (blank) while loading" is ambiguous. I targeted the **stat cards on the monthly summary** (`MonthStatCard` grid) — the only element literally named a "month … card" and where a real blank/spinner gap existed. The alternative is the place-summary **month grid cells** (`MonthCell`): those already sit behind a full-screen loader on initial load, but have no per-cell indicator during an on-demand month download (`isDownloadingMonth` is tracked but never rendered). If that was the intended target, it's a small repoint — flagging rather than guessing. Issue was in Backlog (no `spec-ready`) but explicitly dispatched by Kimmo; moving to In Progress + in-review.

