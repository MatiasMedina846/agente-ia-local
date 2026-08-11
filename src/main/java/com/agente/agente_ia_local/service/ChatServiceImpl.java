package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.dto.ChatRequestDTO;
import com.agente.agente_ia_local.dto.ChatResponseDTO;
import com.agente.agente_ia_local.model.ChatMessage;
import com.agente.agente_ia_local.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        String aiResponse = aiService.generateResponse(request.getPrompt());
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

    private ChatResponseDTO toDto(ChatMessage message) {
        return new ChatResponseDTO(
                message.getId(),
                message.getPrompt(),
                message.getResponse(),
                message.getCreatedAt()
        );
    }
}
