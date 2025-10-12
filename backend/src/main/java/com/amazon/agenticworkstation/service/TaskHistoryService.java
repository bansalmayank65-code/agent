package com.amazon.agenticworkstation.service;

import com.amazon.agenticworkstation.entity.TaskEntity;
import com.amazon.agenticworkstation.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing task history and user access to tasks
 */
@Service
@Transactional(readOnly = true)
public class TaskHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskHistoryService.class);
    
    @Autowired
    private TaskRepository taskRepository;
    
    /**
     * Get all tasks for the user
     */
    public List<TaskEntity> getTasksForUser(String userId) {
        logger.debug("Getting all tasks for user: {}", userId);
        
        // Get all active tasks for this user
        List<TaskEntity> tasks = taskRepository.findByUserIdAndIsActiveTrue(userId);
        
        logger.info("Found {} tasks for user: {}", tasks.size(), userId);
        return tasks;
    }
    
    /**
     * Get tasks by status for a user
     */
    public List<TaskEntity> getTasksByStatus(String userId, String status) {
        logger.debug("Getting tasks with status {} for user: {}", status, userId);
        
        // Convert string status to enum
        TaskEntity.TaskStatus taskStatus;
        try {
            taskStatus = TaskEntity.TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid task status: " + status);
        }
        
        // Get tasks by status for this user
        List<TaskEntity> tasks = taskRepository.findByUserIdAndTaskStatusAndIsActiveTrue(userId, taskStatus);
        
        logger.info("Found {} tasks with status {} for user: {}", tasks.size(), status, userId);
        return tasks;
    }
    
    /**
     * Get task statistics for a user
     */
    public Map<String, Object> getTaskStatistics(String userId) {
        logger.debug("Getting task statistics for user: {}", userId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Get total task count
        long totalTasks = taskRepository.countByUserIdAndIsActiveTrue(userId);
        
        // Get status breakdown
        Map<String, Long> statusBreakdown = new HashMap<>();
        statusBreakdown.put("DRAFT", taskRepository.countByUserIdAndTaskStatusAndIsActiveTrue(userId, TaskEntity.TaskStatus.DRAFT));
        statusBreakdown.put("SUBMITTED", taskRepository.countByUserIdAndTaskStatusAndIsActiveTrue(userId, TaskEntity.TaskStatus.SUBMITTED));
        statusBreakdown.put("APPROVED", taskRepository.countByUserIdAndTaskStatusAndIsActiveTrue(userId, TaskEntity.TaskStatus.APPROVED));
        statusBreakdown.put("REJECTED", taskRepository.countByUserIdAndTaskStatusAndIsActiveTrue(userId, TaskEntity.TaskStatus.REJECTED));
        statusBreakdown.put("NEEDS_CHANGES", taskRepository.countByUserIdAndTaskStatusAndIsActiveTrue(userId, TaskEntity.TaskStatus.NEEDS_CHANGES));
        statusBreakdown.put("MERGED", taskRepository.countByUserIdAndTaskStatusAndIsActiveTrue(userId, TaskEntity.TaskStatus.MERGED));
        statusBreakdown.put("DISCARDED", taskRepository.countByUserIdAndTaskStatusAndIsActiveTrue(userId, TaskEntity.TaskStatus.DISCARDED));
        
        statistics.put("totalTasks", totalTasks);
        statistics.put("statusBreakdown", statusBreakdown);
        statistics.put("userId", userId);
        
        logger.info("Generated task statistics for user {}: {} total tasks", userId, totalTasks);
        
        return statistics;
    }
    
    /**
     * Get detailed task information with access control
     */
    public TaskEntity getTaskDetails(String userId, String taskId) {
        logger.debug("Getting task details for user: {} and task: {}", userId, taskId);
        
        // Find the task
        TaskEntity task = taskRepository.findByTaskIdAndIsActiveTrue(taskId);
        
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        // Check if user has access to this task
        if (!userId.equals(task.getUserId())) {
            throw new IllegalArgumentException("Access denied: User does not have access to this task");
        }
        
        logger.info("Retrieved task details for user: {} and task: {}", userId, taskId);
        return task;
    }
    
    /**
     * Get recent tasks for a user (limited number)
     */
    public List<TaskEntity> getRecentTasks(String userId, int limit) {
        logger.debug("Getting {} recent tasks for user: {}", limit, userId);
        
        // Get recent tasks ordered by creation date
        PageRequest pageRequest = PageRequest.of(0, limit, 
            Sort.by(Sort.Direction.DESC, "createdDateTime"));
        
        List<TaskEntity> tasks = taskRepository.findByUserIdAndIsActiveTrueOrderByCreatedDateTimeDesc(userId, pageRequest);
        
        logger.info("Found {} recent tasks for user: {}", tasks.size(), userId);
        return tasks;
    }
    
    /**
     * Check if user has access to a specific task
     */
    public boolean hasUserAccessToTask(String userId, String taskId) {
        logger.debug("Checking access for user: {} to task: {}", userId, taskId);
        
        try {
            getTaskDetails(userId, taskId);
            return true;
        } catch (IllegalArgumentException e) {
            logger.debug("Access denied for user: {} to task: {} - {}", userId, taskId, e.getMessage());
            return false;
        }
    }
}