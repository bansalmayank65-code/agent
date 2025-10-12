package com.amazon.agenticworkstation.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Complete DTO representing the full task.json structure
 * Based on the Amazon Tau Bench task format
 */
public class TaskDto {
    // Top-level configuration fields
    private String env;
    
    @JsonProperty("model_provider")
    private String modelProvider;
    
    private String model;
    
    @JsonProperty("num_trials")
    private Integer numTrials;
    
    private Double temperature;
    
    @JsonProperty("interface_num")
    private Integer interfaceNum;
    
    // Main task object
    private TaskDetails task;

    /**
     * Inner class representing the main task details
     */
    public static class TaskDetails {
        @JsonProperty("user_id")
        private String userId;
        
        private String instruction;
        private List<ActionDto> actions;
        private List<EdgeDto> edges;
        private List<String> outputs;
        
        @JsonProperty("num_edges")
        private Integer numEdges;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }

        public List<ActionDto> getActions() { return actions; }
        public void setActions(List<ActionDto> actions) { this.actions = actions; }

        public List<EdgeDto> getEdges() { return edges; }
        public void setEdges(List<EdgeDto> edges) { this.edges = edges; }

        public List<String> getOutputs() { return outputs; }
        public void setOutputs(List<String> outputs) { this.outputs = outputs; }

        public Integer getNumEdges() { return numEdges; }
        public void setNumEdges(Integer numEdges) { this.numEdges = numEdges; }
    }

    /**
     * DTO representing an action within a task
     */
    public static class ActionDto {
        private String name;
        private Map<String, Object> arguments;
        private Object output; // Can be List, Map, or simple value

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Map<String, Object> getArguments() { return arguments; }
        public void setArguments(Map<String, Object> arguments) { this.arguments = arguments; }

        public Object getOutput() { return output; }
        public void setOutput(Object output) { this.output = output; }
    }

    /**
     * DTO representing an edge connection between actions
     */
    public static class EdgeDto {
        private String from;
        private String to;
        private ConnectionDto connection;

        // Getters and setters
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        public ConnectionDto getConnection() { return connection; }
        public void setConnection(ConnectionDto connection) { this.connection = connection; }
    }

    /**
     * DTO representing the connection details within an edge
     */
    public static class ConnectionDto {
        private String output;
        private String input;

        // Getters and setters
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
    }

    // Main TaskDto getters and setters
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

    public TaskDetails getTask() { return task; }
    public void setTask(TaskDetails task) { this.task = task; }
}
