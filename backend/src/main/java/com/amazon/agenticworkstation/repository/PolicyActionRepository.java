package com.amazon.agenticworkstation.repository;

import com.amazon.agenticworkstation.entity.PolicyActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PolicyActionEntity
 */
@Repository
public interface PolicyActionRepository extends JpaRepository<PolicyActionEntity, Long> {
    
    /**
     * Find all policy actions by environment name
     */
    List<PolicyActionEntity> findByEnvNameOrderByLastUpdatedAtDesc(String envName);
    
    /**
     * Find all policy actions by environment name and interface number
     */
    List<PolicyActionEntity> findByEnvNameAndInterfaceNumOrderByLastUpdatedAtDesc(String envName, Integer interfaceNum);
    
    /**
     * Find all policy actions by environment name, interface number, and policy_cat1
     */
    List<PolicyActionEntity> findByEnvNameAndInterfaceNumAndPolicyCat1OrderByLastUpdatedAtDesc(
            String envName, Integer interfaceNum, String policyCat1);
    
    /**
     * Find all policy actions by environment name, interface number, policy_cat1, and policy_cat2
     */
    List<PolicyActionEntity> findByEnvNameAndInterfaceNumAndPolicyCat1AndPolicyCat2OrderByLastUpdatedAtDesc(
            String envName, Integer interfaceNum, String policyCat1, String policyCat2);
    
    /**
     * Get distinct environment names
     */
    @Query("SELECT DISTINCT p.envName FROM PolicyActionEntity p ORDER BY p.envName")
    List<String> findDistinctEnvNames();
    
    /**
     * Get distinct interface numbers for a given environment
     */
    @Query("SELECT DISTINCT p.interfaceNum FROM PolicyActionEntity p WHERE p.envName = :envName ORDER BY p.interfaceNum")
    List<Integer> findDistinctInterfaceNumsByEnvName(@Param("envName") String envName);
    
    /**
     * Get distinct policy_cat1 values for a given environment and interface
     */
    @Query("SELECT DISTINCT p.policyCat1 FROM PolicyActionEntity p WHERE p.envName = :envName AND p.interfaceNum = :interfaceNum ORDER BY p.policyCat1")
    List<String> findDistinctPolicyCat1ByEnvNameAndInterfaceNum(
            @Param("envName") String envName, 
            @Param("interfaceNum") Integer interfaceNum);
    
    /**
     * Get distinct policy_cat2 values for a given environment, interface, and policy_cat1
     */
    @Query("SELECT DISTINCT p.policyCat2 FROM PolicyActionEntity p WHERE p.envName = :envName AND p.interfaceNum = :interfaceNum AND p.policyCat1 = :policyCat1 ORDER BY p.policyCat2")
    List<String> findDistinctPolicyCat2ByEnvNameAndInterfaceNumAndPolicyCat1(
            @Param("envName") String envName, 
            @Param("interfaceNum") Integer interfaceNum, 
            @Param("policyCat1") String policyCat1);
    
    /**
     * Find all policy actions ordered by last updated
     */
    List<PolicyActionEntity> findAllByOrderByLastUpdatedAtDesc();
    
    /**
     * Search policy actions by description
     */
    @Query("SELECT p FROM PolicyActionEntity p WHERE LOWER(p.policyDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY p.lastUpdatedAt DESC")
    List<PolicyActionEntity> searchByDescription(@Param("searchTerm") String searchTerm);
    
    /**
     * Count policy actions by environment name
     */
    long countByEnvName(String envName);
    
    /**
     * Count policy actions by environment name and interface number
     */
    long countByEnvNameAndInterfaceNum(String envName, Integer interfaceNum);
    
    /**
     * Check if a policy action already exists with the given combination
     * Used to prevent duplicate entries
     */
    boolean existsByEnvNameAndInterfaceNumAndPolicyCat1AndPolicyCat2(
            String envName, Integer interfaceNum, String policyCat1, String policyCat2);
    
    /**
     * Find existing policy action by unique combination (excluding a specific ID for updates)
     */
    @Query("SELECT p FROM PolicyActionEntity p WHERE p.envName = :envName AND p.interfaceNum = :interfaceNum " +
           "AND p.policyCat1 = :policyCat1 AND p.policyCat2 = :policyCat2 AND p.policyActionId != :excludeId")
    List<PolicyActionEntity> findDuplicateExcludingId(
            @Param("envName") String envName,
            @Param("interfaceNum") Integer interfaceNum,
            @Param("policyCat1") String policyCat1,
            @Param("policyCat2") String policyCat2,
            @Param("excludeId") Long excludeId);
}
