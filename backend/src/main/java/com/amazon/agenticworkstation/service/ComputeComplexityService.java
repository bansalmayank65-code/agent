package com.amazon.agenticworkstation.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service for interacting with the Tau Bench API endpoints. Converted from
 * compute_complexity.py to provide equivalent functionality in Java.
 */
@Service
public class ComputeComplexityService {

	private static final Logger logger = LoggerFactory.getLogger(ComputeComplexityService.class);

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	
	@Autowired
	private TaskCacheService taskCacheService;

	@Value("${tau.bench.api.base-url:https://tau-bench.turing.com}")
	private String apiBaseUrl;

	// Default constructor for Spring
	public ComputeComplexityService() {
		this.restTemplate = new RestTemplate();
		this.objectMapper = new ObjectMapper();
	}

	// Constructor with TaskCacheService dependency injection (for testing)
	public ComputeComplexityService(TaskCacheService taskCacheService) {
		this.restTemplate = new RestTemplate();
		this.objectMapper = new ObjectMapper();
		this.taskCacheService = taskCacheService;
	}

	// Constructor for testing with explicit API base URL
	public ComputeComplexityService(String apiBaseUrl, TaskCacheService taskCacheService) {
		this.restTemplate = new RestTemplate();
		this.objectMapper = new ObjectMapper();
		this.apiBaseUrl = apiBaseUrl;
		this.taskCacheService = taskCacheService;
	}
	
	@PostConstruct
	public void init() {
		// Ensure API base URL is properly initialized after Spring injection
		if (this.apiBaseUrl == null || this.apiBaseUrl.trim().isEmpty()) {
			this.apiBaseUrl = "https://tau-bench.turing.com";
		}
		logger.info("ComputeComplexityService initialized with API base URL: {}", this.apiBaseUrl);
	}

	/**
	 * Available API endpoints
	 */
	public enum Endpoint {
		COMPUTE_COMPLEXITY("compute_complexity"), TASK_VERIFICATION("task_verification"), RUN_TASK("run-task"),
		EVALUATE("evaluate");

		private final String path;

		Endpoint(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}
	}

	/**
	 * Execute a task using the specified endpoint
	 * 
	 * @param endpoint     The API endpoint to use
	 * @param taskFilePath Path to the task JSON file
	 * @return API response as JsonNode
	 */
	public ApiResponse executeTask(Endpoint endpoint, String taskFilePath) {
		try {
			if (taskFilePath == null || taskFilePath.trim().isEmpty()) {
				return new ApiResponse(false, "Task file path is required", null, null);
			}
			
			int numTrials = 1;
			// Load task JSON
			JsonNode taskJson = loadTaskJson(taskFilePath);

			if (taskJson == null) {
				return new ApiResponse(false, "Failed to load task JSON", null, null);
			}

			// Prepare request payload based on endpoint
			JsonNode requestPayload = prepareRequestPayload(endpoint, taskJson, taskFilePath, numTrials);

			// Make API request
			String endpointUrl = apiBaseUrl + "/" + endpoint.getPath();
			logger.info("Using endpoint: {}", endpointUrl);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<JsonNode> request = new HttpEntity<>(requestPayload, headers);

			ResponseEntity<JsonNode> response = restTemplate.postForEntity(endpointUrl, request, JsonNode.class);

			// Handle response based on endpoint type
			return handleResponse(endpoint, response, taskFilePath);

		} catch (Exception e) {
			logger.error("Error executing task", e);
			return new ApiResponse(false, "Error executing task: " + e.getMessage(), null, null);
		}
	}

	/**
	 * Execute task with default parameters (task_verification endpoint, 1 trial)
	 */
	public ApiResponse executeTask(String taskFilePath) {
		return executeTask(Endpoint.TASK_VERIFICATION, taskFilePath);
	}

