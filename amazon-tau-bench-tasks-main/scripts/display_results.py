#!/usr/bin/env python3
"""
Display detailed validation res                                    # Show execution outcome
            print(f'      🔧 Execution outcome:')
            execution_result = {
                "name": action['action_name'],
                "arguments": action.get('arguments', {}),
                "output": action.get('actual_output') if action.get('actual_output') is not None else 'No output available'
            }
            print(json.dumps(execution_result, indent=6, ensure_ascii=False))
            print()
            
            # Show any errors
            if action.get('error'):
                print(f'      ❌ Error: {action["error"]}')
                print()ution outcome
            print(f'      🔧 Execution outcome:')
            execution_result = {
                "name": action['action_name'],
                "arguments": action.get('arguments', {}),
                "output": action.get('actual_output') if action.get('actual_output') is not None else 'No output available'
            }
            print(json.dumps(execution_result, indent=6, ensure_ascii=False))
            print() execution outcome
            print(f'      🔧 Execution outcome:')
            execution_result = {
                "name": action['action_name'],
                "arguments": action.get('arguments', {}),
                "output": action.get('actual_output') if action.get('actual_output') is not None else 'No output available'
            }
            print(json.dumps(execution_result, indent=6, ensure_ascii=False))
            print() execution outcome
            execution_result = {
                "name": action['action_name'],
                "arguments": action.get('arguments', {}),
                "output": action.get('actual_output') if action.get('actual_output') is not None else 'No output available'
            }
            print(f'      🔧 Execution outcome:')
            print(json.dumps(execution_result, indent=6, ensure_ascii=False))
            print()e terminal.
"""

import argparse
import json
import sys

def display_validation_results(report_data: dict):
    """Display detailed validation results with full action comparison format"""
    
    print('\n' + '='*80)
    print('🧪 DETAILED VALIDATION RESULTS')
    print('='*80)
    
    summary = report_data['summary']
    print(f'📊 SUMMARY: {summary["passed"]}/{summary["total"]} tasks passed ({summary["success_rate"]:.1f}%)')
    print()
    
    for result in report_data['results']:
        status = '✅' if result['success'] else '❌'
        print(f'{status} TASK: {result["task_file"]}')
        print(f'   Environment: {result["environment"]}')
        print(f'   Actions: {result["actions_validated"]}')
        print(f'   Time: {result["execution_time_ms"]:.2f}ms')
        
        if result.get('error'):
            print(f'   ❌ Task Error: {result["error"]}')
            print()
            print('-' * 60)
            print()
            continue
        
        print()
        
        # Load the original task data to get ground truth
        task_data = None
        try:
            with open(result["task_file"], 'r') as f:
                task_json = json.load(f)
                task_data = task_json.get('task', {})
        except:
            pass
        
        for i, action in enumerate(result.get('action_results', []), 1):
            action_status = '✅' if action['success'] else '❌'
            print(f'   {action_status} ACTION {i}: {action["action_name"]}')
            print(f'      Time: {action.get("execution_time_ms", 0):.2f}ms')
            print()
            
            # Show ground truth action
            if task_data and 'actions' in task_data and len(task_data['actions']) >= i:
                ground_truth = task_data['actions'][i-1]  # 0-indexed
                print(f'      📋 Ground truth action:')
                print(json.dumps(ground_truth, indent=6, ensure_ascii=False))
                print()
            
            # Show execution outcome
            if action.get('actual_output') is not None:
                print(f'      � Execution outcome:')
                execution_result = {
                    "name": action['action_name'],
                    "arguments": action.get('arguments', {}),
                    "output": action['actual_output']
                }
                print(json.dumps(execution_result, indent=6, ensure_ascii=False))
                print()
            
            # Show any errors
            if action.get('error'):
                print(f'      ❌ Error: {action["error"]}')
                print()
            
            # Show comparison result
            if action['success']:
                print(f'      ✅ VALIDATION: Outputs match expected ground truth')
            else:
                print(f'      ❌ VALIDATION: Outputs do not match expected ground truth')
                
                # Show detailed differences if available
                if 'expected_output' in action and 'actual_output' in action:
                    expected = action['expected_output']
                    actual = action['actual_output']
                    
                    if isinstance(expected, dict) and isinstance(actual, dict):
                        expected_keys = set(expected.keys()) if expected else set()
                        actual_keys = set(actual.keys()) if actual else set()
                        
                        missing_keys = expected_keys - actual_keys
                        extra_keys = actual_keys - expected_keys
                        
                        if missing_keys:
                            print(f'      🔍 Missing keys: {", ".join(missing_keys)}')
                        if extra_keys:
                            print(f'      🔍 Extra keys: {", ".join(extra_keys)}')
                        
                        # Check value differences for common keys
                        common_keys = expected_keys & actual_keys
                        for key in common_keys:
                            if expected[key] != actual[key]:
                                print(f'      🔍 Different value for "{key}": expected {expected[key]} → got {actual[key]}')
            
            print()
            print('   ' + '='*60)
            print()
        
        print('-' * 60)
        print()
    
    print('='*80)

def main():
    """Main function"""
    parser = argparse.ArgumentParser(description="Display validation results")
    parser.add_argument("--input", required=True, help="Input JSON report file")
    
    args = parser.parse_args()
    
    try:
        # Load JSON report
        with open(args.input, 'r') as f:
            report_data = json.load(f)
        
        # Display results
        display_validation_results(report_data)
        
    except Exception as e:
        print(f"Error displaying results: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())