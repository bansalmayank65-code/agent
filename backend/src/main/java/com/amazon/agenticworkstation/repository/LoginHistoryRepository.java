package com.amazon.agenticworkstation.repository;

import com.amazon.agenticworkstation.entity.LoginHistoryEntity;
import com.amazon.agenticworkstation.entity.LoginHistoryEntity.LoginStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for LoginHistoryEntity
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {
    
    /**
     * Find login history by user ID
     */
    List<LoginHistoryEntity> findByUserIdOrderByLoginTimestampDesc(String userId);
    
    /**
     * Find login history by user ID and status
     */
    List<LoginHistoryEntity> findByUserIdAndLoginStatusOrderByLoginTimestampDesc(String userId, LoginStatus loginStatus);
    
    /**
     * Find login history within a date range
     */
    @Query("SELECT lh FROM LoginHistoryEntity lh WHERE lh.loginTimestamp BETWEEN :startDate AND :endDate ORDER BY lh.loginTimestamp DESC")
    List<LoginHistoryEntity> findLoginHistoryBetween(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find recent failed login attempts for a user
     */
    @Query("SELECT lh FROM LoginHistoryEntity lh WHERE lh.userId = :userId AND lh.loginStatus = 'FAILED' AND lh.loginTimestamp >= :since ORDER BY lh.loginTimestamp DESC")
    List<LoginHistoryEntity> findRecentFailedLogins(@Param("userId") String userId, 
                                                   @Param("since") LocalDateTime since);
    
    /**
     * Count login attempts by status for a user within a time period
     */
    @Query("SELECT COUNT(lh) FROM LoginHistoryEntity lh WHERE lh.userId = :userId AND lh.loginStatus = :status AND lh.loginTimestamp >= :since")
    long countLoginAttemptsByStatus(@Param("userId") String userId, 
                                  @Param("status") LoginStatus status, 
                                  @Param("since") LocalDateTime since);
    
    /**
     * Find login history by IP address
     */
    List<LoginHistoryEntity> findByIpAddressOrderByLoginTimestampDesc(String ipAddress);
    
    /**
     * Find latest successful login for a user
     */
    @Query("SELECT lh FROM LoginHistoryEntity lh WHERE lh.userId = :userId AND lh.loginStatus = 'SUCCESS' ORDER BY lh.loginTimestamp DESC LIMIT 1")
    LoginHistoryEntity findLatestSuccessfulLogin(@Param("userId") String userId);
}