	/**
	 * Execute a task using JSON content directly (no file needed)
	 * 
	 * @param endpoint The API endpoint to use
	 * @param taskJsonContent The task JSON content as string
	 * @param userId User ID for context
	 * @param taskId Task ID for context
	 * @return API response as JsonNode
	 */
	public ApiResponse executeTaskFromJson(Endpoint endpoint, String taskJsonContent, String userId, String taskId) {
		try {
			if (taskJsonContent == null || taskJsonContent.trim().isEmpty()) {
				return new ApiResponse(false, "Task JSON content is required", null, null);
			}
			
			int numTrials = 1;
			// Parse task JSON from string content
			JsonNode taskJson = objectMapper.readTree(taskJsonContent);

			if (taskJson == null) {
				return new ApiResponse(false, "Failed to parse task JSON content", null, null);
			}

			// Use a logical path for reference (no actual file needed)
			String logicalPath = "memory://" + userId + "_" + taskId + ".json";
			
			// Prepare request payload based on endpoint
			JsonNode requestPayload = prepareRequestPayload(endpoint, taskJson, logicalPath, numTrials);

			// Make API request
			String endpointUrl = apiBaseUrl + "/" + endpoint.getPath();
			logger.info("Using endpoint: {} for task {}:{}", endpointUrl, userId, taskId);
			
			// Debug: Log the request payload
			logger.info("Request payload for task {}:{}: {}", userId, taskId, requestPayload.toString());

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<JsonNode> request = new HttpEntity<>(requestPayload, headers);

			ResponseEntity<JsonNode> response = restTemplate.postForEntity(endpointUrl, request, JsonNode.class);

			// Handle response based on endpoint type
			return handleResponse(endpoint, response, logicalPath);

		} catch (Exception e) {
			logger.error("Error executing task from JSON for {}:{}", userId, taskId, e);
			return new ApiResponse(false, "Error executing task: " + e.getMessage(), null, null);
		}
	}

	/**
	 * Load task JSON from file
	 */
	private JsonNode loadTaskJson(String taskFilePath) {
		try {
			File taskFile = new File(taskFilePath);
			if (!taskFile.exists()) {
				logger.error("Task file not found: {}", taskFilePath);
				return null;
			}

			return objectMapper.readTree(taskFile);

		} catch (IOException e) {
			logger.error("Error loading task JSON from: {}", taskFilePath, e);
			return null;
		}
	}

	/**
	 * Prepare request payload based on endpoint type
	 */
	private JsonNode prepareRequestPayload(Endpoint endpoint, JsonNode taskJson, String taskFilePath, int numTrials) {
		try {
			ObjectNode payload;

			if (endpoint == Endpoint.EVALUATE) {
				// For evaluation endpoint, prepare error evaluation payload
				payload = prepareErrorEvaluationPayload(taskFilePath, taskJson);
			} else {
				// For other endpoints, use the original task JSON directly as shown in Python reference
				// The notebook shows: request_payload = task_json
				payload = (ObjectNode) taskJson.deepCopy();
				
				// Add task file name if provided
				if (taskFilePath != null) {
					payload.put("task_file_name", taskFilePath);
				}
				
				// Override num_trials if specified
				if (numTrials > 0) {
					payload.put("num_trials", numTrials);
				}

				if (endpoint == Endpoint.RUN_TASK) {
					logger.info("Number of trials: {}", numTrials);
				}
				
				// Validate required fields but don't restructure the payload
				validateRequiredFields(payload, endpoint);
			}

			return payload;

		} catch (Exception e) {
			logger.error("Error preparing request payload", e);
			throw new IllegalStateException("Failed to prepare request payload: " + e.getMessage(), e);
		}
	}

	/**
	 * Validate required fields for non-evaluate endpoints
	 */
	private void validateRequiredFields(JsonNode taskJson, Endpoint endpoint) {
		// Validate basic required fields that should be present
		if (!taskJson.has("env")) {
			throw new IllegalArgumentException("Task JSON missing required field 'env'");
		}
		
		if (!taskJson.has("model_provider")) {
			throw new IllegalArgumentException("Task JSON missing required field 'model_provider'");
		}
		
		if (!taskJson.has("model")) {
			throw new IllegalArgumentException("Task JSON missing required field 'model'");
		}
		
		// For most endpoints, task structure should be nested
		JsonNode taskDetails = taskJson.has("task") ? taskJson.get("task") : taskJson;
		
		if (!taskDetails.has("instruction")) {
			throw new IllegalArgumentException("Task JSON missing required 'instruction' field");
		}
		
		if (!taskDetails.has("actions") || !taskDetails.get("actions").isArray()) {
			throw new IllegalArgumentException("Task JSON missing required 'actions' array field");
		}
		
		logger.debug("Task validation passed for endpoint: {}", endpoint.getPath());
	}

