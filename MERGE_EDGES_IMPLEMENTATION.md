# Merge Edges Tool Implementation

## Overview
Added a new "Merge Edges" tool that merges duplicate edges in task.json files. This tool follows the same coding guidelines and UI design pattern as the HR Expert Interface Changer, using a side-by-side JSON viewer.

## Features
- **Side-by-side JSON viewer**: View original and merged task.json files simultaneously
- **Backend edge merging logic**: Uses the proven merging algorithm from EdgeGenerator.java
- **Maintains input-output pairing**: Preserves the logical connections between inputs and outputs
- **In-memory processing**: Fast, fail-fast operation without database dependencies
- **User-friendly interface**: Clear visual feedback and error messages

## Implementation Details

### Backend Components

#### EdgeMergerController.java
- **Location**: `backend/src/main/java/com/amazon/agenticworkstation/controller/EdgeMergerController.java`
- **Endpoint**: `POST /api/edges/merge-duplicate-edges`
- **Functionality**:
  - Accepts a full task.json as input
  - Removes exact duplicate edges (same from, to, and connections)
  - Merges edges with the same "from" and "to" values by combining their inputs and outputs
  - Returns the modified task.json with merged edges
  - Updates the `num_edges` field automatically

#### Merging Algorithm
The merging follows a two-step process (from EdgeGenerator.java):

1. **Step 1: Remove Exact Duplicates**
   - Identifies edges that are completely identical (same from, to, and connection details)
   - Keeps only the first occurrence of each duplicate

2. **Step 2: Merge Edges with Same From/To**
   - For edges with identical "from" and "to" values but different connections
   - Combines their input and output fields while maintaining proper pairing
   - Avoids duplicate input-output pairs

**Example:**
```
Before:
  Edge 1: from="action1", to="action2", outputs="skill_id", inputs="reference_id"
  Edge 2: from="action1", to="action2", outputs="email", inputs="user_email"

After Merge:
  Edge: from="action1", to="action2", outputs="skill_id, email", inputs="reference_id, user_email"
```

### Frontend Components

#### EdgeMergerScreen.dart
- **Location**: `frontend/lib/screens/edge_merger_screen.dart`
- **Features**:
  - Side-by-side JSON editor using `JsonEditorViewer` widget
  - "Merge Edges" button that calls the backend API
  - Validation of input JSON before sending to backend
  - Clear success/error messages with statistics
  - Loading indicator during processing

#### Navigation Integration
The tool is accessible from multiple locations:

1. **Home Screen Navigation Panel** (left sidebar)
   - Listed under "Utility tools section"
   - Position: After "HR Expert Interface Changer", before "Refine task.json"

2. **Main Screen Popup Menu** (mobile/desktop)
   - Available in the "more" menu (⋮)
   - Listed as "Merge Edges"

3. **Content Router**
   - Integrated into the main content routing system
   - Can be embedded in other layouts (standalone = false)

## Code Consistency

### Follows Same Pattern as HR Expert Interface Changer

1. **Controller Structure**:
   - Uses `@RestController` with dedicated endpoint
   - Returns `ResponseEntity<Map<String, Object>>` with success/error info
   - Includes detailed logging
   - Follows same error handling pattern

2. **Frontend Screen Structure**:
   - Stateless wrapper widget with `standalone` parameter
   - Internal stateless body widget
   - Side-by-side JSON editors
   - Controls row with action button
   - Same styling and color scheme

3. **Navigation Pattern**:
   - Added to NavigationService
   - Added to ContentRouterService
   - Added to MainScreen popup menu
   - Uses same navigation method pattern

4. **Error Handling**:
   - Fail-fast validation of input
   - Clear error messages for users
   - Graceful handling of API failures
   - Loading indicators during processing

## Files Modified/Created

### Created Files:
1. `backend/src/main/java/com/amazon/agenticworkstation/controller/EdgeMergerController.java`
2. `frontend/lib/screens/edge_merger_screen.dart`
3. `MERGE_EDGES_IMPLEMENTATION.md` (this file)

