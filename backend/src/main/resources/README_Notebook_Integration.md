# TauBench Validation Service - Notebook Integration

This service now executes the `compute_complexity.ipynb` notebook programmatically to perform validation steps against the remote tau-bench API.

## 🏗️ Architecture

The service has been refactored to use the original Jupyter notebook directly instead of reimplementing the logic in Java. This ensures:

- ✅ **100% consistency** with the original notebook behavior
- ✅ **Easier maintenance** - updates only needed in one place
- ✅ **Full feature support** - all notebook capabilities available
- ✅ **Error handling** - captures notebook execution errors

## 📋 Prerequisites

### Python Environment
The service requires Python 3.8+ with specific packages. Run the setup script:

**Windows:**
```bash
cd src/main/resources
setup_python_env.bat
```

**Linux/Mac:**
```bash
cd src/main/resources
chmod +x setup_python_env.sh
./setup_python_env.sh
```

### Required Files
- `compute_complexity.ipynb` - The original notebook (already included)
- `run_notebook.py` - Python script to execute notebook programmatically
- `requirements.txt` - Python package dependencies

## 🔧 How It Works

### 1. Java Service Call
```java
TauBenchValidationService service = new TauBenchValidationService();
Map<String, Object> result = service.run("compute_complexity", repositoryPath);
```

### 2. Notebook Execution
The service:
1. Locates the notebook and Python script in classpath
2. Modifies notebook parameters (task_path, selected_endpoint)
3. Executes the notebook using `nbclient`
4. Captures outputs and results
5. Returns structured JSON response

### 3. Process Flow
```
Java Service → Python Script → Notebook Execution → Results Collection → Java Response
```

## 📊 Supported Steps

- **compute_complexity** - Calculate task complexity metrics
- **task_verification** - Verify task structure and validity  
- **run_task** - Execute the task and save results
- **evaluate** - Analyze task execution results

## 📁 Output Files

The service creates several output files in the repository directory:

- `{step}_executed_notebook.ipynb` - Executed notebook with outputs
- `result.json` - Task execution results (for run_task)
- `{step}_response.json` - API response data
- Various analysis files depending on the step

## 🎯 Response Format

```json
{
  "success": true,
  "step": "compute_complexity",
  "timestamp": "2025-10-05T10:30:00Z",
  "notebook_result": {
    "success": true,
    "step": "compute_complexity",
    "task_path": "/path/to/task.json",
    "outputs": ["Execution logs..."],
    "response_data": { "complexity": "medium", ... }
  },
  "executed_notebook": "/path/to/executed_notebook.ipynb",
  "response_file": "/path/to/response.json"
}
```

## 🔍 Error Handling

The service handles various error scenarios:

- **Missing Python/packages** - Clear setup instructions
- **Notebook execution errors** - Captured in response
- **Timeout** - 10-minute execution limit
- **File not found** - Detailed error messages

## 🧪 Testing

Test the setup:
```bash
# Install dependencies
setup_python_env.bat  # or .sh

# Test notebook execution
python run_notebook.py compute_complexity.ipynb /path/to/task.json compute_complexity --json-output
```

## 🚀 Integration

The service is used by the validation endpoints in the REST API:

```java
@PostMapping("/run-step")
public ResponseEntity<Map<String, Object>> runValidationStep(
    @RequestBody Map<String, String> request,
    HttpServletRequest httpRequest
) {
    String step = request.get("step");
    Path repositoryPath = getRepositoryPath(httpRequest);
    
    Map<String, Object> result = tauBenchService.run(step, repositoryPath);
    return ResponseEntity.ok(result);
}
```

## 🔄 Fallback Option

A `runWithRestApi()` method is available for fallback if notebook execution is not available, though it's currently not implemented (the notebook approach is preferred).

## 📝 Maintenance

To update validation logic:
1. Modify `compute_complexity.ipynb` 
2. Test locally
3. Redeploy - no Java code changes needed!

This approach significantly simplifies maintenance and ensures the service always matches the notebook behavior.