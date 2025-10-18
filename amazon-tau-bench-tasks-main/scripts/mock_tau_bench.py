#!/usr/bin/env python3
"""
Mock tau_bench module to bypass dependencies during validation.
This allows tools to import tau_bench classes without requiring the full tau_bench package.
"""

import sys
from types import ModuleType

class Tool:
    """Mock Tool class that replaces tau_bench.envs.tool.Tool"""
    
    @staticmethod
    def invoke(data=None, **kwargs):
        """Default invoke method - should be overridden by actual tools"""
        return {"success": False, "error": "Tool not implemented"}

class Env:
    """Mock Env class that replaces tau_bench.envs.base.Env"""
    pass

class UserStrategy:
    """Mock UserStrategy class"""
    LLM = "mock_llm"  # Add the LLM attribute that's being accessed
    pass

def load_user(*args, **kwargs):
    """Mock load_user function"""
    return None

# Mock tau_bench.types
class Action:
    """Mock Action class"""
    pass

class Task:
    """Mock Task class"""
    pass

# Create comprehensive mock module structure
def create_mock_module(name):
    """Create a mock module with the given name"""
    module = ModuleType(name)
    sys.modules[name] = module
    return module

# Create the complete tau_bench module hierarchy
tau_bench = create_mock_module('tau_bench')
tau_bench_envs = create_mock_module('tau_bench.envs')
tau_bench_envs_base = create_mock_module('tau_bench.envs.base')
tau_bench_envs_tool = create_mock_module('tau_bench.envs.tool')
tau_bench_envs_user = create_mock_module('tau_bench.envs.user')
tau_bench_types = create_mock_module('tau_bench.types')

# Create environment-specific mock modules
tau_bench_envs_fund_finance = create_mock_module('tau_bench.envs.fund_finance')
tau_bench_envs_fund_finance_data = create_mock_module('tau_bench.envs.fund_finance.data')
tau_bench_envs_fund_finance_rules = create_mock_module('tau_bench.envs.fund_finance.rules')
tau_bench_envs_fund_finance_tools = create_mock_module('tau_bench.envs.fund_finance.tools')
tau_bench_envs_fund_finance_wiki = create_mock_module('tau_bench.envs.fund_finance.wiki')
tau_bench_envs_fund_finance_tasks = create_mock_module('tau_bench.envs.fund_finance.tasks')
tau_bench_envs_fund_finance_interface_1 = create_mock_module('tau_bench.envs.fund_finance.interface_1_tasks')
tau_bench_envs_fund_finance_interface_2 = create_mock_module('tau_bench.envs.fund_finance.interface_2_tasks')
tau_bench_envs_fund_finance_interface_3 = create_mock_module('tau_bench.envs.fund_finance.interface_3_tasks')
tau_bench_envs_fund_finance_interface_4 = create_mock_module('tau_bench.envs.fund_finance.interface_4_tasks')
tau_bench_envs_fund_finance_interface_5 = create_mock_module('tau_bench.envs.fund_finance.interface_5_tasks')

# Create hr_experts mock modules
tau_bench_envs_hr_experts = create_mock_module('tau_bench.envs.hr_experts')
tau_bench_envs_hr_experts_data = create_mock_module('tau_bench.envs.hr_experts.data')
tau_bench_envs_hr_experts_rules = create_mock_module('tau_bench.envs.hr_experts.rules')
tau_bench_envs_hr_experts_tools = create_mock_module('tau_bench.envs.hr_experts.tools')
tau_bench_envs_hr_experts_wiki = create_mock_module('tau_bench.envs.hr_experts.wiki')
tau_bench_envs_hr_experts_tasks = create_mock_module('tau_bench.envs.hr_experts.tasks')
tau_bench_envs_hr_experts_tasks_test = create_mock_module('tau_bench.envs.hr_experts.tasks_test')
tau_bench_envs_hr_experts_interface_1 = create_mock_module('tau_bench.envs.hr_experts.interface_1_tasks')
tau_bench_envs_hr_experts_interface_2 = create_mock_module('tau_bench.envs.hr_experts.interface_2_tasks')
tau_bench_envs_hr_experts_interface_3 = create_mock_module('tau_bench.envs.hr_experts.interface_3_tasks')
tau_bench_envs_hr_experts_interface_4 = create_mock_module('tau_bench.envs.hr_experts.interface_4_tasks')
tau_bench_envs_hr_experts_interface_5 = create_mock_module('tau_bench.envs.hr_experts.interface_5_tasks')

