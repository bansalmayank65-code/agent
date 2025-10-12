package com.amazon.agenticworkstation.repository;

import com.amazon.agenticworkstation.entity.TaskEntity;
import com.amazon.agenticworkstation.entity.TaskEntity.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TaskEntity
 */
@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    
    /**
     * Find active task by task ID
     */
    Optional<TaskEntity> findByTaskIdAndIsActive(String taskId, Boolean isActive);
    
    /**
     * Find tasks by user ID
     */
    List<TaskEntity> findByUserIdAndIsActiveOrderByUpdatedDateTimeDesc(String userId, Boolean isActive);
    
    /**
     * Find tasks by user ID and status
     */
    List<TaskEntity> findByUserIdAndTaskStatusAndIsActiveOrderByUpdatedDateTimeDesc(
            String userId, TaskStatus taskStatus, Boolean isActive);
    
    /**
     * Find tasks by status
     */
    List<TaskEntity> findByTaskStatusAndIsActiveOrderByUpdatedDateTimeDesc(TaskStatus taskStatus, Boolean isActive);
    
    /**
     * Find tasks by environment name
     */
    List<TaskEntity> findByEnvNameAndIsActiveOrderByUpdatedDateTimeDesc(String envName, Boolean isActive);
    
    /**
     * Find tasks by environment name and interface number
     */
    List<TaskEntity> findByEnvNameAndInterfaceNumAndIsActiveOrderByUpdatedDateTimeDesc(
            String envName, Integer interfaceNum, Boolean isActive);
    
    /**
     * Search tasks by instruction content
     */
    @Query("SELECT t FROM TaskEntity t WHERE LOWER(t.instruction) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND t.isActive = :isActive ORDER BY t.updatedDateTime DESC")
    List<TaskEntity> findByInstructionContaining(@Param("searchTerm") String searchTerm, 
                                               @Param("isActive") Boolean isActive);
    
    /**
     * Find tasks created within a date range
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.createdDateTime BETWEEN :startDate AND :endDate AND t.isActive = :isActive ORDER BY t.createdDateTime DESC")
    List<TaskEntity> findTasksCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           @Param("isActive") Boolean isActive);
    
    /**
     * Find tasks updated within a date range
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.updatedDateTime BETWEEN :startDate AND :endDate AND t.isActive = :isActive ORDER BY t.updatedDateTime DESC")
    List<TaskEntity> findTasksUpdatedBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           @Param("isActive") Boolean isActive);
    
    /**
     * Count tasks by status for a user
     */
    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.userId = :userId AND t.taskStatus = :status AND t.isActive = true")
    long countTasksByStatus(@Param("userId") String userId, @Param("status") TaskStatus status);
    
    /**
     * Find tasks with result JSON
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.resultJson IS NOT NULL AND t.isActive = :isActive ORDER BY t.updatedDateTime DESC")
    List<TaskEntity> findTasksWithResults(@Param("isActive") Boolean isActive);
    
    /**
     * Find tasks without result JSON
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.resultJson IS NULL AND t.isActive = :isActive ORDER BY t.updatedDateTime DESC")
    List<TaskEntity> findTasksWithoutResults(@Param("isActive") Boolean isActive);
    
    /**
     * Find all active tasks ordered by updated date
     */
    List<TaskEntity> findByIsActiveOrderByUpdatedDateTimeDesc(Boolean isActive);
    
    /**
     * Soft delete task by setting isActive to false
     */
    @Query("UPDATE TaskEntity t SET t.isActive = false, t.updatedDateTime = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    int softDeleteTask(@Param("taskId") String taskId);
    
    // ====================================================================
    // Methods for Task History Service (User ID Integration)
    // ====================================================================
    
    /**
     * Find tasks by user ID
     */
    List<TaskEntity> findByUserIdAndIsActiveTrue(String userId);
    
    /**
     * Find tasks by user ID and status
     */
    List<TaskEntity> findByUserIdAndTaskStatusAndIsActiveTrue(String userId, TaskStatus status);
    
    /**
     * Count tasks by user ID
     */
    long countByUserIdAndIsActiveTrue(String userId);
    
    /**
     * Count tasks by user ID and status
     */
    long countByUserIdAndTaskStatusAndIsActiveTrue(String userId, TaskStatus status);
    
    /**
     * Find task by ID (active only)
     */
    TaskEntity findByTaskIdAndIsActiveTrue(String taskId);
    
    /**
     * Find recent tasks by user ID with pagination
     */
    List<TaskEntity> findByUserIdAndIsActiveTrueOrderByCreatedDateTimeDesc(String userId, Pageable pageable);
}