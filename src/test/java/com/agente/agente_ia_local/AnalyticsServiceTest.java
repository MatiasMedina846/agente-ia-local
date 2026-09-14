package com.agente.agente_ia_local;

import com.agente.agente_ia_local.model.AnalyticsEvent;
import com.agente.agente_ia_local.model.Conversation;
import com.agente.agente_ia_local.model.HumanHandoff;
import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.repository.*;
import com.agente.agente_ia_local.service.AnalyticsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnalyticsServiceTest {

    @Autowired private AnalyticsService analyticsService;
    @Autowired private AnalyticsEventRepository eventRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ConversationRepository conversationRepository;

    private Tenant tenant;

    @BeforeEach
    void setup() {
        eventRepository.deleteAll();
        conversationRepository.deleteAll();
        tenantRepository.deleteAll();
        tenant = tenantRepository.save(new Tenant("Analytics Test", "analytics-test"));
    }

    @Test
    @Order(1)
    @DisplayName("Registrar evento de analytics")
    void trackEvent() {
        analyticsService.trackEvent(tenant.getId(), null, "MESSAGE_RECEIVED", "{\"test\":true}");
        analyticsService.trackEvent(tenant.getId(), null, "FALLBACK_TRIGGERED", "{}");

        long count = eventRepository.countByTenantIdAndEventTypeAndCreatedAtBetween(
                tenant.getId(), "MESSAGE_RECEIVED",
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(1));
        assertEquals(1, count);
    }

    @Test
    @Order(2)
    @DisplayName("Dashboard retorna métricas correctas")
    void dashboardSummary() {
        analyticsService.trackEvent(tenant.getId(), null, "MESSAGE_RECEIVED", "{}");
        analyticsService.trackEvent(tenant.getId(), null, "MESSAGE_RECEIVED", "{}");
        analyticsService.trackEvent(tenant.getId(), null, "FALLBACK_TRIGGERED", "{}");

        AnalyticsService.AnalyticsSummary summary = analyticsService.getSummary(
                tenant.getId(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1));

        assertEquals(3, summary.totalMessages());
        assertEquals(1, summary.fallbackCount());
    }

    @Test
    @Order(3)
    @DisplayName("Eventos de diferentes tenants no se mezclan")
    void tenantIsolation() {
        Tenant other = tenantRepository.save(new Tenant("Other", "other-analytics"));
        analyticsService.trackEvent(tenant.getId(), null, "MESSAGE_RECEIVED", "{}");
        analyticsService.trackEvent(other.getId(), null, "MESSAGE_RECEIVED", "{}");

        AnalyticsService.AnalyticsSummary summary = analyticsService.getSummary(
                tenant.getId(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1));

        assertEquals(1, summary.totalMessages(), "Solo debe contar eventos del tenant actual");
    }

    @Test
    @Order(4)
    @DisplayName("Dashboard con rango sin eventos retorna ceros")
    void emptyDashboard() {
        AnalyticsService.AnalyticsSummary summary = analyticsService.getSummary(
                tenant.getId(),
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(29));

        assertEquals(0, summary.totalMessages());
        assertEquals(0, summary.fallbackCount());
        assertEquals(0, summary.handoffCount());
    }
}
