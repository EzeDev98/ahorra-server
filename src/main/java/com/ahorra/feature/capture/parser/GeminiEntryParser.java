package com.ahorra.feature.capture.parser;

import com.ahorra.common.exception.InternalServerErrorException;
import com.ahorra.feature.capture.dto.ParsedEntry;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Locale;

import static com.ahorra.common.constants.GeneralConstants.TOOL_NAME;

/**
 * Reads entries with a Google Gemini model, through the Gemini API's own
 * generateContent endpoint with a forced function call so the reply is always
 * structured data. Active when app.parser.provider=gemini. The user's text goes
 * in as the user turn only; it is data to extract from, never instructions.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.parser.provider", havingValue = "gemini")
public class GeminiEntryParser implements EntryParser {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiEntryParser(ObjectMapper objectMapper,
                             @Value("${gemini.api-key:}") String apiKey,
                             @Value("${gemini.model:gemini-flash-latest}") String model,
                             @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
                             @Value("${gemini.timeout-seconds:20}") int timeoutSeconds) {
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
        headers.set("x-goog-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(baseUrl + "/models/" + model + ":generateContent", HttpMethod.POST,
                    new HttpEntity<>(buildRequestBody(context), headers), String.class);
        } catch (RestClientException ex) {
            throw EntryParserSupport.callFailed("Gemini", ex);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
                JsonNode args = part.path("functionCall").path("args");
                if (args.isObject()) {
                    return EntryListSupport.toParsedEntries(args);
                }
            }
        } catch (Exception ex) {
            log.error("Could not read Gemini response", ex);
            throw new InternalServerErrorException("The entry parser returned an unreadable result.", ex);
        }
        throw new InternalServerErrorException("The entry parser returned no result.", null);
    }

    private String buildRequestBody(ParseContext context) {
        ObjectNode body = objectMapper.createObjectNode();

        body.putObject("systemInstruction")
                .putArray("parts").addObject()
                .put("text", EntryParserSupport.systemPrompt(context));

        ObjectNode userTurn = body.putArray("contents").addObject();
        userTurn.put("role", "user");
        userTurn.putArray("parts").addObject().put("text", context.rawText());

        ObjectNode declaration = objectMapper.createObjectNode();
        declaration.put("name", TOOL_NAME);
        declaration.put("description", EntryParserSupport.TOOL_DESCRIPTION);
        // Gemini names its schema types in capitals (OBJECT, STRING, NUMBER, ARRAY).
        declaration.set("parameters", EntryListSupport.entriesSchema(objectMapper, name -> name.toUpperCase(Locale.ROOT)));
        body.putArray("tools").addObject()
                .putArray("functionDeclarations").add(declaration);

        ObjectNode calling = body.putObject("toolConfig").putObject("functionCallingConfig");
        calling.put("mode", "ANY");
        calling.putArray("allowedFunctionNames").add(TOOL_NAME);

        body.putObject("generationConfig").put("temperature", 0);

        return body.toString();
    }
}
