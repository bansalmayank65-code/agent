# Task Refiner Tool - Implementation Summary

## Overview
Created a new utility tool "Refine task.json" similar to the HR Expert Interface Changer that performs automated refinement operations on task.json files.

## Features
The tool performs the following operations on task.json files:

1. **Merge Duplicate Actions** - Identifies and removes duplicate actions, keeping only the first occurrence
2. **Move Audit Log Actions to End** - Relocates all `*_audit_logs` actions to the end while preserving their relative order
3. **Add/Fix Edges** - Uses the existing EdgeGenerator service to calculate edges based on action dependencies
4. **Fix num_of_edges** - Updates the `num_of_edges` field based on the calculated edges

## Implementation Details

### Backend Components

#### 1. TaskRefinementService.java
**Location:** `backend/src/main/java/com/amazon/agenticworkstation/service/TaskRefinementService.java`

**Key Methods:**
- `refineTask(Map<String, Object> taskData)` - Main refinement method
- `mergeDuplicateActions()` - Removes duplicate actions based on name and arguments
- `moveAuditLogsToEnd()` - Reorders actions to move audit logs to end
- `createActionSignature()` - Creates unique signature for duplicate detection
- `isAuditLogAction()` - Identifies audit log actions by name patterns

**Audit Log Detection Patterns:**
- `audit_log`
- `audit_trail`
- `create_audit`
- `insert_audit`
- `manage_audit`
- `handle_audit`
- `process_audit`
- `administer_audit`
- `execute_audit`

#### 2. TaskRefinementController.java
**Location:** `backend/src/main/java/com/amazon/agenticworkstation/controller/TaskRefinementController.java`

**Endpoints:**
- `POST /api/refine-task` - Main refinement endpoint
- `GET /api/refine-task/health` - Health check endpoint

**Request Format:**
```json
{
  "task": {
    "actions": [...],
    ...
  }
}
```
OR
```json
{
  "actions": [...],
  ...
}
```

**Response Format:**
```json
{
  "success": true,
  "refined_task": {...},
  "message": "Task refined successfully"
}
```

### Frontend Components

#### 1. TaskRefinerScreen.dart
**Location:** `frontend/lib/screens/task_refiner_screen.dart`

**Features:**
- Side-by-side JSON viewer (Before/After)
- Real-time refinement via backend API
- Loading state during processing
- Error handling with user-friendly messages
- Info footer explaining refinement operations
- Can be used standalone or embedded

**UI Design:**
- Follows same pattern as HR Expert Interface Changer
- Uses JsonEditorViewer widget for JSON editing
- Material Design with consistent color scheme
- Responsive layout

#### 2. ApiService.dart Updates
**Location:** `frontend/lib/services/api_service.dart`

**New Method:**
```dart
Future<Map<String, dynamic>> refineTask(dynamic taskData)
```

### Navigation Integration

#### 1. navigation_service.dart
**Location:** `frontend/lib/services/home_screen/navigation_service.dart`

- Added "Refine task.json" to utility tools section
- Icon: `Icons.auto_fix_high`
- Section key: `task_refiner`

#### 2. content_router_service.dart
**Location:** `frontend/lib/services/home_screen/content_router_service.dart`

- Added routing case for index 13
- Renders TaskRefinerScreen in standalone mode

#### 3. main_screen.dart
**Location:** `frontend/lib/screens/main_screen.dart`

- Added menu item for mobile view
- Added navigation methods for both tools
- Updated menu selection handler

## Usage

### From Left Navigation (New UI)
1. Click on "Refine task.json" in the left navigation menu
2. Paste task.json into the left editor
3. Click "Refine" button
4. View refined task.json in the right editor

### From Main Screen Menu (Old UI)
1. Click the menu (⋮) in top right
2. Select "Refine task.json"
3. Follow same process as above

## Input Requirements
- Task must contain an `actions` field (mandatory)
- Other fields (instruction, user_id, outputs, etc.) are optional
- Supports both nested format `{ "task": {...} }` and flat format `{ "actions": [...] }`

## Processing Steps
1. **Validation** - Ensures task contains actions array
2. **Merge Duplicates** - Removes duplicate actions based on signature
3. **Reorder Actions** - Moves audit log actions to end
4. **Generate Edges** - Uses EdgeGenerator to calculate dependencies
5. **Update Count** - Sets num_of_edges based on generated edges

## Error Handling
- Invalid JSON detection
- Missing actions validation
- Backend API error handling
- User-friendly error messages via SnackBar

## Coding Guidelines Followed
✅ Same UI design as HR Expert Interface Changer
✅ Side-by-side JSON viewer
✅ Fail-fast validation
✅ No database or cache usage (in-memory processing)
✅ Consistent color scheme and styling
✅ Proper error handling
✅ Loading states for async operations

## Files Created
1. `backend/src/main/java/com/amazon/agenticworkstation/service/TaskRefinementService.java`
2. `backend/src/main/java/com/amazon/agenticworkstation/controller/TaskRefinementController.java`
3. `frontend/lib/screens/task_refiner_screen.dart`

## Files Modified
1. `frontend/lib/services/api_service.dart` - Added refineTask method
2. `frontend/lib/services/home_screen/navigation_service.dart` - Added navigation item
3. `frontend/lib/services/home_screen/content_router_service.dart` - Added routing
4. `frontend/lib/screens/main_screen.dart` - Added menu items and navigation

## Testing Recommendations
1. Test with task.json containing duplicate actions
2. Test with task.json containing audit log actions
3. Test with malformed JSON
4. Test with missing actions field
5. Test with both nested and flat formats
6. Test edge generation with various action sequences

## Future Enhancements
- Add option to download refined task.json
- Add diff viewer to highlight changes
- Add statistics (e.g., "Merged 3 duplicates, moved 2 audit logs")
- Add undo/redo functionality
- Add batch processing for multiple files
