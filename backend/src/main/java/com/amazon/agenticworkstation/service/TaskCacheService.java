package com.amazon.agenticworkstation.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.amazon.agenticworkstation.dto.CacheUpdateRequest;
import com.amazon.agenticworkstation.dto.TaskDto;
import com.amazon.agenticworkstation.entity.TaskEntity;
import com.amazon.agenticworkstation.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Enhanced multi-user task cache with automatic expiration and database
 * integration. Caches task data by key (taskId + userId) to support multiple
 * users simultaneously.
 */
@Service
public class TaskCacheService {

	private static final Logger logger = LoggerFactory.getLogger(TaskCacheService.class);
	private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	@Autowired
	private TaskRepository taskRepository;

	// Multi-user cache storage with automatic expiration
	private final Map<String, TaskCacheEntry> userTaskCache = new ConcurrentHashMap<>();

	// Cache entry with expiration and task data
	private static class TaskCacheEntry {
		private final LocalDateTime createdTime;
		private final LocalDateTime expirationTime;
		private String repositoryPath;
		private String env = "finance";
		private Integer interfaceNum = 4;
		private String instruction = "";
		private String userId = "";
		private String taskId = "";
		private final List<String> actions = new ArrayList<>();
		private final List<Map<String, Object>> actionObjects = new ArrayList<>();
		private final List<String> outputs = new ArrayList<>();
		private final List<Map<String, Object>> edges = new ArrayList<>();
		private Map<String, Object> resultData;
		private String resultFilePath;

		public TaskCacheEntry() {
			this.createdTime = LocalDateTime.now();
			this.expirationTime = createdTime.plusHours(4); // 4-hour expiration
		}

		public boolean isExpired() {
			return LocalDateTime.now().isAfter(expirationTime);
		}

		public LocalDateTime getCreatedTime() {
			return createdTime;
		}

		public LocalDateTime getExpirationTime() {
			return expirationTime;
		}
	}

	/**
	 * Generate cache key from user ID and task ID
	 */
	private String generateCacheKey(String userId, String taskId) {
		return userId + "_" + taskId;
	}

	/**
	 * Get or create cache entry for user and task
	 */
	private TaskCacheEntry getOrCreateCacheEntry(String userId, String taskId) {
		String cacheKey = generateCacheKey(userId, taskId);
		TaskCacheEntry entry = userTaskCache.get(cacheKey);

		if (entry == null || entry.isExpired()) {
			if (entry != null && entry.isExpired()) {
				logger.info("Cache expired for user {} task {}, creating new entry", userId, taskId);
			}
			entry = new TaskCacheEntry();
			entry.userId = userId;
			entry.taskId = taskId;
			userTaskCache.put(cacheKey, entry);

			// Try to load task from database
			loadTaskFromDatabase(userId, taskId, entry);
		}

		return entry;
	}

	/**
	 * Load task data from database into cache entry
	 */
	private void loadTaskFromDatabase(String userId, String taskId, TaskCacheEntry entry) {
		try {
			Optional<TaskEntity> taskEntity = taskRepository.findById(taskId);
			if (taskEntity.isPresent()) {
				TaskEntity task = taskEntity.get();

				// Load basic task data
				entry.env = task.getEnvName();
				entry.interfaceNum = task.getInterfaceNum();
				entry.instruction = task.getInstruction();

				// Parse and load task JSON if available
				if (task.getTaskJson() != null && !task.getTaskJson().trim().isEmpty()) {
					try {
						JsonNode taskNode = mapper.readTree(task.getTaskJson());
						populateCacheFromTaskJson(entry, taskNode);
						logger.info("Loaded task {} from database for user {}", taskId, userId);
					} catch (Exception e) {
						logger.warn("Failed to parse task JSON for task {}: {}", taskId, e.getMessage());
					}
				}

				// Load result JSON if available
				if (task.getResultJson() != null && !task.getResultJson().trim().isEmpty()) {
					try {
						@SuppressWarnings("unchecked")
						Map<String, Object> resultMap = mapper.readValue(task.getResultJson(), Map.class);
						entry.resultData = resultMap;
						logger.info("Loaded result data for task {} user {}", taskId, userId);
					} catch (Exception e) {
						logger.warn("Failed to parse result JSON for task {}: {}", taskId, e.getMessage());
					}
				}
			} else {
				logger.info("Task {} not found in database, using empty cache entry", taskId);
			}
		} catch (Exception e) {
			logger.error("Error loading task {} from database for user {}: {}", taskId, userId, e.getMessage());
		}
	}

