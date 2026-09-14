package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.Conversation;
import com.agente.agente_ia_local.model.ConversationMessage;
import com.agente.agente_ia_local.repository.ConversationMessageRepository;
import com.agente.agente_ia_local.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository, ConversationMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Conversation findOrCreateActive(Long tenantId, String userPhone, Conversation.Channel channel) {
        Optional<Conversation> existing = conversationRepository
                .findByTenantIdAndUserPhoneAndStatus(tenantId, userPhone, Conversation.Status.ACTIVE);
        if (existing.isPresent()) {
            return existing.get();
        }
        Conversation conv = new Conversation(tenantId, userPhone, channel);
        return conversationRepository.save(conv);
    }

    @Transactional
    public ConversationMessage saveMessage(Long conversationId, Long tenantId,
                                           ConversationMessage.Role role, String content,
                                           ConversationMessage.MessageType messageType) {
        ConversationMessage msg = new ConversationMessage(conversationId, tenantId, role, content, messageType);
        ConversationMessage saved = messageRepository.save(msg);

        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv != null) {
            conv.setLastMessageAt(LocalDateTime.now());
            conversationRepository.save(conv);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getRecentMessages(Long conversationId, int limit) {
        List<ConversationMessage> recent = messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);
        List<ConversationMessage> chronological = new ArrayList<>(recent);
        Collections.reverse(chronological);
        return chronological.size() > limit
                ? chronological.subList(chronological.size() - limit, chronological.size())
                : chronological;
    }

    @Transactional
    public void activateBot(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv != null) {
            conv.setStatus(Conversation.Status.BOT);
            conv.setHandoffActive(false);
            conversationRepository.save(conv);
        }
    }

    @Transactional
    public void deactivateBot(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv != null) {
            conv.setStatus(Conversation.Status.HUMAN);
            conv.setHandoffActive(true);
            conversationRepository.save(conv);
        }
    }

    @Transactional
    public void closeConversation(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv != null) {
            conv.setStatus(Conversation.Status.CLOSED);
            conversationRepository.save(conv);
        }
    }

    public List<Conversation> getActiveHandoffs(Long tenantId) {
        return conversationRepository.findByTenantIdAndHandoffActiveTrue(tenantId);
    }

    public long countActiveConversations(Long tenantId) {
        return conversationRepository.countByTenantIdAndStatus(tenantId, Conversation.Status.ACTIVE);
    }

    public long countTotalMessages(Long tenantId, LocalDateTime start, LocalDateTime end) {
        return messageRepository.countByTenantIdAndCreatedAtBetween(tenantId, start, end);
    }

    public Conversation findById(Long conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }
}
