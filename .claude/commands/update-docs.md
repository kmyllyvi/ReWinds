Invoke Phill (doc-agent) to update `/docs/agent/` based on recent changes.

## Usage

`/update-docs` — updates docs based on the latest merged commit
`/update-docs PR#42` — updates docs based on a specific PR number
`/update-docs KIM-123` — updates docs based on a Linear ticket

## What this does

Delegate to the **Phill (doc-agent)** agent with the following context:

- If a PR number was given (`$ARGUMENTS` starts with `PR#` or a number): fetch the PR diff and linked Linear ticket, then update `/docs/agent/` accordingly.
- If a Linear ticket ID was given (e.g. `KIM-123`): read that ticket and the latest commit on its branch, then update docs.
- If no argument: use `git log -1` to identify the latest commit and update docs based on that change.

Phill's job is to keep `/docs/agent/` in sync with the actual codebase and workflow. He should update or create doc files that reflect what changed — architecture, agent workflow, feature notes — without touching CLAUDE.md or agent files.

$ARGUMENTS
