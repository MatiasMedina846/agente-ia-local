package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop10ByOrderByCreatedAtDesc();
}
