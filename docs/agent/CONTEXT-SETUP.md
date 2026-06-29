# Claude context setup (brief)

How ReWinds keeps Claude's session context lean: a small **always-loaded** core, with
everything else **loaded on demand** when a task actually needs it.

## Always loaded (every session)
| Source | What it carries |
|--------|-----------------|
| `CLAUDE.md` | Build commands, config gotchas, the agent workflow, **summary** of architecture rules |
| `memory/MEMORY.md` | A one-line-per-fact index — pointers only, no inlined content |

That's it. Together ~120 lines. The goal: enough to orient any session (code or not) without paying for detail most sessions never use.

## Loaded on demand
- **`docs/agent/ARCHITECTURE-RULES.md`** — full MV\* + KMP-safety rules with code examples.
  CLAUDE.md has the 2-line summary; the coding agents (**Randy**, **Marcy**) read the full file
  before writing or reviewing code. It is no longer `@import`-ed into CLAUDE.md, so non-code
  sessions don't carry ~130 lines of Kotlin examples.
- **`docs/agent/WORKFLOW.md`, `iOS_BUILD_GUIDE.md`, `architecture.md`, `modules/`, `decisions/`, `plans/`** — read when relevant.
- **`memory/*.md`** — each fact is its own file; Claude opens one when its MEMORY.md index line looks relevant.

## The rules of thumb
1. **CLAUDE.md = summary + pointers**, not the full text of anything.
2. **Detail lives in a doc**, referenced by a one-liner. If a session needs it, Claude reads it.
3. **MEMORY.md is an index.** One line per memory; the body goes in `memory/<slug>.md`.
4. **Code rules belong with the code agents.** Randy/Marcy point at ARCHITECTURE-RULES.md directly,
   so the rules load where they're used rather than into every session.

## If you add something
- A new **always-true, every-session** fact → one tight line in CLAUDE.md.
- A **detailed guide / examples** → its own doc under `docs/agent/`, pointer from CLAUDE.md.
- A **learned preference or project fact** → a `memory/<slug>.md` file + one index line in MEMORY.md.
