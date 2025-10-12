# Compute Complexity Service - Java Implementation

This Java implementation provides the same functionality as the original Python script `compute_complexity.py`, converted to work within the Spring Boot agentic workstation backend.

## Overview

The `ComputeComplexityService` is a Spring service that interacts with the Tau Bench API to perform various task operations:

- **Compute Complexity**: Analyze task complexity
- **Task Verification**: Validate task definitions  
- **Run Task**: Execute tasks and get results
- **Evaluate**: Analyze task execution results for errors

## Key Features

### 1. Multiple API Endpoints
- `compute_complexity` - Analyzes task complexity
- `task_verification` - Validates task structure
- `run_task` - Executes tasks and saves results
- `evaluate` - Performs error analysis on task results

### 2. File Management
- Automatic loading of task JSON files
- Saving execution results to `result.json`
- Support for both absolute and relative file paths
- Directory status checking and validation

### 3. Response Handling
- Structured response wrapper (`ApiResponse`)
- Plot data extraction (base64 encoded images)
- Error handling with detailed messages
- JSON data processing and validation

## Configuration

Configure the Tau Bench API in `application.yml`:

```yaml
tau:
  bench:
    api:
      base-url: https://tau-bench.turing.com  # or http://localhost:8000 for local development
      connect-timeout: 30000   # 30 seconds
      read-timeout: 300000     # 5 minutes (for long-running tasks)
```

## REST API Endpoints

The service is exposed via REST endpoints in `TauBenchController`:

### Execute Task (Generic)
```
POST /api/tau-bench/execute
Content-Type: application/json

{
  "endpoint": "task_verification",
  "taskFilePath": "/path/to/task.json",
  "numTrials": 1
}
```

### Specific Operations
- `POST /api/tau-bench/compute-complexity` - Compute task complexity
- `POST /api/tau-bench/task-verification` - Verify task structure
- `POST /api/tau-bench/run-task` - Execute task
- `POST /api/tau-bench/evaluate` - Analyze results

### Utility Endpoints
- `GET /api/tau-bench/endpoints` - List available endpoints
- `GET /api/tau-bench/results-status` - Check results directory status

## Usage Examples

### Java Service Usage
```java
@Autowired
private ComputeComplexityService computeComplexityService;

// Execute task verification
ApiResponse response = computeComplexityService.executeTask(
    Endpoint.TASK_VERIFICATION, 
    "/path/to/task.json", 
    1
);

if (response.isSuccess()) {
    JsonNode data = response.getData();
    if (response.hasPlot()) {
        String plotBase64 = response.getPlotBase64();
        // Display or save plot
    }
}
```

### REST API Usage
```bash
# Verify a task
curl -X POST http://localhost:8080/api/tau-bench/task-verification \
  -H "Content-Type: application/json" \
  -d '{"taskFilePath": "/path/to/task.json", "numTrials": 1}'

# Execute a task
curl -X POST http://localhost:8080/api/tau-bench/run-task \
  -H "Content-Type: application/json" \
  -d '{"taskFilePath": "/path/to/task.json", "numTrials": 1}'
```

## Error Handling

The service provides comprehensive error handling:

1. **File Not Found**: Returns error when task JSON file doesn't exist
2. **API Errors**: Handles HTTP errors from Tau Bench API
3. **JSON Parsing**: Manages malformed JSON responses
4. **Network Issues**: Timeout and connection error handling

## Key Differences from Python Version

### Advantages of Java Implementation
- **Type Safety**: Strong typing prevents runtime errors
- **Spring Integration**: Seamless integration with Spring Boot ecosystem  
- **REST API**: Direct HTTP endpoint exposure
- **Dependency Injection**: Configurable and testable components
- **Enterprise Features**: Built-in logging, monitoring, security

### Functional Equivalence
- All Python functionality preserved
- Same API endpoints and parameters
- Identical request/response handling
- File management and result saving
- Plot data extraction and processing

## File Structure

```
backend/src/main/java/com/amazon/agenticworkstation/
├── service/
│   └── ComputeComplexityService.java      # Main service implementation
├── controller/
│   └── TauBenchController.java            # REST API endpoints
└── config/
    └── TauBenchConfig.java                # Configuration properties

backend/src/test/java/com/amazon/agenticworkstation/
└── service/
    └── ComputeComplexityServiceTest.java  # Unit tests
```

## Testing

The service includes comprehensive unit tests covering:
- Successful API interactions
- Error handling scenarios  
- File operations
- Response processing
- Configuration validation

Run tests with:
```bash
mvn test
```

## Dependencies

The implementation uses standard Spring Boot dependencies:
- `spring-boot-starter-web` - REST API and HTTP client
- `jackson-databind` - JSON processing
- `spring-boot-starter-test` - Testing framework

No additional external dependencies required beyond the existing project setup.