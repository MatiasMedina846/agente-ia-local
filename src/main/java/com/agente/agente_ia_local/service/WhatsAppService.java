package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.TenantCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String GRAPH_API = "https://graph.facebook.com/";

    private final RestTemplate restTemplate;

    public WhatsAppService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restTemplate = new RestTemplate(factory);
    }

    public void sendTextMessage(TenantCredentials credentials, String to, String text) {
        String url = GRAPH_API + credentials.getWhatsappPhoneNumberId() + "/messages";
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );
        sendRequest(credentials, url, body);
    }

    public void sendTypingIndicator(TenantCredentials credentials, String to) {
        String url = GRAPH_API + credentials.getWhatsappPhoneNumberId() + "/messages";
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", "..."),
                "status", "composing"
        );
        try {
            sendRequest(credentials, url, body);
        } catch (Exception e) {
            log.debug("Typing indicator failed (non-critical): {}", e.getMessage());
        }
    }

    public String downloadMedia(TenantCredentials credentials, String mediaId) {
        String url = GRAPH_API + mediaId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credentials.getWhatsappApiKey());
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.has("url")) {
                String mediaUrl = body.get("url").asText();
                ResponseEntity<byte[]> mediaResponse = restTemplate.exchange(mediaUrl, HttpMethod.GET,
                        new HttpEntity<>(headers), byte[].class);
                if (mediaResponse.getBody() != null) {
                    return java.util.Base64.getEncoder().encodeToString(mediaResponse.getBody());
                }
            }
        } catch (Exception e) {
            log.error("Error descargando media {}: {}", mediaId, e.getMessage());
        }
        return null;
    }

    private void sendRequest(TenantCredentials credentials, String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(credentials.getWhatsappApiKey());
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), JsonNode.class);
            if (response.getStatusCode().isError()) {
                log.error("WhatsApp API error: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("Error enviando mensaje WhatsApp: {}", e.getMessage());
        }
    }
}
