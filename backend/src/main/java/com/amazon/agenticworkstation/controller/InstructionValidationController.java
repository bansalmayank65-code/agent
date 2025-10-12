package com.amazon.agenticworkstation.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class InstructionValidationController {

    private static final Logger logger = LoggerFactory.getLogger(InstructionValidationController.class);

    private static final String EXTERNAL_API_URL = "https://turing-amazon-toolings.vercel.app/instruction_validation";
    private final RestTemplate restTemplate;

    public InstructionValidationController() {
        this.restTemplate = new RestTemplate();
        logger.info("InstructionValidationController initialized with external API URL: {}", EXTERNAL_API_URL);
    }

    // Removed manual /auth/google and /auth/callback endpoints in favor of Spring Security's oauth2Login()

    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication) {
        logger.info("Checking authentication status");
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("authenticated", isAuthenticated);
        if (isAuthenticated && authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                response.put("user_email", oidcUser.getEmail());
                response.put("user_name", oidcUser.getFullName());
            } else {
                response.put("principal_class", principal.getClass().getName());
            }
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/instruction_validation")
    public ResponseEntity<Map<String, Object>> instructionValidation(@RequestBody Map<String, Object> request,
                                                                     HttpServletRequest httpRequest) {
        logger.info("Starting instruction validation request");
        logger.debug("Request payload keys: {}", request.keySet());
        
        try {
            // Forward the request to the external API with proper headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "*/*");
            headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            headers.set("Origin", "https://turing-amazon-toolings.vercel.app");
            headers.set("Referer", "https://turing-amazon-toolings.vercel.app/instruction_validation");
            headers.set("Sec-Ch-Ua", "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Google Chrome\";v=\"140\"");
            headers.set("Sec-Ch-Ua-Mobile", "?0");
            headers.set("Sec-Ch-Ua-Platform", "\"Windows\"");
            headers.set("Sec-Fetch-Dest", "empty");
            headers.set("Sec-Fetch-Mode", "cors");
            headers.set("Sec-Fetch-Site", "same-origin");
            headers.set("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36");

            // Get cookies from the incoming HTTP request and forward them
            String cookieHeader = httpRequest.getHeader("Cookie");
            if (cookieHeader != null) {
                headers.set("Cookie", cookieHeader);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> externalResponse = restTemplate.exchange(
                    EXTERNAL_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("code", externalResponse.getStatusCode().value());
            result.put("raw", externalResponse.getBody());
            return ResponseEntity.ok(result);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("External HTTP error: {} {}", e.getStatusCode(), e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("code", e.getStatusCode().value());
            error.put("message", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(error);
        } catch (RestClientException e) {
            logger.error("Rest client exception: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}