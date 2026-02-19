# ReWinds Development Documentation

## Quick Navigation

### For Claude AI Sessions
- **Start Here**: Check `CURRENT-SESSION.md` to see what was last worked on
- **Quick Ref**: See `../CLAUDE.md` for build commands and project overview
- **Full History**: See dated `DEVELOPMENT-MMDDYY.md` files below

### Session Files (Latest First)
- `DEVELOPMENT-180226.md` - Feb 18, 2026: iOS build resolution & Android-focused strategy (feature gating, OOM documented)
- `DEVELOPMENT-170226.md` - Feb 17, 2026: iOS database import implementation & build optimization attempts
- `DEVELOPMENT-160226.md` - Feb 16, 2026: Database export/import plan & architecture setup
- `DEVELOPMENT-160226-2.md` - Feb 16, 2026: Additional session work (continuation)
- `DEVELOPMENT-120226.md` - Feb 12, 2026: SQLite3 iOS cocoapods setup (iOS simulator working, device builds OOM)

### How to Use This Repo

**For Development**:
1. Read CLAUDE.md for quick start
2. Build appropriate target (see iOS Build Commands below)
3. Check docs/DEVELOPMENT-*.md if issues arise

### iOS Build Commands

```bash
# iOS Simulator Builds (Recommended for Development)
./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorArm64      # Arm64 simulator
./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorX64        # Intel simulator

# iOS Device Builds (May require 8GB+ heap, known OOM issues)
./gradlew :composeApp:linkPodReleaseFrameworkIosArm64               # Physical device

# Android Builds
./gradlew :composeApp:assembleDebug                                 # Android debug APK
./gradlew buildAndroidOnly                                          # Android without iOS
```

**Note**: iOS builds currently have memory constraints. See `DEVELOPMENT-180226.md` for details.

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
