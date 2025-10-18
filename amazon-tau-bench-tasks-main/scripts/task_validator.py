#!/usr/bin/env python3
"""
Task Validator - Executes action sequences from task.json files and validates outputs.

This script:
1. Loads task.json files 
2. Dynamically loads the specified environment
3. Executes each action in sequence
4. Compares actual outputs with expected outputs
5. Reports detailed validation results
"""

# Import mock tau_bench first to bypass dependency issues
try:
    from mock_tau_bench import *  # This sets up the mock modules
except ImportError:
    pass  # If mock not available, continue (for local development)

import argparse
import json
import os
import sys
import importlib
import inspect
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Union
from dataclasses import dataclass
from datetime import datetime
import logging

# Add the project root to Python path for imports
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

@dataclass
class ValidationResult:
    """Result of validating a single action"""
    action_name: str
    success: bool
    expected_output: Any
    actual_output: Any
    error: Optional[str] = None
    execution_time_ms: Optional[float] = None
    data_type_matches: bool = True

@dataclass
class TaskValidationResult:
    """Result of validating an entire task"""
    task_file: str
    environment: str
    success: bool
    actions_validated: int
    action_results: List[ValidationResult]
    error: Optional[str] = None
    execution_time_ms: float = 0.0

class OutputComparator:
    """Handles precise comparison of expected vs actual outputs"""
    
    @staticmethod
    def compare_outputs(expected: Any, actual: Any) -> Tuple[bool, List[str]]:
        """
        Compare two outputs with strict type and value checking.
        
        Returns:
            (matches, differences): Boolean and list of difference descriptions
        """
        differences = []
        
        # Check if both are None
        if expected is None and actual is None:
            return True, []
        
        if expected is None or actual is None:
            differences.append(f"One value is None: expected={expected}, actual={actual}")
            return False, differences
        
        # Check type matching
        if type(expected) != type(actual):
            differences.append(f"Type mismatch: expected {type(expected).__name__}, got {type(actual).__name__}")
            
            # Try to see if values are equivalent despite type difference
            try:
                if str(expected) == str(actual):
                    differences.append("Values are equivalent as strings but types differ")
                elif isinstance(expected, (int, float)) and isinstance(actual, (int, float)):
                    if float(expected) == float(actual):
                        differences.append("Numeric values are equal but types differ (int vs float)")
            except:
                pass
            
            return False, differences
        
        # Compare based on type
        if isinstance(expected, dict):
            return OutputComparator._compare_dicts(expected, actual)
        elif isinstance(expected, list):
            return OutputComparator._compare_lists(expected, actual)
        elif isinstance(expected, (int, float)):
            return OutputComparator._compare_numbers(expected, actual)
        elif isinstance(expected, str):
            matches = expected == actual
            if not matches:
                differences.append(f"String mismatch: expected '{expected}', got '{actual}'")
            return matches, differences
        elif isinstance(expected, bool):
            matches = expected == actual
            if not matches:
                differences.append(f"Boolean mismatch: expected {expected}, got {actual}")
            return matches, differences
        else:
            # Generic comparison for other types
            matches = expected == actual
            if not matches:
                differences.append(f"Value mismatch: expected {expected}, got {actual}")
            return matches, differences
    
    @staticmethod
    def _compare_dicts(expected: dict, actual: dict) -> Tuple[bool, List[str]]:
        """Compare two dictionaries"""
        differences = []
        all_keys = set(expected.keys()) | set(actual.keys())
        
        for key in all_keys:
            if key not in expected:
                differences.append(f"Unexpected key '{key}' in actual output")
            elif key not in actual:
                differences.append(f"Missing key '{key}' in actual output")
            else:
                key_matches, key_diffs = OutputComparator.compare_outputs(expected[key], actual[key])
                if not key_matches:
                    for diff in key_diffs:
                        differences.append(f"In key '{key}': {diff}")
        
        return len(differences) == 0, differences
    
    @staticmethod
    def _compare_lists(expected: list, actual: list) -> Tuple[bool, List[str]]:
        """Compare two lists"""
        differences = []
        
        if len(expected) != len(actual):
            differences.append(f"List length mismatch: expected {len(expected)}, got {len(actual)}")
            return False, differences
        
        for i, (exp_item, act_item) in enumerate(zip(expected, actual)):
            item_matches, item_diffs = OutputComparator.compare_outputs(exp_item, act_item)
            if not item_matches:
                for diff in item_diffs:
                    differences.append(f"At index {i}: {diff}")
        
        return len(differences) == 0, differences
    
    @staticmethod
    def _compare_numbers(expected: Union[int, float], actual: Union[int, float]) -> Tuple[bool, List[str]]:
        """Compare two numbers with tolerance for floating point precision"""
        differences = []
        
        # For exact integer comparison
        if isinstance(expected, int) and isinstance(actual, int):
            matches = expected == actual
            if not matches:
                differences.append(f"Integer mismatch: expected {expected}, got {actual}")
            return matches, differences
        
        # For floating point comparison with small tolerance
        tolerance = 1e-10
        if abs(float(expected) - float(actual)) <= tolerance:
            return True, []
        else:
            differences.append(f"Number mismatch: expected {expected}, got {actual}")
            return False, differences

