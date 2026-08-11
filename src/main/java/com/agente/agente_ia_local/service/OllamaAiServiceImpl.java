package com.agente.agente_ia_local.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OllamaAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiServiceImpl.class);

    private final RestClient restClient;

    @Value("${ai.ollama.model:llama3.2}")
    private String model;

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    public OllamaAiServiceImpl() {
        this.restClient = RestClient.create();
    }

    @Override
    public String generateResponse(String prompt) {
        try {
            JsonNode body = restClient.post()
                    .uri(ollamaUrl + "/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "prompt", prompt, "stream", false))
                    .retrieve()
                    .body(JsonNode.class);
            if (body != null && body.hasNonNull("response")) {
                return body.get("response").asText().trim();
            }
        } catch (Exception e) {
            log.warn("No se pudo conectar con Ollama ({}). Usando respuesta simulada.", e.getMessage());
        }
        return simulatedResponse(prompt);
    }

    private String simulatedResponse(String prompt) {
        String lower = prompt.trim().toLowerCase();
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
}