# Set up the module attributes
tau_bench.envs = tau_bench_envs
tau_bench_envs.base = tau_bench_envs_base
tau_bench_envs.tool = tau_bench_envs_tool
tau_bench_envs.user = tau_bench_envs_user
tau_bench_envs.fund_finance = tau_bench_envs_fund_finance
tau_bench_envs.hr_experts = tau_bench_envs_hr_experts
tau_bench.types = tau_bench_types

# Add classes to appropriate modules
tau_bench_envs_tool.Tool = Tool
tau_bench_envs_user.load_user = load_user
tau_bench_envs_user.UserStrategy = UserStrategy
tau_bench_types.Action = Action
tau_bench_types.Task = Task

# Add base classes that might be imported
tau_bench_envs_base.Tool = Tool
tau_bench_envs_base.Env = Env
tau_bench_envs_base.load_user = load_user
tau_bench_envs_base.UserStrategy = UserStrategy

# Add mock functions and data for fund_finance environment
tau_bench_envs_fund_finance_data.load_data = lambda: {}
tau_bench_envs_fund_finance_rules.RULES = {}
tau_bench_envs_fund_finance_wiki.WIKI = {}
tau_bench_envs_fund_finance_tasks.tasks = []

# Add mock test interfaces for fund_finance
tau_bench_envs_fund_finance_interface_1.INTERFACE_1_TEST = []
tau_bench_envs_fund_finance_interface_2.INTERFACE_2_TEST = []
tau_bench_envs_fund_finance_interface_3.INTERFACE_3_TEST = []
tau_bench_envs_fund_finance_interface_4.INTERFACE_4_TEST = []
tau_bench_envs_fund_finance_interface_5.INTERFACE_5_TEST = []

# Add mock functions and data for hr_experts environment
tau_bench_envs_hr_experts_data.load_data = lambda: {}
tau_bench_envs_hr_experts_rules.RULES = {}
tau_bench_envs_hr_experts_wiki.WIKI = {}
tau_bench_envs_hr_experts_tasks.tasks = []
tau_bench_envs_hr_experts_tasks_test.TASKS_TEST = []

# Add mock test interfaces for hr_experts
tau_bench_envs_hr_experts_interface_1.INTERFACE_1_TEST = []
tau_bench_envs_hr_experts_interface_2.INTERFACE_2_TEST = []
tau_bench_envs_hr_experts_interface_3.INTERFACE_3_TEST = []
tau_bench_envs_hr_experts_interface_4.INTERFACE_4_TEST = []
tau_bench_envs_hr_experts_interface_5.INTERFACE_5_TEST = []

# Add mock tool functions for fund_finance
def mock_search_user_entities(**kwargs):
    return {"success": False, "error": "Mock function - not implemented"}

tau_bench_envs_fund_finance_tools.search_user_entities = mock_search_user_entities
tau_bench_envs_fund_finance_tools.ALL_TOOLS_INTERFACE_1 = []
tau_bench_envs_fund_finance_tools.ALL_TOOLS_INTERFACE_2 = []
tau_bench_envs_fund_finance_tools.ALL_TOOLS_INTERFACE_3 = []
tau_bench_envs_fund_finance_tools.ALL_TOOLS_INTERFACE_4 = []
tau_bench_envs_fund_finance_tools.ALL_TOOLS_INTERFACE_5 = []

# Add the missing class that the environment is looking for
class MockFundFinanceDomainEnv(Env):
    """Mock fund finance domain environment class"""
    def __init__(self, *args, **kwargs):
        pass

# Make it available in the fund_finance module
tau_bench_envs_fund_finance.MockFundFinanceDomainEnv = MockFundFinanceDomainEnv

# HR Experts Mock Tools
class MockHRExpertsDomainEnv(Env):
    """Mock HR Experts Domain Environment"""
    def __init__(self, user_strategy="LLM", user_model="gpt-4o", 
                 user_provider=None, task_split="test", task_index=None, interface_num=None):
        self.user_strategy = user_strategy
        self.user_model = user_model
        self.interface_num = interface_num

