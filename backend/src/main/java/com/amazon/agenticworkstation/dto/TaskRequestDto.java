package com.amazon.agenticworkstation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for receiving task creation requests from the frontend
 * This represents the form data collected from the UI steps
 */
public class TaskRequestDto {
    private String instruction;
    
    @JsonProperty("user_id")
    private String userId;
    
    private List<String> actions;
    private List<String> outputs;
    private List<EdgeInputDto> edges;
    
    // Optional configuration fields with defaults
    private String env = "finance";
    
    @JsonProperty("model_provider")
    private String modelProvider = "openai";
    
    private String model = "gpt-4o";
    
    @JsonProperty("num_trials")
    private Integer numTrials = 3;
    
    private Double temperature = 1.0;
    
    @JsonProperty("interface_num")
    private Integer interfaceNum = 4;

    /**
     * Simplified edge input for frontend form
     */
    public static class EdgeInputDto {
        private String from;
        private String to;
        private String outputField;
        private String inputField;

        // Getters and setters
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        public String getOutputField() { return outputField; }
        public void setOutputField(String outputField) { this.outputField = outputField; }

        public String getInputField() { return inputField; }
        public void setInputField(String inputField) { this.inputField = inputField; }
    }

    // Getters and setters
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }

    public List<String> getOutputs() { return outputs; }
    public void setOutputs(List<String> outputs) { this.outputs = outputs; }

    public List<EdgeInputDto> getEdges() { return edges; }
    public void setEdges(List<EdgeInputDto> edges) { this.edges = edges; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getNumTrials() { return numTrials; }
    public void setNumTrials(Integer numTrials) { this.numTrials = numTrials; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getInterfaceNum() { return interfaceNum; }
    public void setInterfaceNum(Integer interfaceNum) { this.interfaceNum = interfaceNum; }
}