### Modified Files:
1. `frontend/lib/services/home_screen/navigation_service.dart`
   - Added NavItem for "Merge Edges"
   
2. `frontend/lib/services/home_screen/content_router_service.dart`
   - Added import for EdgeMergerScreen
   - Added case 13 for routing to EdgeMergerScreen
   - Renumbered subsequent cases
   
3. `frontend/lib/screens/main_screen.dart`
   - Added import for EdgeMergerScreen
   - Added `_navigateToEdgeMerger()` method
   - Added "merge_edges" to popup menu
   - Added "merge_edges" case to `_handleMenuSelection()`

## Usage

1. **Access the Tool**:
   - Click "Merge Edges" in the left navigation panel
   - Or select "Merge Edges" from the popup menu (⋮)

2. **Paste Task JSON**:
   - Copy your task.json content
   - Paste it into the left editor ("Before")

3. **Merge**:
   - Click the "Merge Edges" button
   - Wait for processing (loading indicator shown)

4. **Review Results**:
   - Merged task.json appears in the right editor ("After")
   - Success message shows original and merged edge counts
   - Copy the merged JSON for use in your project

## API Reference

### Endpoint
```
POST http://localhost:8080/api/edges/merge-duplicate-edges
```

### Request
```json
{
  "env": "hr_experts",
  "model_provider": "bedrock",
  "model": "claude-3-5-sonnet-v2",
  "task": {
    "user_id": "123",
    "instruction": "...",
    "actions": [...],
    "edges": [
      {
        "from": "action1",
        "to": "action2",
        "connection": {
          "output": "skill_id",
          "input": "reference_id"
        }
      },
      {
        "from": "action1",
        "to": "action2",
        "connection": {
          "output": "email",
          "input": "user_email"
        }
      }
    ],
    "outputs": [...],
    "num_edges": 2
  }
}
```

### Response (Success)
```json
{
  "success": true,
  "message": "Successfully merged edges. Original: 2, Merged: 1",
  "originalCount": 2,
  "mergedCount": 1,
  "task": {
    "env": "hr_experts",
    "model_provider": "bedrock",
    "model": "claude-3-5-sonnet-v2",
    "task": {
      "user_id": "123",
      "instruction": "...",
      "actions": [...],
      "edges": [
        {
          "from": "action1",
          "to": "action2",
          "connection": {
            "output": "skill_id, email",
            "input": "reference_id, user_email"
          }
        }
      ],
      "outputs": [...],
      "num_edges": 1
    }
  }
}
```

### Response (Error)
```json
{
  "success": false,
  "message": "Invalid task: task or edges are null"
}
```

## Testing

### Manual Testing Steps
1. Start the backend server: `mvn spring-boot:run`
2. Start the frontend: `flutter run -d web-server`
3. Navigate to "Merge Edges" tool
4. Paste a task.json with duplicate edges
5. Click "Merge Edges"
6. Verify the merged result

### Test Cases
1. **Valid task with duplicate edges**: Should merge successfully
2. **Valid task with no duplicates**: Should return same edges
3. **Invalid JSON**: Should show error message
4. **Missing edges field**: Should show error message
5. **Empty edges array**: Should return empty array

## Benefits

1. **Reduces Edge Redundancy**: Combines duplicate edges into single entries
2. **Maintains Data Integrity**: Preserves input-output pairing
3. **Improves Performance**: Fewer edges mean faster processing in evaluation
4. **Better Readability**: Cleaner task.json files are easier to understand
5. **Consistent with Existing Tools**: Follows established patterns and guidelines

## Future Enhancements

Potential improvements for future versions:
- Batch processing of multiple task files
- Visual diff highlighting between before/after
- Export merged results directly to file
- Integration with task validation workflow
- Statistics and analytics on merge operations
