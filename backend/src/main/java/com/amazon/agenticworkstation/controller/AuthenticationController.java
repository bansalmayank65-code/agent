package com.amazon.agenticworkstation.controller;

import com.amazon.agenticworkstation.service.AuthenticationService;
import com.amazon.agenticworkstation.service.AuthenticationService.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication operations
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class AuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, 
                                                   HttpServletRequest httpRequest) {
        logger.info("Login attempt for user: {}", request.getUserId());
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthenticationResult result = authenticationService.authenticateUser(
            request.getUserId(), 
            request.getPassword(), 
            ipAddress, 
            userAgent
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        if (result.isSuccess()) {
            response.put("sessionId", result.getSessionId());
            response.put("userId", result.getUserId());
            
            // Store session in HTTP session as well
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("userId", result.getUserId());
            session.setAttribute("sessionId", result.getSessionId());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                    HttpServletRequest httpRequest) {
        String sessionId = extractSessionId(authHeader);
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        if (sessionId != null) {
            authenticationService.logout(sessionId, ipAddress, userAgent);
        }
        
        // Invalidate HTTP session
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate session endpoint
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSession(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String sessionId = extractSessionId(authHeader);
        boolean valid = sessionId != null && authenticationService.validateSession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        
        if (valid) {
            String userId = authenticationService.getUserFromSession(sessionId);
            response.put("userId", userId);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Register endpoint (optional, for user self-registration)
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody LoginRequest request) {
        logger.info("Registration attempt for user: {}", request.getUserId());
        
        AuthenticationResult result = authenticationService.registerUser(
            request.getUserId(), 
            request.getPassword()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user info
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String sessionId = extractSessionId(authHeader);
        
        Map<String, Object> response = new HashMap<>();
        
        if (sessionId == null || !authenticationService.validateSession(sessionId)) {
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }
        
        String userId = authenticationService.getUserFromSession(sessionId);
        response.put("authenticated", true);
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Security status endpoint for monitoring
     */
    @GetMapping("/security-status")
    public ResponseEntity<Map<String, Object>> getSecurityStatus(@RequestParam String userId) {
        long recentFailures = authenticationService.getRecentFailedAttempts(userId, 30); // Last 30 minutes
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("recentFailedAttempts", recentFailures);
        response.put("securityAlert", recentFailures > 5); // Alert if more than 5 failures in 30 minutes
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract session ID from Authorization header
     */
    private String extractSessionId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    /**
     * Get client IP address with proxy support
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Login request DTO
     */
    public static class LoginRequest {
        private String userId;
        private String password;
        
        // Constructors
        public LoginRequest() {}
        
        public LoginRequest(String userId, String password) {
            this.userId = userId;
            this.password = password;
        }
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}