class DiscoverUserEntities(Tool):
    @staticmethod
    def invoke(data=None, email=None, user_id=None, **kwargs):
        """Mock discover_user_entities tool"""
        if email == "sarah.mitchell@company.com":
            return [{
                "user_id": "5",
                "first_name": "Sarah",
                "last_name": "Mitchell",
                "email": "sarah.mitchell@company.com",
                "phone_number": "+15551234567",
                "role": "hr_manager",
                "status": "active",
                "mfa_enabled": True,
                "created_at": "2023-01-15T09:30:00",
                "updated_at": "2024-08-20T14:45:00"
            }]
        elif email == "david.chen@company.com":
            return [{
                "user_id": "12",
                "first_name": "David",
                "last_name": "Chen",
                "email": "david.chen@company.com",
                "phone_number": "+15557654321",
                "role": "hiring_manager",
                "status": "active",
                "mfa_enabled": True,
                "created_at": "2022-05-10T08:00:00",
                "updated_at": "2024-06-15T12:30:00"
            }]
        else:
            return [{
                "user_id": str(hash(email or str(user_id)) % 100),
                "first_name": "John",
                "last_name": "Doe",
                "email": email or f"user{user_id}@company.com",
                "phone_number": "+15551234567",
                "role": "employee",
                "status": "active",
                "mfa_enabled": True,
                "created_at": "2023-01-15T09:30:00",
                "updated_at": "2024-08-20T14:45:00"
            }]

class DiscoverDepartmentEntities(Tool):
    @staticmethod
    def invoke(data=None, department_id=None, **kwargs):
        """Mock discover_department_entities tool"""
        if department_id == "1":
            return [{
                "department_id": "1",
                "department_name": "Engineering",
                "manager_id": None,
                "budget": 4771808.47,
                "status": "active",
                "created_at": "2022-10-01T03:53:36",
                "updated_at": "2022-12-03T19:12:31"
            }]
        else:
            return [{
                "department_id": department_id or "1",
                "department_name": "General",
                "manager_id": None,
                "budget": 1000000.0,
                "status": "active",
                "created_at": "2022-10-01T03:53:36",
                "updated_at": "2022-12-03T19:12:31"
            }]

class DiscoverJobEntities(Tool):
    @staticmethod
    def invoke(data=None, department_id=None, job_level=None, title=None, status=None, position_id=None, **kwargs):
        """Mock discover_job_entities tool"""
        if position_id == "25":
            return [
                {"position_id": "25", "skill_id": "12"},
                {"position_id": "25", "skill_id": "18"},
                {"position_id": "25", "skill_id": "23"},
                {"position_id": "25", "skill_id": "31"}
            ]
        else:
            return [{
                "position_id": position_id or "25",
                "title": title or "Software Engineer",
                "department_id": department_id or "1",
                "job_level": job_level or "mid",
                "employment_type": "full_time",
                "hourly_rate_min": 45.0,
                "hourly_rate_max": 60.0,
                "status": status or "open",
                "created_at": "2024-08-15T10:00:00",
                "updated_at": "2024-09-01T16:30:00"
            }]

class DiscoverEmployeeEntities(Tool):
    @staticmethod
    def invoke(data=None, email=None, **kwargs):
        """Mock discover_employee_entities tool"""
        if email == "maria.garcia@outlook.com":
            return [{
                "candidate_id": "45",
                "first_name": "Maria",
                "last_name": "Garcia",
                "email": "maria.garcia@outlook.com",
                "phone_number": "+15559876543",
                "status": "screening",
                "source": "career_fair",
                "address": "123 Tech Street, San Francisco, CA 94105",
                "created_at": "2024-07-20T11:15:00",
                "updated_at": "2024-09-10T09:30:00"
            }]
        else:
            return [{
                "candidate_id": str(hash(email) % 100),
                "first_name": "John",
                "last_name": "Candidate",
                "email": email or "candidate@example.com",
                "phone_number": "+15551234567",
                "status": "active",
                "source": "online",
                "address": "123 Main St, City, State 12345",
                "created_at": "2024-07-20T11:15:00",
                "updated_at": "2024-09-10T09:30:00"
            }]

class DiscoverRecruitmentEntities(Tool):
    @staticmethod
    def invoke(data=None, candidate_id=None, **kwargs):
        """Mock discover_recruitment_entities tool"""
        return [{
            "application_id": str(hash(candidate_id) % 1000 + 100),
            "candidate_id": candidate_id or "45",
            "position_id": "18",
            "recruiter_id": "8",
            "status": "screening",
            "application_date": "2024-09-01",
            "ai_screening_score": 82.5,
            "final_decision": None,
            "created_at": "2024-09-01T14:20:00",
            "updated_at": "2024-09-15T10:45:00"
        }]

