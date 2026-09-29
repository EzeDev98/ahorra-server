package com.ahorra.feature.report.service.impl;

import com.ahorra.common.exception.InternalServerErrorException;
import com.ahorra.feature.report.dto.CategoryLine;
import com.ahorra.feature.report.dto.ReportFacts;
import com.ahorra.feature.report.enums.ReportVerdict;
import com.ahorra.feature.report.service.NarrativeWriter;
import com.ahorra.feature.report.service.ReportFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Asks the model to phrase finished facts as one paragraph. It is given only
 * pre-formatted figures and told to copy them exactly; whatever it returns is
 * checked by NarrativeGuard before it is ever stored.
 */
@Slf4j
@Component
public class AnthropicNarrativeWriter implements NarrativeWriter {

    private static final String API_VERSION = "2023-06-01";

    private static final String SYSTEM_PROMPT = """
            You write one short paragraph (at most four sentences) summarising a person's spending day \
            for their personal finance app. The user message is a JSON object of facts.
            Rules:
            - Use only the facts given. Copy every amount and percentage exactly as written. \
            Never calculate, round, estimate or invent a number.
            - Do not mention dates.
            - Be descriptive and neutral: state the pattern and the figure and let the reader draw \
            their own conclusion. No advice, no scolding.
            - Only if goodDay is true, include one specific, factual acknowledgement of what went well.
            - Output only the paragraph.""";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public AnthropicNarrativeWriter(ObjectMapper objectMapper,
                                    @Value("${anthropic.api-key:}") String apiKey,
                                    @Value("${anthropic.model:claude-haiku-4-5}") String model,
                                    @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
                                    @Value("${anthropic.timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String write(ReportFacts facts) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InternalServerErrorException("Report narrative is not configured", null);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 400);
        body.put("system", SYSTEM_PROMPT);
        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", factsJson(facts).toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", API_VERSION);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(baseUrl + "/v1/messages", HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers), String.class);
        } catch (RestClientException ex) {
            log.error("Anthropic narrative call failed: {}", ex.getMessage());
            throw new InternalServerErrorException("Could not reach the narrative writer.", ex);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    String text = block.path("text").asText().trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        } catch (Exception ex) {
            throw new InternalServerErrorException("The narrative writer returned an unreadable result.", ex);
        }
        throw new InternalServerErrorException("The narrative writer returned no text.", null);
    }

    private ObjectNode factsJson(ReportFacts f) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("entriesToday", f.entryCount());
        n.put("spendToday", ReportFormat.money(f.daySpend()));

        ArrayNode dayCategories = n.putArray("spendTodayByCategory");
        for (CategoryLine line : f.dayCategories()) {
            ObjectNode c = dayCategories.addObject();
            c.put("category", line.name());
            c.put("amount", ReportFormat.money(line.amount()));
            c.put("shareOfToday", ReportFormat.percent(line.sharePercent()));
        }

        if (f.verdict() != ReportVerdict.NO_INCOME_SET) {
            n.put("periodType", f.periodType().name().toLowerCase());
            n.put("spentSoFarThisPeriod", ReportFormat.money(f.periodSpend()));
            n.put("incomeBaseline", ReportFormat.money(f.baseline()));
            n.put("incomeUpperEnd", ReportFormat.money(f.incomeMax()));
            n.put("differenceFromBaseline", ReportFormat.money(f.variance()));
            n.put("differenceDirection", f.variance().signum() > 0 ? "above the baseline" : "below the baseline");
            n.put("paceAssessment", f.verdict().name());
        }
        if (f.comparisonAvailable() && f.rollingAvg() != null) {
            n.put("usualDailySpendLast30Days", ReportFormat.money(f.rollingAvg()));
        }
        if (f.insight() != null) {
            n.put("keyInsight", f.insight());
        }
        n.put("goodDay", f.goodDay());
        return n;
    }
}
