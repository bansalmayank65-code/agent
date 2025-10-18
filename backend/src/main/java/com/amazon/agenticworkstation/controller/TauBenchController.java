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

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(
					new TaskExecutionResponse(false, "Invalid endpoint: " + request.getEndpoint(), null, null, false));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(
					new TaskExecutionResponse(false, "Error executing task: " + e.getMessage(), null, null, false));
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
	public ResponseEntity<TaskExecutionResponse> computeComplexity(@RequestParam(required = false) String taskFilePath,
			@RequestParam(defaultValue = "1") int numTrials) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.COMPUTE_COMPLEXITY, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Execute task verification
	 */
	@PostMapping("/task-verification")
	public ResponseEntity<TaskExecutionResponse> taskVerification(@RequestParam(required = false) String taskFilePath,
			@RequestParam(defaultValue = "1") int numTrials) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.TASK_VERIFICATION, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Run task execution
	 */
	@PostMapping("/run-task")
	public ResponseEntity<TaskExecutionResponse> runTask(@RequestParam(required = false) String taskFilePath,
			@RequestParam(defaultValue = "1") int numTrials) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.RUN_TASK, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Evaluate task results
	 */
	@PostMapping("/evaluate")
	public ResponseEntity<TaskExecutionResponse> evaluate(@RequestParam(required = false) String taskFilePath) {

		ApiResponse response = computeComplexityService.executeTask(Endpoint.EVALUATE, taskFilePath);

		return ResponseEntity.ok(new TaskExecutionResponse(response.isSuccess(), response.getMessage(),
				response.getData(), response.getPlotBase64(), response.hasPlot()));
	}

	/**
	 * Check results directory status
	 */
	@GetMapping("/results-status")
	public ResponseEntity<Map<String, Object>> checkResultsStatus(@RequestParam(required = false) String taskFilePath) {

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
	 * Get result.json content from memory cache
	 */
	@GetMapping("/result")
	public ResponseEntity<Map<String, Object>> getResult(@RequestParam(required = false) String taskFilePath) {
		try {
			// Check if result data exists in memory cache
			if (!taskCacheService.hasResultData()) {
				Map<String, Object> response = new HashMap<>();
				response.put("success", false);
				response.put("message", "No result data available in memory cache");
				response.put("data", null);
				return ResponseEntity.ok(response);
			}

			// Get result data from memory cache
			Map<String, Object> resultData = taskCacheService.getResultData();
			String cachedFilePath = taskCacheService.getResultFilePath();

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Result retrieved successfully from memory cache");
			response.put("data", resultData);
			response.put("filePath", cachedFilePath != null ? cachedFilePath : "memory://result.json");
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Error reading result data: " + e.getMessage());
			errorResponse.put("data", null);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Generate edges using EdgeGenerator service
	 */
	@PostMapping("/generate-edges")
	public ResponseEntity<Map<String, Object>> generateEdges(@RequestParam(required = false) String taskFilePath) {
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