class EnvironmentLoader:
    """Dynamically loads and manages different environments"""
    
    @staticmethod
    def load_environment(env_name: str) -> Tuple[Any, Dict[str, Any]]:
        """
        Load environment and its data.
        
        Returns:
            (env_module, env_data): Environment module and loaded data
        """
        try:
            # Load environment module with better error handling
            env_module_path = f"envs.{env_name}"
            
            # Try to suppress tau_bench import errors during module loading
            import warnings
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                
                try:
                    env_module = importlib.import_module(env_module_path)
                except ModuleNotFoundError as e:
                    if 'tau_bench' in str(e):
                        # If it's a tau_bench import error, create a minimal module
                        print(f"Warning: tau_bench dependency issue in {env_name}, using minimal module")
                        env_module = type('MinimalEnvModule', (), {})()
                    else:
                        raise e
                except SyntaxError as e:
                    if 'match' in str(e) or 'case' in str(e):
                        print(f"Warning: Python version incompatibility in {env_name} (requires Python 3.10+), using minimal module")
                        env_module = type('MinimalEnvModule', (), {})()
                    else:
                        raise e
                except (ImportError, AttributeError) as e:
                    if 'tau_bench' in str(e) or any(name in str(e) for name in ['MockFundFinanceDomainEnv', 'MockFinanceDomainEnv', 'load_data', 'RULES', 'WIKI']):
                        print(f"Warning: tau_bench dependency issue in {env_name}, using minimal module: {e}")
                        env_module = type('MinimalEnvModule', (), {})()
                    else:
                        raise e
            
            # Load environment data
            env_data_path = project_root / "envs" / env_name / "data"
            env_data = {}
            
            if env_data_path.exists():
                for data_file in env_data_path.glob("*.json"):
                    with open(data_file, 'r') as f:
                        key = data_file.stem  # filename without extension
                        env_data[key] = json.load(f)
            else:
                print(f"Warning: No data directory found for environment '{env_name}'")
            
            return env_module, env_data
            
        except ImportError as e:
            if 'tau_bench' in str(e) or any(name in str(e) for name in ['MockFundFinanceDomainEnv', 'MockFinanceDomainEnv', 'load_data', 'RULES', 'WIKI']):
                print(f"Warning: Skipping environment '{env_name}' due to tau_bench dependency: {e}")
                # Return minimal environment for tau_bench related errors
                minimal_module = type('MinimalEnvModule', (), {})()
                return minimal_module, {}
            else:
                raise ImportError(f"Could not load environment '{env_name}': {e}")
        except SyntaxError as e:
            print(f"Warning: Skipping environment '{env_name}' due to Python version incompatibility: {e}")
            minimal_module = type('MinimalEnvModule', (), {})()
            return minimal_module, {}
        except Exception as e:
            print(f"Warning: Skipping environment '{env_name}' due to error: {e}")
            minimal_module = type('MinimalEnvModule', (), {})()
            return minimal_module, {}
    
    @staticmethod
    def get_tool_class(env_module: Any, tool_name: str, interface_num: int = 1) -> Any:
        """
        Get the tool class from the environment module.
        
        Args:
            env_module: Loaded environment module
            tool_name: Name of the tool to load
            interface_num: Interface number (1, 2, 3, etc.)
        """
        try:
            # Handle minimal environment modules - provide mock tools
            if not hasattr(env_module, '__name__') or 'MinimalEnvModule' in str(type(env_module)):
                # Import the mock tools we've created
                try:
                    import scripts.mock_tau_bench as mock_module
                    
                    # Map tool names to mock classes for hr_experts
                    hr_tools_map = {
                        'discover_user_entities': getattr(mock_module, 'DiscoverUserEntities', None),
                        'discover_department_entities': getattr(mock_module, 'DiscoverDepartmentEntities', None),
                        'discover_job_entities': getattr(mock_module, 'DiscoverJobEntities', None),
                        'discover_employee_entities': getattr(mock_module, 'DiscoverEmployeeEntities', None),
                        'discover_recruitment_entities': getattr(mock_module, 'DiscoverRecruitmentEntities', None),
                        'check_approval': getattr(mock_module, 'CheckApproval', None),
                        'manage_job_position': getattr(mock_module, 'ManageJobPosition', None),
                        'manage_job_position_skills': getattr(mock_module, 'ManageJobPositionSkills', None),
                        'manage_job_application': getattr(mock_module, 'ManageJobApplication', None),
                        'manage_interview': getattr(mock_module, 'ManageInterview', None),
                        'manage_audit_logs': getattr(mock_module, 'ManageAuditLogs', None)
                    }
                    
                    if tool_name in hr_tools_map and hr_tools_map[tool_name]:
                        print(f"Info: Using mock tool '{tool_name}' for testing")
                        return hr_tools_map[tool_name]
                    else:
                        print(f"Warning: Mock tool '{tool_name}' not available")
                        return None
                        
                except Exception as e:
                    print(f"Warning: Cannot load mock tools: {e}")
                    return None
            
            # Try to load from tools/interface_X/tool_name.py
            tools_module_path = f"envs.{env_module.__name__.split('.')[-1]}.tools.interface_{interface_num}.{tool_name}"
            
            try:
                tool_module = importlib.import_module(tools_module_path)
            except ModuleNotFoundError as e:
                if 'tau_bench' in str(e):
                    print(f"Warning: Cannot load tool '{tool_name}' due to tau_bench dependency: {e}")
                    return None
                else:
                    raise e
            
            # Find the tool class (should be a subclass of Tool)
            for name, obj in inspect.getmembers(tool_module):
                if (inspect.isclass(obj) and 
                    hasattr(obj, 'invoke') and 
                    name != 'Tool' and
                    name.lower().replace('_', '') == tool_name.lower().replace('_', '')):
                    return obj
            
            raise ImportError(f"Tool class not found in {tools_module_path}")
            
        except ImportError as e:
            if 'tau_bench' in str(e):
                print(f"Warning: Cannot load tool '{tool_name}' due to tau_bench dependency: {e}")
                return None
            else:
                raise ImportError(f"Could not load tool '{tool_name}' from interface {interface_num}: {e}")

