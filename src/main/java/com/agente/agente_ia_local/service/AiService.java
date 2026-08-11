package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.ChatMessage;

import java.util.List;

public interface AiService {

    String generateResponse(String prompt, List<ChatMessage> history);
}
