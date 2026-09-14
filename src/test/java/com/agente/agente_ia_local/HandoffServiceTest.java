package com.agente.agente_ia_local;

import com.agente.agente_ia_local.model.*;
import com.agente.agente_ia_local.repository.*;
import com.agente.agente_ia_local.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HandoffServiceTest {

    @Autowired private HandoffService handoffService;
    @Autowired private ConversationService conversationService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMessageRepository messageRepository;
    @Autowired private HumanHandoffRepository handoffRepository;

    private Tenant tenant;
    private Conversation conversation;

    @BeforeEach
    void setup() {
        tenant = tenantRepository.save(new Tenant("Handoff Test", "handoff-test"));
        conversation = conversationService.findOrCreateActive(
                tenant.getId(), "+5491177777777", Conversation.Channel.WHATSAPP);
        conversationService.saveMessage(conversation.getId(), tenant.getId(),
                ConversationMessage.Role.USER, "Tengo un problema con mi pedido", ConversationMessage.MessageType.TEXT);
        conversationService.saveMessage(conversation.getId(), tenant.getId(),
                ConversationMessage.Role.BOT, "Entiendo, ¿podrías darme más detalles?", ConversationMessage.MessageType.TEXT);
    }

    @AfterEach
    void cleanup() {
        handoffRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    @DisplayName("Solicitar handoff crea registro y pausa bot")
    void requestHandoff() {
        HumanHandoff handoff = handoffService.requestHandoff(
                conversation.getId(), tenant.getId(), "Frustración detectada");

        assertNotNull(handoff.getId());
        assertEquals("PENDING", handoff.getStatus());
        assertNotNull(handoff.getSummary(), "El resumen debe generado automáticamente");
        assertTrue(handoff.getSummary().contains("Tengo un problema"));

        Conversation conv = conversationRepository.findById(conversation.getId()).orElseThrow();
        assertTrue(conv.isHandoffActive());
        assertEquals(Conversation.Status.HUMAN, conv.getStatus());
    }

    @Test
    @DisplayName("Resolver handoff reactiva el bot")
    void resolveHandoff() {
        HumanHandoff handoff = handoffService.requestHandoff(
                conversation.getId(), tenant.getId(), "Test");
        handoffService.resolveHandoff(handoff.getId());

        Conversation conv = conversationRepository.findById(conversation.getId()).orElseThrow();
        assertFalse(conv.isHandoffActive());
        assertEquals(Conversation.Status.BOT, conv.getStatus());

        HumanHandoff resolved = handoffRepository.findById(handoff.getId()).orElseThrow();
        assertEquals("RESOLVED", resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
    }

    @Test
    @DisplayName("Contar handoffs pendientes")
    void countPending() {
        handoffService.requestHandoff(conversation.getId(), tenant.getId(), "Reason 1");
        long count = handoffService.countPending(tenant.getId());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Listar handoffs pendientes")
    void listPending() {
        handoffService.requestHandoff(conversation.getId(), tenant.getId(), "List test");
        var pending = handoffService.getPendingHandoffs(tenant.getId());
        assertFalse(pending.isEmpty());
        assertEquals("List test", pending.get(0).getReason());
    }
}
