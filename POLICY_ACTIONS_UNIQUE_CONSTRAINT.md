# Policy Actions Builder - Unique Constraint Implementation

## Overview
Added a unique constraint to ensure that each combination of `env_name`, `interface_num`, `policy_cat1`, and `policy_cat2` is unique in the database. The UI now provides clear feedback when users attempt to create duplicate policy actions.

## Changes Made

### 1. Database Schema (`schema.sql`)
**Location:** `backend/src/main/resources/schema.sql`

**Added:**
```sql
-- Unique constraint to prevent duplicate policy combinations
ALTER TABLE policy_actions 
ADD CONSTRAINT uk_policy_actions_combination 
UNIQUE (env_name, interface_num, policy_cat1, policy_cat2);
```

**Purpose:** Enforces uniqueness at the database level to prevent duplicate policy combinations.

---

### 2. Entity Layer (`PolicyActionEntity.java`)
**Location:** `backend/src/main/java/com/amazon/agenticworkstation/entity/PolicyActionEntity.java`

**Added:**
```java
@Table(name = "policy_actions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_policy_actions_combination",
            columnNames = {"env_name", "interface_num", "policy_cat1", "policy_cat2"}
        )
    }
)
```

**Purpose:** JPA-level constraint definition that matches the database constraint.

---

### 3. Repository Layer (`PolicyActionRepository.java`)
**Location:** `backend/src/main/java/com/amazon/agenticworkstation/repository/PolicyActionRepository.java`

**Added Methods:**
```java
// Check if duplicate exists (for create operations)
boolean existsByEnvNameAndInterfaceNumAndPolicyCat1AndPolicyCat2(
    String envName, Integer interfaceNum, String policyCat1, String policyCat2);

// Find duplicates excluding a specific ID (for update operations)
@Query("SELECT p FROM PolicyActionEntity p WHERE p.envName = :envName AND p.interfaceNum = :interfaceNum " +
       "AND p.policyCat1 = :policyCat1 AND p.policyCat2 = :policyCat2 AND p.policyActionId != :excludeId")
List<PolicyActionEntity> findDuplicateExcludingId(
    @Param("envName") String envName,
    @Param("interfaceNum") Integer interfaceNum,
    @Param("policyCat1") String policyCat1,
    @Param("policyCat2") String policyCat2,
    @Param("excludeId") Long excludeId);
```

**Purpose:** Query methods to check for duplicates before saving.

---

### 4. Service Layer (`PolicyActionService.java`)
**Location:** `backend/src/main/java/com/amazon/agenticworkstation/service/PolicyActionService.java`

**Modified Methods:**

#### `createPolicyAction()` - Added duplicate check:
```java
// Check for duplicate combination
boolean exists = policyActionRepository.existsByEnvNameAndInterfaceNumAndPolicyCat1AndPolicyCat2(
    policyAction.getEnvName(),
    policyAction.getInterfaceNum(),
    policyAction.getPolicyCat1(),
    policyAction.getPolicyCat2()
);

if (exists) {
    throw new IllegalArgumentException(
        String.format("A policy action already exists for the combination: env_name='%s', interface_num=%d, policy_cat1='%s', policy_cat2='%s'",
            policyAction.getEnvName(), policyAction.getInterfaceNum(), 
            policyAction.getPolicyCat1(), policyAction.getPolicyCat2())
    );
}
```

#### `updatePolicyAction()` - Added duplicate check:
```java
// Check if the new combination conflicts with another record (excluding current ID)
List<PolicyActionEntity> duplicates = policyActionRepository.findDuplicateExcludingId(
    updatedPolicyAction.getEnvName(),
    updatedPolicyAction.getInterfaceNum(),
    updatedPolicyAction.getPolicyCat1(),
    updatedPolicyAction.getPolicyCat2(),
    id
);

if (!duplicates.isEmpty()) {
    throw new IllegalArgumentException(
        String.format("A policy action already exists for the combination: env_name='%s', interface_num=%d, policy_cat1='%s', policy_cat2='%s'",
            updatedPolicyAction.getEnvName(), updatedPolicyAction.getInterfaceNum(), 
            updatedPolicyAction.getPolicyCat1(), updatedPolicyAction.getPolicyCat2())
    );
}
```

**Purpose:** Fail-fast validation before database operations.

---

### 5. Frontend UI (`policy_actions_builder_screen.dart`)
**Location:** `frontend/lib/screens/policy_actions_builder_screen.dart`

**Enhanced Error Handling:**
```dart
catch (e) {
  String errorMsg = e.toString();
  
  // Check for duplicate policy combination error
  if (errorMsg.contains('already exists for the combination')) {
    errorMsg = 'This policy has already been added!\n\n' +
               'A policy action with the same Environment, Interface Number, ' +
               'Category 1, and Category 2 already exists in the database. ' +
               'Please use different values or edit the existing policy.';
  }
  
  _showError(errorMsg);
}
```

