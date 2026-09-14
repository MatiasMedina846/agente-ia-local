package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    long countByTenantIdAndEventTypeAndCreatedAtBetween(Long tenantId, String eventType, LocalDateTime start, LocalDateTime end);

    long countByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT ae.eventType, COUNT(ae) FROM AnalyticsEvent ae WHERE ae.tenantId = :tenantId AND ae.createdAt BETWEEN :start AND :end GROUP BY ae.eventType")
    List<Object[]> countByEventTypeBetween(@Param("tenantId") Long tenantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
