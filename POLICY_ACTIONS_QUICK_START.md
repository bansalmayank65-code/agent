# Policy Actions Builder - Quick Start Guide

## Starting the Application

### 1. Backend Setup
```bash
cd backend
# The database table will be created automatically on first run
mvn spring-boot:run
```

### 2. Frontend Setup
```bash
cd frontend
flutter pub get
flutter run -d chrome
```

## Testing the Feature

### Step 1: Navigate to Policy Actions Builder
1. Open the application
2. Look for the hamburger menu or utility tools section
3. Click on "Policy Actions Builder" (first item in Utility Tools)

### Step 2: Add a Policy Action

1. You'll start in "Add Actions" mode by default
2. Fill in the form:
   - **Environment Name**: `hr_experts`
   - **Interface Number**: `1`
   - **Policy Category 1**: `employee_management`
   - **Policy Category 2**: `onboarding`
   - **Policy Description**: `Actions for new employee onboarding process`
   - **Actions JSON**: Paste the following sample JSON:

```json
{
  "actions": [
    {
      "action_id": "1",
      "name": "create_employee_record",
      "description": "Create a new employee record in the system",
      "required_params": ["employee_name", "email", "department"]
    },
    {
      "action_id": "2",
      "name": "assign_equipment",
      "description": "Assign laptop and other equipment",
      "required_params": ["employee_id", "equipment_list"]
    },
    {
      "action_id": "3",
      "name": "setup_access",
      "description": "Setup system access and credentials",
      "required_params": ["employee_id", "access_level"]
    }
  ]
}
```

3. Click "Add Policy Action"
4. You should see a green success message

### Step 3: Add Another Policy Action

Add a second policy action with different categories:
- **Environment Name**: `hr_experts`
- **Interface Number**: `1`
- **Policy Category 1**: `performance_review`
- **Policy Category 2**: `quarterly`
- **Policy Description**: `Actions for quarterly performance review process`
- **Actions JSON**:

```json
{
  "actions": [
    {
      "action_id": "1",
      "name": "schedule_review",
      "description": "Schedule performance review meeting",
      "required_params": ["employee_id", "reviewer_id", "date"]
    },
    {
      "action_id": "2",
      "name": "collect_feedback",
      "description": "Collect 360-degree feedback",
      "required_params": ["employee_id", "feedback_sources"]
    },
    {
      "action_id": "3",
      "name": "submit_rating",
      "description": "Submit performance rating",
      "required_params": ["employee_id", "rating", "comments"]
    }
  ]
}
```

### Step 4: View Policy Actions

1. Click on the "View Actions" tab
2. Select filters:
   - **Environment Name**: `hr_experts` (should be auto-selected)
   - **Interface Number**: `1` (should be auto-selected)
3. You should see both policy actions listed
4. Click to expand each card to view details
5. The Actions JSON should be displayed in a formatted viewer

### Step 5: Apply Filters

1. Select **Policy Category 1**: `employee_management`
2. The list should now show only the onboarding policy action
3. Select **Policy Category 2**: `onboarding`
4. Same result (since we only have one action in this category)
5. Clear filters by selecting "All" to see all actions again

### Step 6: Edit a Policy Action

1. In the list view, click "Edit" on the onboarding policy action
2. You'll be switched to edit mode with the form pre-filled
3. Modify the description: `Updated: Actions for new employee onboarding process`
4. Click "Update Policy Action"
5. You should see a green success message
6. Switch to "View Actions" to verify the change

### Step 7: Delete a Policy Action

1. In the list view, click "Delete" on one of the policy actions
2. A confirmation dialog will appear
3. Click "Delete" to confirm
4. The policy action should be removed from the list
5. Verify it's also removed from the database

## Validation Testing

### Test Fail-Fast Validation:

1. Go to "Add Actions" mode
2. Try to save without filling any fields - should show error
3. Fill only Environment Name, try to save - should show error
4. Fill required fields but enter invalid JSON in Actions JSON field:
   ```
   {invalid json}
   ```
5. Try to save - should show "Invalid JSON format" error
6. Enter valid JSON and save - should succeed

## Sample Policy Actions for Different Scenarios

### Leave Management
```json
{
  "env_name": "hr_experts",
  "interface_num": 2,
  "policy_cat1": "leave_management",
  "policy_cat2": "vacation",
  "policy_description": "Actions for vacation leave requests",
  "actions_json": {
    "actions": [
      {"action_id": "1", "name": "submit_leave_request"},
      {"action_id": "2", "name": "approve_leave"},
      {"action_id": "3", "name": "update_calendar"}
    ]
  }
}
```

### Payroll Processing
```json
{
  "env_name": "hr_experts",
  "interface_num": 1,
  "policy_cat1": "payroll",
  "policy_cat2": "monthly",
  "policy_description": "Actions for monthly payroll processing",
  "actions_json": {
    "actions": [
      {"action_id": "1", "name": "calculate_salary"},
      {"action_id": "2", "name": "process_deductions"},
      {"action_id": "3", "name": "generate_payslip"}
    ]
  }
}
```

## Expected Behavior

✅ **Add Actions**: Form validation, JSON validation, success feedback
✅ **View Actions**: Filtered list, expandable cards, JSON viewer
✅ **Edit Actions**: Pre-filled form, update capability
✅ **Delete Actions**: Confirmation dialog, removal from DB
✅ **Cascading Dropdowns**: Options updated based on selections
✅ **Error Handling**: Clear error messages for all failures

## Troubleshooting

### Backend not starting
- Check if PostgreSQL is running
- Verify database connection in application.yml
- Check for port conflicts (8080)

### Frontend not connecting
- Verify backend is running on localhost:8080
- Check browser console for errors
- Ensure CORS is properly configured

### Dropdowns empty
- Add at least one policy action first
- Refresh the page
- Check backend logs for errors

### JSON validation failing
- Use a JSON validator to check format
- Ensure proper quotes (double quotes for JSON)
- Remove trailing commas

## Database Verification

To verify data is being stored correctly:

```sql
-- Connect to PostgreSQL
psql -U postgres -d agenticworkstation

-- View all policy actions
SELECT * FROM policy_actions;

-- View distinct environment names
SELECT DISTINCT env_name FROM policy_actions;

-- Count by category
SELECT policy_cat1, COUNT(*) 
FROM policy_actions 
GROUP BY policy_cat1;
```

## Next Steps

After successful testing:
1. Add more policy actions for your specific scenarios
2. Integrate with other parts of the application
3. Export/Import functionality (future enhancement)
4. Policy action templates (future enhancement)
