import 'dart:convert';

const String _hrMappingJson = r'''
{
  "environment": "hr_experts",
  "description": "Method mappings between different interfaces for HR Experts environment.",
  "version": "1.0",
  "last_updated": "2025-10-18",
  "interface_count": 5,
  "total_methods_per_interface": 36,
  "is_audit_log": true,
  "method_categories": {
    "management_operations": {
      "description": "CRUD operations for various HR entities",
      "is_crud": true
    },
    "discovery_operations": {
      "description": "Search and query operations for finding entities",
      "is_crud": false
    },
    "approval_operations": {
      "description": "Approval workflow validation and processing",
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
    "manage_audit_logs": {
      "category": "management_operations",
      "description": "Manage audit log entries for compliance and tracking",
      "interface_1": "manage_audit_logs",
      "interface_2": "handle_audit_logs",
      "interface_3": "process_audit_logs",
      "interface_4": "administer_audit_logs",
      "interface_5": "execute_audit_logs"
    },
    "manage_benefits_plan": {
      "category": "management_operations",
      "description": "Manage employee benefits plans",
      "interface_1": "manage_benefits_plan",
      "interface_2": "handle_benefits_plan",
      "interface_3": "process_benefits_plan",
      "interface_4": "administer_benefits_plan",
      "interface_5": "execute_benefits_plan"
    },
    "manage_candidate": {
      "category": "management_operations",
      "description": "Manage recruitment candidates",
      "interface_1": "manage_candidate",
      "interface_2": "handle_candidate",
      "interface_3": "process_candidate",
      "interface_4": "administer_candidate",
      "interface_5": "execute_candidate"
    },
    "manage_department": {
      "category": "management_operations",
      "description": "Manage organizational departments",
      "interface_1": "manage_department",
      "interface_2": "handle_department",
      "interface_3": "process_department",
      "interface_4": "administer_department",
      "interface_5": "execute_department"
    },
    "manage_document_storage": {
      "category": "management_operations",
      "description": "Manage document storage and retrieval",
      "interface_1": "manage_document_storage",
      "interface_2": "handle_document_storage",
      "interface_3": "process_document_storage",
      "interface_4": "administer_document_storage",
      "interface_5": "execute_document_storage"
    },
    "manage_employee": {
      "category": "management_operations",
      "description": "Manage employee records and information",
      "interface_1": "manage_employee",
      "interface_2": "handle_employee",
      "interface_3": "process_employee",
      "interface_4": "administer_employee",
      "interface_5": "execute_employee"
    },
    "manage_employee_benefits": {
      "category": "management_operations",
      "description": "Manage employee benefit enrollments and assignments",
      "interface_1": "manage_employee_benefits",
      "interface_2": "handle_employee_benefits",
      "interface_3": "process_employee_benefits",
      "interface_4": "administer_employee_benefits",
      "interface_5": "execute_employee_benefits"
    },
    "manage_employee_training": {
      "category": "management_operations",
      "description": "Manage employee training assignments and completions",
      "interface_1": "manage_employee_training",
      "interface_2": "handle_employee_training",
      "interface_3": "process_employee_training",
      "interface_4": "administer_employee_training",
      "interface_5": "execute_employee_training"
    },
    "manage_expense_reimbursements": {
      "category": "management_operations",
      "description": "Manage employee expense reimbursement requests",
      "interface_1": "manage_expense_reimbursements",
      "interface_2": "handle_expense_reimbursements",
      "interface_3": "process_expense_reimbursements",
      "interface_4": "administer_expense_reimbursements",
      "interface_5": "execute_expense_reimbursements"
    },
    "manage_interview": {
      "category": "management_operations",
      "description": "Manage recruitment interviews",
      "interface_1": "manage_interview",
      "interface_2": "handle_interview",
      "interface_3": "process_interview",
      "interface_4": "administer_interview",
      "interface_5": "execute_interview"
    },
    "manage_job_application": {
      "category": "management_operations",
      "description": "Manage job applications from candidates",
      "interface_1": "manage_job_application",
      "interface_2": "handle_job_application",
      "interface_3": "process_job_application",
      "interface_4": "administer_job_application",
      "interface_5": "execute_job_application"
    },
    "manage_job_position": {
      "category": "management_operations",
      "description": "Manage job positions and openings",
      "interface_1": "manage_job_position",
      "interface_2": "handle_job_position",
      "interface_3": "process_job_position",
      "interface_4": "administer_job_position",
      "interface_5": "execute_job_position"
    },
    "manage_job_position_skills": {
      "category": "management_operations",
      "description": "Manage skills required for job positions",
      "interface_1": "manage_job_position_skills",
      "interface_2": "handle_job_position_skills",
      "interface_3": "process_job_position_skills",
      "interface_4": "administer_job_position_skills",
      "interface_5": "execute_job_position_skills"
    },
    "manage_leave_requests": {
      "category": "management_operations",
      "description": "Manage employee leave requests",
      "interface_1": "manage_leave_requests",
      "interface_2": "handle_leave_requests",
      "interface_3": "process_leave_requests",
      "interface_4": "administer_leave_requests",
      "interface_5": "execute_leave_requests"
    },
    "manage_payroll_deduction": {
      "category": "management_operations",
      "description": "Manage payroll deductions",
      "interface_1": "manage_payroll_deduction",
      "interface_2": "handle_payroll_deduction",
      "interface_3": "process_payroll_deduction",
      "interface_4": "administer_payroll_deduction",
      "interface_5": "execute_payroll_deduction"
    },
    "manage_payroll_record": {
      "category": "management_operations",
      "description": "Manage payroll records and processing",
      "interface_1": "manage_payroll_record",
      "interface_2": "handle_payroll_record",
      "interface_3": "process_payroll_record",
      "interface_4": "administer_payroll_record",
      "interface_5": "execute_payroll_record"
    },
    "manage_performance_review": {
      "category": "management_operations",
      "description": "Manage employee performance reviews",
      "interface_1": "manage_performance_review",
      "interface_2": "handle_performance_review",
      "interface_3": "process_performance_review",
      "interface_4": "administer_performance_review",
      "interface_5": "execute_performance_review"
    },
    "manage_skill": {
      "category": "management_operations",
      "description": "Manage skills catalog",
      "interface_1": "manage_skill",
      "interface_2": "handle_skill",
      "interface_3": "process_skill",
      "interface_4": "administer_skill",
      "interface_5": "execute_skill"
    },
    "manage_timesheet_entries": {
      "category": "management_operations",
      "description": "Manage employee timesheet entries",
      "interface_1": "manage_timesheet_entries",
      "interface_2": "handle_timesheet_entries",
      "interface_3": "process_timesheet_entries",
      "interface_4": "administer_timesheet_entries",
      "interface_5": "execute_timesheet_entries"
    },
    "manage_training_programs": {
      "category": "management_operations",
      "description": "Manage training programs catalog",
      "interface_1": "manage_training_programs",
      "interface_2": "handle_training_programs",
      "interface_3": "process_training_programs",
      "interface_4": "administer_training_programs",
      "interface_5": "execute_training_programs"
    },
    "manage_user": {
      "category": "management_operations",
      "description": "Manage user accounts and provisioning",
      "interface_1": "manage_user",
      "interface_2": "handle_user",
      "interface_3": "process_user",
      "interface_4": "administer_user",
      "interface_5": "execute_user"
    },
    "discover_benefits_entities": {
      "category": "discovery_operations",
      "description": "Search and discover benefits-related entities",
      "interface_1": "discover_benefits_entities",
      "interface_2": "search_benefits_entities",
      "interface_3": "find_benefits_entities",
      "interface_4": "lookup_benefits_entities",
      "interface_5": "retrieve_benefits_entities"
    },
    "discover_department_entities": {
      "category": "discovery_operations",
      "description": "Search and discover department entities",
      "interface_1": "discover_department_entities",
      "interface_2": "search_department_entities",
      "interface_3": "find_department_entities",
      "interface_4": "lookup_department_entities",
      "interface_5": "retrieve_department_entities"
    },
    "discover_document_entities": {
      "category": "discovery_operations",
      "description": "Search and discover document entities",
      "interface_1": "discover_document_entities",
      "interface_2": "search_document_entities",
      "interface_3": "find_document_entities",
      "interface_4": "lookup_document_entities",
      "interface_5": "retrieve_document_entities"
    },
    "discover_expense_entities": {
      "category": "discovery_operations",
      "description": "Search and discover expense-related entities",
      "interface_1": "discover_expense_entities",
      "interface_2": "search_expense_entities",
      "interface_3": "find_expense_entities",
      "interface_4": "lookup_expense_entities",
      "interface_5": "retrieve_expense_entities"
    },
    "discover_job_entities": {
      "category": "discovery_operations",
      "description": "Search and discover job-related entities",
      "interface_1": "discover_job_entities",
      "interface_2": "search_job_entities",
      "interface_3": "find_job_entities",
      "interface_4": "lookup_job_entities",
      "interface_5": "retrieve_job_entities"
    },
    "discover_leave_entities": {
      "category": "discovery_operations",
      "description": "Search and discover leave-related entities",
      "interface_1": "discover_leave_entities",
      "interface_2": "search_leave_entities",
      "interface_3": "find_leave_entities",
      "interface_4": "lookup_leave_entities",
      "interface_5": "retrieve_leave_entities"
    },
    "discover_payroll_entities": {
      "category": "discovery_operations",
      "description": "Search and discover payroll-related entities",
      "interface_1": "discover_payroll_entities",
      "interface_2": "search_payroll_entities",
      "interface_3": "find_payroll_entities",
      "interface_4": "lookup_payroll_entities",
      "interface_5": "retrieve_payroll_entities"
    },
    "discover_performance_entities": {
      "category": "discovery_operations",
      "description": "Search and discover performance-related entities",
      "interface_1": "discover_performance_entities",
      "interface_2": "search_performance_entities",
      "interface_3": "find_performance_entities",
      "interface_4": "lookup_performance_entities",
      "interface_5": "retrieve_performance_entities"
    },
    "discover_recruitment_entities": {
      "category": "discovery_operations",
      "description": "Search and discover recruitment-related entities",
      "interface_1": "discover_recruitment_entities",
      "interface_2": "search_recruitment_entities",
      "interface_3": "find_recruitment_entities",
      "interface_4": "lookup_recruitment_entities",
      "interface_5": "retrieve_recruitment_entities"
    },
    "discover_timesheet_entities": {
      "category": "discovery_operations",
      "description": "Search and discover timesheet-related entities",
      "interface_1": "discover_timesheet_entities",
      "interface_2": "search_timesheet_entities",
      "interface_3": "find_timesheet_entities",
      "interface_4": "lookup_timesheet_entities",
      "interface_5": "retrieve_timesheet_entities"
    },
    "discover_training_entities": {
      "category": "discovery_operations",
      "description": "Search and discover training-related entities",
      "interface_1": "discover_training_entities",
      "interface_2": "search_training_entities",
      "interface_3": "find_training_entities",
      "interface_4": "lookup_training_entities",
      "interface_5": "retrieve_training_entities"
    },
    "discover_user_employee_entities": {
      "category": "discovery_operations",
      "description": "Search and discover user and employee entities",
      "interface_1": "discover_user_employee_entities",
      "interface_2": "search_user_employee_entities",
      "interface_3": "find_user_employee_entities",
      "interface_4": "lookup_user_employee_entities",
      "interface_5": "retrieve_user_employee_entities"
    },
    "check_approval": {
      "category": "approval_operations",
      "description": "Check and validate approval workflows",
      "interface_1": "check_approval",
      "interface_2": "validate_approval",
      "interface_3": "verify_approval",
      "interface_4": "confirm_approval",
      "interface_5": "authenticate_approval"
    },
    "transfer_to_human": {
      "category": "human_transfer",
      "description": "Transfer control to human agents",
      "interface_1": "transfer_to_human",
      "interface_2": "switch_to_human",
      "interface_3": "escalate_to_human",
      "interface_4": "handover_to_human",
      "interface_5": "route_to_human"
    }
  },
  "functional_equivalence": {
    "statement": "⚠️ ABSOLUTE EQUIVALENCE: All methods across all interfaces are 100% functionally identical. The ONLY difference is the method name itself.",
    "what_is_identical": [
      "Business logic and processing rules",
      "Input parameters (names, types, validation)",
      "Return values (structure, content, format)",
      "Error handling and error messages",
      "Policy enforcement (same policy.md with method names swapped)",
      "Approval requirements and workflow",
      "Audit logging behavior",
      "Database operations and queries",
      "Validation rules and constraints",
      "Halt conditions and error states",
      "Role-based access control logic",
      "All Standard Operating Procedures (SOPs)"
    ],
    "what_differs": [
      "Method/function names ONLY"
    ],
    "example": {
      "functionality": "Create a new user account - IDENTICAL implementation across all interfaces",
      "interface_1_call": "manage_user(action='create', first_name='John', last_name='Doe', email='john@example.com', role='employee')",
      "interface_2_call": "handle_user(action='create', first_name='John', last_name='Doe', email='john@example.com', role='employee')",
      "interface_3_call": "process_user(action='create', first_name='John', last_name='Doe', email='john@example.com', role='employee')",
      "interface_4_call": "administer_user(action='create', first_name='John', last_name='Doe', email='john@example.com', role='employee')",
      "interface_5_call": "execute_user(action='create', first_name='John', last_name='Doe', email='john@example.com', role='employee')",
      "result": "ALL calls produce IDENTICAL results - same success response, same error messages, same audit logs, same database changes"
    }
  },
  "usage_guidelines": {
    "interface_selection": "Interface selection is ARBITRARY - choose any interface based on naming preference or testing requirements. Functionality is IDENTICAL.",
    "interchangeability": "Methods are 100% interchangeable - you can switch from one interface to another at any time by simply changing method names",
    "parameter_consistency": "ALL interfaces accept EXACTLY the same parameters with EXACTLY the same names, types, and validation rules",
    "return_format": "ALL interfaces return IDENTICAL JSON-formatted responses with IDENTICAL structure and content",
    "error_handling": "Error codes, error messages, and error handling logic are IDENTICAL across all interfaces",
    "policy_enforcement": "All interfaces follow the SAME Standard Operating Procedures (SOPs) with IDENTICAL halt conditions and approval requirements",
    "translation_rule": "To translate from one interface to another, simply replace the method name prefix - all other code remains unchanged"
  },
  "statistics": {
    "management_operations": 21,
    "discovery_operations": 13,
    "approval_operations": 1,
    "human_transfer_operations": 1,
    "total_unique_operations": 36,
    "total_methods_across_all_interfaces": 180
  }
}
''';

Map<String, dynamic> loadHrMapping() {
  return jsonDecode(_hrMappingJson) as Map<String, dynamic>;
}