	/**
	 * Populate cache entry from parsed task JSON
	 */
	private void populateCacheFromTaskJson(TaskCacheEntry entry, JsonNode taskNode) throws Exception {
		// Extract interface_num and env from root level
		if (taskNode.has("interface_num")) {
			entry.interfaceNum = taskNode.get("interface_num").asInt();
		}
		if (taskNode.has("env")) {
			entry.env = taskNode.get("env").asText();
		}

		// Extract task details
		JsonNode taskDetails = taskNode.get("task");
		if (taskDetails != null) {
			if (taskDetails.has("user_id")) {
				entry.userId = taskDetails.get("user_id").asText();
			}
			if (taskDetails.has("instruction")) {
				entry.instruction = taskDetails.get("instruction").asText();
			}

			// Load outputs
			if (taskDetails.has("outputs")) {
				entry.outputs.clear();
				taskDetails.get("outputs").forEach(node -> entry.outputs.add(node.asText()));
			}

			// Load actions
			if (taskDetails.has("actions")) {
				entry.actions.clear();
				entry.actionObjects.clear();
				taskDetails.get("actions").forEach(actionNode -> {
					if (actionNode.isObject()) {
						Map<String, Object> actionMap = mapper.convertValue(actionNode, Map.class);
						entry.actionObjects.add(actionMap);
						if (actionMap.containsKey("name")) {
							entry.actions.add(actionMap.get("name").toString());
						}
					} else {
						entry.actions.add(actionNode.asText());
					}
				});
			}

			// Load edges
			if (taskDetails.has("edges")) {
				entry.edges.clear();
				taskDetails.get("edges").forEach(edgeNode -> {
					Map<String, Object> edgeMap = mapper.convertValue(edgeNode, Map.class);
					entry.edges.add(edgeMap);
				});
			}
		}
	}

	/**
	 * Load task into cache for specific user and task ID
	 */
	public synchronized void loadTaskIntoCache(String userId, String taskId) {
		logger.info("Loading task {} into cache for user {}", taskId, userId);
		getOrCreateCacheEntry(userId, taskId);
	}

	/**
	 * Clear expired cache entries every hour
	 */
	@Scheduled(fixedRate = 3600000) // Run every hour
	public void clearExpiredEntries() {
		int initialSize = userTaskCache.size();
		userTaskCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
		int finalSize = userTaskCache.size();
		if (initialSize > finalSize) {
			logger.info("Cleared {} expired cache entries, {} entries remaining", initialSize - finalSize, finalSize);
		}
	}

	// Legacy compatibility methods (using current user context)
	private String currentUserId = "";
	private String currentTaskId = "";

	/**
	 * Build aggregated task DTO for specific user and task ID
	 */
	public synchronized TaskDto buildAggregatedTaskDto(String userId, String taskId) {
		// Try to get task from database first (preserves original JSON with model
		// configuration)
		Optional<TaskEntity> taskEntityOpt = taskRepository.findById(taskId);
		if (taskEntityOpt.isPresent()) {
			TaskEntity taskEntity = taskEntityOpt.get();
			String originalJson = taskEntity.getTaskJson();
			if (originalJson != null && !originalJson.trim().isEmpty()) {
				try {
					TaskDto taskDto = mapper.readValue(originalJson, TaskDto.class);

					// Fix num_edges field to reflect actual number of edges
					if (taskDto.getTask() != null && taskDto.getTask().getEdges() != null) {
						int actualEdgeCount = taskDto.getTask().getEdges().size();
						taskDto.getTask().setNumEdges(actualEdgeCount);
						logger.debug("Fixed num_edges for task {}: {} edges", taskId, actualEdgeCount);
					}

					return taskDto;
				} catch (Exception e) {
					// If original JSON parsing fails, fall back to reconstructing from cache
					// but this will lack model configuration
				}
			}
		}

		// If no database record or original JSON, this is an error for task execution
		throw new IllegalStateException("Task " + taskId + " not found in database with original JSON. "
				+ "Cannot build TaskDto without model configuration.");
	}

