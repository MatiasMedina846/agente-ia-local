package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.dto.ChatRequestDTO;
import com.agente.agente_ia_local.dto.ChatResponseDTO;

import java.util.List;

public interface ChatService {

    ChatResponseDTO processMessage(ChatRequestDTO request);

    List<ChatResponseDTO> getHistory();
}
