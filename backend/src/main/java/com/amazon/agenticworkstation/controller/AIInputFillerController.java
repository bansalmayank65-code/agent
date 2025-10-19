package com.amazon.agenticworkstation.controller;

import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.scenario.models.ScenarioInputDefinition;
import com.amazon.agenticworkstation.service.openai.AIInputFillerService;
import com.amazon.agenticworkstation.service.openai.AIServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/ai")
public class AIInputFillerController {

    private final AIInputFillerService service;

    public AIInputFillerController(AIInputFillerService service) {
        this.service = service;
    }

    public static class FillRequest {
        @NotNull
        public String instructionString;

        @NotNull
        public List<ScenarioInputDefinition> requiredInputs;
    }

    @PostMapping("/fill-inputs")
    public Map<String, String> fillInputs(@RequestBody FillRequest req) {
        return service.fillRequiredInputs(req.instructionString, req.requiredInputs);
    }

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<String> handleAIError(AIServiceException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
