package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OllamaAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiServiceImpl.class);

    private final RestTemplate restTemplate;

    @Value("${ai.ollama.model:llama3}")
    private String model;

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    public OllamaAiServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generateResponse(String prompt, List<ChatMessage> history) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> request = Map.of("model", model, "prompt", prompt, "stream", false);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    ollamaUrl + "/api/generate", request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.hasNonNull("response")) {
                return body.get("response").asText().trim();
            }
        } catch (Exception e) {
            log.warn("No se pudo conectar con Ollama ({}). Usando respuesta simulada.", e.getMessage());
        }
        return simulatedResponse(prompt, history);
    }

    private String simulatedResponse(String enrichedPrompt, List<ChatMessage> history) {
        String question = currentQuestion(enrichedPrompt).trim();
        String lower = question.toLowerCase(Locale.ROOT);
        if (lower.contains("estábamos hablando") || lower.contains("estabamos hablando")
                || lower.contains("de qué estábamos") || lower.contains("de que estábamos")
                || lower.contains("seguimos hablando")) {
            String topic = (history == null || history.isEmpty())
                    ? "tu última consulta"
                    : history.get(0).getPrompt();
            return "Estábamos hablando sobre \"" + topic + "\". ¿Querés que profundicemos en ese tema?";
        }
        if (lower.contains("hola") || lower.contains("buenas") || lower.contains("saludos")) {
            return "¡Hola! Gracias por contactarnos. ¿En qué puedo ayudarte hoy?";
        }
        if (lower.contains("horario") || lower.contains("abierto") || lower.contains("hora")) {
            return "Nuestro horario de atención es de lunes a sábado de 9:00 a 20:00 hs.";
        }
        if (lower.contains("precio") || lower.contains("cuánto") || lower.contains("cuesta")) {
            return "Contamos con precios muy accesibles. ¿Te gustaría que te compartamos nuestro catálogo actual?";
        }
        if (lower.contains("dirección") || lower.contains("ubicación") || lower.contains("dónde")) {
            return "Estamos ubicados en Av. Principal 1234, local 5. Te esperamos en persona.";
        }
        return "Entendido. Un asesor se comunicará contigo a la brevedad con la información que necesitas.";
    }

    private String currentQuestion(String enrichedPrompt) {
        int idx = enrichedPrompt.lastIndexOf("Usuario: ");
        if (idx >= 0) {
            return enrichedPrompt.substring(idx + "Usuario: ".length());
        }
        return enrichedPrompt;
    }
}
