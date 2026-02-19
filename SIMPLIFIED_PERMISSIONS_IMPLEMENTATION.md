# Simplified Permissions System Implementation

## Overview
The permission system has been simplified from 16+ permissions down to 5 core permissions as requested by the user.

## 🎯 The 5 Core Permissions

### 1. **Create Assessments** ✅ Always Enabled
- **Description**: Main feature - allows users to create new assessments
- **Default**: Always TRUE, cannot be disabled
- **UI**: Shown as disabled/grayed checkbox with explanation
- **Applies to**: All users (standalone and enterprise)

### 2. **View Own Assessments** ✅ Always Enabled  
- **Description**: Users can view their own assessment history
- **Default**: Always TRUE, cannot be disabled
- **UI**: Shown as disabled/grayed checkbox with explanation
- **Applies to**: All users (standalone and enterprise)

### 3. **View Department Assessments** (Optional)
- **Description**: View assessments from the same department (includes own assessments)
- **Default**: FALSE
- **Hierarchy**: Auto-selects "View Own Assessments" when enabled
- **UI Logic**: If unchecked, also unchecks "View All Assessments"

### 4. **View All Assessments** (Optional)
- **Description**: View all assessments within the enterprise
- **Default**: FALSE  
- **Hierarchy**: Auto-selects both "View Department" and "View Own" when enabled
- **UI Logic**: When checked, automatically ticks "View Department Assessments" too

### 5. **FDA 483 Analysis Access** (Optional)
- **Description**: Show/hide FDA 483 Analysis tab in bottom navigation
- **Default**: FALSE
- **Effect**: Controls visibility of FDA 483 bottom bar item
- **Applies to**: Enterprise users only (standalone users always have access)

## 📋 Implementation Status

### ✅ Completed
1. **Data Model** (`UserPermission.kt`)
   - Removed 11 old permission fields
   - Kept only 5 core permissions
   - Updated documentation

2. **Create Invitation Screen** (`CreateInvitationScreen.kt`)
   - Simplified permission UI with 2 sections:
     - Core Permissions (always enabled, grayed out)
     - Optional Permissions (3 checkboxes)
   - Implemented auto-select logic with `LaunchedEffect`
   - Visual feedback for hierarchical selections
   - Updated `CreateInvitationData` class

3. **Enterprise Admin ViewModel** (`EnterpriseAdminViewModel.kt`)
   - Updated `createInvitationWithPermissions()` signature
   - Simplified permission object creation
   - Updated `updateUserPermissions()` signature

4. **Permission Checker** (`PermissionChecker.kt`)
   - Removed 11 old helper functions
   - Kept only 5 core permission checks
   - Added `canAccessFda483Analysis()` function
   - Updated `getAssessmentViewScope()` logic
   - Removed `AssessmentViewScope.NONE` (users always have at least OWN)

5. **Firestore Security Rules** (`FIRESTORE_RULES.txt`)
   - Updated `canCreateAssessments()` - now always true
   - Updated `canViewOwnAssessments()` - now always true
   - Removed `canAccessDomain()` function
   - Added `canAccessFda483Analysis()` function

6. **Bottom Navigation** (`BottomNavigation.kt`)
   - Added `user` and `permissions` parameters
   - Filters FDA 483 tab based on `canAccessFda483Analysis()`
   - Standalone users always see FDA 483
   - Enterprise users only see it if permission is granted

7. **Updated Screens with Permission-aware Bottom Bar**
   - ✅ `HomeScreen.kt` - passes `currentUser` and `currentUserPermissions`
   - ✅ `HistoryScreen.kt` - added `AuthViewModel`, passes user/permissions
   - ⚠️ **Remaining screens need similar updates** (see below)

### ⚠️ Remaining Work

#### Bottom Navigation Updates Needed
The following screens need `AuthViewModel` added and user/permissions passed to `BottomNavigationBar`:

```kotlin
// Pattern to follow:
fun YourScreen(
    ...existing params...,
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserPermissions by authViewModel.currentUserPermissions.collectAsState()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "your_route",
                onNavigate = { ... },
                user = currentUser,
                permissions = currentUserPermissions
            )
        }
    ) { ... }
}
```

