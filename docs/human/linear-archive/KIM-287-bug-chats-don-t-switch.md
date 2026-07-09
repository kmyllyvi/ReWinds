# KIM-287: Bug: chats don't switch

**Status:** Done · **Priority:** No priority · **Labels:** _none_
**Created:** 2026-06-15T06:32:59.712Z · **Completed:** 2026-06-15T09:20:44.289Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-287/bug-chats-dont-switch
**Related:** related: KIM-286 — UI: Chat session list/switcher

## Description

Precondtion:

Add a new place and go to chat:

Expected:

New chat opens and pills show at least "All places" and newly added place (+ any other places with chat tags)

Actual:

new chat opens with "All places" pill/tag

=> added two new places and opened chat for both of them

=>

open place, go to chat

Expected:

Chat opens, Place 1 pill is selected and only chat about this place is shown.

Selected another pill changes chat content accordignly

Actual:

chat open, last selected pill is selected (not the place) - chat shows correctly.

Changing to other place has no effect.

Another:

selecting "all places" should show complete chat history

Actual:

all places is empty with new chat message "Let's talk about the weather"

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T06:55:00.707Z

Resolution (Kimmo, 2026-06-15) for issues 2b and 3:

If a chat cannot actually be switched between places, then it shouldn't show selectable pills that do nothing — that's the bug. Rule:

- **Place-tagged chat** (opened via "Ask AI about this place" / has a `placeId`): show ONLY that place's pill, non-interactive/informational. No "All places" pill, no other places' pills — there's nothing to select.
- **Untagged/general chat** (`placeId = null`): no place pills shown either (or just "All places" as a label, non-interactive) — same logic, nothing to select.

This also resolves issue 1 (new place's pill not appearing) — since only the current chat's own place pill is shown, there's no "list of all tagged places" to keep in sync.

Switching between chats (place-specific or general) is the job of the chat session switcher (KIM-286, already shipped), not these in-chat pills.

Net effect: `ContextChipRow` becomes a static place-tag display (matches Mr.T's original KIM-129b design intent), not an interactive switcher.

