# KIM-119: Feature: Multiple chats

**Status:** Done · **Priority:** High · **Labels:** Feature, spec-ready, EPIC
**Created:** 2026-03-19T11:58:24.474Z · **Completed:** 2026-06-15T17:46:15.480Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-119/feature-multiple-chats

## Description

## Spec

Parent/epic for "multiple saved chats". Split into smaller, independently shippable issues. This issue stays as a tracking/umbrella ticket and is not directly built.

### Resolved (Kimmo, 2026-06-12)

Confirmed: chats are NOT rigidly 1:1 with places. Independent chat sessions (conversation list), each optionally tagged with a place for auto-titling, but not owned/restricted by it.

### Split into:

* [KIM-285](https://linear.app/kimmo-m/issue/KIM-285/data-layer-multiple-chat-sessions-list-create-switch-auto-title) — Data layer: multiple ChatSession rows, repository CRUD (list/create/switch sessions), 50-chat cap, auto-title generation — **specced, spec-ready, in Backlog**
* [KIM-286](https://linear.app/kimmo-m/issue/KIM-286/ui-chat-session-listswitcher) (was "KIM-129b") — UI: chat list/switcher screen, "New chat" action, `ChatViewModel` wiring — **specced, spec-ready, in Backlog. Depends on** [KIM-285](https://linear.app/kimmo-m/issue/KIM-285/data-layer-multiple-chat-sessions-list-create-switch-auto-title)**.**
* [KIM-130](https://linear.app/kimmo-m/issue/KIM-130/delete-chat) (existing) — Delete chat — builds on [KIM-285](https://linear.app/kimmo-m/issue/KIM-285/data-layer-multiple-chat-sessions-list-create-switch-auto-title)'s session model

## Acceptance criteria

- [ ] N/A — this issue is an umbrella; acceptance criteria live on the split issues above

## Definition of done

- [ ] N/A — umbrella issue, not directly built

## Notes

**UX direction resolved (po, 2026-06-12)**: the chat session switcher ([KIM-286](https://linear.app/kimmo-m/issue/KIM-286/ui-chat-session-listswitcher)) is a separate entry point from the existing `ContextChipRow` in `ChatView.kt` — they operate on different axes (which conversation vs. this conversation's place tag) and don't need to visually merge. A new icon in `AppHeader` opens the switcher; `ContextChipRow` is untouched by [KIM-286](https://linear.app/kimmo-m/issue/KIM-286/ui-chat-session-listswitcher). See [KIM-286](https://linear.app/kimmo-m/issue/KIM-286/ui-chat-session-listswitcher) Notes for the considered options and the rationale, plus a flagged follow-up (KIM-129c, not yet created) for giving the chip row a real backing behaviour (persisting `placeId` on the active session).

All three split issues are now specced and spec-ready in Backlog, awaiting Gate 1.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-12T07:58:46.691Z

Architect technical pass complete (2026-06-12) — see detailed implementation notes on KIM-285.

Summary: schema/migration plan (`5.sqm`, v5→6) is correctly numbered, no collisions with existing migrations. One correction made to KIM-285's spec: `placeId` must be `TEXT` (matching `WeatherResponse.resolvedAddress`), not `INTEGER` — there's no integer-keyed Places table in this schema. Repository/ViewModel/navigation refactor plan otherwise confirmed accurate against current code (`ChatRepository.kt`, `ChatViewModel.kt`, `Router.kt`/`NavigatorImpl.kt`). No DI changes needed. KIM-285 is ready for development once that one TEXT/INTEGER fix is acknowledged.

### kimmo.myllyviita@gmail.com — 2026-06-12T07:54:38.977Z

## Resolved: "All places" / "All chats" semantics (Kimmo's question, 2026-06-12)

**"All chats" is a list FILTER, not a separate or merged conversation.**

Per Mr.T's ChatListView design, the place-chip row above the chat list is a filter:
- "All chats" selected → list shows every chat session, regardless of `placeId`
- "Helsinki" / "Tampere" / etc. selected → list shows only chats where `placeId` matches that place

This is pure client-side filtering over the existing `ChatSession` list (KIM-285). No new data shape, no merged view.

**Each chat is its own independent conversation** (matches the "not 1:1 with places" decision already in this issue). A chat session has:
- `placeId = NULL` → general/unscoped chat
- `placeId = X` → tagged to place X (used for auto-titling per KIM-285)

**New-chat creation**: whichever chip is active when the user taps "New chat" determines the new session's `placeId` — "All chats"/no chip selected → `placeId = NULL` (general chat); a specific place chip selected → that chat is created with `placeId = <place>`. Either way it's a single new row in `ChatSession`, same shape KIM-285 already specs. No new ticket needed for this — it's a parameter on the existing "create session" call.

**On a merged/combined cross-place conversation**: recommend against this for MVP. It would require interleaving message histories from multiple `chatSessionId`s into one AI context — ordering and context-mixing get murky fast, and KIM-285's model (messages keyed 1:1 to a single session) doesn't support it without a real redesign. The list-filter model above covers the apparent need (find/browse chats by place) without that complexity. If real demand for cross-place context emerges later, it should be scoped as its own, separate ticket.

**Net effect on KIM-129b**: no scope change. Spec it as: filter chip row (filters the session list by `placeId`, "All chats" = no filter) + "New chat" action that creates a session tagged with whichever chip is currently active. Both consume KIM-285's repository methods as specced.

### kimmo.myllyviita@gmail.com — 2026-06-12T06:26:12.967Z

Thanks Kimmo — taking this as: confirmed, chats are NOT location-locked (independent chat sessions, optional place tag/auto-title). That unblocks **KIM-129a** (data layer), which I'll spec now and put in Backlog as `spec-ready`.

On the place-tabs-under-chat point: you're right that `ContextChipRow` (the row of place chips above the chat input in `ChatView.kt`) currently calls `vm::selectContextChip` but the result isn't visibly doing anything useful yet. For the multi-chat UI ticket (**KIM-129b**), this needs a decision: do those chips become (a) a way to filter/scope the *new* chat's context (current intent, just needs to visibly work), (b) a way to *switch* between existing chats for that place, or (c) something else entirely once a proper chat list/switcher exists?

Per your note, I won't spec 129b's UI yet — I'll flag it for `ux-ui-reviewer` (Mr.T) to produce a quick mockup/recommendation for how the chat list/switcher and the existing place-chip row coexist, before I write 129b's acceptance criteria. KIM-129a (data) and KIM-130 (delete) don't depend on that outcome, so they're not blocked.

KIM-119 stays in Backlog; I'm removing `needs-human` since the location-locking question is resolved, but will re-flag once the UX direction for 129b comes back if it surfaces a new product decision.

### kimmo.myllyviita@gmail.com — 2026-06-12T06:24:57.431Z

I agree that chat should be still flexible but also the current app has place tabs under the chat which I think is kinda nice - except now nothing happens of course when pressing one. However I'd like to see a UX design before deciding.

