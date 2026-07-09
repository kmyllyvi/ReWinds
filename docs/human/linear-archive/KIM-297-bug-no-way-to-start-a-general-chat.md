# KIM-297: Bug: No way to start a general chat

**Status:** Done · **Priority:** Medium · **Labels:** _none_
**Created:** 2026-06-15T09:22:44.999Z · **Completed:** 2026-06-16T07:19:57.113Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-297/bug-no-way-to-start-a-general-chat

## Description

expected:

app tab menu "chat" opens a generic chat - never place specific

actual:

first time all chats opened but after opening place chat, last opened chat opens

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-16T07:09:54.152Z

**Dev handoff**

Branch: `kimmomyllyviita/kim-297-bug-no-way-to-start-a-general-chat`
PR: https://github.com/kmyllyvi/ReWinds/pull/36
Build: pass (`./gradlew :composeApp:compileDebugKotlinAndroid`)
Tests: pass (`./gradlew :composeApp:testDebugUnitTest` — 39 tests)
New tests written: yes — `composeApp/src/commonTest/kotlin/ai/ChatTabGeneralSessionViewModelTest.kt` (3 new tests for `ensureGeneralChat()`: switch-away from place session, no-op when already general, create general session when only place chats exist)

**Summary**
- Added `ChatViewModel.ensureGeneralChat()` — re-resolves to the most-recent general session when the active one is place-tagged.
- `ChatView.LaunchedEffect(placeId)` calls it on plain (no-placeId) tab entry.
- Guarded against the placeId-consume re-fire (non-null → null when `consumePlaceContext` clears the route) via a `hadPlaceId` flag so `openPlaceChat` isn't immediately reverted.
- Also replaced the "Chat" text button in the place summary header with the Chat icon (consistent with tab bar).

**Deviations**
None — follows MV* pattern, no direct logic in Composables.

### kimmo.myllyviita@gmail.com — 2026-06-15T18:51:49.574Z

randy investigating…

