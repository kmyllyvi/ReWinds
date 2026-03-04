# ReWinds — AI & MCP Highlevel Plan

## Context

ReWinds is a Kotlin Multiplatform (KMP) app with Compose Multiplatform UI, SQLDelight for local persistence, and Ktor for HTTP. It is **fully on-device** — no backend. Weather data comes from the Visual Crossing API.

The goal is to make the app smart with AI, while also learning MCP concepts progressively. There is no rush to build a real MCP server, but it remains a possible future step.

---

## Guiding Insight

> We are not "embedding AI into the app" — we are **giving AI access to the app's capabilities via tools**. The difference is subtle until Phase 2, where it clicks.

Tool-calling is the heart of MCP. The MCP server is just a transport layer that wraps those same tools for external consumption. Building tool-calling well inside the app first makes extracting to a proper MCP server later mostly a plumbing exercise.

---

## The Three Phases

### Phase 1 — Claude API with structured context (no tools yet)

**What:** Call Anthropic's `/v1/messages` endpoint via Ktor. Pass structured weather data from SQLDelight as context in the prompt. Claude reasons over it and returns a natural language response.

**Goal:** Get a working "smart" feature fast. Learn prompt engineering with domain-specific data.

**Example interaction:** *"Was last November good for kitesurfing in Tarifa?"* → app fetches relevant data from SQLDelight → passes it to Claude → Claude responds conversationally.

**Where this lives:** Shared KMP code (`commonMain`) using Ktor — fits naturally with the existing stack.

---

### Phase 2 — Tool-calling (the MCP pattern, inside the app)

**What:** Define tools as JSON schemas (e.g. `get_wind_data`, `get_monthly_stats`, `search_locations`). Claude decides which tools to call, the app executes them against SQLDelight, returns results, and Claude synthesizes a final response.

**Goal:** Learn the core MCP concept — tools, schemas, the request/response loop — without any server or SDK involved. Claude can now reason about combinations of data it fetches itself, rather than relying on whatever context we manually stuff into the prompt.

**Example:** *"Find days with sustained wind 15–25 knots, gusts under 30, and no rain"* — Claude issues tool calls, the app runs the queries, Claude interprets the results. No need to write that compound query logic explicitly.

**Why this matters:** This *is* MCP conceptually. The server layer is just packaging.

---

### Phase 3 — Standalone MCP Server (learning exercise, optional production use)

**What:** Extract the tools from Phase 2 into a standalone JVM server using the [Kotlin MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk). This server speaks the MCP protocol over stdio/SSE and can be consumed by any MCP-compatible client (Claude Desktop, etc.).

**Goal:** Understand the transport layer, the MCP server lifecycle, and how the SDK abstracts what we built manually in Phase 2.

**Why not start here:**
- The Kotlin MCP SDK is JVM-only and cannot live in shared KMP `commonMain`
- Adds transport complexity before the concepts are solid
- The app doesn't have a backend, so a real server has no natural home yet
- Building it raw first means the SDK won't be magic — you'll know exactly what it's doing

---

## Key Technical Decisions

| Decision | Choice | Reason |
|---|---|---|
| HTTP client for Anthropic API | Ktor | Already in the stack, works in `commonMain` |
| MCP SDK (Phase 1 & 2) | Not used | JVM-only, adds complexity before concepts are clear |
| MCP SDK (Phase 3) | `kotlin-sdk` | Right tool for building an actual MCP server on JVM |
| AI logic placement | `commonMain` shared code | Keeps logic portable across Android and iOS |
| Data source for AI context | SQLDelight (local DB) | All data is on-device; no backend needed |

---

## What to Build First

Start in `commonMain`:

1. An `AnthropicClient` wrapping Ktor that handles auth, request/response serialization, and streaming if needed
2. A `ChatMessage` / `Tool` / `ToolResult` data model matching the Anthropic API shape
3. A first tool: `get_wind_summary(location, date_range)` backed by existing SQLDelight queries
4. A simple UI surface (chat input or contextual "ask about this data" button) wired to the above
