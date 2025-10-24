import 'dart:convert';

const String _hrTalentManagementMappingJson = r'''
{
  "environment": "hr_talent_management",
  "description": "Method mappings between different interfaces for HR Talent Management environment.",
  "version": "1.0",
  "last_updated": "2025-10-23",
  "interface_count": 5,
  "interfaces": {
    "interface_1": {},
    "interface_2": {},
    "interface_3": {},
    "interface_4": {},
    "interface_5": {}
  },
  "method_mappings": {
    "manage_job_operations": {
      "category": "management_operations",
      "description": "Manage job requisitions, postings, and positions",
      "interface_1": "manage_job_operations",
      "interface_2": "administer_job_operations",
      "interface_3": "execute_job_operations",
      "interface_4": "process_job_operations",
      "interface_5": "handle_job_operations"
    },
    "discover_reference_entities": {
      "category": "discovery_operations",
      "description": "Search and discover reference entities",
      "interface_1": "discover_reference_entities",
      "interface_2": "fetch_reference_entities",
      "interface_3": "lookup_reference_entities",
      "interface_4": "get_reference_entities",
      "interface_5": "retrieve_reference_entities"
    },
    "discover_job_entities": {
      "category": "discovery_operations",
      "description": "Search and discover job-related entities",
      "interface_1": "discover_job_entities",
      "interface_2": "fetch_job_entities",
      "interface_3": "lookup_job_entities",
      "interface_4": "get_job_entities",
      "interface_5": "retrieve_job_entities"
    },
    "create_audit_entry": {
      "category": "approval_operations",
      "description": "Create audit entries for compliance tracking",
      "interface_1": "create_audit_entry",
      "interface_2": "add_audit_entry",
      "interface_3": "make_audit_entry",
      "interface_4": "build_audit_entry",
      "interface_5": "open_audit_entry"
    },
    "manage_user_operations": {
      "category": "management_operations",
      "description": "Manage user accounts and access control",
      "interface_1": "manage_user_operations",
      "interface_2": "administer_user_operations",
      "interface_3": "execute_user_operations",
      "interface_4": "process_user_operations",
      "interface_5": "handle_user_operations"
    },
    "manage_employee_operations": {
      "category": "management_operations",
      "description": "Manage employee records and information",
      "interface_1": "manage_employee_operations",
      "interface_2": "administer_employee_operations",
      "interface_3": "execute_employee_operations",
      "interface_4": "process_employee_operations",
      "interface_5": "handle_employee_operations"
    },
    "discover_employee_entities": {
      "category": "discovery_operations",
      "description": "Search and discover employee information",
      "interface_1": "discover_employee_entities",
      "interface_2": "fetch_employee_entities",
      "interface_3": "lookup_employee_entities",
      "interface_4": "get_employee_entities",
      "interface_5": "retrieve_employee_entities"
    }
  }
}
''';

Map<String, dynamic> loadHrTalentManagementMapping() {
  return jsonDecode(_hrTalentManagementMappingJson) as Map<String, dynamic>;
}