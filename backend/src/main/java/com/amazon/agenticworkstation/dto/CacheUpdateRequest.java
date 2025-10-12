package com.amazon.agenticworkstation.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Partial update request for caching task building fields. All fields optional.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheUpdateRequest {
    private String repositoryPath; // Local folder where task.json should be saved
    private String env;
    private Integer interfaceNum; // interface_num
    private String instruction;
    private String userId;
    private List<String> actions; // simple list of action names
    private List<Map<String, Object>> actionObjects; // full action objects with arguments/output
    private List<String> outputs; // list of output names
    private List<Map<String, Object>> edges; // raw edge objects (from, to, etc.)

    public String getRepositoryPath() { return repositoryPath; }
    public void setRepositoryPath(String repositoryPath) { this.repositoryPath = repositoryPath; }
    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }
    public Integer getInterfaceNum() { return interfaceNum; }
    public void setInterfaceNum(Integer interfaceNum) { this.interfaceNum = interfaceNum; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
    public List<Map<String, Object>> getActionObjects() { return actionObjects; }
    public void setActionObjects(List<Map<String, Object>> actionObjects) { this.actionObjects = actionObjects; }
    public List<String> getOutputs() { return outputs; }
    public void setOutputs(List<String> outputs) { this.outputs = outputs; }
    public List<Map<String, Object>> getEdges() { return edges; }
    public void setEdges(List<Map<String, Object>> edges) { this.edges = edges; }
}
