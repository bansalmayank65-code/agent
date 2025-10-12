package com.amazon.agenticworkstation.repository;

import com.amazon.agenticworkstation.entity.LoginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LoginEntity
 */
@Repository
public interface LoginRepository extends JpaRepository<LoginEntity, String> {
    
    /**
     * Find active user by user ID
     */
    Optional<LoginEntity> findByUserIdAndIsActive(String userId, Boolean isActive);
    
    /**
     * Find all active users
     */
    List<LoginEntity> findByIsActive(Boolean isActive);
    
    /**
     * Check if user exists and is active
     */
    boolean existsByUserIdAndIsActive(String userId, Boolean isActive);
    
    /**
     * Find users created within a date range
     */
    @Query("SELECT l FROM LoginEntity l WHERE l.createdDateTime BETWEEN :startDate AND :endDate")
    List<LoginEntity> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Soft delete user by setting isActive to false
     */
    @Query("UPDATE LoginEntity l SET l.isActive = false, l.updatedDateTime = CURRENT_TIMESTAMP WHERE l.userId = :userId")
    int softDeleteUser(@Param("userId") String userId);
}