package com.agente.agente_ia_local;

import com.agente.agente_ia_local.model.ChatMessage;
import com.agente.agente_ia_local.model.Product;
import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.repository.ChatMessageRepository;
import com.agente.agente_ia_local.repository.TenantRepository;
import com.agente.agente_ia_local.service.AiService;
import com.agente.agente_ia_local.service.HybridSearchService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatServiceTest {

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private AiService aiService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private HybridSearchService hybridSearchService;

    @AfterEach
    void cleanup() {
        chatMessageRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("AI genera respuesta para mensaje simple")
    void generateResponse() {
        String response = aiService.generateResponse("Hola, ¿cómo estás?", List.of());
        assertNotNull(response);
        assertFalse(response.isBlank());
        assertTrue(response.length() > 5);
    }

    @Test
    @Order(2)
    @DisplayName("AI responde sobre horario")
    void scheduleQuestion() {
        String response = aiService.generateResponse("¿Cuál es el horario?", List.of());
        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("horario") || response.toLowerCase().contains("9:00"));
    }

    @Test
    @Order(3)
    @DisplayName("AI responde sobre precios")
    void priceQuestion() {
        String response = aiService.generateResponse("¿Cuánto cuesta?", List.of());
        assertNotNull(response);
        assertTrue(response.length() > 5);
    }

    @Test
    @Order(4)
    @DisplayName("AI responde sobre dirección")
    void locationQuestion() {
        String response = aiService.generateResponse("¿Dónde están ubicados?", List.of());
        assertNotNull(response);
        assertTrue(response.length() > 5);
    }

    @Test
    @Order(5)
    @DisplayName("Guardar y recuperar mensaje")
    void saveAndGetMessage() {
        ChatMessage msg = new ChatMessage("Hola", "¡Hola! ¿En qué puedo ayudarte?");
        ChatMessage saved = chatMessageRepository.save(msg);

        assertNotNull(saved.getId());
        assertEquals("Hola", saved.getPrompt());
        assertEquals("¡Hola! ¿En qué puedo ayudarte?", saved.getResponse());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @Order(6)
    @DisplayName("Historial en orden cronológico")
    void historyOrder() {
        chatMessageRepository.save(new ChatMessage("Primero", "Respuesta 1"));
        chatMessageRepository.save(new ChatMessage("Segundo", "Respuesta 2"));
        chatMessageRepository.save(new ChatMessage("Tercero", "Respuesta 3"));

        List<ChatMessage> all = chatMessageRepository.findAll();
        assertEquals(3, all.size());
        assertEquals("Primero", all.get(0).getPrompt());
        assertEquals("Tercero", all.get(2).getPrompt());
    }

    @Test
    @Order(7)
    @DisplayName("findTop10ByOrderByCreatedAtDesc retorna últimos 10")
    void top10() {
        for (int i = 0; i < 15; i++) {
            chatMessageRepository.save(new ChatMessage("Msg " + i, "Resp " + i));
        }
        List<ChatMessage> top10 = chatMessageRepository.findTop10ByOrderByCreatedAtDesc();
        assertEquals(10, top10.size());
    }

    @Test
    @Order(8)
    @DisplayName("Prompt vacío no genera respuesta")
    void emptyPrompt() {
        String response = aiService.generateResponse("", List.of());
        assertEquals("", response);
    }

    @Test
    @Order(9)
    @DisplayName("Prompt null no genera respuesta")
    void nullPrompt() {
        String response = aiService.generateResponse(null, List.of());
        assertEquals("", response);
    }
}