class TaskExecutor:
    """Executes task action sequences and validates outputs"""
    
    def __init__(self, env_name: str, interface_num: int = 1):
        self.env_name = env_name
        self.interface_num = interface_num
        self.env_module, self.env_data = EnvironmentLoader.load_environment(env_name)
        self.execution_context = {}  # Store outputs from previous actions
        
    def execute_action(self, action: Dict[str, Any]) -> ValidationResult:
        """Execute a single action and validate its output"""
        action_name = action["name"]
        action_args = action["arguments"]
        expected_output = action["output"]
        
        start_time = datetime.now()
        
        try:
            # Load the tool class
            tool_class = EnvironmentLoader.get_tool_class(
                self.env_module, action_name, self.interface_num
            )
            
            # Handle case where tool couldn't be loaded (tau_bench dependency issues)
            if tool_class is None:
                execution_time = (datetime.now() - start_time).total_seconds() * 1000
                return ValidationResult(
                    action_name=action_name,
                    success=False,
                    expected_output=expected_output,
                    actual_output=None,
                    error=f"Tool '{action_name}' could not be loaded due to dependency issues",
                    execution_time_ms=execution_time
                )
            
            # Prepare arguments by resolving any references to previous outputs
            resolved_args = self._resolve_arguments(action_args)
            
            # Execute the tool
            if hasattr(tool_class, 'invoke'):
                # Add environment data to arguments if the tool expects it
                if 'data' in inspect.signature(tool_class.invoke).parameters:
                    actual_output = tool_class.invoke(data=self.env_data, **resolved_args)
                else:
                    actual_output = tool_class.invoke(**resolved_args)
            else:
                raise AttributeError(f"Tool {action_name} does not have invoke method")
            
            # Parse JSON output if it's a string
            if isinstance(actual_output, str):
                try:
                    actual_output = json.loads(actual_output)
                except json.JSONDecodeError:
                    pass  # Keep as string if not valid JSON
            
            # Store output for future action references
            self.execution_context[action_name] = actual_output
            
            # Compare outputs
            matches, differences = OutputComparator.compare_outputs(expected_output, actual_output)
            
            execution_time = (datetime.now() - start_time).total_seconds() * 1000
            
            return ValidationResult(
                action_name=action_name,
                success=matches,
                expected_output=expected_output,
                actual_output=actual_output,
                error=None if matches else "; ".join(differences),
                execution_time_ms=execution_time,
                data_type_matches=type(expected_output) == type(actual_output)
            )
            
        except Exception as e:
            execution_time = (datetime.now() - start_time).total_seconds() * 1000
            return ValidationResult(
                action_name=action_name,
                success=False,
                expected_output=expected_output,
                actual_output=None,
                error=f"Execution failed: {str(e)}",
                execution_time_ms=execution_time,
                data_type_matches=False
            )
    
    def _resolve_arguments(self, args: Dict[str, Any]) -> Dict[str, Any]:
        """Resolve argument references to previous action outputs"""
        # For now, return args as-is since the example doesn't show cross-references
        # This can be enhanced to resolve edge connections if needed
        return args
    
    def validate_task(self, task_data: Dict[str, Any]) -> TaskValidationResult:
        """Validate an entire task by executing all actions in sequence"""
        task_info = task_data.get("task", {})
        actions = task_info.get("actions", [])
        
        start_time = datetime.now()
        action_results = []
        
        try:
            for action in actions:
                result = self.execute_action(action)
                action_results.append(result)
                
                # Stop on first failure for now
                if not result.success:
                    break
            
            execution_time = (datetime.now() - start_time).total_seconds() * 1000
            overall_success = all(result.success for result in action_results)
            
            return TaskValidationResult(
                task_file="",  # Will be set by caller
                environment=self.env_name,
                success=overall_success,
                actions_validated=len(action_results),
                action_results=action_results,
                execution_time_ms=execution_time
            )
            
        except Exception as e:
            execution_time = (datetime.now() - start_time).total_seconds() * 1000
            return TaskValidationResult(
                task_file="",  # Will be set by caller
                environment=self.env_name,
                success=False,
                actions_validated=len(action_results),
                action_results=action_results,
                error=f"Task execution failed: {str(e)}",
                execution_time_ms=execution_time
            )

