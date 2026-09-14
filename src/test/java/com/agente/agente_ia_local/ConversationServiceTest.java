package com.agente.agente_ia_local;

import com.agente.agente_ia_local.model.*;
import com.agente.agente_ia_local.repository.*;
import com.agente.agente_ia_local.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ConversationServiceTest {

    @Autowired private ConversationService conversationService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMessageRepository messageRepository;

    private Tenant tenant;

    @BeforeEach
    void setup() {
        tenant = tenantRepository.save(new Tenant("Conv Test", "conv-test"));
    }

    @AfterEach
    void cleanup() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    @DisplayName("Crear conversación activa nueva")
    void createNewConversation() {
        Conversation conv = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);

        assertNotNull(conv.getId());
        assertEquals(Conversation.Status.ACTIVE, conv.getStatus());
        assertEquals("+5491112345678", conv.getUserPhone());
    }

    @Test
    @DisplayName("findOrCreateActive reutiliza conversación existente")
    void reuseExistingConversation() {
        Conversation first = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);
        Conversation second = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);

        assertEquals(first.getId(), second.getId(), "Debe reutilizar la misma conversación");
    }

    @Test
    @DisplayName("Guardar mensaje actualiza lastMessageAt")
    void saveMessageUpdatesLastMessageAt() {
        Conversation conv = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);

        conversationService.saveMessage(conv.getId(), tenant.getId(),
                ConversationMessage.Role.USER, "Hola", ConversationMessage.MessageType.TEXT);

        Conversation updated = conversationRepository.findById(conv.getId()).orElseThrow();
        assertNotNull(updated.getLastMessageAt());
    }

    @Test
    @DisplayName("Obtener mensajes recientes en orden cronológico")
    void getRecentMessagesChronological() {
        Conversation conv = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);

        conversationService.saveMessage(conv.getId(), tenant.getId(),
                ConversationMessage.Role.USER, "Msg 1", ConversationMessage.MessageType.TEXT);
        conversationService.saveMessage(conv.getId(), tenant.getId(),
                ConversationMessage.Role.BOT, "Resp 1", ConversationMessage.MessageType.TEXT);
        conversationService.saveMessage(conv.getId(), tenant.getId(),
                ConversationMessage.Role.USER, "Msg 2", ConversationMessage.MessageType.TEXT);

        var messages = conversationService.getRecentMessages(conv.getId(), 10);
        assertEquals(3, messages.size());
        assertEquals("Msg 1", messages.get(0).getContent());
        assertEquals("Resp 1", messages.get(1).getContent());
        assertEquals("Msg 2", messages.get(2).getContent());
    }

    @Test
    @DisplayName("Activar bot en conversación")
    void activateBot() {
        Conversation conv = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);
        conversationService.deactivateBot(conv.getId());
        conversationService.activateBot(conv.getId());

        Conversation updated = conversationRepository.findById(conv.getId()).orElseThrow();
        assertEquals(Conversation.Status.BOT, updated.getStatus());
        assertFalse(updated.isHandoffActive());
    }

    @Test
    @DisplayName("Desactivar bot (handoff)")
    void deactivateBot() {
        Conversation conv = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);
        conversationService.deactivateBot(conv.getId());

        Conversation updated = conversationRepository.findById(conv.getId()).orElseThrow();
        assertEquals(Conversation.Status.HUMAN, updated.getStatus());
        assertTrue(updated.isHandoffActive());
    }

    @Test
    @DisplayName("Cerrar conversación")
    void closeConversation() {
        Conversation conv = conversationService.findOrCreateActive(
                tenant.getId(), "+5491112345678", Conversation.Channel.WHATSAPP);
        conversationService.closeConversation(conv.getId());

        Conversation updated = conversationRepository.findById(conv.getId()).orElseThrow();
        assertEquals(Conversation.Status.CLOSED, updated.getStatus());
    }
}
