package com.agente.agente_ia_local.service;

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
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    private final RestTemplate restTemplate;

    public ImageService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
    }

    public ImageClassification classifyImage(String imageBase64) {
        try {
            Map<String, Object> request = Map.of(
                    "model", "llava",
                    "prompt", "Clasifica esta imagen en UNA de estas categorías exactas: PRODUCTO, COMPROBANTE_PAGO, DOCUMENTO, OTRO. Responde SOLO con la categoría.",
                    "images", new String[]{imageBase64},
                    "stream", false
            );
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    ollamaUrl + "/api/generate", request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.hasNonNull("response")) {
                String category = body.get("response").asText().trim().toUpperCase();
                return new ImageClassification(category, body.get("response").asText().trim());
            }
        } catch (Exception e) {
            log.warn("Error clasificando imagen con LLaVA: {}", e.getMessage());
        }
        return new ImageClassification("OTRO", "No se pudo clasificar");
    }

    public String extractReceiptData(String imageBase64) {
        try {
            Map<String, Object> request = Map.of(
                    "model", "llava",
                    "prompt", "Extrae los datos de este comprobante de pago: monto total, fecha, vendedor/comercio, productos si los hay. Responde en JSON con campos: amount, date, vendor, items. Si no puedes extraer algo, pon null.",
                    "images", new String[]{imageBase64},
                    "stream", false
            );
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    ollamaUrl + "/api/generate", request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.hasNonNull("response")) {
                return body.get("response").asText().trim();
            }
        } catch (Exception e) {
            log.warn("Error extrayendo datos de comprobante: {}", e.getMessage());
        }
        return null;
    }

    public record ImageClassification(String category, String description) {}
}
