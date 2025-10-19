package com.amazon.agenticworkstation.service.openai;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.scenario.models.ScenarioInputDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AIInputFillerService {
    private final OpenAIClient client;
    private final OpenAIProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIInputFillerService(OpenAIClient client, OpenAIProperties props) {
        this.client = client;
        this.props = props;
    }

    /**
     * Fill required inputs using AI based on an instruction string.
     * Returns a map of inputName -> filledValue (strings).
     */
    public Map<String, String> fillRequiredInputs(String instructionString, List<ScenarioInputDefinition> requiredInputs) {
        if (instructionString == null) {
            throw new AIServiceException("instructionString cannot be null");
        }

        String prompt = buildPrompt(instructionString, requiredInputs);

        // Build a minimal Responses API style request
        Map<String, Object> request = Map.of(
                "model", props.getModel(),
                "input", prompt
        );

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String resp = client.chatCompletion(requestJson);
            // Parse the response body and extract text
            JsonNode root = objectMapper.readTree(resp);
            // Attempt to find a top-level 'output' or 'choices' area depending on API
            String text = null;
            if (root.has("output")) {
                JsonNode output = root.get("output");
                if (output.isArray() && output.size() > 0) {
                    text = output.get(0).asText();
                } else if (output.isTextual()) {
                    text = output.asText();
                }
            } else if (root.has("choices")) {
                JsonNode choices = root.get("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode first = choices.get(0);
                    if (first.has("message") && first.get("message").has("content")) {
                        JsonNode content = first.get("message").get("content");
                        if (content.isTextual()) {
                            text = content.asText();
                        } else if (content.isArray() && content.size() > 0) {
                            text = content.get(0).asText();
                        }
                    } else if (first.has("text")) {
                        text = first.get("text").asText();
                    }
                }
            }

            if (text == null) {
                throw new AIServiceException("Could not parse OpenAI response: " + resp);
            }

            // Expecting the model to return a JSON object mapping names to values.
            Map<String, String> result = new HashMap<>();
            try {
                JsonNode parsed = objectMapper.readTree(text.trim());
                if (parsed.isObject()) {
                    for (ScenarioInputDefinition def : requiredInputs) {
                        String key = def.getName();
                        if (parsed.has(key)) {
                            result.put(key, parsed.get(key).asText());
                        }
                    }
                } else {
                    // fallback: try to parse as lines like 'name: value'
                    String[] lines = text.split("\n");
                    for (String line : lines) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String k = parts[0].trim();
                            String v = parts[1].trim();
                            result.put(k, v);
                        }
                    }
                }
            } catch (IOException e) {
                throw new AIServiceException("Failed to parse model output as JSON", e);
            }

            return result;
        } catch (IOException | InterruptedException e) {
            throw new AIServiceException("Error calling OpenAI API", e);
        }
    }

    private String buildPrompt(String instructionString, List<ScenarioInputDefinition> requiredInputs) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an assistant that fills missing inputs for a task.\n");
        sb.append("Instruction: \"").append(instructionString).append("\"\n");
        sb.append("Return a JSON object mapping the following field names to an appropriate string value. If an input is optional and you cannot infer it, omit it or set it to null.\n");
        sb.append("Fields:\n");
        for (ScenarioInputDefinition def : requiredInputs) {
            sb.append("- ").append(def.getName()).append(" (type: ").append(def.getType()).append(")");
            if (def.getDescription() != null && !def.getDescription().isEmpty()) {
                sb.append(" - ").append(def.getDescription());
            }
            sb.append("\n");
        }
        sb.append("Provide only valid JSON in the response (no surrounding explanation).\n");
        return sb.toString();
    }
    
    public static void main(String[] args) {
        // Sample usage demonstration using a fake OpenAIClient that returns a fixed response.
        OpenAIProperties props = new OpenAIProperties();
        props.setModel("test-model");

        // Create a real OpenAIClient using environment variables or system properties.
        // Priority: system properties, then environment variables.
        String apiKey = System.getProperty("openai.apiKey");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }
        String baseUrl = System.getProperty("openai.baseUrl");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = System.getenv("OPENAI_BASE_URL");
        }
        String model = System.getProperty("openai.model");
        if (model == null || model.isEmpty()) {
            model = System.getenv("OPENAI_MODEL");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: OpenAI API key not set. Set system property 'openai.apiKey' or env var 'OPENAI_API_KEY'.");
            return;
        }

        if (baseUrl != null && !baseUrl.isEmpty()) {
            props.setBaseUrl(baseUrl);
        }
        if (model != null && !model.isEmpty()) {
            props.setModel(model);
        }

        OpenAIProperties clientProps = props;
        clientProps.setApiKey(apiKey);
        OpenAIClient realClient = new OpenAIClient(clientProps);

        AIInputFillerService service = new AIInputFillerService(realClient, props);

        List<ScenarioInputDefinition> defs = List.of(
                ScenarioInputDefinition.builder().name("requester_email").type("email").build(),
                ScenarioInputDefinition.builder().name("department_name").type("string").build()
            );

        Map<String, String> filled = service.fillRequiredInputs("Create a department for Alice", defs);
        System.out.println("Filled inputs:");
        filled.forEach((k, v) -> System.out.println("  " + k + " => " + v));
    }
}