class CheckApproval(Tool):
    @staticmethod
    def invoke(data=None, action=None, requester_email=None, **kwargs):
        """Mock check_approval tool for HR experts"""
        if action == "create_job_position":
            return {
                "approved": True,
                "approval_id": "APP-2025-001",
                "approver": "hr_director",
                "message": "Job position creation approved"
            }
        else:
            return {
                "approved": True,
                "approval_id": f"APP-2025-{hash(action) % 1000}",
                "approver": "manager",
                "message": f"{action} approved"
            }

class ManageJobPosition(Tool):
    @staticmethod
    def invoke(data=None, action=None, position_id=None, title=None, department_id=None, 
               job_level=None, employment_type=None, hourly_rate_min=None, 
               hourly_rate_max=None, status=None, **kwargs):
        """Mock manage_job_position tool"""
        if action == "create":
            new_id = str(hash(f"{title}{department_id}") % 1000 + 50)
            return {
                "position_id": new_id,
                "success": True,
                "message": "Job position created successfully"
            }
        elif action == "update":
            return {
                "success": True,
                "message": f"Job position {status} updated successfully"
            }
        else:
            return {
                "success": True,
                "message": f"Job position {action} completed successfully"
            }

class ManageJobPositionSkills(Tool):
    @staticmethod
    def invoke(data=None, action=None, position_id=None, skill_id=None, **kwargs):
        """Mock manage_job_position_skills tool"""
        return {
            "success": True,
            "message": "Skill assigned to position successfully"
        }

class ManageJobApplication(Tool):
    @staticmethod
    def invoke(data=None, action=None, application_id=None, candidate_id=None, 
               position_id=None, recruiter_id=None, application_date=None, 
               status=None, **kwargs):
        """Mock manage_job_application tool"""
        if action == "create":
            new_id = str(hash(f"{candidate_id}{position_id}") % 1000 + 200)
            return {
                "application_id": new_id,
                "success": True,
                "message": "Job application created successfully"
            }
        elif action == "update":
            return {
                "success": True,
                "message": f"Application status updated to {status}"
            }
        else:
            return {
                "success": True,
                "message": f"Job application {action} completed successfully"
            }

class ManageInterview(Tool):
    @staticmethod
    def invoke(data=None, action=None, application_id=None, interviewer_id=None, 
               interview_type=None, scheduled_date=None, duration_minutes=None, 
               status=None, **kwargs):
        """Mock manage_interview tool"""
        if action == "create":
            new_id = str(hash(f"{application_id}{interviewer_id}") % 100 + 70)
            return {
                "interview_id": new_id,
                "success": True,
                "message": "Interview scheduled successfully"
            }
        else:
            return {
                "success": True,
                "message": f"Interview {action} completed successfully"
            }

class ManageAuditLogs(Tool):
    @staticmethod
    def invoke(data=None, action=None, reference_type=None, reference_id=None, 
               user_id=None, field_name=None, old_value=None, new_value=None, **kwargs):
        """Mock manage_audit_logs tool"""
        log_id = str(hash(f"{reference_type}{reference_id}{user_id}") % 10000 + 1000)
        return {
            "log_id": log_id,
            "success": True,
            "message": "Audit log created successfully"
        }

# Make HR tools available
tau_bench_envs_hr_experts.MockHRExpertsDomainEnv = MockHRExpertsDomainEnv
tau_bench_envs_hr_experts_tools.ALL_TOOLS_INTERFACE_1 = [
    DiscoverUserEntities, DiscoverDepartmentEntities, DiscoverJobEntities, 
    DiscoverEmployeeEntities, DiscoverRecruitmentEntities, CheckApproval,
    ManageJobPosition, ManageJobPositionSkills, ManageJobApplication, 
    ManageInterview, ManageAuditLogs
]
tau_bench_envs_hr_experts_tools.ALL_TOOLS_INTERFACE_2 = []
tau_bench_envs_hr_experts_tools.ALL_TOOLS_INTERFACE_3 = []
tau_bench_envs_hr_experts_tools.ALL_TOOLS_INTERFACE_4 = []
tau_bench_envs_hr_experts_tools.ALL_TOOLS_INTERFACE_5 = []