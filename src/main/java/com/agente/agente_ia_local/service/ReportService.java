package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.model.TenantCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final TenantService tenantService;
    private final AnalyticsService analyticsService;
    private final ConversationService conversationService;
    private final HandoffService handoffService;
    private final WhatsAppService whatsAppService;

    public ReportService(TenantService tenantService, AnalyticsService analyticsService,
                         ConversationService conversationService, HandoffService handoffService,
                         WhatsAppService whatsAppService) {
        this.tenantService = tenantService;
        this.analyticsService = analyticsService;
        this.conversationService = conversationService;
        this.handoffService = handoffService;
        this.whatsAppService = whatsAppService;
    }

    @Scheduled(cron = "${report.cron:0 0 8 * * MON}", zone = "${report.timezone:America/Argentina/Buenos_Aires}")
    public void sendWeeklyReports() {
        log.info("Iniciando envío de reportes semanales...");
        List<Tenant> tenants = tenantService.findAll();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusWeeks(1);

        for (Tenant tenant : tenants) {
            try {
                sendReport(tenant, start, end);
            } catch (Exception e) {
                log.error("Error enviando reporte para tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
        log.info("Reportes semanales completados para {} tenants", tenants.size());
    }

    public void sendReport(Tenant tenant, LocalDateTime start, LocalDateTime end) {
        AnalyticsService.AnalyticsSummary summary = analyticsService.getSummary(tenant.getId(), start, end);
        long pendingHandoffs = handoffService.countPending(tenant.getId());

        String report = formatReport(tenant, summary, pendingHandoffs, start, end);

        tenantService.getCredentials(tenant.getId()).ifPresent(creds -> {
            if (creds.getWhatsappApiKey() != null && creds.getWhatsappPhoneNumberId() != null) {
                whatsAppService.sendTextMessage(creds, tenant.getBusinessName(), report);
            }
        });
        log.info("Reporte enviado para tenant {} ({})", tenant.getId(), tenant.getName());
    }

    private String formatReport(Tenant tenant, AnalyticsService.AnalyticsSummary summary,
                                long pendingHandoffs, LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("📊 *Reporte Semanal - ").append(tenant.getName()).append("*\n");
        sb.append("📅 ").append(start.format(fmt)).append(" al ").append(end.format(fmt)).append("\n\n");
        sb.append("💬 Mensajes totales: ").append(summary.totalMessages()).append("\n");
        sb.append("🔄 Fallbacks activados: ").append(summary.fallbackCount()).append("\n");
        sb.append("👤 Handoffs pendientes: ").append(pendingHandoffs).append("\n");
        sb.append("🟢 Conversaciones activas: ").append(summary.activeConversations()).append("\n\n");

        if (!summary.eventBreakdown().isEmpty()) {
            sb.append("*Desglose por tipo:*\n");
            summary.eventBreakdown().forEach((type, count) ->
                sb.append("• ").append(type).append(": ").append(count).append("\n")
            );
        }

        if (summary.fallbackCount() > 0) {
            double fallbackRate = (double) summary.fallbackCount() / Math.max(summary.totalMessages(), 1) * 100;
            sb.append("\n⚠️ Tasa de fallback: ").append(String.format("%.1f%%", fallbackRate)).append("\n");
            if (fallbackRate > 20) {
                sb.append("💡 Recomendación: Considera agregar más respuestas a tu base de conocimiento.\n");
            }
        }

        return sb.toString();
    }
}