**Files to update:**
- `SubDomainScreen.kt`
- `CustomAssessmentScreen.kt`
- `PersonalDetailsScreen.kt`
- `Fda483MainScreen.kt`
- `Fda483DetailScreen.kt`
- `SuperAdminDashboardScreen.kt`
- `EnterpriseAdminDashboardScreen.kt`
- `SuperAdminTrainingResourcesScreen.kt`

#### Edit User Permissions Dialog
`PermissionManagementDialog.kt` needs to be updated to:
1. Show only 5 permissions (not 16+)
2. Display "Create Assessments" and "View Own" as disabled (always true)
3. Implement same auto-select logic as Create Invitation screen
4. Remove all old permission checkboxes
5. Update the "Domains" tab or remove it entirely (no longer used)
6. Simplify the "Advanced" tab to only show FDA 483 access

**Suggested approach:**
- Create a new `SimplifiedPermissionManagementDialog.kt` file
- Copy the structure from `CreateInvitationScreen.kt` permission section
- Adapt it to work with existing `UserPermission` instead of `CreateInvitationData`

## 🔄 Auto-Select Logic

The permission hierarchy is implemented using `LaunchedEffect` in Compose:

```kotlin
// Auto-select "View Department" when "View All" is checked
LaunchedEffect(canViewAllAssessments) {
    if (canViewAllAssessments) {
        canViewDepartmentAssessments = true
    }
}

// Auto-uncheck "View All" when "View Department" is unchecked
LaunchedEffect(canViewDepartmentAssessments) {
    if (!canViewDepartmentAssessments && canViewAllAssessments) {
        canViewAllAssessments = false
    }
}
```

This provides **immediate visual feedback** to the admin, making the permission hierarchy clear.

## 🎨 UI Guidelines

### Core Permissions Card
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    Text("Core Permissions (Always Enabled)")
    
    PermissionCheckbox(
        checked = true,
        onCheckedChange = { }, // No-op
        text = "1. Create Assessments",
        description = "Main feature - always enabled",
        enabled = false // Grayed out
    )
    
    PermissionCheckbox(
        checked = true,
        onCheckedChange = { },
        text = "2. View Own Assessments",
        description = "Always enabled",
        enabled = false
    )
}
```

### Optional Permissions
```kotlin
PermissionCheckbox(
    checked = canViewDepartmentAssessments,
    onCheckedChange = { 
        canViewDepartmentAssessments = it
        if (!it) canViewAllAssessments = false
    },
    text = "3. View Department Assessments",
    description = "Includes own assessments"
)

PermissionCheckbox(
    checked = canViewAllAssessments,
    onCheckedChange = { 
        canViewAllAssessments = it
        if (it) canViewDepartmentAssessments = true
    },
    text = "4. View All Assessments",
    description = "Auto-selects department + own"
)

