package com.ahorra.feature.capture.parser;

import com.ahorra.common.exception.InternalServerErrorException;
import com.ahorra.feature.capture.dto.ParsedEntry;
import java.util.List;
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
 * Reads entries with a Mistral model, through Mistral's chat completions API
 * with a forced function call so the reply is always structured data.
 * Active when app.parser.provider=mistral. The user's text goes in as the user
 * message only; it is data to extract from, never instructions.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.parser.provider", havingValue = "mistral")
public class MistralEntryParser implements EntryParser {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public MistralEntryParser(ObjectMapper objectMapper,
                              @Value("${mistral.api-key:}") String apiKey,
                              @Value("${mistral.model:mistral-small-latest}") String model,
                              @Value("${mistral.base-url:https://api.mistral.ai/v1}") String baseUrl,
                              @Value("${mistral.timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.restTemplate = EntryParserSupport.newRestTemplate(timeoutSeconds);
    }

    @Override
    public List<ParsedEntry> parse(ParseContext context) {
        EntryParserSupport.requireKey(apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(baseUrl + "/chat/completions", HttpMethod.POST,
                    new HttpEntity<>(buildRequestBody(context), headers), String.class);
        } catch (RestClientException ex) {
            throw EntryParserSupport.callFailed("Mistral", ex);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode arguments = root.path("choices").path(0).path("message")
                    .path("tool_calls").path(0).path("function").path("arguments");
            JsonNode input = EntryParserSupport.readArguments(objectMapper, arguments);
            if (input != null) {
                return EntryListSupport.toParsedEntries(input);
            }
        } catch (Exception ex) {
            log.error("Could not read Mistral response", ex);
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
        function.set("parameters", EntryListSupport.entriesSchema(objectMapper));
        body.putArray("tools").addObject()
                .put("type", "function")
                .set("function", function);

        // Mistral forces a tool call with "any"; there is only the one tool.
        body.put("tool_choice", "any");

        return body.toString();
    }
}
