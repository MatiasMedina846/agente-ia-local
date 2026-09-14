package com.agente.agente_ia_local.controller;

import com.agente.agente_ia_local.model.ConversationMessage;
import com.agente.agente_ia_local.model.TenantCredentials;
import com.agente.agente_ia_local.repository.TenantCredentialsRepository;
import com.agente.agente_ia_local.service.AnalyticsService;
import com.agente.agente_ia_local.service.MessagePipelineService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Value("${whatsapp.verify-token:agenteia_verify_token}")
    private String verifyToken;

    private final TenantCredentialsRepository credentialsRepository;
    private final MessagePipelineService pipelineService;
    private final AnalyticsService analyticsService;

    public WebhookController(TenantCredentialsRepository credentialsRepository,
                             MessagePipelineService pipelineService, AnalyticsService analyticsService) {
        this.credentialsRepository = credentialsRepository;
        this.pipelineService = pipelineService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook WhatsApp verificado exitosamente");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Verificación de webhook fallida: mode={}, token={}", mode, token);
        return ResponseEntity.status(403).body("Forbidden");
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> handleMessage(@RequestBody JsonNode payload) {
        try {
            if (payload == null || !payload.has("entry")) {
                return ResponseEntity.ok(Map.of("status", "ok"));
            }

            JsonNode entry = payload.get("entry").get(0);
            JsonNode changes = entry.get("changes").get(0);
            JsonNode value = changes.get("value");

            if (!"messages".equals(changes.get("field").asText())) {
                return ResponseEntity.ok(Map.of("status", "ok"));
            }

            String phoneNumberId = value.get("metadata").get("phone_number_id").asText();

            TenantCredentials creds = credentialsRepository.findByWhatsappPhoneNumberId(phoneNumberId).orElse(null);
            if (creds == null) {
                log.warn("No se encontraron credenciales para phone_number_id: {}", phoneNumberId);
                return ResponseEntity.ok(Map.of("status", "unknown_tenant"));
            }

            if (value.has("messages")) {
                JsonNode messages = value.get("messages");
                for (JsonNode msg : messages) {
                    String from = msg.get("from").asText();
                    String type = msg.get("type").asText();
                    String content = extractContent(msg, type);

                    if (content != null) {
                        ConversationMessage.MessageType msgType = switch (type) {
                            case "audio" -> ConversationMessage.MessageType.AUDIO;
                            case "image" -> ConversationMessage.MessageType.IMAGE;
                            default -> ConversationMessage.MessageType.TEXT;
                        };

                        analyticsService.trackEvent(creds.getTenantId(), null, "MESSAGE_RECEIVED",
                                "{\"from\":\"" + from + "\",\"type\":\"" + type + "\"}");

                        pipelineService.processIncomingMessage(creds.getTenantId(), from, content, msgType);
                    }
                }
            }

            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            log.error("Error procesando webhook WhatsApp", e);
            return ResponseEntity.ok(Map.of("status", "error"));
        }
    }

    private String extractContent(JsonNode msg, String type) {
        return switch (type) {
            case "text" -> msg.get("text").get("body").asText();
            case "audio" -> msg.get("audio").has("id") ? msg.get("audio").get("id").asText() : null;
            case "image" -> msg.get("image").has("id") ? msg.get("image").get("id").asText() : null;
            default -> null;
        };
    }
}
