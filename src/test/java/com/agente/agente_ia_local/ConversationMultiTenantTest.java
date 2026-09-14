package com.agente.agente_ia_local;

import com.agente.agente_ia_local.model.Conversation;
import com.agente.agente_ia_local.model.ConversationMessage;
import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.repository.ConversationMessageRepository;
import com.agente.agente_ia_local.repository.ConversationRepository;
import com.agente.agente_ia_local.repository.TenantRepository;
import com.agente.agente_ia_local.service.ConversationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConversationMultiTenantTest {

    @Autowired private ConversationService conversationService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMessageRepository messageRepository;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setup() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        tenantRepository.deleteAll();
        tenantA = tenantRepository.save(new Tenant("Business A", "biz-a"));
        tenantB = tenantRepository.save(new Tenant("Business B", "biz-b"));
    }

    @Test
    @Order(1)
    @DisplayName("Cada tenant tiene sus propias conversaciones")
    void isolatedConversations() {
        conversationService.findOrCreateActive(tenantA.getId(), "+549111111", Conversation.Channel.WHATSAPP);
        conversationService.findOrCreateActive(tenantA.getId(), "+549112222", Conversation.Channel.WHATSAPP);
        conversationService.findOrCreateActive(tenantB.getId(), "+549113333", Conversation.Channel.WHATSAPP);

        long countA = conversationService.countActiveConversations(tenantA.getId());
        long countB = conversationService.countActiveConversations(tenantB.getId());

        assertEquals(2, countA);
        assertEquals(1, countB);
    }

    @Test
    @Order(2)
    @DisplayName("Mensajes de tenant A no aparecen en tenant B")
    void messageIsolation() {
        Conversation convA = conversationService.findOrCreateActive(
                tenantA.getId(), "+549111111", Conversation.Channel.WHATSAPP);
        Conversation convB = conversationService.findOrCreateActive(
                tenantB.getId(), "+549112222", Conversation.Channel.WHATSAPP);

        conversationService.saveMessage(convA.getId(), tenantA.getId(),
                ConversationMessage.Role.USER, "Mensaje privado de A", ConversationMessage.MessageType.TEXT);
        conversationService.saveMessage(convB.getId(), tenantB.getId(),
                ConversationMessage.Role.USER, "Mensaje privado de B", ConversationMessage.MessageType.TEXT);

        var messagesA = conversationService.getRecentMessages(convA.getId(), 10);
        var messagesB = conversationService.getRecentMessages(convB.getId(), 10);

        assertEquals(1, messagesA.size());
        assertEquals("Mensaje privado de A", messagesA.get(0).getContent());
        assertEquals(1, messagesB.size());
        assertEquals("Mensaje privado de B", messagesB.get(0).getContent());
    }

    @Test
    @Order(3)
    @DisplayName("El mismo teléfono puede tener conversación con diferentes tenants")
    void samePhoneDifferentTenants() {
        String phone = "+5491199999999";
        Conversation convA = conversationService.findOrCreateActive(tenantA.getId(), phone, Conversation.Channel.WHATSAPP);
        Conversation convB = conversationService.findOrCreateActive(tenantB.getId(), phone, Conversation.Channel.WHATSAPP);

        assertNotEquals(convA.getId(), convB.getId(), "Conversaciones deben ser diferentes");
        assertEquals(phone, convA.getUserPhone());
        assertEquals(phone, convB.getUserPhone());
    }

    @Test
    @Order(4)
    @DisplayName("Cerrar conversación de un tenant no afecta al otro")
    void closeIsolation() {
        Conversation convA = conversationService.findOrCreateActive(
                tenantA.getId(), "+549111111", Conversation.Channel.WHATSAPP);
        Conversation convB = conversationService.findOrCreateActive(
                tenantB.getId(), "+549112222", Conversation.Channel.WHATSAPP);

        conversationService.closeConversation(convA.getId());

        assertEquals(Conversation.Status.CLOSED,
                conversationRepository.findById(convA.getId()).orElseThrow().getStatus());
        assertEquals(Conversation.Status.ACTIVE,
                conversationRepository.findById(convB.getId()).orElseThrow().getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("Handoff activo en un tenant no afecta al otro")
    void handoffIsolation() {
        Conversation convA = conversationService.findOrCreateActive(
                tenantA.getId(), "+549111111", Conversation.Channel.WHATSAPP);
        Conversation convB = conversationService.findOrCreateActive(
                tenantB.getId(), "+549112222", Conversation.Channel.WHATSAPP);

        conversationService.deactivateBot(convA.getId());

        assertTrue(conversationRepository.findById(convA.getId()).orElseThrow().isHandoffActive());
        assertFalse(conversationRepository.findById(convB.getId()).orElseThrow().isHandoffActive());
    }
}
