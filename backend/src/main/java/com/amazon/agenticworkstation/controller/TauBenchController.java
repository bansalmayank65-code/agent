package com.amazon.agenticworkstation.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.amazon.agenticworkstation.service.ComputeComplexityService;
import com.amazon.agenticworkstation.service.ComputeComplexityService.ApiResponse;
import com.amazon.agenticworkstation.service.ComputeComplexityService.Endpoint;
import com.amazon.agenticworkstation.service.TaskCacheService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * REST Controller for Tau Bench API operations. Provides endpoints to interact
 * with the Tau Bench service for task validation and execution.
 */
@RestController
@RequestMapping("/api/tau-bench")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"}, allowCredentials = "false")
public class TauBenchController {

	@Autowired
	private ComputeComplexityService computeComplexityService;
	
	@Autowired
	private TaskCacheService taskCacheService;
	
	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Execute a task using the specified endpoint
	 */
	@PostMapping("/execute")
	public ResponseEntity<TaskExecutionResponse> executeTask(@RequestBody TaskExecutionRequest request) {

		try {
			Endpoint endpoint = Endpoint.valueOf(request.getEndpoint().toUpperCase());
			ApiResponse response = computeComplexityService.executeTask(endpoint, request.getTaskFilePath());

			return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
					response.getData(), response.getPlotBase64(), response.hasPlot()));
		} catch (Exception e) {
			return ResponseEntity.ok(new TaskExecutionResponse(false, "Task execution failed: " + e.getMessage(),
					null, null, false));
		}
	}
					
	/**
	 * Execute task validation using user_id and task_id (database-based, no temp files)
	 */
	@PostMapping("/validate-by-id")
	public ResponseEntity<TaskExecutionResponse> validateTaskById(
			@RequestParam String userId, 
			@RequestParam String taskId) {
		
		try {
			// Load task into cache if not already present
			taskCacheService.loadTaskIntoCache(userId, taskId);
			
			// Set current context for legacy compatibility
			taskCacheService.setCurrentContext(userId, taskId);
			
			// Get task JSON directly from cache/database
			String taskJson = taskCacheService.aggregatedJson(userId, taskId);
			
			if (taskJson == null || taskJson.trim().isEmpty()) {
				return ResponseEntity.ok(new TaskExecutionResponse(false, 
					"Task not found in database for userId: " + userId + ", taskId: " + taskId,
					null, null, false));
			}
			
			// Execute validation using JSON content directly (no file needed)
			ApiResponse response = computeComplexityService.executeTaskFromJson(Endpoint.TASK_VERIFICATION, taskJson, userId, taskId);
			
			return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
					response.getData(), response.getPlotBase64(), response.hasPlot()));
		} catch (Exception e) {
			return ResponseEntity.ok(new TaskExecutionResponse(false, "Validation failed: " + e.getMessage(),
					null, null, false));
		}
	}
	
	/**
	 * Execute task using user_id and task_id (database-based, no temp files)
	 */
	@PostMapping("/execute-by-id")
	public ResponseEntity<TaskExecutionResponse> executeTaskById(
			@RequestParam String userId, 
			@RequestParam String taskId,
			@RequestParam String endpoint) {
		
		try {
			// Load task into cache if not already present
			taskCacheService.loadTaskIntoCache(userId, taskId);
			
			// Set current context for legacy compatibility
			taskCacheService.setCurrentContext(userId, taskId);
			
			// Get task JSON directly from cache/database
			String taskJson = taskCacheService.aggregatedJson(userId, taskId);
			
			if (taskJson == null || taskJson.trim().isEmpty()) {
				return ResponseEntity.ok(new TaskExecutionResponse(false, 
					"Task not found in database for userId: " + userId + ", taskId: " + taskId,
					null, null, false));
			}
			
			// Execute task using JSON content directly (no file needed)
			Endpoint ep = Endpoint.valueOf(endpoint.toUpperCase());
			ApiResponse response = computeComplexityService.executeTaskFromJson(ep, taskJson, userId, taskId);
			
			// Store result in cache if available (skip run_task as it's handled in response handler)
			if (response.isSuccess() && response.getData() != null && !"run_task".equalsIgnoreCase(endpoint)) {
				// For non-run_task endpoints, wrap with metadata and store in cache
				Map<String, Object> resultData = new HashMap<>();
				resultData.put("endpoint", endpoint);
				resultData.put("result", response.getData());
				resultData.put("timestamp", System.currentTimeMillis());
				
				taskCacheService.storeResultData(userId, taskId, resultData, "memory://" + userId + "_" + taskId);
			}
			
			return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
					response.getData(), response.getPlotBase64(), response.hasPlot()));
		} catch (Exception e) {
			return ResponseEntity.ok(new TaskExecutionResponse(false, "Task execution failed: " + e.getMessage(),
					null, null, false));
		}
	}

	/**
	 * Execute task with default endpoint (task_verification)
	 */
	@PostMapping("/execute/default")
	public ResponseEntity<TaskExecutionResponse> executeTaskDefault(@RequestParam String taskFilePath) {

		ApiResponse response = computeComplexityService.executeTask(taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Execute compute complexity analysis
	 */
	@PostMapping("/compute-complexity")
	public ResponseEntity<TaskExecutionResponse> computeComplexity(@RequestParam String taskFilePath,
			@RequestParam(defaultValue = "1") int numTrials) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.COMPUTE_COMPLEXITY, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Execute task verification
	 */
	@PostMapping("/task-verification")
	public ResponseEntity<TaskExecutionResponse> taskVerification(@RequestParam String taskFilePath,
			@RequestParam(defaultValue = "1") int numTrials) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.TASK_VERIFICATION, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Run task execution
	 */
	@PostMapping("/run-task")
	public ResponseEntity<TaskExecutionResponse> runTask(@RequestParam String taskFilePath,
			@RequestParam(defaultValue = "1") int numTrials) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.RUN_TASK, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Evaluate task results
	 */
	@PostMapping("/evaluate")
	public ResponseEntity<TaskExecutionResponse> evaluate(@RequestParam String taskFilePath) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.EVALUATE, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Check results directory status
	 */
	@GetMapping("/results-status")
	public ResponseEntity<Map<String, Object>> checkResultsStatus(@RequestParam String taskFilePath) {

		Map<String, Object> status = computeComplexityService.checkResultsDirectory(taskFilePath);
		return ResponseEntity.ok(status);
	}

	/**
	 * Get available endpoints
	 */
	@GetMapping("/endpoints")
	public ResponseEntity<String[]> getAvailableEndpoints() {
		String[] endpoints = new String[Endpoint.values().length];
		for (int i = 0; i < Endpoint.values().length; i++) {
			endpoints[i] = Endpoint.values()[i].name().toLowerCase();
		}
		return ResponseEntity.ok(endpoints);
	}

	/**
	 * Get raw result.json content from run_task validation (database storage)
	 */
	@GetMapping("/result.json")
	public ResponseEntity<Map<String, Object>> getResultJson(
			@RequestParam String userId, 
			@RequestParam String taskId) {
		try {
			// Get raw result.json data directly from database
			String resultJson = taskCacheService.getResultJsonFromDatabase(userId, taskId);
			
			if (resultJson == null || resultJson.trim().isEmpty()) {
				Map<String, Object> response = new HashMap<>();
				response.put("success", false);
				response.put("message", "No result.json data available for userId: " + userId + ", taskId: " + taskId);
				response.put("data", null);
				return ResponseEntity.ok(response);
			}

			// Parse the JSON and return as object
			@SuppressWarnings("unchecked")
			Map<String, Object> resultData = objectMapper.readValue(resultJson, Map.class);
			
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "result.json retrieved successfully");
			response.put("data", resultData);
			response.put("userId", userId);
			response.put("taskId", taskId);
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "Error retrieving result.json: " + e.getMessage());
			response.put("data", null);
			return ResponseEntity.ok(response);
		}
	}

	/**
	 * Get result.json content from memory cache using userId and taskId
	 */
	@GetMapping("/result")
	public ResponseEntity<Map<String, Object>> getResult(
			@RequestParam String userId, 
			@RequestParam String taskId) {
		try {
			// Set current context for the specified user and task
			taskCacheService.setCurrentContext(userId, taskId);
			
			// Check if result data exists for this specific user/task
			if (!taskCacheService.hasResultData()) {
				Map<String, Object> response = new HashMap<>();
				response.put("success", false);
				response.put("message", "No result data available for userId: " + userId + ", taskId: " + taskId);
				response.put("data", null);
				return ResponseEntity.ok(response);
			}

			// Get result data from memory cache for current context
			Map<String, Object> resultData = taskCacheService.getResultData();
			String cachedFilePath = taskCacheService.getResultFilePath();

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Result retrieved successfully for userId: " + userId + ", taskId: " + taskId);
			response.put("data", resultData);
			response.put("userId", userId);
			response.put("taskId", taskId);
			response.put("filePath", cachedFilePath != null ? cachedFilePath : "memory://" + userId + "_" + taskId + ".result.json");
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Error reading result data for userId: " + userId + ", taskId: " + taskId + " - " + e.getMessage());
			errorResponse.put("data", null);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Generate edges using EdgeGenerator service
	 */
	@PostMapping("/generate-edges")
	public ResponseEntity<Map<String, Object>> generateEdges(@RequestParam String taskFilePath) {
		try {
			// Get the current task from cache service
			com.amazon.agenticworkstation.dto.TaskDto currentTask = taskCacheService.buildAggregatedTaskDto();
			
			if (currentTask == null || currentTask.getTask() == null || 
				currentTask.getTask().getActions() == null || currentTask.getTask().getActions().isEmpty()) {
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "No task or actions found to generate edges from");
				return ResponseEntity.badRequest().body(errorResponse);
			}

			// Generate edges using EdgeGenerator
			List<com.amazon.agenticworkstation.dto.TaskDto.EdgeDto> generatedEdges = 
				com.amazon.agenticworkstation.service.EdgeGenerator.edgesFromActions(currentTask.getTask().getActions());

			// Convert EdgeDto to Map format for response
			List<Map<String, Object>> edgesList = new ArrayList<>();
			for (com.amazon.agenticworkstation.dto.TaskDto.EdgeDto edge : generatedEdges) {
				Map<String, Object> edgeMap = new HashMap<>();
				edgeMap.put("from", edge.getFrom());
				edgeMap.put("to", edge.getTo());
				
				// Convert connection to map
				if (edge.getConnection() != null) {
					Map<String, Object> connectionMap = new HashMap<>();
					connectionMap.put("output", edge.getConnection().getOutput());
					connectionMap.put("input", edge.getConnection().getInput());
					edgeMap.put("connection", connectionMap);
				}
				
				edgesList.add(edgeMap);
			}

			Map<String, Object> response = Map.of(
				"success", true,
				"message", "Edges generated successfully using EdgeGenerator",
				"edges", edgesList,
				"edgeCount", edgesList.size()
			);
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("error", "Error generating edges: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Request DTO for task execution
	 */
	public static class TaskExecutionRequest {
		private String endpoint;
		private String taskFilePath;

		public TaskExecutionRequest() {
		}

		public TaskExecutionRequest(String endpoint, String taskFilePath) {
			this.endpoint = endpoint;
			this.taskFilePath = taskFilePath;
		}

		// Getters and setters
		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getTaskFilePath() {
			return taskFilePath;
		}

		public void setTaskFilePath(String taskFilePath) {
			this.taskFilePath = taskFilePath;
		}

	}

	/**
	 * Response DTO for task execution
	 */
	public static class TaskExecutionResponse {
		private boolean success;
		private String message;
		private JsonNode data;
		private String plotBase64;
		private boolean hasPlot;

		public TaskExecutionResponse() {
		}

		public TaskExecutionResponse(boolean success, String message, JsonNode data, String plotBase64,
				boolean hasPlot) {
			this.success = success;
			this.message = message;
			this.data = data;
			this.plotBase64 = plotBase64;
			this.hasPlot = hasPlot;
		}

		// Getters and setters
		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public JsonNode getData() {
			return data;
		}

		public void setData(JsonNode data) {
			this.data = data;
		}

		public String getPlotBase64() {
			return plotBase64;
		}

		public void setPlotBase64(String plotBase64) {
			this.plotBase64 = plotBase64;
		}

		public boolean isHasPlot() {
			return hasPlot;
		}

		public void setHasPlot(boolean hasPlot) {
			this.hasPlot = hasPlot;
		}
	}
}