package com.ahorra.feature.capture.parser;

import com.ahorra.common.exception.InternalServerErrorException;
import com.ahorra.feature.capture.dto.ParsedEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static com.ahorra.common.constants.GeneralConstants.TOOL_NAME;

/**
 * Reads entries with a Groq model, through Groq's OpenAI-style chat completions
 * API with a forced function call so the reply is always structured data.
 * Active when app.parser.provider=groq. The user's text goes in as the user
 * message only; it is data to extract from, never instructions.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.parser.provider", havingValue = "groq")
public class GroqEntryParser implements EntryParser {

    /** Groq checks tool arguments against the schema and rejects null for a string; the model sends null for fields the note did not state. */
    private static ObjectNode allowNullOptionals(ObjectNode schema) {
        JsonNode required = schema.path("required");
        ObjectNode props = (ObjectNode) schema.get("properties");
        props.fieldNames().forEachRemaining(name -> {
            boolean isRequired = false;
            for (JsonNode r : required) {
                if (r.asText().equals(name)) {
                    isRequired = true;
                }
            }
            JsonNode type = props.get(name).get("type");
            if (!isRequired && type != null && type.isTextual() && type.asText().equals("string")) {
                ((ObjectNode) props.get(name)).putArray("type").add("string").add("null");
            }
        });
        return schema;
    }

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GroqEntryParser(ObjectMapper objectMapper,
                           @Value("${groq.api-key:}") String apiKey,
                           @Value("${groq.model:llama-3.3-70b-versatile}") String model,
                           @Value("${groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
                           @Value("${groq.timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.restTemplate = EntryParserSupport.newRestTemplate(timeoutSeconds);
    }

    @Override
    public ParsedEntry parse(ParseContext context) {
        EntryParserSupport.requireKey(apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(baseUrl + "/chat/completions", HttpMethod.POST,
                    new HttpEntity<>(buildRequestBody(context), headers), String.class);
        } catch (RestClientException ex) {
            throw EntryParserSupport.callFailed("Groq", ex);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode arguments = root.path("choices").path(0).path("message")
                    .path("tool_calls").path(0).path("function").path("arguments");
            JsonNode input = EntryParserSupport.readArguments(objectMapper, arguments);
            if (input != null) {
                return EntryParserSupport.toParsedEntry(input);
            }
        } catch (Exception ex) {
            log.error("Could not read Groq response", ex);
            throw new InternalServerErrorException("The entry parser returned an unreadable result.", ex);
        }
        throw new InternalServerErrorException("The entry parser returned no result.", null);
    }

    private String buildRequestBody(ParseContext context) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("max_tokens", 1024);

        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", EntryParserSupport.systemPrompt(context));
        messages.addObject()
                .put("role", "user")
                .put("content", context.rawText());

        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", TOOL_NAME);
        function.put("description", EntryParserSupport.TOOL_DESCRIPTION);
        function.set("parameters", allowNullOptionals(EntryParserSupport.entrySchema(objectMapper)));
        body.putArray("tools").addObject()
                .put("type", "function")
                .set("function", function);

        ObjectNode toolChoice = body.putObject("tool_choice");
        toolChoice.put("type", "function");
        toolChoice.putObject("function").put("name", TOOL_NAME);

        return body.toString();
    }
}