**User Experience:**
- Clear, non-technical error message
- Explains what went wrong
- Suggests next steps (use different values or edit existing)
- Uses dialog box for visibility

---

### 6. Migration Scripts

#### Migration SQL (`migration_add_unique_constraint.sql`)
**Location:** `backend/src/main/resources/migration_add_unique_constraint.sql`

- Checks if constraint already exists before adding
- Idempotent - safe to run multiple times
- Provides verification query
- Includes helpful RAISE NOTICE messages

#### Migration Batch Script (`run_unique_constraint_migration.bat`)
**Location:** `backend/src/main/resources/run_unique_constraint_migration.bat`

- Windows batch script for easy execution
- Prompts for PostgreSQL password
- Provides clear success/failure feedback
- Pauses at end to show results

---

## How to Apply Changes

### For Existing Databases
Run the migration script:
```bash
cd backend\src\main\resources
run_unique_constraint_migration.bat
```

Or manually execute:
```bash
psql -h localhost -p 5432 -U postgres -d agenticworkstation -f migration_add_unique_constraint.sql
```

### For New Installations
The constraint is already included in `schema.sql` and will be created automatically.

---

## Testing the Constraint

### Test Case 1: Create Duplicate
1. Add a policy action with:
   - env_name: "production"
   - interface_num: 123
   - policy_cat1: "security"
   - policy_cat2: "authentication"

2. Try to add another policy with the same values
3. **Expected:** Error message "This policy has already been added!"

### Test Case 2: Update to Duplicate
1. Create two different policies
2. Edit one policy to match the combination of the other
3. **Expected:** Error message "This policy has already been added!"

### Test Case 3: Edit Same Record
1. Create a policy
2. Edit the same policy (changing only description or actions_json)
3. **Expected:** Update succeeds (same record can keep its own combination)

### Test Case 4: Partial Match is OK
1. Create policy: prod/123/security/authentication
2. Create policy: prod/123/security/authorization
3. **Expected:** Both succeed (policy_cat2 is different)

---

## Error Messages

### Backend Error (technical)
```
A policy action already exists for the combination: env_name='production', interface_num=123, policy_cat1='security', policy_cat2='authentication'
```

### Frontend Error (user-friendly)
```
This policy has already been added!

A policy action with the same Environment, Interface Number, Category 1, and Category 2 already exists in the database. Please use different values or edit the existing policy.
```

---

## Database Constraint Details

**Constraint Name:** `uk_policy_actions_combination`

**Constraint Type:** UNIQUE

**Columns:** 
- env_name
- interface_num
- policy_cat1
- policy_cat2

**Behavior:**
- NULL values: Each NULL is considered unique (PostgreSQL standard behavior)
- Case sensitivity: String comparisons are case-sensitive
- Whitespace: Leading/trailing spaces are significant

---

## Implementation Notes

1. **Fail-Fast Approach:** Validation happens at multiple levels:
   - Application service layer (before database call)
   - Database constraint (as final safety net)

2. **Edit Mode Safety:** When editing, the constraint checks exclude the current record ID, allowing users to modify other fields without triggering false positives.

3. **User Experience:** Error messages are clear and actionable, guiding users on what to do next.

4. **Data Integrity:** The database constraint ensures data integrity even if direct database access bypasses the application layer.

5. **Idempotent Migration:** The migration script can be run multiple times safely without errors.

---

## Rollback (if needed)

To remove the unique constraint:

```sql
ALTER TABLE policy_actions 
DROP CONSTRAINT IF EXISTS uk_policy_actions_combination;
```

---

## Related Files

- `backend/src/main/resources/schema.sql`
- `backend/src/main/java/com/amazon/agenticworkstation/entity/PolicyActionEntity.java`
- `backend/src/main/java/com/amazon/agenticworkstation/repository/PolicyActionRepository.java`
- `backend/src/main/java/com/amazon/agenticworkstation/service/PolicyActionService.java`
- `frontend/lib/screens/policy_actions_builder_screen.dart`
- `backend/src/main/resources/migration_add_unique_constraint.sql`
- `backend/src/main/resources/run_unique_constraint_migration.bat`

---

## Summary

✅ Database-level unique constraint added  
✅ JPA entity constraint annotation added  
✅ Service layer duplicate validation implemented  
✅ User-friendly error messages in UI  
✅ Idempotent migration script created  
✅ Windows batch script for easy migration  
✅ Documentation completed

The implementation ensures data integrity while providing an excellent user experience with clear, actionable error messages.
