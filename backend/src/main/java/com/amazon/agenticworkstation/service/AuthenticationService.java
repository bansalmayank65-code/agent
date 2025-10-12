package com.amazon.agenticworkstation.service;

import com.amazon.agenticworkstation.entity.LoginEntity;
import com.amazon.agenticworkstation.entity.LoginHistoryEntity;
import com.amazon.agenticworkstation.entity.LoginHistoryEntity.LoginStatus;
import com.amazon.agenticworkstation.repository.LoginRepository;
import com.amazon.agenticworkstation.repository.LoginHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling user authentication
 */
@Service
@Transactional
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    @Autowired
    private LoginRepository loginRepository;
    
    @Autowired
    private LoginHistoryRepository loginHistoryRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Simple in-memory session storage (in production, use Redis or database)
    private final Map<String, String> activeSessions = new HashMap<>();
    
    /**
     * Authenticate user with username and password
     */
    public AuthenticationResult authenticateUser(String userId, String password, String ipAddress, String userAgent) {
        logger.info("Authentication attempt for user: {}", userId);
        
        try {
            // Find active user
            Optional<LoginEntity> userOpt = loginRepository.findByUserIdAndIsActive(userId, true);
            
            if (userOpt.isEmpty()) {
                logger.warn("Authentication failed - user not found or inactive: {}", userId);
                logLoginAttempt(userId, LoginStatus.FAILED, "User not found or inactive", ipAddress, userAgent, null);
                return AuthenticationResult.failure("Invalid username or password");
            }
            
            LoginEntity user = userOpt.get();
            
            // Check password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.warn("Authentication failed - invalid password for user: {}", userId);
                logLoginAttempt(userId, LoginStatus.FAILED, "Invalid password", ipAddress, userAgent, null);
                return AuthenticationResult.failure("Invalid username or password");
            }
            
            // Generate session token
            String sessionId = UUID.randomUUID().toString();
            activeSessions.put(sessionId, userId);
            
            // Log successful login
            logLoginAttempt(userId, LoginStatus.SUCCESS, null, ipAddress, userAgent, sessionId);
            
            logger.info("Authentication successful for user: {}", userId);
            return AuthenticationResult.success(sessionId, userId);
            
        } catch (Exception e) {
            logger.error("Authentication error for user: {}", userId, e);
            logLoginAttempt(userId, LoginStatus.FAILED, "System error", ipAddress, userAgent, null);
            return AuthenticationResult.failure("Authentication error occurred");
        }
    }
    
    /**
     * Validate session token
     */
    public boolean validateSession(String sessionId) {
        return sessionId != null && activeSessions.containsKey(sessionId);
    }
    
    /**
     * Get user ID from session
     */
    public String getUserFromSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Logout user and invalidate session
     */
    public void logout(String sessionId, String ipAddress, String userAgent) {
        String userId = activeSessions.get(sessionId);
        if (userId != null) {
            activeSessions.remove(sessionId);
            logLoginAttempt(userId, LoginStatus.LOGOUT, null, ipAddress, userAgent, sessionId);
            logger.info("User logged out: {}", userId);
        }
    }
    
    /**
     * Register new user (if registration is enabled)
     */
    public AuthenticationResult registerUser(String userId, String password) {
        try {
            // Check if user already exists
            if (loginRepository.existsByUserIdAndIsActive(userId, true)) {
                return AuthenticationResult.failure("User already exists");
            }
            
            // Create new user
            String encodedPassword = passwordEncoder.encode(password);
            LoginEntity newUser = new LoginEntity(userId, encodedPassword);
            loginRepository.save(newUser);
            
            logger.info("New user registered: {}", userId);
            return AuthenticationResult.success(null, userId);
            
        } catch (Exception e) {
            logger.error("Registration error for user: {}", userId, e);
            return AuthenticationResult.failure("Registration error occurred");
        }
    }
    
    /**
     * Log login attempt to history
     */
    private void logLoginAttempt(String userId, LoginStatus status, String failureReason, 
                               String ipAddress, String userAgent, String sessionId) {
        try {
            LoginHistoryEntity loginHistory = new LoginHistoryEntity(userId, status);
            loginHistory.setFailureReason(failureReason);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setSessionId(sessionId);
            
            loginHistoryRepository.save(loginHistory);
        } catch (Exception e) {
            logger.error("Failed to log login attempt for user: {}", userId, e);
        }
    }
    
    /**
     * Get recent failed login attempts for security monitoring
     */
    public long getRecentFailedAttempts(String userId, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginHistoryRepository.countLoginAttemptsByStatus(userId, LoginStatus.FAILED, since);
    }
    
    /**
     * Authentication result class
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String message;
        private final String sessionId;
        private final String userId;
        
        private AuthenticationResult(boolean success, String message, String sessionId, String userId) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.userId = userId;
        }
        
        public static AuthenticationResult success(String sessionId, String userId) {
            return new AuthenticationResult(true, "Authentication successful", sessionId, userId);
        }
        
        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, message, null, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
    }
}