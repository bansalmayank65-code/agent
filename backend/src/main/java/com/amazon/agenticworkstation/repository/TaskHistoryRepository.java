package com.amazon.agenticworkstation.repository;

import com.amazon.agenticworkstation.entity.TaskHistoryEntity;
import com.amazon.agenticworkstation.entity.TaskHistoryEntity.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for TaskHistoryEntity
 */
@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, Long> {
    
    /**
     * Find task history by task ID
     */
    List<TaskHistoryEntity> findByTaskIdOrderByChangeTimestampDesc(String taskId);
    
    /**
     * Find task history by task ID and action type
     */
    List<TaskHistoryEntity> findByTaskIdAndActionTypeOrderByChangeTimestampDesc(String taskId, ActionType actionType);
    
    /**
     * Find task history by changed by (user)
     */
    List<TaskHistoryEntity> findByChangedByOrderByChangeTimestampDesc(String changedBy);
    
    /**
     * Find task history within a date range
     */
    @Query("SELECT th FROM TaskHistoryEntity th WHERE th.changeTimestamp BETWEEN :startDate AND :endDate ORDER BY th.changeTimestamp DESC")
    List<TaskHistoryEntity> findTaskHistoryBetween(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find task history by action type within a date range
     */
    @Query("SELECT th FROM TaskHistoryEntity th WHERE th.actionType = :actionType AND th.changeTimestamp BETWEEN :startDate AND :endDate ORDER BY th.changeTimestamp DESC")
    List<TaskHistoryEntity> findByActionTypeBetween(@Param("actionType") ActionType actionType,
                                                  @Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find recent changes for a task
     */
    @Query("SELECT th FROM TaskHistoryEntity th WHERE th.taskId = :taskId AND th.changeTimestamp >= :since ORDER BY th.changeTimestamp DESC")
    List<TaskHistoryEntity> findRecentChanges(@Param("taskId") String taskId, 
                                            @Param("since") LocalDateTime since);
    
    /**
     * Count changes by user within a time period
     */
    @Query("SELECT COUNT(th) FROM TaskHistoryEntity th WHERE th.changedBy = :userId AND th.changeTimestamp >= :since")
    long countChangesByUser(@Param("userId") String userId, @Param("since") LocalDateTime since);
    
    /**
     * Find task history by IP address
     */
    List<TaskHistoryEntity> findByIpAddressOrderByChangeTimestampDesc(String ipAddress);
    
    /**
     * Find latest change for a task
     */
    @Query("SELECT th FROM TaskHistoryEntity th WHERE th.taskId = :taskId ORDER BY th.changeTimestamp DESC LIMIT 1")
    TaskHistoryEntity findLatestChange(@Param("taskId") String taskId);
}