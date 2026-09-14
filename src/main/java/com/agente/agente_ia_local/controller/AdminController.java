package com.agente.agente_ia_local.controller;

import com.agente.agente_ia_local.config.TenantContext;
import com.agente.agente_ia_local.model.AnalyticsEvent;
import com.agente.agente_ia_local.model.Conversation;
import com.agente.agente_ia_local.model.HumanHandoff;
import com.agente.agente_ia_local.service.AnalyticsService;
import com.agente.agente_ia_local.service.ConversationService;
import com.agente.agente_ia_local.service.HandoffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final ConversationService conversationService;
    private final HandoffService handoffService;

    public AdminController(AnalyticsService analyticsService, ConversationService conversationService,
                           HandoffService handoffService) {
        this.analyticsService = analyticsService;
        this.conversationService = conversationService;
        this.handoffService = handoffService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsService.AnalyticsSummary> dashboard(
            @RequestParam(defaultValue = "7") int days) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        return ResponseEntity.ok(analyticsService.getSummary(tenantId, start, end));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> listConversations() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(conversationService.getActiveHandoffs(tenantId));
    }

    @GetMapping("/handoffs")
    public ResponseEntity<List<HumanHandoff>> listPendingHandoffs() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(handoffService.getPendingHandoffs(tenantId));
    }

    @PostMapping("/handoffs/{id}/resolve")
    public ResponseEntity<Map<String, String>> resolveHandoff(@PathVariable Long id) {
        handoffService.resolveHandoff(id);
        return ResponseEntity.ok(Map.of("status", "resolved"));
    }
}
