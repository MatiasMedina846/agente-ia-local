package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.HumanHandoff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HumanHandoffRepository extends JpaRepository<HumanHandoff, Long> {

    List<HumanHandoff> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status);

    long countByTenantIdAndStatus(Long tenantId, String status);

    List<HumanHandoff> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