def setup_logging():
    """Setup logging configuration"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s'
    )

def load_task_file(task_file_path: str) -> Dict[str, Any]:
    """Load and parse a task.json file"""
    try:
        with open(task_file_path, 'r') as f:
            return json.load(f)
    except Exception as e:
        raise Exception(f"Failed to load task file {task_file_path}: {e}")

def validate_single_task(task_file_path: str) -> TaskValidationResult:
    """Validate a single task file"""
    logging.info(f"Validating task: {task_file_path}")
    
    try:
        # Load task file
        task_data = load_task_file(task_file_path)
        
        # Get environment and interface info
        env_name = task_data.get("env")
        interface_num = task_data.get("interface_num", 1)
        
        if not env_name:
            raise ValueError("Task file missing 'env' field")
        
        # Create executor and validate
        executor = TaskExecutor(env_name, interface_num)
        result = executor.validate_task(task_data)
        result.task_file = task_file_path
        
        return result
        
    except Exception as e:
        return TaskValidationResult(
            task_file=task_file_path,
            environment="unknown",
            success=False,
            actions_validated=0,
            action_results=[],
            error=str(e)
        )

def main():
    """Main validation function"""
    parser = argparse.ArgumentParser(description="Validate task.json files")
    parser.add_argument("--tasks", required=True, help="Space-separated list of task.json files")
    parser.add_argument("--environment", help="Filter by specific environment")
    parser.add_argument("--output-format", choices=["json", "text"], default="json")
    parser.add_argument("--report-file", default="validation-report.json")
    
    args = parser.parse_args()
    
    setup_logging()
    
    # Parse task files
    task_files = args.tasks.strip().split()
    if not task_files or task_files == ['']:
        logging.info("No task files provided - creating empty report")
        
        # Create empty report
        empty_report = {
            "timestamp": datetime.now().isoformat(),
            "summary": {
                "total": 0,
                "passed": 0,
                "failed": 0,
                "success_rate": 100.0
            },
            "results": [],
            "message": "No task files found to validate"
        }
        
        # Save empty report
        with open(args.report_file, 'w') as f:
            json.dump(empty_report, f, indent=2)
        
        print("✅ No validation needed - no task files found")
        return 0
    
    logging.info(f"Found {len(task_files)} task files to validate")
    
    # Validate each task
    results = []
    for task_file in task_files:
        if not os.path.exists(task_file):
            logging.warning(f"Task file not found: {task_file}")
            continue
            
        # Filter by environment if specified
        if args.environment:
            try:
                task_data = load_task_file(task_file)
                if task_data.get("env") != args.environment:
                    logging.info(f"Skipping {task_file} (environment {task_data.get('env')} != {args.environment})")
                    continue
            except:
                logging.warning(f"Could not check environment for {task_file}")
                continue
        
        result = validate_single_task(task_file)
        results.append(result)
        
        # Log result
        status = "✅ PASS" if result.success else "❌ FAIL"
        logging.info(f"{status} {task_file} ({result.actions_validated} actions)")
        if not result.success and result.error:
            logging.error(f"  Error: {result.error}")
    
    # Generate summary
    passed = sum(1 for r in results if r.success)
    failed = len(results) - passed
    
    summary = {
        "total": len(results),
        "passed": passed,
        "failed": failed,
        "success_rate": (passed / len(results) * 100) if results else 0
    }
    
    # Create report
    report = {
        "timestamp": datetime.now().isoformat(),
        "summary": summary,
        "results": []
    }
    
    # Process each result with proper argument extraction
    for r in results:
        # Load task file to get arguments
        task_data = None
        try:
            task_data = load_task_file(r.task_file)
        except:
            pass
        
        task_actions = task_data.get('task', {}).get('actions', []) if task_data else []
        
        result_entry = {
            "task_file": r.task_file,
            "environment": r.environment,
            "success": r.success,
            "actions_validated": r.actions_validated,
            "execution_time_ms": r.execution_time_ms,
            "error": r.error,
            "action_results": []
        }
        
        for i, ar in enumerate(r.action_results):
            # Get arguments from original task if available
            arguments = {}
            if i < len(task_actions):
                arguments = task_actions[i].get('arguments', {})
            
            action_result = {
                "action_name": ar.action_name,
                "success": ar.success,
                "error": ar.error,
                "data_type_matches": ar.data_type_matches,
                "execution_time_ms": ar.execution_time_ms,
                "arguments": arguments,
                "expected_output": ar.expected_output,
                "actual_output": ar.actual_output
            }
            result_entry["action_results"].append(action_result)
        
        report["results"].append(result_entry)
    
    # Save report
    with open(args.report_file, 'w') as f:
        json.dump(report, f, indent=2)
    
    # Print summary
    print(f"\n{'='*60}")
    print(f"VALIDATION SUMMARY")
    print(f"{'='*60}")
    print(f"Total tasks: {summary['total']}")
    print(f"Passed: {summary['passed']}")
    print(f"Failed: {summary['failed']}")
    print(f"Success rate: {summary['success_rate']:.1f}%")
    print(f"Report saved to: {args.report_file}")
    
    if failed > 0:
        print(f"\n❌ {failed} task(s) failed validation")
        sys.exit(1)
    else:
        print(f"\n✅ All tasks passed validation!")

if __name__ == "__main__":
    main()