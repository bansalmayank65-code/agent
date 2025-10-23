import 'dart:convert';

const String _hrTalentManagementMappingJson = r'''
{
  "environment": "hr_talent_management",
  "description": "Method mappings between different interfaces for HR Talent Management environment.",
  "version": "1.0",
  "last_updated": "2025-10-23",
  "interface_count": 5,
  "total_methods_per_interface": 35,
  "is_audit_log": true,
  "method_categories": {
    "management_operations": {
      "description": "CRUD operations for various HR talent management entities",
      "is_crud": true
    },
    "discovery_operations": {
      "description": "Search and query operations for finding talent-related entities",
      "is_crud": false
    },
    "approval_operations": {
      "description": "Approval workflow validation and processing for talent operations",
      "is_crud": false
    },
    "human_transfer": {
      "description": "Operations to transfer control to human agents",
      "is_crud": false
    }
  },
  "interfaces": {
    "interface_1": {
      "prefix_patterns": {
        "management": "Manage",
        "discovery": "Discover",
        "approval": "Check",
        "human_transfer": "TransferTo"
      }
    },
    "interface_2": {
      "prefix_patterns": {
        "management": "Handle",
        "discovery": "Search",
        "approval": "Validate",
        "human_transfer": "SwitchTo"
      }
    },
    "interface_3": {
      "prefix_patterns": {
        "management": "Process",
        "discovery": "Find",
        "approval": "Verify",
        "human_transfer": "EscalateTo"
      }
    },
    "interface_4": {
      "prefix_patterns": {
        "management": "Administer",
        "discovery": "Lookup",
        "approval": "Confirm",
        "human_transfer": "HandOverTo"
      }
    },
    "interface_5": {
      "prefix_patterns": {
        "management": "Execute",
        "discovery": "Retrieve",
        "approval": "Authenticate",
        "human_transfer": "RouteTo"
      }
    }
  },
  "method_mappings": {
    "manage_application_operations": {
      "category": "management_operations",
      "description": "Manage job applications from candidates",
      "interface_1": "manage_application_operations",
      "interface_2": "handle_application_operations",
      "interface_3": "process_application_operations",
      "interface_4": "administer_application_operations",
      "interface_5": "execute_application_operations"
    },
    "manage_benefit_enrollment_operations": {
      "category": "management_operations",
      "description": "Manage employee benefit enrollment processes",
      "interface_1": "manage_benefit_enrollment_operations",
      "interface_2": "handle_benefit_enrollment_operations",
      "interface_3": "process_benefit_enrollment_operations",
      "interface_4": "administer_benefit_enrollment_operations",
      "interface_5": "execute_benefit_enrollment_operations"
    },
    "manage_benefit_plan_operations": {
      "category": "management_operations",
      "description": "Manage benefit plans and configurations",
      "interface_1": "manage_benefit_plan_operations",
      "interface_2": "handle_benefit_plan_operations",
      "interface_3": "process_benefit_plan_operations",
      "interface_4": "administer_benefit_plan_operations",
      "interface_5": "execute_benefit_plan_operations"
    }
  }
}
''';

Map<String, dynamic> loadHrTalentManagementMapping() {
  return jsonDecode(_hrTalentManagementMappingJson) as Map<String, dynamic>;
}