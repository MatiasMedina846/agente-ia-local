package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.Conversation;
import com.agente.agente_ia_local.model.HumanHandoff;
import com.agente.agente_ia_local.repository.HumanHandoffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HandoffService {

    private static final Logger log = LoggerFactory.getLogger(HandoffService.class);

    private final HumanHandoffRepository handoffRepository;
    private final ConversationService conversationService;
    private final WhatsAppService whatsAppService;

    public HandoffService(HumanHandoffRepository handoffRepository, ConversationService conversationService, WhatsAppService whatsAppService) {
        this.handoffRepository = handoffRepository;
        this.conversationService = conversationService;
        this.whatsAppService = whatsAppService;
    }

    @Transactional
    public HumanHandoff requestHandoff(Long conversationId, Long tenantId, String reason) {
        Conversation conv = conversationService.findById(conversationId);
        if (conv == null) {
            throw new IllegalArgumentException("Conversación no encontrada: " + conversationId);
        }

        String summary = generateSummary(conversationId);
        HumanHandoff handoff = new HumanHandoff(conversationId, tenantId, reason);
        handoff.setSummary(summary);
        handoff.setStatus("PENDING");
        HumanHandoff saved = handoffRepository.save(handoff);

        conversationService.deactivateBot(conversationId);

        log.info("Handoff solicitado para conversación {}: {}", conversationId, reason);
        return saved;
    }

    @Transactional
    public void resolveHandoff(Long handoffId) {
        HumanHandoff handoff = handoffRepository.findById(handoffId).orElse(null);
        if (handoff != null) {
            handoff.setStatus("RESOLVED");
            handoff.setResolvedAt(LocalDateTime.now());
            handoffRepository.save(handoff);
            conversationService.activateBot(handoff.getConversationId());
        }
    }

    public List<HumanHandoff> getPendingHandoffs(Long tenantId) {
        return handoffRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "PENDING");
    }

    public long countPending(Long tenantId) {
        return handoffRepository.countByTenantIdAndStatus(tenantId, "PENDING");
    }

    private String generateSummary(Long conversationId) {
        List<com.agente.agente_ia_local.model.ConversationMessage> messages =
                conversationService.getRecentMessages(conversationId, 20);
        StringBuilder sb = new StringBuilder();
        sb.append("Resumen de conversación:\n");
        for (com.agente.agente_ia_local.model.ConversationMessage msg : messages) {
            sb.append(msg.getRole()).append(": ").append(truncate(msg.getContent(), 200)).append("\n");
        }
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
