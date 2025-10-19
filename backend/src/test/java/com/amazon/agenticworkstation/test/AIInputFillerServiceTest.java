package com.amazon.agenticworkstation.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.scenario.models.ScenarioInputDefinition;
import com.amazon.agenticworkstation.service.openai.AIInputFillerService;
import com.amazon.agenticworkstation.service.openai.OpenAIClient;
import com.amazon.agenticworkstation.service.openai.OpenAIProperties;
import org.junit.jupiter.api.Test;

/**
 * Unit test for AIInputFillerService using a fake OpenAIClient.
 */
public class AIInputFillerServiceTest {

    private static class FakeOpenAIClient extends OpenAIClient {
        private final String response;

        public FakeOpenAIClient(String response) {
            super(new OpenAIProperties());
            this.response = response;
        }

        @Override
        public String chatCompletion(String messagesJson) {
            return response;
        }
    }

    @Test
    void testFillRequiredInputs_parsesJsonResponse() {
        // Fake response body simulating the Responses API returning output text
        String modelText = "{\"requester_email\": \"alice@example.com\", \"department_name\": \"Engineering\"}";
        // Build a top-level Responses-like JSON that our parser will find the 'output' or 'choices' text
        String responsesBody = "{\"output\": [\"" + modelText.replace("\"", "\\\"") + "\"] }";

        OpenAIClient fakeClient = new FakeOpenAIClient(responsesBody);
        OpenAIProperties props = new OpenAIProperties();
        props.setModel("test-model");

        AIInputFillerService service = new AIInputFillerService(fakeClient, props);

        List<ScenarioInputDefinition> defs = List.of(
                ScenarioInputDefinition.builder().name("requester_email").type("email").build(),
                ScenarioInputDefinition.builder().name("department_name").type("string").build()
        );

        Map<String, String> result = service.fillRequiredInputs("Create a department for Alice (alice.p@gmail.com)", defs);

        assertEquals(2, result.size());
        assertEquals("alice@example.com", result.get("requester_email"));
        assertEquals("Engineering", result.get("department_name"));
    }

    @Test
    void testFillRequiredInputs_parsesChoicesStyle() {
        String content = "{\"requester_email\": \"bob@example.com\", \"department_name\": \"HR\"}";
        String choicesBody = "{\"choices\": [{\"message\": {\"content\": \"" + content.replace("\"", "\\\"") + "\"}}]}";

        OpenAIClient fakeClient = new FakeOpenAIClient(choicesBody);
        OpenAIProperties props = new OpenAIProperties();
        props.setModel("test-model");

        AIInputFillerService service = new AIInputFillerService(fakeClient, props);

        List<ScenarioInputDefinition> defs = List.of(
                ScenarioInputDefinition.builder().name("requester_email").type("email").build(),
                ScenarioInputDefinition.builder().name("department_name").type("string").build()
        );

        Map<String, String> result = service.fillRequiredInputs("Create a department for Bob", defs);

        assertEquals(2, result.size());
        assertEquals("bob@example.com", result.get("requester_email"));
        assertEquals("HR", result.get("department_name"));
    }
}