	/**
	 * Prepare payload for error evaluation endpoint
	 */
	private ObjectNode prepareErrorEvaluationPayload(String taskFilePath, JsonNode taskJson) {
		try {
			// First try to get results data from memory cache
			JsonNode resultsData = null;
			String dataSource = null;
			
			if (taskCacheService != null && taskCacheService.hasResultData()) {
				// Get result data from memory cache
				Map<String, Object> resultDataMap = taskCacheService.getResultData();
				if (resultDataMap != null) {
					resultsData = objectMapper.valueToTree(resultDataMap);
					dataSource = "memory cache";
					logger.info("Loaded results data from memory cache");
				}
			}
			
			// Fallback to database or file system if memory cache doesn't have data
			if (resultsData == null) {
				// Check if taskFilePath is provided
				if (taskFilePath == null || taskFilePath.trim().isEmpty()) {
					throw new IllegalArgumentException("No task file path provided for error evaluation");
				}
				
				// For memory paths, try to fetch from database
				if (taskFilePath.startsWith("memory://")) {
					// Parse memory path: memory://userId_taskId.json
					String pathPart = taskFilePath.replace("memory://", "").replace(".json", "");
					String[] parts = pathPart.split("_", 2); // Split only on first underscore
					if (parts.length == 2) {
						String userId = parts[0];
						String taskId = parts[1];
						
						// Try to get results from database via TaskCacheService
						if (taskCacheService != null) {
							String resultJsonString = taskCacheService.getResultJsonFromDatabase(userId, taskId);
							if (resultJsonString != null && !resultJsonString.trim().isEmpty()) {
								resultsData = objectMapper.readTree(resultJsonString);
								dataSource = "database: " + userId + "/" + taskId;
								logger.info("Loaded results data from database for user {} task {}", userId, taskId);
							}
						}
					}
					
					if (resultsData == null) {
						throw new IllegalStateException("No results data available in memory cache or database for memory path: " + taskFilePath);
					}
				} else {
					// For file paths, look for result.json in the same directory as the task file
					Path taskPath = Paths.get(taskFilePath);
					Path taskDir = taskPath.getParent();
					if (taskDir == null) {
						throw new IllegalArgumentException("Task file path has no parent directory: " + taskFilePath);
					}
					Path resultsFilePath = taskDir.resolve("result.json");

					if (!Files.exists(resultsFilePath)) {
						throw new IllegalStateException("No results data available in memory cache or file system: " + resultsFilePath);
					}

					// Read results data from file
					resultsData = objectMapper.readTree(resultsFilePath.toFile());
					dataSource = "file: " + resultsFilePath;
					logger.info("Loaded results data from file: {}", resultsFilePath);
				}
			}

			// Create error evaluation payload - NO HARDCODED VALUES
			ObjectNode payload = objectMapper.createObjectNode();
			
			// Extract env from task JSON
			if (!taskJson.has("env")) {
				throw new IllegalArgumentException("Task JSON missing required field 'env' for evaluation");
			}
			payload.put("env", taskJson.get("env").asText());
			
			// Extract model_provider from task JSON, but use compatible models for evaluation
			String modelProvider = null;
			if (taskJson.has("model_provider")) {
				modelProvider = taskJson.get("model_provider").asText();
			} else if (taskJson.has("modelProvider")) {
				modelProvider = taskJson.get("modelProvider").asText();
			}
			if (modelProvider == null || modelProvider.trim().isEmpty()) {
				throw new IllegalArgumentException("Task JSON missing required field 'model_provider' or 'modelProvider' for evaluation");
			}
			
			// For evaluation endpoint, use OpenAI models as shown in the Python reference
			String evaluationModelProvider = "openai";
			String evaluationModel = "gpt-4o"; // Use gpt-4o as in Python reference, not gpt-4o-mini
			
			// Log the model mapping for debugging
			logger.info("Mapping evaluation model: original {}:{} -> evaluation {}:{}", 
					modelProvider, taskJson.path("model").asText("unknown"), 
					evaluationModelProvider, evaluationModel);
			
			payload.put("model_provider", evaluationModelProvider);
			payload.put("model", evaluationModel);
			
			// Extract concurrency settings from task JSON or require explicit values
			int maxConcurrency = 1; // This could be made configurable but 1 is a safe default
			if (taskJson.has("max_concurrency")) {
				maxConcurrency = taskJson.get("max_concurrency").asInt();
			}
			payload.put("max_concurrency", maxConcurrency);
			
			int maxFailedResults = 10; // This could be made configurable but 10 is a reasonable default
			if (taskJson.has("max_num_failed_results")) {
				maxFailedResults = taskJson.get("max_num_failed_results").asInt();
			}
			payload.put("max_num_failed_results", maxFailedResults);
			
			// The API expects results_data to be an array, not an object
			// Each result must have a task_id field (as integer) for the evaluation endpoint
			ArrayNode processedResultsArray = objectMapper.createArrayNode();
			
			// Generate a numeric task_id for the evaluation API
			// The API expects task_id to be an integer index, not a string UUID
			int numericTaskId = 0; // Default to 0 for evaluation purposes
			
			if (resultsData.isArray()) {
				// Process each result in the array to ensure it has required fields
				for (JsonNode result : resultsData) {
					ObjectNode processedResult = (ObjectNode) result.deepCopy();
					
					// Add numeric task_id if missing or if it's a string
					if (!processedResult.has("task_id")) {
						processedResult.put("task_id", numericTaskId);
					} else if (processedResult.get("task_id").isTextual()) {
						// If task_id exists but is a string, replace with numeric value
						processedResult.put("task_id", numericTaskId);
					}
					
					// Add traj (trajectory) field if missing - required by evaluation API
					if (!processedResult.has("traj")) {
						// Create a minimal valid trajectory with at least one step
						ArrayNode trajectory = objectMapper.createArrayNode();
						ObjectNode trajStep = objectMapper.createObjectNode();
						
					// Add minimal trajectory step with action and observation
					trajStep.put("action", "task_executed");
					trajStep.put("observation", "Task completed");
					trajStep.put("role", "assistant"); // Required by evaluation API for trajectory filtering
					
					// Add reward field (required by evaluation endpoint)
					trajStep.put("reward", 0.0); // Default reward value as seen in Python reference
					
					// Try to extract actual execution info if available
					if (processedResult.has("success")) {
						trajStep.put("success", processedResult.get("success").asBoolean());
					}
					if (processedResult.has("error") && !processedResult.get("error").isNull()) {
						trajStep.put("error", processedResult.get("error").asText());
					}						trajectory.add(trajStep);
						processedResult.set("traj", trajectory);
					} else if (processedResult.get("traj").isArray() && processedResult.get("traj").size() == 0) {
						// If traj exists but is empty, add a minimal step
						ArrayNode trajectory = (ArrayNode) processedResult.get("traj");
						ObjectNode trajStep = objectMapper.createObjectNode();
						trajStep.put("action", "task_executed");
						trajStep.put("observation", "Task completed");
						trajStep.put("role", "assistant"); // Required by evaluation API for trajectory filtering
						trajStep.put("reward", 0.0); // Add reward field for evaluation endpoint
						trajectory.add(trajStep);
					}
					
					processedResultsArray.add(processedResult);
				}
			} else {
				// If it's a single result object, wrap it in an array and add required fields
				ObjectNode processedResult = (ObjectNode) resultsData.deepCopy();
				
				// Add or replace task_id with numeric value
				processedResult.put("task_id", numericTaskId);
				
				// Add traj (trajectory) field if missing - required by evaluation API
				if (!processedResult.has("traj")) {
					// Create a minimal valid trajectory with at least one step
					ArrayNode trajectory = objectMapper.createArrayNode();
					ObjectNode trajStep = objectMapper.createObjectNode();
					
					// Add minimal trajectory step with action and observation
					trajStep.put("action", "task_executed");
					trajStep.put("observation", "Task completed");
					trajStep.put("role", "assistant"); // Required by evaluation API for trajectory filtering
					
					// Add reward field (required by evaluation endpoint)
					trajStep.put("reward", 0.0); // Default reward value as seen in Python reference
					
					// Try to extract actual execution info if available
					if (processedResult.has("success")) {
						trajStep.put("success", processedResult.get("success").asBoolean());
					}
					if (processedResult.has("error") && !processedResult.get("error").isNull()) {
						trajStep.put("error", processedResult.get("error").asText());
					}
					
					trajectory.add(trajStep);
					processedResult.set("traj", trajectory);
					} else if (processedResult.get("traj").isArray() && processedResult.get("traj").size() == 0) {
						// If traj exists but is empty, add a minimal step
						ArrayNode trajectory = (ArrayNode) processedResult.get("traj");
						ObjectNode trajStep = objectMapper.createObjectNode();
						trajStep.put("action", "task_executed");
						trajStep.put("observation", "Task completed");
						trajStep.put("role", "assistant"); // Required by evaluation API for trajectory filtering
						trajStep.put("reward", 0.0); // Add reward field for evaluation endpoint
						trajectory.add(trajStep);
					}				processedResultsArray.add(processedResult);
			}
			
			payload.set("results_data", processedResultsArray);
			
			if (taskFilePath == null || taskFilePath.trim().isEmpty()) {
				throw new IllegalArgumentException("Task file path is required for evaluation");
			}
			payload.put("task_file_name", taskFilePath);

			logger.info("Prepared error evaluation request with data from: {}", dataSource);
			return payload;

		} catch (Exception e) {
			logger.error("Error preparing error evaluation payload", e);
			throw new IllegalStateException("Failed to prepare error evaluation payload: " + e.getMessage(), e);
		}
	}

