# Database Export/Import Plan

**Scope**: Developer-only feature for transferring data between Android emulator and iOS device

## Design Decisions

### 1. Export Format
**Decision**: Raw `.db` file
- Simplest approach
- Direct SQLite database copy
- No transformation needed
- File: `rewinds.db`

### 2. Export Location
**Decision**: Shared accessible location
- Option: Dropbox/iCloud/shared folder
- Goal: Easy to access from both Android emulator and iOS device
- Manual file management by developer

### 3. Import Method
**Decision**: Debug menu in app
- Add "Dev Tools" or "Debug" menu (hidden or in-app)
- Option to select `.db` file from file system
- Replace current database with selected file
- Show success/error confirmation

### 4. Safety & Validation
**Decision**: Minimal but present
- File extension validation (must be `.db`)
- File exists check
- Optional: Simple checksum/hash verification
- No rollback needed (developer can keep backups)

## Implementation Steps

### Phase 1: Export (Android)
- [ ] Add "Export Database" button in debug menu
- [ ] Copy `rewinds.db` to accessible location (Documents/Shared folder)
- [ ] Show success message with file location
- [ ] Document the process in code comments

### Phase 2: Import (iOS)
- [ ] Add "Import Database" button in debug menu
- [ ] File picker to select `.db` file
- [ ] Validate file before import
- [ ] Close database connection
- [ ] Replace database file
- [ ] Restart database connection
- [ ] Show success/error message

### Phase 3: Testing
- [ ] Test export from Android emulator
- [ ] Transfer file manually to iOS
- [ ] Test import on iOS Simulator
- [ ] Verify data integrity after import
- [ ] Test import on actual iOS device

## Technical Notes

### Database File Locations
- **Android**: `/data/data/com.km.rewinds/databases/rewinds.db`
- **iOS**: `~/Library/Application Support/ReWinds/` or similar

### Import Logic
```
1. User selects file from file picker
2. Validate file is a valid SQLite database
3. Backup current database (optional)
4. Close active database connection
5. Replace database file with selected one
6. Reopen database connection
7. Verify tables/schema
8. Show confirmation
```

### Error Handling
- File not found
- Invalid SQLite database
- Permission issues
- Database locked
- Schema mismatch (optional check)

## Debug Menu Location
- Add to settings or app menu
- Label: "Dev Tools" or "Debug"
- Only visible in development builds (optional)
- Buttons:
  - Export Database (Android)
  - Import Database (iOS)
  - Clear Database (optional)

## Files to Modify
- `composeApp/src/commonMain/kotlin/` - Common export/import logic
- `composeApp/src/androidMain/kotlin/` - Android file access
- `composeApp/src/iosMain/kotlin/` - iOS file access
- UI components for debug menu

## Success Criteria
- ✅ Can export database from Android
- ✅ Can transfer file to iOS device
- ✅ Can import database on iOS
- ✅ Data persists after import
- ✅ App continues to work normally with imported data
