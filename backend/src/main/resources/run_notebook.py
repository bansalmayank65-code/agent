#!/usr/bin/env python3
"""
Python script to execute compute_complexity.ipynb notebook programmatically.
This script is called from Java TauBenchValidationService.
"""

import sys
import os
import json
import argparse
from pathlib import Path

try:
    from nbclient import NotebookClient
    from nbformat import read, write, NO_CONVERT
    import nbformat
except ImportError as e:
    print(f"Error: Required packages not installed. Run: pip install nbclient nbformat")
    print(f"Import error: {e}")
    sys.exit(1)


def execute_notebook(notebook_path, task_path, step, output_path=None):
    """
    Execute the compute_complexity notebook with given parameters.
    
    Args:
        notebook_path: Path to the .ipynb file
        task_path: Path to the task.json file 
        step: The validation step to run (compute_complexity, task_verification, run_task, evaluate)
        output_path: Optional path to save executed notebook
    
    Returns:
        dict: Result containing success status and any outputs
    """
    try:
        # Validate inputs
        if not os.path.exists(notebook_path):
            return {"success": False, "error": f"Notebook not found: {notebook_path}"}
        
        if not os.path.exists(task_path):
            return {"success": False, "error": f"Task file not found: {task_path}"}
        
        # Load the notebook
        with open(notebook_path, 'r', encoding='utf-8') as f:
            nb = read(f, as_version=NO_CONVERT)
        
        # Modify the first cell to use the provided parameters
        if nb.cells and nb.cells[0].cell_type == 'code':
            # Update the notebook parameters in the first cell
            cell_source = nb.cells[0].source
            
            # Replace task_path
            lines = cell_source.split('\n')
            new_lines = []
            for line in lines:
                if line.strip().startswith('task_path ='):
                    new_lines.append(f'task_path = "{task_path.replace(os.sep, "/")}"')
                elif line.strip().startswith('selected_endpoint ='):
                    new_lines.append(f'selected_endpoint = "{step}"')
                else:
                    new_lines.append(line)
            
            nb.cells[0].source = '\n'.join(new_lines)
        
        # Create a client to execute the notebook
        client = NotebookClient(
            nb, 
            timeout=600,  # 10 minutes timeout
            kernel_name='python3',
            allow_errors=True  # Don't stop on errors, capture them
        )
        
        # Execute all cells
        print(f"Executing notebook for step: {step}")
        client.execute()
        
        # Save the executed notebook if output path provided
        if output_path:
            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            with open(output_path, 'w', encoding='utf-8') as f:
                write(nb, f)
        
        # Extract outputs and results
        result = {
            "success": True,
            "step": step,
            "task_path": task_path,
            "executed_notebook": output_path if output_path else None
        }
        
        # Collect outputs from cells
        outputs = []
        errors = []
        
        for i, cell in enumerate(nb.cells):
            if cell.cell_type == 'code' and hasattr(cell, 'outputs'):
                for output in cell.outputs:
                    if output.output_type == 'stream' and output.name == 'stdout':
                        outputs.append(output.text)
                    elif output.output_type == 'error':
                        errors.append({
                            "cell": i,
                            "error": output.ename,
                            "message": output.evalue,
                            "traceback": output.traceback
                        })
        
        result["outputs"] = outputs
        if errors:
            result["errors"] = errors
            result["success"] = len(errors) == 0  # Success only if no errors
        
        # Try to extract specific results based on step
        task_dir = os.path.dirname(task_path)
        
        if step == "run_task":
            result_file = os.path.join(task_dir, "result.json")
            if os.path.exists(result_file):
                result["result_file"] = result_file
                try:
                    with open(result_file, 'r') as f:
                        result["result_data"] = json.load(f)
                except Exception as e:
                    result["result_file_error"] = str(e)
        
        elif step in ["compute_complexity", "task_verification"]:
            response_file = os.path.join(task_dir, f"{step}_response.json")
            if os.path.exists(response_file):
                result["response_file"] = response_file
                try:
                    with open(response_file, 'r') as f:
                        result["response_data"] = json.load(f)
                except Exception as e:
                    result["response_file_error"] = str(e)
        
        elif step == "evaluate":
            # Look for evaluation results
            for pattern in ["evaluate_response.json", "evaluation_result.json"]:
                eval_file = os.path.join(task_dir, pattern)
                if os.path.exists(eval_file):
                    result["evaluation_file"] = eval_file
                    try:
                        with open(eval_file, 'r') as f:
                            result["evaluation_data"] = json.load(f)
                        break
                    except Exception as e:
                        result["evaluation_file_error"] = str(e)
        
        return result
        
    except Exception as e:
        return {
            "success": False,
            "error": f"Failed to execute notebook: {str(e)}",
            "step": step,
            "task_path": task_path
        }


def main():
    parser = argparse.ArgumentParser(description='Execute compute_complexity notebook')
    parser.add_argument('notebook_path', help='Path to the .ipynb file')
    parser.add_argument('task_path', help='Path to the task.json file')
    parser.add_argument('step', help='Validation step to run')
    parser.add_argument('--output', help='Path to save executed notebook')
    parser.add_argument('--json-output', action='store_true', help='Output result as JSON')
    
    args = parser.parse_args()
    
    result = execute_notebook(
        args.notebook_path,
        args.task_path, 
        args.step,
        args.output
    )
    
    if args.json_output:
        print(json.dumps(result, indent=2))
    else:
        if result["success"]:
            print(f"✅ Successfully executed {args.step} step")
            if "outputs" in result:
                print("📝 Outputs:")
                for output in result["outputs"]:
                    print(output.strip())
        else:
            print(f"❌ Failed to execute {args.step} step")
            print(f"Error: {result.get('error', 'Unknown error')}")
            if "errors" in result:
                print("Notebook errors:")
                for error in result["errors"]:
                    print(f"  Cell {error['cell']}: {error['error']} - {error['message']}")
    
    return 0 if result["success"] else 1


if __name__ == "__main__":
    sys.exit(main())