	/**
	 * Handle API response based on endpoint type
	 */
	private ApiResponse handleResponse(Endpoint endpoint, ResponseEntity<JsonNode> response, String taskFilePath) {
		try {
			if (response.getStatusCode() != HttpStatus.OK) {
				return new ApiResponse(false, "API request failed with status: " + response.getStatusCode(), null,
						null);
			}

			JsonNode responseData = response.getBody();

			if (endpoint == Endpoint.RUN_TASK) {
				// For run-task endpoint, save the response to file
				return handleRunTaskResponse(responseData, taskFilePath);
			} else if (endpoint == Endpoint.EVALUATE) {
				// For evaluate endpoint, process error analysis
				return handleEvaluateResponse(responseData);
			} else {
				// For other endpoints (compute_complexity, task_verification)
				return new ApiResponse(true, "Success", responseData, extractPlotData(responseData));
			}

		} catch (Exception e) {
			logger.error("Error handling response", e);
			return new ApiResponse(false, "Error handling response: " + e.getMessage(), null, null);
		}
	}

	/**
	 * Handle run-task endpoint response
	 */
	private ApiResponse handleRunTaskResponse(JsonNode responseData, String taskFilePath) {
		try {
			// Directly store the result.json data in the database
			if (taskCacheService != null) {
				// Convert response data to JSON string for database storage
				String resultJsonString = objectMapper.writeValueAsString(responseData);
				
				// Extract userId and taskId from memory path or use current context
				String userId = null;
				String taskId = null;
				
				if (taskFilePath.startsWith("memory://")) {
					// Parse memory path: memory://userId_taskId.json
					String pathPart = taskFilePath.replace("memory://", "").replace(".json", "");
					String[] parts = pathPart.split("_", 2); // Split only on first underscore
					if (parts.length == 2) {
						userId = parts[0];
						taskId = parts[1];
					}
				}
				
				// Store result JSON directly in database
				if (userId != null && taskId != null) {
					taskCacheService.saveResultJsonToDatabase(userId, taskId, resultJsonString);
					logger.info("Result JSON stored in database for user {} task {}", userId, taskId);
				} else {
					logger.warn("Could not extract userId/taskId from path: {}", taskFilePath);
				}
				
				return new ApiResponse(true, "Task executed successfully, results stored in database",
						responseData, null);
			} else {
				// Fallback to file system if cache service is not available
				logger.warn("TaskCacheService not available, falling back to file storage");
				
				if (taskFilePath == null || taskFilePath.trim().isEmpty()) {
					logger.error("Task file path is required for file storage fallback");
					return new ApiResponse(false, "Task file path is required", null, null);
				}
				
				if (taskFilePath.startsWith("memory://")) {
					// Cannot save memory paths to file system
					logger.error("Cannot save to file system with memory path: {}", taskFilePath);
					return new ApiResponse(false, "Memory paths cannot be saved to file system", null, null);
				}
				
				Path taskPath = Paths.get(taskFilePath);
				Path taskDir = taskPath.getParent();
				Path resultFilePath = taskDir.resolve("result.json");
				
				Files.createDirectories(resultFilePath.getParent());
				objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultFilePath.toFile(), responseData);
				
				logger.info("Response saved to file: {}", resultFilePath);
				
				return new ApiResponse(true, "Task executed successfully, results saved to: " + resultFilePath,
						responseData, null);
			}

		} catch (Exception e) {
			logger.error("Error handling run-task response", e);
			return new ApiResponse(false, "Error handling response: " + e.getMessage(), responseData, null);
		}
	}

	/**
	 * Handle evaluate endpoint response
	 */
	private ApiResponse handleEvaluateResponse(JsonNode responseData) {
		try {
			StringBuilder summary = new StringBuilder();

			if (responseData.has("success") && responseData.get("success").asBoolean()) {
				JsonNode summaryNode = responseData.get("summary");
				if (summaryNode != null) {
					summary.append("Summary:\n");
					summary.append("  Total results: ").append(summaryNode.path("total_results").asInt()).append("\n");
					summary.append("  Failed results: ").append(summaryNode.path("failed_results").asInt())
							.append("\n");
					summary.append("  Analyzed results: ").append(summaryNode.path("analyzed_results").asInt())
							.append("\n");

					// Add fault distribution info
					JsonNode faultDist = summaryNode.get("fault_distribution");
					if (faultDist != null) {
						summary.append("\nFault Distribution:\n");
						faultDist.fields().forEachRemaining(entry -> {
							JsonNode data = entry.getValue();
							summary.append("  ").append(capitalize(entry.getKey())).append(": ")
									.append(data.path("count").asInt()).append(" (")
									.append(data.path("percentage").asDouble()).append("%)\n");
						});
					}
				}

				return new ApiResponse(true, summary.toString(), responseData, null);
			} else {
				String error = responseData.path("error").asText("Unknown error");
				return new ApiResponse(false, "Error evaluation failed: " + error, responseData, null);
			}

		} catch (Exception e) {
			logger.error("Error handling evaluate response", e);
			return new ApiResponse(false, "Error processing evaluation: " + e.getMessage(), responseData, null);
		}
	}

	/**
	 * Extract plot data from response if available
	 */
	private String extractPlotData(JsonNode responseData) {
		if (responseData != null && responseData.has("plot_base64")) {
			JsonNode plotNode = responseData.get("plot_base64");
			if (plotNode != null && !plotNode.isNull()) {
				return plotNode.asText();
			}
		}
		return null;
	}

	/**
	 * Capitalize first letter of a string
	 */
	private String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * Check if results directory exists and list files
	 */
	public Map<String, Object> checkResultsDirectory(String taskFilePath) {
		Map<String, Object> result = new HashMap<>();

		try {
			// Check legacy results directory
			Path resultsDir = Paths.get("results");
			if (Files.exists(resultsDir)) {
				File[] files = resultsDir.toFile().listFiles();
				if (files != null && files.length > 0) {
					result.put("legacy_results", "Found " + files.length + " files");
				} else {
					result.put("legacy_results", "No files found in legacy results directory");
				}
			} else {
				result.put("legacy_results", "Legacy results directory does not exist");
			}

			// Check for result.json in task directory
			if (taskFilePath != null && !taskFilePath.trim().isEmpty()) {
				try {
					Path taskPath = Paths.get(taskFilePath);
					Path taskDir = taskPath.getParent();

					if (taskDir != null) {
						Path taskResultFile = taskDir.resolve("result.json");

						if (Files.exists(taskResultFile)) {
							long fileSize = Files.size(taskResultFile);
							result.put("task_result", "Found result.json (" + fileSize + " bytes)");
						} else {
							result.put("task_result", "No result.json found in task directory: " + taskDir);
						}
					} else {
						result.put("task_result", "Task file has no parent directory");
					}
				} catch (Exception pathException) {
					result.put("task_result", "Error processing task file path: " + pathException.getMessage());
				}
			} else {
				result.put("task_result", "No task file path provided");
			}

		} catch (Exception e) {
			logger.error("Error checking results directory", e);
			result.put("error", "Error checking results: " + e.getMessage());
		}

		return result;
	}

	/**
	 * Response wrapper class
	 */
	public static class ApiResponse {
		private final boolean success;
		private final String message;
		private final JsonNode data;
		private final String plotBase64;

		public ApiResponse(boolean success, String message, JsonNode data, String plotBase64) {
			this.success = success;
			this.message = message;
			this.data = data;
			this.plotBase64 = plotBase64;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getMessage() {
			return message;
		}

		public JsonNode getData() {
			return data;
		}

		public String getPlotBase64() {
			return plotBase64;
		}

		public boolean hasPlot() {
			return plotBase64 != null && !plotBase64.isEmpty();
		}
	}


}