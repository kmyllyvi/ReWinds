# KIM-298: Bug: chat - text input has big space between keyboard when open

**Status:** Done · **Priority:** Medium · **Labels:** Bug
**Created:** 2026-06-15T09:24:44.184Z · **Completed:** 2026-06-24T13:47:20.322Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-298/bug-chat-text-input-has-big-space-between-keyboard-when-open

## Description

go to chat

select text input to start typing => keyboard opens.

Text input has probably app tab menu height of empty space between keyboard

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-23T07:02:07.263Z

**Handoff → review** (PR [#51](https://github.com/kmyllyvi/ReWinds/pull/51))

**Root cause of the reopen:** the real fix for this bug (`217deb2`, the `max(tabBarInset, imeInset)` approach) was written, marked Done, and documented back in June — but **never merged to `develop`**. The branch has been running the earlier, weaker `consumeWindowInsets(innerPadding)` fix this whole time, which `217deb2`'s own commit message notes does not reliably propagate the inset subtraction through the Scaffold on iOS CMP. So iOS kept the tab-bar-height gap.

**Fix:** re-landed `217deb2` onto current develop (clean cherry-pick, no conflicts):
- `core/Router.kt` — Scaffold content host now collapses the tab-bar inset and IME inset into a single bottom value `maxOf(innerPadding.bottom, WindowInsets.ime.bottom)`; removed `consumeWindowInsets(innerPadding)`.
- `ai/ChatView.kt` — removed `.imePadding()` from the main `Column` so the IME inset isn't double-counted on top of the tab-bar inset.

**Verification:**
- ✅ `./gradlew buildAndroidOnly` green; no tests broken.
- ⚠️ **iOS manual verification in Xcode is the real acceptance gate** — this is an iOS-only layout bug and Gradle iOS builds are unreliable. Open chat, tap the input: the keyboard should sit directly under the input with no tab-bar-height gap.

Branch: `kim--298-chat-input-keyboard-spacing`. Marcy auto-triggers on PR open; iOS on-device check still needed before close.

### kimmo.myllyviita@gmail.com — 2026-06-23T06:43:21.889Z

reopened → tried to fix this earlier but still happening on iOS

