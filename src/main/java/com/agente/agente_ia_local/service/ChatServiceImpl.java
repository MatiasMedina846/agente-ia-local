package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.dto.ChatRequestDTO;
import com.agente.agente_ia_local.dto.ChatResponseDTO;
import com.agente.agente_ia_local.model.ChatMessage;
import com.agente.agente_ia_local.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository, AiService aiService) {
        this.chatMessageRepository = chatMessageRepository;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public ChatResponseDTO processMessage(ChatRequestDTO request) {
        List<ChatMessage> recent = chatMessageRepository.findTop10ByOrderByCreatedAtDesc();
        String enrichedPrompt = buildContextPrompt(request.getPrompt(), recent);
        String aiResponse = aiService.generateResponse(enrichedPrompt, recent);
        ChatMessage saved = chatMessageRepository.save(new ChatMessage(request.getPrompt(), aiResponse));
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponseDTO> getHistory() {
        return chatMessageRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private String buildContextPrompt(String userPrompt, List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return userPrompt;
        }
        List<ChatMessage> chronological = new ArrayList<>(history);
        Collections.reverse(chronological);
        StringBuilder sb = new StringBuilder();
        sb.append("[Historial de Conversación Anterior]").append(System.lineSeparator());
        for (ChatMessage message : chronological) {
            sb.append("Usuario: ").append(message.getPrompt()).append(System.lineSeparator());
            sb.append("Asistente: ").append(message.getResponse()).append(System.lineSeparator());
        }
        sb.append("[Nueva Pregunta]").append(System.lineSeparator());
        sb.append("Usuario: ").append(userPrompt);
        return sb.toString();
    }

    private ChatResponseDTO toDto(ChatMessage message) {
        return new ChatResponseDTO(
                message.getId(),
                message.getPrompt(),
                message.getResponse(),
                message.getCreatedAt()
        );
    }
}
