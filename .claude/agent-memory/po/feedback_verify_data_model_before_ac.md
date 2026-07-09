---
name: feedback_verify_data_model_before_ac
description: Always verify the actual DB schema/repository behind a data-shaped feature ask before writing AC — the raw idea's assumed mechanism may not match what the schema supports
metadata:
  type: feedback
---

When a raw idea describes data behaviour ("X is kept but hidden", "Y restores automatically", "archive
≠ delete"), don't transcribe the idea into AC verbatim — read the actual `.sq` schema file and the
repository method(s) involved first. Grep for the entity's table definition and its existing
delete/insert queries before assuming a flag or table exists to support the ask.

**Why**: for KIM-364/KIM-365 ("archive place" vs the old "delete place"), the raw idea assumed archive
vs. delete was mostly a UI question. Reading `AppDatabase.sq` + `WeatherRepository.deletePlace()` showed
there is no separate "known places" table at all — a place's existence *is* its `WeatherResponse` row,
and the existing `deletePlace()` is a hard delete via `ON DELETE CASCADE`. "Archive without losing data"
required inventing a new nullable `archivedAt` column and a repository method, which is real, ticket-
worthy scope on its own — not a free side effect of a UI change. Writing the AC without this check would
have produced an untestable/infeasible spec that Randy would have had to reinterpret mid-build.

**How to apply**: for any ticket touching persisted entities (Place, ChatSession, Settings, etc.),
before drafting AC: (1) grep the `.sq` file for the table and any existing queries touching it, (2)
grep the repository/`Database.kt` for the method(s) the ViewModel calls, (3) only then decide whether
the ask is achievable as pure UI/ViewModel work or needs a schema migration — and if it needs a
migration, that's usually its own foundation ticket, split from the UI (see
[[feedback_ticket_splitting]] pattern: schema/data-layer ticket blocks the UI ticket).

Also: when the raw idea references a past attempt (e.g. "this was done once as swipe-to-delete but it
was buggy"), grep for whether that old implementation is still present in the code (it may have already
been ripped out, as it was here — `PlaceButton.kt` had its delete `IconButton` commented out and no
swipe gesture at all). This changes how the AC should treat "restore" vs. "build new" and is worth
checking rather than assuming.
