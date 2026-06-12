---
name: fk-cascade-not-enforced
description: SQLite ON DELETE CASCADE is declared in the schema but NOT enforced at runtime — drivers never enable PRAGMA foreign_keys
metadata:
  type: project
---

The SQLDelight schema (`composeApp/src/commonMain/sqldelight/com/km/rewinds/db/AppDatabase.sq`)
declares `ON DELETE CASCADE` on several FKs (ChatMessage→ChatSession, WeatherStation→WeatherResponse,
Day→..., etc). But **foreign key enforcement is OFF at runtime**: neither driver enables it.

- iOS: `NativeSqliteDriver(AppDatabase.Schema, "app.db")` in `iosMain/.../Platform.apple.kt`
- Android: `AndroidSqliteDriver(AppDatabase.Schema, context, "app.db")` in `androidMain/.../Platform.android.kt`

SQLite defaults `PRAGMA foreign_keys = OFF` per-connection. No `onConfigure`/callback sets it on.

**Why:** prior migrations even rely on FKs being off — `4.sqm` does `DROP TABLE WeatherResponse` which
would fail/cascade if FKs were enforced.

**How to apply:** when reviewing any new code that DELETEs a parent row and relies on cascade to clean
up children (e.g. KIM-285 `deleteOldestSession` expecting ChatMessage rows to vanish), flag it — the
children will be ORPHANED, not deleted. The fix is either an explicit child-delete in the same
transaction, or enabling `PRAGMA foreign_keys = ON` globally (which is risky given existing migrations
assume it's off). Verify driver setup before trusting any cascade comment in the .sq file.
