package com.agente.agente_ia_local.controller;

import com.agente.agente_ia_local.dto.ChatRequestDTO;
import com.agente.agente_ia_local.dto.ChatResponseDTO;
import com.agente.agente_ia_local.model.ChatMessage;
import com.agente.agente_ia_local.model.Product;
import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.repository.ChatMessageRepository;
import com.agente.agente_ia_local.service.AiService;
import com.agente.agente_ia_local.service.HybridSearchService;
import com.agente.agente_ia_local.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;
    private final TenantService tenantService;
    private final HybridSearchService hybridSearchService;

    public ChatController(ChatMessageRepository chatMessageRepository, AiService aiService,
                          TenantService tenantService, HybridSearchService hybridSearchService) {
        this.chatMessageRepository = chatMessageRepository;
        this.aiService = aiService;
        this.tenantService = tenantService;
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping
    public ResponseEntity<ChatResponseDTO> sendMessage(@Valid @RequestBody ChatRequestDTO request) {
        Tenant tenant = tenantService.findBySlug("demo").orElse(null);
        if (tenant == null) {
            tenant = tenantService.findAll().stream().findFirst().orElse(null);
        }

        List<Product> searchResults = List.of();
        if (tenant != null) {
            searchResults = hybridSearchService.search(tenant.getId(), request.getPrompt());
        }
        String productContext = hybridSearchService.formatProductContext(searchResults);

        List<ChatMessage> recent = chatMessageRepository.findTop10ByOrderByCreatedAtDesc();
        String systemPrompt = (tenant != null && tenant.getSystemPrompt() != null) ? tenant.getSystemPrompt() : "";
        String enrichedPrompt = buildEnrichedPrompt(request.getPrompt(), productContext, systemPrompt, recent);

        String aiResponse = aiService.generateResponse(enrichedPrompt, recent);
        ChatMessage saved = chatMessageRepository.save(new ChatMessage(request.getPrompt(), aiResponse));
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatResponseDTO>> getHistory() {
        return ResponseEntity.ok(chatMessageRepository.findAll().stream()
                .map(this::toDto).toList());
    }

    private String buildEnrichedPrompt(String userMessage, String productContext, String systemPrompt,
                                        List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        if (!systemPrompt.isBlank()) {
            sb.append("[Instrucciones del sistema]").append(System.lineSeparator());
            sb.append(systemPrompt).append(System.lineSeparator()).append(System.lineSeparator());
        }
        if (!productContext.isBlank()) {
            sb.append(productContext).append(System.lineSeparator());
        }
        if (history != null && !history.isEmpty()) {
            sb.append("[Historial de Conversación Anterior]").append(System.lineSeparator());
            List<ChatMessage> chronological = new ArrayList<>(history);
            Collections.reverse(chronological);
            for (ChatMessage msg : chronological) {
                sb.append("Usuario: ").append(msg.getPrompt()).append(System.lineSeparator());
                sb.append("Asistente: ").append(msg.getResponse()).append(System.lineSeparator());
            }
        }
        sb.append("[Nueva Pregunta]").append(System.lineSeparator());
        sb.append("Usuario: ").append(userMessage);
        return sb.toString();
    }

    private ChatResponseDTO toDto(ChatMessage msg) {
        return new ChatResponseDTO(msg.getId(), msg.getPrompt(), msg.getResponse(), msg.getCreatedAt());
    }
}
