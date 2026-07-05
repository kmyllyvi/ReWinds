# Handling credit-limit interruptions

What to do when a Claude Code on the web session gets cut off mid-task because the account hit its
credit/usage limit, and why this can't be fully automated today.

## Why there's no automatic "sweep"

There is no tool available to a session that lists *other* sessions on the account or their
interruption state. The scheduling tools this repo's sessions have access to
(`create_trigger`, `send_later`, `fire_trigger`, `list_triggers`) all operate on triggers *you*
created — they can wake a specific session back up, but they can't discover "which sessions died
mid-task from a credit limit" account-wide. That discovery only exists in the claude.ai session
list UI, which isn't exposed as a tool.

So: no cron job can sweep the account for interrupted sessions. What follows is the manual process,
plus an optional self-check-in pattern for sessions you know are at risk.

## Manual recovery (the actual fix)

1. Open the session in the claude.ai web/session list — interrupted sessions stay resumable
   indefinitely; nothing is lost.
2. Send any message (e.g. "continue") to resume it. Full prior context (including anything
   summarized) carries forward automatically per the harness's context management — the session
   does not need to be re-briefed.
3. If the credit limit was account-wide (not session-specific), wait for the limit window to reset
   before retrying, per the plan's reset schedule shown in claude.ai billing settings.

## Optional: self check-in for a specific long-running session

If you're kicking off a task you expect might run long enough to hit a limit, you can have that
*same* session schedule its own wake-up rather than relying on you to notice it stalled:

```
send_later(message: "check whether the previous task finished; if it was cut off, resume it",
           delay_minutes: 60)
```

or, for a recurring babysit pattern, `create_trigger` bound to the same session
(`persistent_session_id` omitted = fires into the calling session). This only covers the one
session that scheduled it — it is not a general sweep, and it must be set up per-session before
the interruption happens (a session that's already dead can't schedule its own wake-up).

## Bottom line

- No cross-session sweep is possible with current tooling — resuming a stalled session is a manual
  step (open it, send a message).
- A session expecting to run long can arm its own check-in via `send_later` / `create_trigger`
  ahead of time.
- If Anthropic exposes a "list my sessions" or "list interrupted sessions" tool in the future, this
  doc should be revisited — true automatic sweeping would become possible.
