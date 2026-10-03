package com.ahorra.feature.capture.transcription;

import com.ahorra.common.exception.InternalServerErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Locale;

/**
 * Transcribes with Whisper on Groq, through Groq's OpenAI-style audio transcription API. It uses the same Groq key
 * as the entry parser. The language is detected from the audio. The recording lives in memory for the length of the
 * call only.
 */
@Slf4j
@Service
public class GroqTranscriptionService implements TranscriptionService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GroqTranscriptionService(ObjectMapper objectMapper,
                                    @Value("${groq.api-key:}") String apiKey,
                                    @Value("${groq.transcription-model:whisper-large-v3-turbo}") String model,
                                    @Value("${groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
                                    @Value("${groq.transcription-timeout-seconds:30}") int timeoutSeconds) {
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
    public String transcribe(byte[] audio, String contentType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InternalServerErrorException("Voice entry is not configured", null);
        }

        String filename = "entry" + extensionFor(contentType);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        form.add("model", model);
        form.add("response_format", "json");
        form.add("temperature", "0");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(baseUrl + "/audio/transcriptions", HttpMethod.POST,
                    new HttpEntity<>(form, headers), String.class);
        } catch (RestClientException ex) {
            log.error("Groq transcription failed: {}", ex.getMessage());
            if (ex instanceof HttpClientErrorException.TooManyRequests) {
                throw new InternalServerErrorException("Voice entry is busy. Please try again in a moment.", ex);
            }
            throw new InternalServerErrorException("Could not turn the recording into text. Please try again.", ex);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("text").asText("").trim();
        } catch (Exception ex) {
            log.error("Could not read the transcription response", ex);
            throw new InternalServerErrorException("The transcription came back unreadable. Please try again.", ex);
        }
    }

    /** Groq decides how to decode the file from its extension, so match it to what the browser recorded. */
    private static String extensionFor(String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("mp4") || type.contains("aac") || type.contains("m4a")) return ".m4a";
        if (type.contains("ogg")) return ".ogg";
        if (type.contains("wav")) return ".wav";
        if (type.contains("mpeg") || type.contains("mp3")) return ".mp3";
        return ".webm";
    }
}
