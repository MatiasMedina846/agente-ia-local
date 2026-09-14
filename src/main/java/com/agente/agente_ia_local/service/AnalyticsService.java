package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.AnalyticsEvent;
import com.agente.agente_ia_local.repository.AnalyticsEventRepository;
import com.agente.agente_ia_local.repository.ConversationRepository;
import com.agente.agente_ia_local.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsEventRepository eventRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    public AnalyticsService(AnalyticsEventRepository eventRepository, ConversationRepository conversationRepository,
                            ConversationMessageRepository messageRepository) {
        this.eventRepository = eventRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public void trackEvent(Long tenantId, Long conversationId, String eventType, String eventData) {
        AnalyticsEvent event = new AnalyticsEvent(tenantId, eventType);
        event.setConversationId(conversationId);
        event.setEventData(eventData);
        eventRepository.save(event);
    }

    public AnalyticsSummary getSummary(Long tenantId, LocalDateTime start, LocalDateTime end) {
        long totalMessages = eventRepository.countByTenantIdAndCreatedAtBetween(tenantId, start, end);
        long fallbackCount = eventRepository.countByTenantIdAndEventTypeAndCreatedAtBetween(tenantId, "FALLBACK_TRIGGERED", start, end);
        long handoffCount = eventRepository.countByTenantIdAndEventTypeAndCreatedAtBetween(tenantId, "HANDOFF_REQUESTED", start, end);
        long activeConversations = conversationRepository.countByTenantIdAndStatus(tenantId, com.agente.agente_ia_local.model.Conversation.Status.ACTIVE);

        List<Object[]> eventCounts = eventRepository.countByEventTypeBetween(tenantId, start, end);
        Map<String, Long> eventBreakdown = new HashMap<>();
        for (Object[] row : eventCounts) {
            eventBreakdown.put((String) row[0], (Long) row[1]);
        }

        return new AnalyticsSummary(totalMessages, fallbackCount, handoffCount, activeConversations, eventBreakdown);
    }

    public record AnalyticsSummary(long totalMessages, long fallbackCount, long handoffCount,
                                   long activeConversations, Map<String, Long> eventBreakdown) {}
}