PermissionCheckbox(
    checked = canAccessFda483Analysis,
    onCheckedChange = { canAccessFda483Analysis = it },
    text = "5. FDA 483 Analysis Access",
    description = "Show FDA 483 tab in navigation"
)
```

## 🧪 Testing Checklist

### Enterprise Admin Tests
- [ ] Create new user with only core permissions (nothing checked)
  - User should see: Home, History, Resources, Profile (NO FDA 483)
  - User can create assessments ✅
  - User can only see their own history ✅

- [ ] Create user with "View Department" checked
  - Should auto-check "View Department" in UI
  - User sees own + department assessments ✅

- [ ] Create user with "View All" checked
  - Should auto-check both "View Department" and "View Own" in UI
  - User sees all enterprise assessments ✅

- [ ] Create user with FDA 483 access
  - User sees FDA 483 tab in bottom bar ✅
  - Can access FDA 483 analysis feature ✅

- [ ] Uncheck "View Department" when "View All" is selected
  - Should auto-uncheck "View All" ✅

### Standalone User Tests
- [ ] Standalone user always sees FDA 483 tab ✅
- [ ] Standalone user can always create assessments ✅
- [ ] Standalone user only sees own assessments (no department/enterprise) ✅

### Edit Permissions Tests
- [ ] Edit existing user's permissions
  - Changes reflect immediately after save
  - User's app refreshes permissions without re-login
  - Bottom bar updates if FDA 483 access changed

## 📝 Migration Notes

### Database Migration
Existing `user_permissions` documents in Firestore may have old permission fields. The app will gracefully handle this:

1. Old fields are ignored (not read)
2. New fields default to:
   - `canCreateAssessments`: `true`
   - `canViewOwnAssessments`: `true`
   - `canViewDepartmentAssessments`: `false`
   - `canViewAllAssessments`: `false`
   - `canAccessFda483Analysis`: `false`

3. When permissions are next updated, only new fields are saved

### Backward Compatibility
- Firestore rules use `.get('field', defaultValue)` syntax
- Missing fields return the default value
- No breaking changes for existing users

## 🔒 Security Rules Summary

```javascript
// Core permissions - always true
function canCreateAssessments() {
  return true; // All users can create
}

function canViewOwnAssessments() {
  return true; // All users can view own
}

// Optional permissions
function canViewAllAssessments() {
  return isAdmin() || isEnterpriseAdmin() || 
         (hasUserPermission() && getUserPermission().get('canViewAllAssessments', false));
}

function canViewDepartmentAssessments() {
  return hasUserPermission() && getUserPermission().get('canViewDepartmentAssessments', false);
}

function canAccessFda483Analysis() {
  return isAdmin() || 
         isEnterpriseAdmin() || 
         getUserEnterprise() == '' ||  // Standalone users
         (hasUserPermission() && getUserPermission().get('canAccessFda483Analysis', false));
}
```

## 🚀 Future Enhancements (Not Implemented)

If more permissions are needed in the future, follow this pattern:

1. Add field to `UserPermission` data class
2. Add UI checkbox in `CreateInvitationScreen`
3. Add corresponding function in `PermissionChecker`
4. Update Firestore rules helper function
5. Apply permission check where needed in app

Keep the number small (5-7 max) to maintain usability!

## 📂 Files Modified

1. `app/src/main/java/com/example/validator/data/models/UserPermission.kt`
2. `app/src/main/java/com/example/validator/ui/screens/CreateInvitationScreen.kt`
3. `app/src/main/java/com/example/validator/viewmodel/EnterpriseAdminViewModel.kt`
4. `app/src/main/java/com/example/validator/utils/PermissionChecker.kt`
5. `app/src/main/java/com/example/validator/ui/components/BottomNavigation.kt`
6. `app/src/main/java/com/example/validator/ui/screens/HomeScreen.kt`
7. `app/src/main/java/com/example/validator/ui/screens/HistoryScreen.kt`
8. `FIRESTORE_RULES.txt`

## ⚠️ Files Needing Updates

1. `app/src/main/java/com/example/validator/ui/components/PermissionManagementDialog.kt` ⚠️ HIGH PRIORITY
2. `app/src/main/java/com/example/validator/ui/screens/SubDomainScreen.kt`
3. `app/src/main/java/com/example/validator/ui/screens/CustomAssessmentScreen.kt`
4. `app/src/main/java/com/example/validator/ui/screens/PersonalDetailsScreen.kt`
5. `app/src/main/java/com/example/validator/ui/screens/Fda483MainScreen.kt`
6. `app/src/main/java/com/example/validator/ui/screens/Fda483DetailScreen.kt`
7. `app/src/main/java/com/example/validator/ui/screens/SuperAdminDashboardScreen.kt`
8. `app/src/main/java/com/example/validator/ui/screens/EnterpriseAdminDashboardScreen.kt`
9. `app/src/main/java/com/example/validator/ui/screens/SuperAdminTrainingResourcesScreen.kt`

---

**Last Updated**: November 27, 2025
**Status**: 80% Complete - Core functionality implemented, UI polish needed









