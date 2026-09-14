package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.ChatMessage;
import com.agente.agente_ia_local.model.Conversation;
import com.agente.agente_ia_local.model.ConversationMessage;
import com.agente.agente_ia_local.model.Product;
import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.model.TenantCredentials;
import com.agente.agente_ia_local.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessagePipelineService {

    private static final Logger log = LoggerFactory.getLogger(MessagePipelineService.class);

    private final TenantService tenantService;
    private final ConversationService conversationService;
    private final AudioService audioService;
    private final ImageService imageService;
    private final HybridSearchService hybridSearchService;
    private final GuardrailsService guardrailsService;
    private final TypingIndicatorService typingService;
    private final HandoffService handoffService;
    private final AnalyticsService analyticsService;
    private final MessageDebounceService debounceService;
    private final WhatsAppService whatsAppService;
    private final AiService aiService;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    public MessagePipelineService(TenantService tenantService, ConversationService conversationService,
                                  AudioService audioService, ImageService imageService,
                                  HybridSearchService hybridSearchService, GuardrailsService guardrailsService,
                                  TypingIndicatorService typingService, HandoffService handoffService,
                                  AnalyticsService analyticsService, MessageDebounceService debounceService,
                                  WhatsAppService whatsAppService, AiService aiService,
                                  ChatMessageRepository chatMessageRepository) {
        this.tenantService = tenantService;
        this.conversationService = conversationService;
        this.audioService = audioService;
        this.imageService = imageService;
        this.hybridSearchService = hybridSearchService;
        this.guardrailsService = guardrailsService;
        this.typingService = typingService;
        this.handoffService = handoffService;
        this.analyticsService = analyticsService;
        this.debounceService = debounceService;
        this.whatsAppService = whatsAppService;
        this.aiService = aiService;
        this.chatMessageRepository = chatMessageRepository;
    }

    public void processIncomingMessage(Long tenantId, String userPhone, String content,
                                       ConversationMessage.MessageType messageType) {
        Tenant tenant = tenantService.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));

        if (!tenant.isActive()) {
            log.warn("Tenant {} inactivo, ignorando mensaje", tenantId);
            return;
        }

        debounceService.addMessage(String.valueOf(tenantId), userPhone, content, (phone, messages) -> {
            String fusedMessage = String.join(" ", messages);
            processFusedMessage(tenant, phone, fusedMessage, messageType);
        });
    }

    @Transactional
    public void processFusedMessage(Tenant tenant, String userPhone, String content,
                                    ConversationMessage.MessageType messageType) {
        Long tenantId = tenant.getId();
        Conversation conversation = conversationService.findOrCreateActive(
                tenantId, userPhone, Conversation.Channel.WHATSAPP);

        if (conversation.isHandoffActive()) {
            conversationService.saveMessage(conversation.getId(), tenantId,
                    ConversationMessage.Role.USER, content, messageType);
            log.debug("Conversación {} en handoff, bot no responde", conversation.getId());
            return;
        }

        conversationService.saveMessage(conversation.getId(), tenantId,
                ConversationMessage.Role.USER, content, messageType);

        String processedContent = content;
        ConversationMessage.MessageType processedType = messageType;

        if (messageType == ConversationMessage.MessageType.AUDIO) {
            TenantCredentials creds = tenantService.getCredentials(tenantId).orElse(null);
            if (creds != null && creds.getWhisperApiKey() != null) {
                String transcribed = audioService.transcribe(content, creds.getWhisperApiKey());
                if (transcribed != null && !transcribed.isBlank()) {
                    processedContent = transcribed;
                    processedType = ConversationMessage.MessageType.TEXT;
                }
            }
        } else if (messageType == ConversationMessage.MessageType.IMAGE) {
            ImageService.ImageClassification classification = imageService.classifyImage(content);
            if ("COMPROBANTE_PAGO".equals(classification.category())) {
                String receiptData = imageService.extractReceiptData(content);
                processedContent = "[Comprobante de pago detectado] " + (receiptData != null ? receiptData : "No se pudo extraer datos");
            } else {
                processedContent = "[Imagen enviada: " + classification.category() + "] " + classification.description();
            }
        }

        GuardrailsService.GuardrailsResult guardrails = guardrailsService.evaluate(
                processedContent, tenantId, conversationService.getRecentMessages(conversation.getId(), 10));

        if (guardrails.requiresAction()) {
            handleGuardrailsAction(conversation, tenant, userPhone, guardrails);
            return;
        }

        List<Product> searchResults = hybridSearchService.search(tenantId, processedContent);
        String productContext = hybridSearchService.formatProductContext(searchResults);

        List<ChatMessage> history = chatMessageRepository.findTop10ByOrderByCreatedAtDesc();
        String systemPrompt = tenant.getSystemPrompt() != null ? tenant.getSystemPrompt() : "";
        String enrichedPrompt = buildEnrichedPrompt(processedContent, productContext, systemPrompt, history);

        String aiResponse = aiService.generateResponse(enrichedPrompt, history);

        if (guardrails.warnings().stream().anyMatch(w -> w.startsWith("PRICE_NOT_VERIFIED"))) {
            aiResponse += "\n\n⚠️ Te recomiendo confirmar el precio directamente con el vendedor.";
        }

        conversationService.saveMessage(conversation.getId(), tenantId,
                ConversationMessage.Role.BOT, aiResponse, ConversationMessage.MessageType.TEXT);

        analyticsService.trackEvent(tenantId, conversation.getId(), "MESSAGE_PROCESSED",
                "{\"type\":\"" + processedType + "\",\"warnings\":" + guardrails.warnings().size() + "}");

        TenantCredentials creds = tenantService.getCredentials(tenantId).orElse(null);
        if (creds != null && creds.getWhatsappApiKey() != null && creds.getWhatsappPhoneNumberId() != null) {
            whatsAppService.sendTextMessage(creds, userPhone, aiResponse);
        }
    }

    private void handleGuardrailsAction(Conversation conversation, Tenant tenant, String userPhone,
                                        GuardrailsService.GuardrailsResult guardrails) {
        Long tenantId = tenant.getId();

        if ("HUMAN_HANDOFF".equals(guardrails.actionType()) || "FRUSTRATION".equals(guardrails.actionType())) {
            handoffService.requestHandoff(conversation.getId(), tenantId, guardrails.reason());

            analyticsService.trackEvent(tenantId, conversation.getId(),
                    "HANDOFF_REQUESTED", "{\"reason\":\"" + guardrails.reason() + "\"}");

            String handoffMsg = guardrails.actionType().equals("FRUSTRATION")
                    ? "Detecté que estás molesto. Un asesor se comunicará contigo a la brevedad. ¡Lamento la inconveniencia!"
                    : "Voy a conectarte con un asesor humano. En breve te atenderán.";

            TenantCredentials creds = tenantService.getCredentials(tenantId).orElse(null);
            if (creds != null && creds.getWhatsappApiKey() != null) {
                whatsAppService.sendTextMessage(creds, userPhone, handoffMsg);
            }
        }
    }

    private String buildEnrichedPrompt(String userMessage, String productContext, String systemPrompt,
                                        List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            sb.append("[Instrucciones del sistema]").append(System.lineSeparator());
            sb.append(systemPrompt).append(System.lineSeparator()).append(System.lineSeparator());
        }
        if (!productContext.isBlank()) {
            sb.append(productContext).append(System.lineSeparator());
        }
        if (history != null && !history.isEmpty()) {
            sb.append("[Historial de Conversación Anterior]").append(System.lineSeparator());
            List<ChatMessage> chronological = new java.util.ArrayList<>(history);
            java.util.Collections.reverse(chronological);
            for (ChatMessage msg : chronological) {
                sb.append("Usuario: ").append(msg.getPrompt()).append(System.lineSeparator());
                sb.append("Asistente: ").append(msg.getResponse()).append(System.lineSeparator());
            }
        }
        sb.append("[Nueva Pregunta]").append(System.lineSeparator());
        sb.append("Usuario: ").append(userMessage);
        return sb.toString();
    }
}