	/**
	 * Build aggregated task DTO using legacy method (for backward compatibility)
	 */
	public synchronized TaskDto buildAggregatedTaskDto() {
		// Use current user/task context or default if not set
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			logger.warn("No current user/task context set, creating empty DTO");
			return createEmptyTaskDto();
		}
		return buildAggregatedTaskDto(currentUserId, currentTaskId);
	}

	/**
	 * Set current user and task context for legacy compatibility
	 */
	public synchronized void setCurrentContext(String userId, String taskId) {
		this.currentUserId = userId;
		this.currentTaskId = taskId;
		// Load task into cache if not already present
		loadTaskIntoCache(userId, taskId);
	}

	/**
	 * Get current user ID from context
	 */
	public synchronized String getCurrentUserId() {
		return this.currentUserId;
	}

	/**
	 * Get current task ID from context
	 */
	public synchronized String getCurrentTaskId() {
		return this.currentTaskId;
	}

	private TaskDto createEmptyTaskDto() {
		TaskDto dto = new TaskDto();
		dto.setEnv("finance");
		dto.setInterfaceNum(4);
		dto.setModelProvider("openai");
		dto.setModel("gpt-4o");
		dto.setNumTrials(3);
		dto.setTemperature(1.0);

		TaskDto.TaskDetails details = new TaskDto.TaskDetails();
		details.setInstruction("");
		details.setUserId("");
		details.setOutputs(new ArrayList<>());
		details.setActions(new ArrayList<>());
		details.setEdges(new ArrayList<>());
		details.setNumEdges(0);
		dto.setTask(details);
		return dto;
	}

	/**
	 * Apply update to cache for specific user and task
	 */
	public synchronized void applyUpdate(String userId, String taskId, CacheUpdateRequest req) {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);

		if (req.getRepositoryPath() != null) {
			entry.repositoryPath = req.getRepositoryPath();
			// Log a warning if web path is received
			if (entry.repositoryPath.startsWith("web:")) {
				logger.warn("Web directory path received ({}). File operations may be limited.", entry.repositoryPath);
			}
		}
		if (req.getEnv() != null)
			entry.env = req.getEnv();
		if (req.getInterfaceNum() != null)
			entry.interfaceNum = req.getInterfaceNum();
		if (req.getInstruction() != null)
			entry.instruction = req.getInstruction();
		if (req.getUserId() != null)
			entry.userId = req.getUserId();
		if (req.getActions() != null) {
			entry.actions.clear();
			entry.actions.addAll(req.getActions());
		}
		if (req.getActionObjects() != null) {
			entry.actionObjects.clear();
			entry.actionObjects.addAll(req.getActionObjects());
		}
		if (req.getOutputs() != null) {
			entry.outputs.clear();
			entry.outputs.addAll(req.getOutputs());
		}
		if (req.getEdges() != null) {
			entry.edges.clear();
			// Clean up connection fields when receiving edges from frontend
			for (Map<String, Object> edge : req.getEdges()) {
				Map<String, Object> cleanedEdge = new LinkedHashMap<>(edge);
				Object connectionObj = cleanedEdge.get("connection");
				if (connectionObj instanceof String) {
					String connStr = (String) connectionObj;
					if (!connStr.trim().isEmpty() && connStr.trim().startsWith("{")) {
						try {
							@SuppressWarnings("unchecked")
							Map<String, Object> parsed = (Map<String, Object>) mapper.readValue(connStr, Map.class);
							cleanedEdge.put("connection", parsed);
							logger.debug("Cleaned up connection string in applyUpdate: {} -> {}", connStr, parsed);
						} catch (Exception ex) {
							logger.warn("Failed to parse connection string: {}", connStr);
						}
					}
				}
				entry.edges.add(cleanedEdge);
			}
		}

		// Save updated cache to database
		saveTaskToDatabase(userId, taskId, entry);
	}

	/**
	 * Legacy applyUpdate method for backward compatibility
	 */
	public synchronized void applyUpdate(CacheUpdateRequest req) {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			logger.warn("No current user/task context set for legacy applyUpdate");
			return;
		}
		applyUpdate(currentUserId, currentTaskId, req);
	}

	/**
	 * Get repository path for specific user and task
	 */
	public synchronized String getRepositoryPath(String userId, String taskId) {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);
		return entry.repositoryPath;
	}

	/**
	 * Legacy getRepositoryPath method
	 */
	public synchronized String getRepositoryPath() {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			return null;
		}
		return getRepositoryPath(currentUserId, currentTaskId);
	}

	/**
	 * Store result data in memory for specific user and task
	 */
	public synchronized void storeResultData(String userId, String taskId, Map<String, Object> data, String filePath) {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);
		entry.resultData = data != null ? new HashMap<>(data) : null;
		entry.resultFilePath = filePath;

		// Save result to database
		saveResultToDatabase(userId, taskId, data);
	}

	/**
	 * Legacy store result data method
	 */
	public synchronized void storeResultData(Map<String, Object> data, String filePath) {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			logger.warn("No current user/task context set for storeResultData");
			return;
		}
		storeResultData(currentUserId, currentTaskId, data, filePath);
	}

	/**
	 * Get result data from memory for specific user and task
	 */
	public synchronized Map<String, Object> getResultData(String userId, String taskId) {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);
		return entry.resultData != null ? new HashMap<>(entry.resultData) : null;
	}

	/**
	 * Legacy get result data method
	 */
	public synchronized Map<String, Object> getResultData() {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			return null;
		}
		return getResultData(currentUserId, currentTaskId);
	}

	/**
	 * Get result file path from memory for specific user and task
	 */
	public synchronized String getResultFilePath(String userId, String taskId) {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);
		return entry.resultFilePath;
	}

	/**
	 * Legacy get result file path method
	 */
	public synchronized String getResultFilePath() {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			return null;
		}
		return getResultFilePath(currentUserId, currentTaskId);
	}

	/**
	 * Clear result data from memory for specific user and task
	 */
	public synchronized void clearResultData(String userId, String taskId) {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);
		entry.resultData = null;
		entry.resultFilePath = null;
	}

	/**
	 * Legacy clear result data method
	 */
	public synchronized void clearResultData() {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			logger.warn("No current user/task context set for clearResultData");
			return;
		}
		clearResultData(currentUserId, currentTaskId);
	}

	/**
	 * Check if result data exists in memory for specific user and task
	 */
	public synchronized boolean hasResultData(String userId, String taskId) {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);
		return entry.resultData != null;
	}

	/**
	 * Legacy has result data method
	 */
	public synchronized boolean hasResultData() {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			return false;
		}
		return hasResultData(currentUserId, currentTaskId);
	}

	/**
	 * Save task data to database
	 */
	private void saveTaskToDatabase(String userId, String taskId, TaskCacheEntry entry) {
		try {
			Optional<TaskEntity> taskEntityOpt = taskRepository.findById(taskId);
			TaskEntity taskEntity;

			if (taskEntityOpt.isPresent()) {
				taskEntity = taskEntityOpt.get();
			} else {
				// Create new task entity
				taskEntity = new TaskEntity();
				taskEntity.setTaskId(taskId);

				// Set user ID directly
				taskEntity.setUserId(userId);
			}

			// Update task data
			taskEntity.setEnvName(entry.env);
			taskEntity.setInterfaceNum(entry.interfaceNum);
			taskEntity.setInstruction(entry.instruction);

			// Set number of edges from the cache entry
			taskEntity.setNumOfEdges(entry.edges.size());

			// Update taskJson with current cache data while preserving model configuration
			if (taskEntityOpt.isPresent()) {
				// Update existing task - reconstruct JSON with current cache data
				updateTaskJsonWithCacheData(taskEntity, entry);
			} else {
				// For new tasks, require original JSON to be provided via overloaded method
				throw new IllegalStateException("Cannot create new task " + taskId + " without original JSON content. "
						+ "Use the overloaded saveTaskToDatabase method with original JSON content.");
			}

			// Save to database
			taskRepository.save(taskEntity);
			logger.info("Saved task {} to database for user {}", taskId, userId);

		} catch (Exception e) {
			logger.error("Error saving task {} to database for user {}: {}", taskId, userId, e.getMessage());
			// Propagate with a clear message so callers can surface it to clients/UI
			throw new IllegalStateException(
					"Failed to save task " + taskId + " for user " + userId + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Update taskJson field with current cache data while preserving model
	 * configuration
	 */
	private void updateTaskJsonWithCacheData(TaskEntity taskEntity, TaskCacheEntry entry) {
		try {
			String currentJson = taskEntity.getTaskJson();
			if (currentJson == null || currentJson.trim().isEmpty()) {
				logger.warn("No existing taskJson found for task {}, cannot update", taskEntity.getTaskId());
				return;
			}

			// Parse existing JSON to preserve model configuration
			TaskDto taskDto = mapper.readValue(currentJson, TaskDto.class);

			// Update task data with current cache values
			if (taskDto.getTask() != null) {
				taskDto.getTask().setInstruction(entry.instruction);
				taskDto.getTask().setUserId(entry.userId);

				// Update actions - convert from actionObjects if available, otherwise use
				// action names
				if (!entry.actionObjects.isEmpty()) {
					List<TaskDto.ActionDto> actions = new ArrayList<>();
					for (Map<String, Object> actionObj : entry.actionObjects) {
						TaskDto.ActionDto actionDto = new TaskDto.ActionDto();
						actionDto.setName((String) actionObj.get("name"));
						if (actionObj.containsKey("arguments")) {
							Object args = actionObj.get("arguments");
							if (args instanceof Map) {
								@SuppressWarnings("unchecked")
								Map<String, Object> argsMap = (Map<String, Object>) args;
								actionDto.setArguments(argsMap);
							}
						}
						if (actionObj.containsKey("output")) {
							actionDto.setOutput((String) actionObj.get("output"));
						}
						actions.add(actionDto);
					}
					taskDto.getTask().setActions(actions);
				} else if (!entry.actions.isEmpty()) {
					// Fallback to simple action names
					List<TaskDto.ActionDto> actions = new ArrayList<>();
					for (String actionName : entry.actions) {
						TaskDto.ActionDto actionDto = new TaskDto.ActionDto();
						actionDto.setName(actionName);
						actions.add(actionDto);
					}
					taskDto.getTask().setActions(actions);
				}

				// Update outputs
				taskDto.getTask().setOutputs(new ArrayList<>(entry.outputs));

				// Update edges
				List<TaskDto.EdgeDto> edges = new ArrayList<>();
				for (Map<String, Object> edgeObj : entry.edges) {
					TaskDto.EdgeDto edgeDto = new TaskDto.EdgeDto();
					edgeDto.setFrom((String) edgeObj.get("from"));
					edgeDto.setTo((String) edgeObj.get("to"));

					// Handle connection object
					Object connectionObj = edgeObj.get("connection");
					if (connectionObj instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> connMap = (Map<String, Object>) connectionObj;
						TaskDto.ConnectionDto connDto = new TaskDto.ConnectionDto();
						connDto.setOutput((String) connMap.get("output"));
						connDto.setInput((String) connMap.get("input"));
						edgeDto.setConnection(connDto);
					}
					edges.add(edgeDto);
				}
				taskDto.getTask().setEdges(edges);
				taskDto.getTask().setNumEdges(edges.size());
			}

			// Update the taskJson with modified data
			String updatedJson = mapper.writeValueAsString(taskDto);
			taskEntity.setTaskJson(updatedJson);

			logger.debug("Updated taskJson for task {} with current cache data", taskEntity.getTaskId());

		} catch (Exception e) {
			logger.error("Failed to update taskJson for task {}: {}", taskEntity.getTaskId(), e.getMessage());
			// Don't throw here, just log the error so the basic task fields are still saved
		}
	}

	/**
	 * Save task data to database with original JSON content (preserves model
	 * configuration)
	 */
	private void saveTaskToDatabase(String userId, String taskId, TaskCacheEntry entry, String originalJsonContent) {
		try {
			Optional<TaskEntity> taskEntityOpt = taskRepository.findById(taskId);
			TaskEntity taskEntity;

			if (taskEntityOpt.isPresent()) {
				taskEntity = taskEntityOpt.get();
			} else {
				// Create new task entity
				taskEntity = new TaskEntity();
				taskEntity.setTaskId(taskId);

				// Set user ID directly
				taskEntity.setUserId(userId);
			}

			// Update task data
			taskEntity.setEnvName(entry.env);
			taskEntity.setInterfaceNum(entry.interfaceNum);
			taskEntity.setInstruction(entry.instruction);

			// Set number of edges from the cache entry
			int actualEdgeCount = entry.edges.size();
			taskEntity.setNumOfEdges(actualEdgeCount);

			// Fix the num_edges field in the JSON content before storing
			String correctedJsonContent = originalJsonContent;
			try {
				TaskDto taskDto = mapper.readValue(originalJsonContent, TaskDto.class);
				if (taskDto.getTask() != null) {
					taskDto.getTask().setNumEdges(actualEdgeCount);
					correctedJsonContent = mapper.writeValueAsString(taskDto);
					logger.debug("Corrected num_edges in JSON for task {}: {} edges", taskId, actualEdgeCount);
				}
			} catch (Exception e) {
				logger.warn("Failed to correct num_edges in JSON for task {}, using original: {}", taskId,
						e.getMessage());
			}

			// Use corrected JSON content to preserve model configuration
			taskEntity.setTaskJson(correctedJsonContent);

			// Save to database
			taskRepository.save(taskEntity);
			logger.info("Saved task {} to database for user {} with original JSON content preserved", taskId, userId);

		} catch (Exception e) {
			logger.error("Error saving task {} to database for user {}: {}", taskId, userId, e.getMessage());
			// Propagate with a clear message so callers can surface it to clients/UI
			throw new IllegalStateException(
					"Failed to save task " + taskId + " for user " + userId + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Save result data to database (REMOVED result_json storage - only run_task
	 * should store result_json) This method now only handles in-memory cache
	 * storage, not database result_json field
	 */
	private void saveResultToDatabase(String userId, String taskId, Map<String, Object> resultData) {
		// NOTE: This method no longer stores result_json in database
		// Only run_task validation should store result_json via
		// saveResultJsonToDatabase method
		logger.debug(
				"Result data cached in memory for task {} user {} (result_json not stored - only run_task stores result_json)",
				taskId, userId);
	}

	/**
	 * Get result.json content directly from database for a specific task
	 */
	public String getResultJsonFromDatabase(String userId, String taskId) {
		try {
			Optional<TaskEntity> taskEntityOpt = taskRepository.findById(taskId);
			if (taskEntityOpt.isPresent()) {
				TaskEntity taskEntity = taskEntityOpt.get();

				// Verify the task belongs to the specified user
				if (!userId.equals(taskEntity.getUserId())) {
					logger.warn("Task {} belongs to user {} but requested by user {}", taskId, taskEntity.getUserId(),
							userId);
					return null;
				}

				return taskEntity.getResultJson();
			} else {
				logger.info("Task {} not found in database", taskId);
				return null;
			}
		} catch (Exception e) {
			logger.error("Error retrieving result.json for task {} user {}: {}", taskId, userId, e.getMessage());
			return null;
		}
	}

	/**
	 * Get the userId for a specific taskId from the database This is used to verify
	 * task ownership before saving result.json
	 */
	public String getUserIdForTask(String taskId) {
		try {
			Optional<TaskEntity> taskEntityOpt = taskRepository.findById(taskId);
			if (taskEntityOpt.isPresent()) {
				String userId = taskEntityOpt.get().getUserId();
				logger.debug("Found userId '{}' for taskId '{}'", userId, taskId);
				return userId;
			} else {
				logger.warn("Task {} not found in database", taskId);
				return null;
			}
		} catch (Exception e) {
			logger.error("Error looking up userId for task {}: {}", taskId, e.getMessage());
			return null;
		}
	}

	/**
	 * Save result JSON string directly to database for a specific task
	 * 
	 * *** IMPORTANT: This is the ONLY method that should store result_json in the
	 * database *** *** Only run_task validation operations should call this method
	 * ***
	 */
	public void saveResultJsonToDatabase(String userId, String taskId, String resultJsonString) {
		try {
			Optional<TaskEntity> taskEntityOpt = taskRepository.findById(taskId);
			if (!taskEntityOpt.isPresent()) {
				String error = "Task " + taskId + " not found in database for saving result.json";
				logger.error(error);
				throw new IllegalStateException(error);
			}

			TaskEntity taskEntity = taskEntityOpt.get();

			// Verify the task belongs to the specified user
			if (!userId.equals(taskEntity.getUserId())) {
				String error = "Task " + taskId + " belongs to user " + taskEntity.getUserId()
						+ " but requested by user " + userId + " - access denied";
				logger.error(error);
				throw new IllegalStateException(error);
			}

			// Store the raw JSON string directly
			taskEntity.setResultJson(resultJsonString);
			taskRepository.save(taskEntity);

			logger.info("Saved result.json for task {} user {} - {} bytes", taskId, userId, resultJsonString.length());
		} catch (IllegalStateException e) {
			// Re-throw our custom exceptions
			throw e;
		} catch (Exception e) {
			String error = "Error saving result.json for task " + taskId + " user " + userId + ": " + e.getMessage();
			logger.error(error, e);
			throw new IllegalStateException(error, e);
		}
	}

	/**
	 * Write task JSON to repository for specific user and task
	 */
	public synchronized String writeTaskJsonToRepository(String userId, String taskId, String overrideDirectory)
			throws IOException {
		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);
		String dir = overrideDirectory != null ? overrideDirectory : entry.repositoryPath;
		if (dir == null || dir.isBlank())
			throw new IOException("Repository path not set");

		// Handle web directories differently - they're not real filesystem paths
		if (dir.startsWith("web:")) {
			// For web directories, we can't actually write to the filesystem
			// Just return the aggregated JSON for display purposes
			TaskDto dto = buildAggregatedTaskDto(userId, taskId);
			String json = mapper.writeValueAsString(dto);
			return fixDoubleEscapedConnections(json);
		}

		Path folder = Path.of(dir);
		if (!Files.exists(folder))
			throw new IOException("Directory does not exist: " + dir);
		TaskDto dto = buildAggregatedTaskDto(userId, taskId);
		String json = mapper.writeValueAsString(dto);

		// Fix any double-escaped connection strings in the final JSON
		json = fixDoubleEscapedConnections(json);

		Files.writeString(folder.resolve("task.json"), json);
		return json;
	}

	/**
	 * Legacy writeTaskJsonToRepository method
	 */
	public synchronized String writeTaskJsonToRepository(String overrideDirectory) throws IOException {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			throw new IOException("No current user/task context set");
		}
		return writeTaskJsonToRepository(currentUserId, currentTaskId, overrideDirectory);
	}

	/**
	 * Get aggregated JSON for specific user and task
	 */
	public synchronized String aggregatedJson(String userId, String taskId) throws IOException {
		String json = mapper.writeValueAsString(buildAggregatedTaskDto(userId, taskId));
		return fixDoubleEscapedConnections(json);
	}

	/**
	 * Legacy aggregatedJson method
	 */
	public synchronized String aggregatedJson() throws IOException {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			return mapper.writeValueAsString(createEmptyTaskDto());
		}
		return aggregatedJson(currentUserId, currentTaskId);
	}

	/**
	 * Fix double-escaped connection strings in the final JSON output
	 */
	private String fixDoubleEscapedConnections(String json) {
		// Look for patterns like "connection": "{\"output\":\"...\",\"input\":\"...\"}"
		// and replace them with "connection": {"output":"...","input":"..."}

		// Handle the most common case: "connection":
		// "{\"output\":\"value\",\"input\":\"value\"}"
		String result = json.replaceAll(
				"\"connection\"\\s*:\\s*\"\\{\\\\\"output\\\\\"\\s*:\\s*\\\\\"([^\"]*)\\\\\",\\s*\\\\\"input\\\\\"\\s*:\\s*\\\\\"([^\"]*)\\\\\"}\"",
				"\"connection\":{\"output\":\"$1\",\"input\":\"$2\"}");

		// Handle reverse order: "connection":
		// "{\"input\":\"value\",\"output\":\"value\"}"
		result = result.replaceAll(
				"\"connection\"\\s*:\\s*\"\\{\\\\\"input\\\\\"\\s*:\\s*\\\\\"([^\"]*)\\\\\",\\s*\\\\\"output\\\\\"\\s*:\\s*\\\\\"([^\"]*)\\\\\"}\"",
				"\"connection\":{\"input\":\"$1\",\"output\":\"$2\"}");

		System.out.println("DEBUG: Fixed double-escaped connections in JSON");
		return result;
	}

	/**
	 * Load existing task from file for specific user and task ID
	 */
	public synchronized void loadExistingTask(String userId, String taskId, Path repo) throws IOException {
		if (repo == null)
			return;
		Path file = repo.resolve("task.json");
		if (!Files.exists(file))
			return; // nothing to load

		TaskCacheEntry entry = getOrCreateCacheEntry(userId, taskId);

		@SuppressWarnings("unchecked")
		Map<String, Object> root = (Map<String, Object>) mapper.readValue(Files.readString(file), Map.class);
		Object envVal = root.get("env");
		if (envVal instanceof String)
			entry.env = (String) envVal;
		Object iface = root.get("interface_num");
		if (iface instanceof Number)
			entry.interfaceNum = ((Number) iface).intValue();
		Object taskObj = root.get("task");
		if (taskObj instanceof Map<?, ?> taskMap) {
			Object instructionVal = taskMap.get("instruction");
			if (instructionVal instanceof String)
				entry.instruction = (String) instructionVal;
			Object userIdVal = taskMap.get("user_id");
			if (userIdVal instanceof String)
				entry.userId = (String) userIdVal;
			Object outputsVal = taskMap.get("outputs");
			if (outputsVal instanceof List<?> outList) {
				entry.outputs.clear();
				entry.outputs.addAll(outList.stream().map(Object::toString).collect(Collectors.toList()));
			}
			Object actionsVal = taskMap.get("actions");
			if (actionsVal instanceof List<?> actList) {
				entry.actions.clear();
				entry.actionObjects.clear();
				for (Object a : actList) {
					if (a instanceof Map<?, ?> am) {
						@SuppressWarnings("unchecked")
						Map<String, Object> full = (Map<String, Object>) am;
						entry.actionObjects.add(full);
						Object name = am.get("name");
						if (name != null)
							entry.actions.add(name.toString());
					} else if (a != null) {
						entry.actions.add(a.toString());
					}
				}
			}
			Object edgesVal = taskMap.get("edges");
			if (edgesVal instanceof List<?> edgeList) {
				entry.edges.clear();
				for (Object e : edgeList) {
					if (e instanceof Map<?, ?> em) {
						// store raw map (cast each key/value to Object)
						@SuppressWarnings("unchecked")
						Map<String, Object> edgeMap = (Map<String, Object>) em;

						// Fix double-escaped connection fields when loading from existing task.json
						Object connectionObj = edgeMap.get("connection");
						if (connectionObj instanceof String) {
							String connStr = (String) connectionObj;
							if (!connStr.trim().isEmpty() && connStr.trim().startsWith("{")) {
								try {
									@SuppressWarnings("unchecked")
									Map<String, Object> parsed = (Map<String, Object>) mapper.readValue(connStr,
											Map.class);
									// Replace the string with the parsed Map to prevent double-escaping
									edgeMap.put("connection", parsed);
								} catch (Exception ex) {
									// If parsing fails, leave the original string
								}
							}
						}

						entry.edges.add(edgeMap);
					}
				}
			}
		}

		// Save the loaded task to database
		saveTaskToDatabase(userId, taskId, entry);
	}

	/**
	 * Legacy loadExistingTask method
	 */
	public synchronized void loadExistingTask(Path repo) throws IOException {
		if (currentUserId.isEmpty() || currentTaskId.isEmpty()) {
			logger.warn("No current user/task context set for loadExistingTask");
			return;
		}
		loadExistingTask(currentUserId, currentTaskId, repo);
	}

	/**
	 * Import and create a task from JSON content
	 * 
	 * @param userId          User ID
	 * @param taskId          Task ID (should be unique)
	 * @param taskJsonContent Task JSON content as string
	 * @return true if successful, false otherwise
	 */
	public synchronized boolean importTaskFromJson(String userId, String taskId, String taskJsonContent) {
		try {
			logger.info("Importing task {} for user {}", taskId, userId);

			// Parse the task JSON
			TaskDto taskDto = mapper.readValue(taskJsonContent, TaskDto.class);

			if (taskDto.getTask() == null) {
				logger.error("Invalid task JSON: missing 'task' section");
				return false;
			}

			// Create cache entry from task DTO
			TaskCacheEntry entry = new TaskCacheEntry();
			entry.env = taskDto.getEnv() != null ? taskDto.getEnv() : "finance";
			entry.interfaceNum = taskDto.getInterfaceNum() != null ? taskDto.getInterfaceNum() : 4;
			entry.instruction = taskDto.getTask().getInstruction() != null ? taskDto.getTask().getInstruction() : "";
			entry.userId = userId;
			entry.taskId = taskId;

			// Convert actions - actions list stores action names as strings
			if (taskDto.getTask().getActions() != null) {
				for (TaskDto.ActionDto action : taskDto.getTask().getActions()) {
					entry.actions.add(action.getName());

					// Store full action objects separately
					Map<String, Object> actionMap = new LinkedHashMap<>();
					actionMap.put("name", action.getName());
					if (action.getArguments() != null) {
						actionMap.put("arguments", action.getArguments());
					}
					if (action.getOutput() != null) {
						actionMap.put("output", action.getOutput());
					}
					entry.actionObjects.add(actionMap);
				}
			}

			// Convert outputs
			if (taskDto.getTask().getOutputs() != null) {
				entry.outputs.addAll(taskDto.getTask().getOutputs());
			}

			// Convert edges
			if (taskDto.getTask().getEdges() != null) {
				for (TaskDto.EdgeDto edge : taskDto.getTask().getEdges()) {
					Map<String, Object> edgeMap = new LinkedHashMap<>();
					edgeMap.put("from", edge.getFrom());
					edgeMap.put("to", edge.getTo());
					if (edge.getConnection() != null) {
						Map<String, Object> connectionMap = new LinkedHashMap<>();
						connectionMap.put("output", edge.getConnection().getOutput());
						connectionMap.put("input", edge.getConnection().getInput());
						edgeMap.put("connection", connectionMap);
					}
					entry.edges.add(edgeMap);
				}
			}

			// Store in memory cache
			String cacheKey = userId + ":" + taskId;
			userTaskCache.put(cacheKey, entry);

			// Save to database with original JSON content to preserve model configuration
			saveTaskToDatabase(userId, taskId, entry, taskJsonContent);

			// Set as current context for immediate use
			setCurrentContext(userId, taskId);

			logger.info("Successfully imported task {} for user {}", taskId, userId);
			return true;

		} catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
			// Invalid JSON should return false (caller may show validation error)
			logger.error("Invalid task JSON for task {} user {}: {}", taskId, userId, jpe.getOriginalMessage());
			return false;
		} catch (Exception e) {
			// Rethrow non-JSON exceptions (e.g., DB constraint violations) so controller
			// can surface details
			logger.error("Error importing task {} for user {}: {}", taskId, userId, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Clear cache for a specific user and task This is useful when importing a new
	 * task to ensure old data doesn't interfere
	 */
	public synchronized void clearCache(String userId, String taskId) {
		String cacheKey = generateCacheKey(userId, taskId);
		TaskCacheEntry removed = userTaskCache.remove(cacheKey);

		if (removed != null) {
			logger.info("Cleared cache entry for user {} task {}", userId, taskId);
		} else {
			logger.info("No cache entry found for user {} task {} (already clear)", userId, taskId);
		}
	}

	/**
	 * Clear all cache entries for a specific user Useful when user logs out or
	 * wants to start fresh
	 */
	public synchronized void clearUserCache(String userId) {
		List<String> keysToRemove = userTaskCache.keySet().stream().filter(key -> key.startsWith(userId + "_"))
				.collect(Collectors.toList());

		keysToRemove.forEach(userTaskCache::remove);

		logger.info("Cleared {} cache entries for user {}", keysToRemove.size(), userId);
	}
}
