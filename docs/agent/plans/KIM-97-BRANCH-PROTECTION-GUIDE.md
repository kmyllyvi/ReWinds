# KIM-97: Branch Protection & Merge Strategy Setup Guide

**Ticket**: KIM-97 - Branch protection & merge strategy
**Status**: In Progress
**Goal**: Configure GitHub branch protection rules for `develop` branch
**Blocking**: KIM-96 (Agent PR workflow automation)

---

## Prerequisites Checklist

- [ ] Admin access to GitHub repository
- [ ] CI pipeline deployed and working:
  - [ ] Android Tests (KIM-94) ✅
  - [ ] iOS Build (KIM-95) ✅

---

## Step-by-Step Configuration Guide

### Step 1: Navigate to Branch Protection Settings

**Location**: GitHub repository Settings

1. Go to your GitHub repository: `anthropics/rewinds` (or your fork)
2. Click **Settings** tab (top right, next to "Code")
3. In left sidebar, click **Branches**
4. Under "Branch protection rules", click **Add rule** button

**Screenshot markers**:
- Look for the green "Add rule" button on the right side
- Should see any existing rules listed above

---

### Step 2: Create Rule for `develop` Branch

**Input field**: "Branch name pattern"
- [ ] Enter: `develop` (exact match)
- [ ] Leave other pattern options unchecked

---

### Step 3: Require Status Checks to Pass

**Section**: "Status checks that are required to pass before merging"

- [ ] ✅ Check: **"Require status checks to pass before merging"**
- [ ] ✅ Check: **"Require branches to be up to date before merging"**

**Search for status checks**:
- [ ] Type in search box: Search for available status checks from your workflow
- [ ] **Select**: `Android Tests` (from KIM-94 CI workflow)
- [ ] **Select**: `iOS Build` (from KIM-95 CI workflow)

**Visual guide**:
```
Status checks section looks like:
┌─────────────────────────────────────────┐
│ ☑ Require status checks to pass         │
│ ☑ Require branches to be up to date     │
│                                         │
│ Search for status checks:               │
│ [search box                      ] 🔍   │
│                                         │
│ ☑ Android Tests                         │
│ ☑ iOS Build                             │
└─────────────────────────────────────────┘
```

---

### Step 4: Require Reviews

**Section**: "Require a pull request before merging"

- [ ] ✅ Check: **"Require a pull request before merging"**
- [ ] ✅ Check: **"Require approvals"**
  - [ ] Set number: **1** (minimum approvals)
- [ ] ✅ Check: **"Dismiss stale pull request approvals when new commits are pushed"**
- [ ] ⬜ Optional: **"Require review from Code Owners"** (check if you have CODEOWNERS file)

**Visual guide**:
```
Pull request section looks like:
┌─────────────────────────────────────────┐
│ ☑ Require a pull request before merge   │
│ ☑ Require approvals                     │
│   Required number of approvals: [1 ▼]   │
│ ☑ Dismiss stale approvals               │
│ ☐ Require CODEOWNERS review             │
└─────────────────────────────────────────┘
```

---

### Step 5: Require Conversation Resolution (Optional)

**Section**: "Rules"

- [ ] ✅ Check: **"Require conversation resolution before merging"** (recommended)

This prevents merging if there are unresolved comments.

---

### Step 6: Auto-Delete Branches

**Section**: "Rules"

- [ ] ✅ Check: **"Automatically delete head branches"**

This cleans up feature branches automatically after merge.

**Visual guide**:
```
┌─────────────────────────────────────────┐
│ ☑ Require conversation resolution       │
│ ☑ Automatically delete head branches     │
└─────────────────────────────────────────┘
```

---

### Step 7: Restrict Who Can Push (SKIP - not needed)

**Section**: "Restrict who can push to matching branches"

- [ ] ⬜ Leave unchecked (unless you have specific access control needs)

---

### Step 8: Include Administrators

**Section**: Bottom of form

- [ ] ✅ Check: **"Include administrators"**

This ensures admins follow the same rules (no direct commits to `develop`).

---

### Step 9: Create the Rule

- [ ] Click blue **"Create"** button at bottom right

**Expected result**: You should see a success message and the new rule listed under "Branch protection rules"

---

## Verification Checklist

Once rule is created, verify it works:

- [ ] **Test 1**: Try to push directly to `develop` branch
  - Expected: Git push rejected with "protected branch" message

- [ ] **Test 2**: Create PR without approval
  - Expected: "Merge" button disabled with message "1 approval required"

- [ ] **Test 3**: Create PR, add approval, then merge
  - Expected: Should merge successfully (if CI passes)

---

## Rule Summary

After configuration, `develop` branch will enforce:

| Rule | Status |
|------|--------|
| All CI checks must pass (Android + iOS) | ✅ Required |
| All changes through PR (no direct commits) | ✅ Required |
| Minimum 1 approval | ✅ Required |
| Branch up to date with main | ✅ Required |
| Stale approvals auto-dismissed | ✅ Enabled |
| Feature branches auto-deleted | ✅ Enabled |
| Admins follow same rules | ✅ Enabled |

---

## Next Steps

1. **Complete this setup** following the checklist above
2. **Run verification tests** to confirm rules work
3. **Update this doc** with the date completed
4. **Mark KIM-97 as complete** in Linear
5. **Move to KIM-96**: Agent PR workflow automation (now unblocked)

---

## Troubleshooting

**"Status checks not appearing?"**
- Ensure CI workflows have run successfully on `develop` branch
- The checks must exist in workflow history before they appear in the dropdown

**"Can't find the Branches section?"**
- Make sure you're in Settings (not Code tab)
- Scroll down in left sidebar if needed

**"Need to edit the rule later?"**
- Go back to Settings → Branches
- Click the pencil icon (Edit) next to the rule
- Make changes and click "Save changes"

---

## Completed Date

- **Started**: Mar 12, 2026
- **Completed**: _________________

**Verified by**: _________________
