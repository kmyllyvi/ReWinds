# ReWinds Development Documentation

## Quick Navigation

### For Claude AI Sessions
- **Start Here**: Check `CURRENT-SESSION.md` to see what was last worked on
- **Quick Ref**: See `../CLAUDE.md` for build commands and project overview
- **Full History**: See dated `DEVELOPMENT-MMDDYY.md` files below

### Session Files (Latest First)
- `DEVELOPMENT-120226.md` - Feb 12, 2026: SQLite3 iOS cocoapods setup (iOS simulator working, device builds OOM)

### How to Use This Repo

**For Development**:
1. Read CLAUDE.md for quick start
2. Run: `./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorArm64` (iOS simulator)
3. Check docs/DEVELOPMENT-*.md if issues arise

**At End of Session (for Claude)**:
- Update `CURRENT-SESSION.md` with what was accomplished
- Create new `DEVELOPMENT-MMDDYY.md` if starting new major task
- Update `../CLAUDE.md` quick reference if config changed

## Key Files

| File | Purpose |
|------|---------|
| `../CLAUDE.md` | Quick reference for builds, config, status |
| `CURRENT-SESSION.md` | What to pick up from - updated at end of each session |
| `DEVELOPMENT-MMDDYY.md` | Detailed session log - one per work session |
| `iOS-Navigation-Plan.md` | Previous work on navigation architecture |
