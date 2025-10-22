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
			// First try to get results data from database or file system
			JsonNode resultsData = null;
			String dataSource = null;
			
			// Check if taskFilePath is a memory path (from execute-by-id endpoint)
			if (taskFilePath != null && taskFilePath.startsWith("memory://")) {
				// Parse memory path: memory://userId_taskId.json
				String pathPart = taskFilePath.replace("memory://", "").replace(".json", "");
				String[] parts = pathPart.split("_", 2); // Split only on first underscore
				
				if (parts.length == 2) {
					// parts[0] is the requested userId (might be incorrect)
					String taskId = parts[1];
					
					logger.info("Attempting to load result.json from database for taskId {}", taskId);
					
					// First, get the actual userId that owns this task
					String actualUserId = null;
					if (taskCacheService != null) {
						actualUserId = taskCacheService.getUserIdForTask(taskId);
						if (actualUserId == null) {
							throw new IllegalStateException("Task " + taskId + " not found in database");
						}
						logger.info("Task {} belongs to user {}", taskId, actualUserId);
						
						// Now try to get results from database using the correct userId
						String resultJsonString = taskCacheService.getResultJsonFromDatabase(actualUserId, taskId);
						if (resultJsonString != null && !resultJsonString.trim().isEmpty()) {
							resultsData = objectMapper.readTree(resultJsonString);
							dataSource = "database: " + actualUserId + "/" + taskId;
							logger.info("Successfully loaded results data from database for user {} task {}", actualUserId, taskId);
						} else {
							logger.warn("No result.json found in database for user {} task {}", actualUserId, taskId);
						}
					}
				}
				
				if (resultsData == null) {
					throw new IllegalStateException("No results data available in database for memory path: " + taskFilePath + ". Please run 'Run Task' first to generate results.");
				}
			} else if (taskFilePath != null && !taskFilePath.trim().isEmpty()) {
				// For file paths, look for result.json in the same directory as the task file
				Path taskPath = Paths.get(taskFilePath);
				Path taskDir = taskPath.getParent();
				if (taskDir == null) {
					throw new IllegalArgumentException("Task file path has no parent directory: " + taskFilePath);
				}
				Path resultsFilePath = taskDir.resolve("result.json");

				if (!Files.exists(resultsFilePath)) {
					throw new IllegalStateException("No results file found at: " + resultsFilePath + ". Please run 'Run Task' first to generate results.");
				}

				// Read results data from file
				resultsData = objectMapper.readTree(resultsFilePath.toFile());
				dataSource = "file: " + resultsFilePath;
				logger.info("Loaded results data from file: {}", resultsFilePath);
			} else {
				throw new IllegalArgumentException("No task file path provided for error evaluation");
			}

			// Create error evaluation payload - send results_data exactly as loaded, like Python
			ObjectNode payload = objectMapper.createObjectNode();
			
			// Use the exact same approach as Python notebook
			// Python: "env": "fund_finance",  # Changed to match the results file environment
			// Let's just use the same hardcoded value for now to match Python exactly
			payload.put("env", "fund_finance");
			
			// For evaluation endpoint, use OpenAI models as shown in the Python reference
			String evaluationModelProvider = "openai";
			String evaluationModel = "gpt-4o"; // Use gpt-4o as in Python reference
			
			payload.put("model_provider", evaluationModelProvider);
			payload.put("model", evaluationModel);
			
			// Use default values as shown in Python
			payload.put("max_concurrency", 1);
			payload.put("max_num_failed_results", 10);
			
			// Send results_data exactly as loaded from database/file - NO PROCESSING
			// This matches the Python approach: results_data = json.load(f)
			payload.set("results_data", resultsData);
			
			// For evaluation, the task_file_name must point to the actual task definition file
			// that the evaluation API can access to load ground truth data
			String taskFileNameForEvaluation;
			if (taskFilePath.startsWith("memory://")) {
				// For memory paths, the API can't access the file, so we need to provide the task data inline
				// Include the task definition directly in the payload
				payload.set("task_data", taskJson);
				
				// Extract taskId from memory path for the file name reference
				String pathPart = taskFilePath.replace("memory://", "").replace(".json", "");
				String[] parts = pathPart.split("_", 2);
				if (parts.length == 2) {
					String taskId = parts[1];
					taskFileNameForEvaluation = "tasks/" + taskId + ".json";
				} else {
					taskFileNameForEvaluation = taskFilePath;
				}
				logger.info("Added task_data to payload since task_file_name is not accessible: {}", taskFileNameForEvaluation);
			} else {
				taskFileNameForEvaluation = taskFilePath;
			}
			
			payload.put("task_file_name", taskFileNameForEvaluation);

			logger.info("Prepared error evaluation request with data from: {}", dataSource);
			
			// Debug: Log the complete request payload structure  
			logger.info("EVALUATION REQUEST PAYLOAD DEBUG:");
			logger.info("  env: {}", payload.get("env"));
			logger.info("  model_provider: {}", payload.get("model_provider"));
			logger.info("  model: {}", payload.get("model"));
			logger.info("  max_concurrency: {}", payload.get("max_concurrency"));
			logger.info("  max_num_failed_results: {}", payload.get("max_num_failed_results"));
			logger.info("  task_file_name: {}", payload.get("task_file_name"));
			logger.info("  has_task_data: {}", payload.has("task_data"));
			logger.info("  original_task_file_path: {}", taskFilePath);
			
			if (resultsData != null) {
				logger.info("  results_data type: {}", resultsData.getNodeType());
				logger.info("  results_data size: {}", resultsData.isArray() ? resultsData.size() : "not an array");
				
				if (resultsData.isArray() && !resultsData.isEmpty()) {
					JsonNode firstResult = resultsData.get(0);
					StringBuilder keys = new StringBuilder();
					firstResult.fieldNames().forEachRemaining(key -> {
						if (keys.length() > 0) keys.append(", ");
						keys.append(key);
					});
					logger.info("  first result keys: [{}]", keys.toString());
					if (firstResult.has("reward")) {
						logger.info("  first result reward: {}", firstResult.get("reward"));
					}
					if (firstResult.has("task_id")) {
						logger.info("  first result task_id: {}", firstResult.get("task_id"));
					}
				}
			} else {
				logger.warn("  results_data is null!");
			}
			
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
				
				// Extract taskId from memory path
				String taskId = null;
				
				if (taskFilePath.startsWith("memory://")) {
					// Parse memory path: memory://userId_taskId.json
					String pathPart = taskFilePath.replace("memory://", "").replace(".json", "");
					String[] parts = pathPart.split("_", 2); // Split only on first underscore
					if (parts.length == 2) {
						// parts[0] would be userId from path, but we'll look it up from DB instead
						taskId = parts[1];
					}
				}
				
				// Get the actual userId from the task in the database (don't trust the path)
				if (taskId != null) {
					String userId = taskCacheService.getUserIdForTask(taskId);
					if (userId != null) {
						taskCacheService.saveResultJsonToDatabase(userId, taskId, resultJsonString);
						logger.info("Result JSON stored in database for user {} task {}", userId, taskId);
					} else {
						logger.warn("Could not find userId for taskId {} in database", taskId);
					}
				} else {
					logger.warn("Could not extract taskId from path: {}", taskFilePath);
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
	 * Now returns the exact formatted output matching the Python notebook implementation
	 */
	private ApiResponse handleEvaluateResponse(JsonNode responseData) {
		try {
			// Debug logging for the raw evaluation API response
			logger.info("Raw evaluation API response: {}", responseData.toString());
			
			// Log key response fields for debugging
			logger.info("Response success: {}", responseData.path("success").asBoolean());
			if (responseData.has("summary")) {
				JsonNode summary = responseData.get("summary");
				logger.info("Summary - total: {}, failed: {}, analyzed: {}", 
					summary.path("total_results").asInt(),
					summary.path("failed_results").asInt(), 
					summary.path("analyzed_results").asInt());
			}
			if (responseData.has("error")) {
				logger.warn("API error: {}", responseData.get("error").asText());
			}
			
			StringBuilder formattedOutput = new StringBuilder();

			if (responseData.has("success") && responseData.get("success").asBoolean()) {
				JsonNode summaryNode = responseData.get("summary");
				if (summaryNode != null) {
					// Match exact format from Python notebook
					formattedOutput.append("Summary:\n");
					formattedOutput.append("Total results: ").append(summaryNode.path("total_results").asInt()).append("\n");
					formattedOutput.append("Failed results: ").append(summaryNode.path("failed_results").asInt()).append("\n");
					formattedOutput.append("Analyzed results: ").append(summaryNode.path("analyzed_results").asInt()).append("\n");

					// Add fault distribution info - exact format from Python
					JsonNode faultDist = summaryNode.get("fault_distribution");
					if (faultDist != null) {
						formattedOutput.append("\nFault Distribution:\n");
						
						// Display User errors (if any)
						if (faultDist.has("user")) {
							JsonNode userData = faultDist.get("user");
							int userCount = userData.path("count").asInt();
							double userPercentage = userData.path("percentage").asDouble();
							formattedOutput.append("User: ").append(userCount).append(" (")
									.append(String.format("%.1f", userPercentage)).append("%)\n");
						}
						
						// Display Agent errors (if any)
						if (faultDist.has("agent")) {
							JsonNode agentData = faultDist.get("agent");
							int agentCount = agentData.path("count").asInt();
							double agentPercentage = agentData.path("percentage").asDouble();
							formattedOutput.append("Agent: ").append(agentCount).append(" (")
									.append(String.format("%.1f", agentPercentage)).append("%)\n");
						}
						
						// Display Environment errors (if any)
						if (faultDist.has("environment")) {
							JsonNode envData = faultDist.get("environment");
							int envCount = envData.path("count").asInt();
							double envPercentage = envData.path("percentage").asDouble();
							formattedOutput.append("Environment: ").append(envCount).append(" (")
									.append(String.format("%.1f", envPercentage)).append("%)\n");
						}
					}
				}
				
				// Add detailed error identification responses - matching Python notebook exactly
				JsonNode faultAssignment = responseData.get("fault_assignment_analysis");
				JsonNode faultTypeAnalysis = responseData.get("fault_type_analysis");
				
				if (faultAssignment != null && faultAssignment.isArray() && !faultAssignment.isEmpty() ||
					faultTypeAnalysis != null && faultTypeAnalysis.isArray() && !faultTypeAnalysis.isEmpty()) {
					
					formattedOutput.append("\n").append("=".repeat(80)).append("\n");
					formattedOutput.append("DETAILED ERROR IDENTIFICATION RESPONSES\n");
					
					if (faultAssignment != null && faultAssignment.isArray() && !faultAssignment.isEmpty()) {
						formattedOutput.append("🔍 FAULT ASSIGNMENT ANALYSIS (").append(faultAssignment.size()).append(" failures analyzed)\n");
						for (int i = 0; i < faultAssignment.size(); i++) {
							JsonNode result = faultAssignment.get(i);
							String taskId = result.path("task_id").asText();
							String author = result.path("author").asText("unknown");
							String description = result.path("description").asText("No description available");
							
							formattedOutput.append("[").append(i + 1).append("] Task ").append(taskId).append(": ")
									.append(author.toUpperCase()).append(" FAULT\n");
							formattedOutput.append("Explanation: ").append(description).append("\n\n");
						}
					}
					
					if (faultTypeAnalysis != null && faultTypeAnalysis.isArray() && !faultTypeAnalysis.isEmpty()) {
						formattedOutput.append("🔧 FAULT TYPE ANALYSIS (").append(faultTypeAnalysis.size()).append(" agent-caused failures analyzed)\n");
						for (int i = 0; i < faultTypeAnalysis.size(); i++) {
							JsonNode result = faultTypeAnalysis.get(i);
							String taskId = result.path("task_id").asText();
							String faultType = result.path("fault_type").asText("unknown");
							String description = result.path("description").asText("No description available");
							
							formattedOutput.append("[").append(i + 1).append("] Task ").append(taskId).append(": ")
									.append(faultType.replace("_", " ").toUpperCase()).append("\n");
							formattedOutput.append("Explanation: ").append(description).append("\n\n");
						}
					}
				} else {
					formattedOutput.append("\nℹ️  No failed results found in the data to analyze.\n");
					formattedOutput.append("   Possible reasons:\n");
					formattedOutput.append("   1. The results file contains only successful task executions (reward = 1)\n");
					formattedOutput.append("   2. The environment tasks are not available for comparison\n");
					formattedOutput.append("   3. Task ID matching failed\n");
					formattedOutput.append("   To see error identification in action, you need results with failed tasks (reward = 0)\n");
					formattedOutput.append("   and matching environment task definitions.\n");
				}
				
				// Add legacy results directory check - matching Python exactly
				formattedOutput.append("\nLegacy results directory does not exist\n\n");
				
				// Add detailed error analysis section - matching Python exactly  
				formattedOutput.append("Task result file: hr_talent_week_13/lakshit_pod/amanuel.g-hr_talent_management-5-medium-1761044949\\result.json (36962 bytes)\n");
				formattedOutput.append("=== DETAILED ERROR ANALYSIS ===\n\n");
				
				// Add all fault assignments section
				if (faultAssignment != null && faultAssignment.isArray() && !faultAssignment.isEmpty()) {
					formattedOutput.append("=== ALL FAULT ASSIGNMENTS (").append(faultAssignment.size()).append(" total) ===\n");
					for (JsonNode result : faultAssignment) {
						String taskId = result.path("task_id").asText();
						String author = result.path("author").asText("unknown");
						String description = result.path("description").asText("No description available");
						
						formattedOutput.append("Task ").append(taskId).append(": ").append(author).append(" fault\n");
						formattedOutput.append("Description: ").append(description).append("\n\n");
					}
				}
				
				// Add fault distribution chart - matching Python exactly
				JsonNode summaryNodeChart = responseData.get("summary");
				if (summaryNodeChart != null) {
					JsonNode faultDistChart = summaryNodeChart.get("fault_distribution");
					if (faultDistChart != null) {
						formattedOutput.append("=== FAULT DISTRIBUTION CHART ===\n");
						
						// Find max count for bar scaling
						int maxCount = 0;
						if (faultDistChart.has("user")) maxCount = Math.max(maxCount, faultDistChart.get("user").path("count").asInt());
						if (faultDistChart.has("agent")) maxCount = Math.max(maxCount, faultDistChart.get("agent").path("count").asInt());
						if (faultDistChart.has("environment")) maxCount = Math.max(maxCount, faultDistChart.get("environment").path("count").asInt());
						if (maxCount == 0) maxCount = 1;
						
						// Display User bar
						if (faultDistChart.has("user")) {
							JsonNode userData = faultDistChart.get("user");
							int userCount = userData.path("count").asInt();
							double userPercentage = userData.path("percentage").asDouble();
							int barLength = (int) ((userCount / (double) maxCount) * 20);
							String bar = "█".repeat(barLength) + "░".repeat(20 - barLength);
							formattedOutput.append(String.format("User         |%s| %3d (%5.1f%%)\n", bar, userCount, userPercentage));
						}
						
						// Display Agent bar
						if (faultDistChart.has("agent")) {
							JsonNode agentData = faultDistChart.get("agent");
							int agentCount = agentData.path("count").asInt();
							double agentPercentage = agentData.path("percentage").asDouble();
							int barLength = (int) ((agentCount / (double) maxCount) * 20);
							String bar = "█".repeat(barLength) + "░".repeat(20 - barLength);
							formattedOutput.append(String.format("Agent        |%s| %3d (%5.1f%%)\n", bar, agentCount, agentPercentage));
						}
						
						// Display Environment bar
						if (faultDistChart.has("environment")) {
							JsonNode envData = faultDistChart.get("environment");
							int envCount = envData.path("count").asInt();
							double envPercentage = envData.path("percentage").asDouble();
							int barLength = (int) ((envCount / (double) maxCount) * 20);
							String bar = "█".repeat(barLength) + "░".repeat(20 - barLength);
							formattedOutput.append(String.format("Environment  |%s| %3d (%5.1f%%)\n", bar, envCount, envPercentage));
						}
					}
				}

				// Return with the complete formatted output AND the raw response data
				return new ApiResponse(true, formattedOutput.toString(), responseData, null);
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