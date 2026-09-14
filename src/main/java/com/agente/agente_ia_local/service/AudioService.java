package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.TenantCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class AudioService {

    private static final Logger log = LoggerFactory.getLogger(AudioService.class);
    private static final int MAX_AUDIO_DURATION_SECONDS = 60;

    @Value("${ai.whisper.url:https://api.openai.com/v1/audio/transcriptions}")
    private String whisperUrl;

    private final RestTemplate restTemplate;

    public AudioService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
    }

    public String transcribe(String audioBase64, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No hay API key de Whisper configurada");
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            byte[] audioBytes = java.util.Base64.getDecoder().decode(audioBase64);

            org.springframework.util.LinkedMultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("file", new org.springframework.core.io.ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return "audio.ogg";
                }
            });
            parts.add("model", "whisper-1");
            parts.add("language", "es");

            HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> request =
                    new HttpEntity<>(parts, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(whisperUrl, request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.has("text")) {
                return cleanTranscription(body.get("text").asText());
            }
        } catch (Exception e) {
            log.error("Error en transcripción Whisper: {}", e.getMessage());
        }
        return null;
    }

    private String cleanTranscription(String text) {
        if (text == null) return null;
        String[] fillerWords = {"eh", "ehh", "ehhh", "mmm", "mm", "uuuh", "uh", "este", "esta", "o sea", "digamos", "bueno", "pues"};
        String cleaned = text;
        for (String filler : fillerWords) {
            cleaned = cleaned.replaceAll("(?i)\\b" + filler + "\\b", "").replaceAll("\\s{2,}", " ").trim();
        }
        return cleaned;
    }
}
