package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByTenantIdAndUserPhoneAndStatus(Long tenantId, String userPhone, Conversation.Status status);

    List<Conversation> findByTenantIdOrderByLastMessageAtDesc(Long tenantId);

    List<Conversation> findByTenantIdAndHandoffActiveTrue(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, Conversation.Status status);
}
