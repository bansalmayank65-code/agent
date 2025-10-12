package com.amazon.agenticworkstation.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("Health check requested at /actuator/health");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running");
        response.put("timestamp", System.currentTimeMillis());
        logger.info("Health check successful");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> simpleHealth() {
        logger.debug("Health check requested at /health");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running");
        response.put("timestamp", System.currentTimeMillis());
        logger.info("Simple health check successful");
        return ResponseEntity.ok(response);
    }
}