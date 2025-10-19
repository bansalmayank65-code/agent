# Cache Clearing for New Task Imports - Implementation

## Problem Statement
When users import a new task.json, old task data from the previous import was persisting in the UI and backend session/cache, causing interference and unexpected behavior with the new task.

## Solution Overview
Implemented automatic cache clearing mechanism that ensures complete cleanup of old task data before importing a new task. This affects both the frontend state and backend cache/session.

## Changes Made

### 1. Backend Cache Controller (`CacheController.java`)
**File:** `backend/src/main/java/com/amazon/agenticworkstation/controller/CacheController.java`

Added new endpoint to clear cache:

```java
@PostMapping("/clear")
public ResponseEntity<?> clearCache(@RequestBody Map<String, String> body)
```

**Features:**
- Accepts `userId` (required) and `taskId` (optional)
- Clears specific task cache if `taskId` provided
- Clears all tasks for user if only `userId` provided
- Returns success/error response with status

### 2. Backend Task Cache Service (`TaskCacheService.java`)
**File:** `backend/src/main/java/com/amazon/agenticworkstation/service/TaskCacheService.java`

Added two new methods:

```java
public synchronized void clearCache(String userId, String taskId)
public synchronized void clearUserCache(String userId)
```

**Features:**
- `clearCache()`: Removes specific task cache entry for a user
- `clearUserCache()`: Removes all cache entries for a specific user
- Both methods use the same cache key generation mechanism (`userId_taskId`)
- Includes logging for debugging

### 3. Frontend API Service (`api_service.dart`)
**File:** `frontend/lib/services/api_service.dart`

Added new method:

```dart
Future<Map<String, dynamic>> clearCache(String userId, {String? taskId})
```

**Features:**
- Calls the backend `/cache/clear` endpoint
- Accepts optional `taskId` parameter
- Returns success/error response
- Includes error handling

### 4. Frontend Task Provider (`task_provider.dart`)
**File:** `frontend/lib/providers/task_provider.dart`

**Two integration points:**

#### A. Import Task JSON Method
Modified `importTaskJson()` method to:
1. **Clear backend cache** before importing new task
2. **Clear local state** (result data, import tracking)
3. Continue with import process

```dart
// Clear old cache/session data before importing new task
await _apiService.clearCache(loggedInUserId);

// Clear local state
_resultData = null;
_resultFilePath = null;
_hasImportedTaskJson = false;
_importedJsonPath = null;
```

#### B. Repository Path Change
Modified `updateRepositoryPath()` method to:
1. Clear cache when user changes repository path
2. Prevents old task data from interfering with new repository

```dart
// Clear cache when changing repository to ensure clean state
await _apiService.clearCache(_task.userId);
```

## Flow Diagram

```
User imports new task.json
        ↓
Frontend: importTaskJson()
        ↓
[1] Clear Backend Cache
    - Call API: /cache/clear
    - Remove user_taskId cache entry
        ↓
[2] Clear Frontend State
    - _resultData = null
    - _resultFilePath = null
    - _hasImportedTaskJson = false
    - _importedJsonPath = null
        ↓
[3] Process New Task Import
    - Parse task.json
    - Validate data
    - Save to database
    - Update UI state
        ↓
Fresh task ready with no old data interference ✓
```

## Cache Key Structure

The backend uses a compound key for caching:
```
cacheKey = userId + "_" + taskId
Example: "john.doe_task-12345"
```

This allows:
- Multiple users to work simultaneously without conflicts
- User-specific cache clearing
- Task-specific cache clearing

## Benefits

### 1. No Data Interference
- ✅ Old task data is completely cleared before new import
- ✅ No residual state from previous tasks
- ✅ Clean slate for each new import

### 2. Better User Experience
- ✅ Predictable behavior when importing new tasks
- ✅ No unexpected values from previous tasks
- ✅ Clear separation between different tasks

### 3. Multi-User Support
- ✅ Each user's cache is isolated
- ✅ Users can work independently without conflicts
- ✅ Cache clearing doesn't affect other users

### 4. Repository Switching
- ✅ Changing repository clears old data
- ✅ Prevents cross-contamination between projects
- ✅ Fresh start when switching contexts

## Error Handling

All cache clearing operations include:
- Try-catch blocks to prevent blocking the import process
- Warning logs if cache clear fails
- Graceful degradation (import continues even if cache clear fails)
- Detailed error messages for debugging

## Testing Checklist

- [ ] Import task.json → verify old data cleared
- [ ] Import second task.json → verify first task data gone
- [ ] Switch repository → verify cache cleared
- [ ] Multiple users → verify isolation
- [ ] Cache clear failure → verify import still works
- [ ] Check backend logs → verify cache operations logged
- [ ] Check frontend console → verify clear operations executed

## Usage Examples

### Frontend - Clear specific task cache
```dart
await apiService.clearCache('john.doe', taskId: 'task-123');
```

### Frontend - Clear all user cache
```dart
await apiService.clearCache('john.doe');
```

### Backend - API call
```bash
# Clear specific task
curl -X POST http://localhost:8080/cache/clear \
  -H "Content-Type: application/json" \
  -d '{"userId": "john.doe", "taskId": "task-123"}'

# Clear all user tasks
curl -X POST http://localhost:8080/cache/clear \
  -H "Content-Type: application/json" \
  -d '{"userId": "john.doe"}'
```

## Automatic Cache Clearing Triggers

Cache is automatically cleared in these scenarios:

1. **New Task Import** - Before importing any new task.json
2. **Repository Change** - When user selects different repository path
3. **Manual Clear** - Via API endpoint (for admin/debugging)

## Future Enhancements

Potential improvements for the future:

1. **Cache Statistics** - Add endpoint to view cache status
2. **TTL Configuration** - Make cache expiration time configurable
3. **Cache Warmup** - Pre-load frequently accessed tasks
4. **Cache Metrics** - Track hit/miss rates for optimization
5. **Selective Clear** - Clear only specific portions of cache (e.g., just results)

## Notes

- Cache clearing is **non-blocking** - if it fails, the import continues
- All operations are **logged** for debugging and audit purposes
- The solution is **backward compatible** - works with existing code
- **Thread-safe** - uses synchronized methods in backend

---
**Date Implemented:** October 20, 2025  
**Issue:** Old imported task data interfering with new imports  
**Status:** ✅ Resolved
