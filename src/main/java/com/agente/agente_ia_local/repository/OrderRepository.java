package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByTenantIdAndUserPhoneOrderByCreatedAtDesc(Long tenantId, String userPhone);

    List<Order> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end);

    long countByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end);
}
