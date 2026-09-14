package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByTenantIdAndCreatedAtBetween(Long tenantId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
