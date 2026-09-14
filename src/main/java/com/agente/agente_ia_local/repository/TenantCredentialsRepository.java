package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.TenantCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantCredentialsRepository extends JpaRepository<TenantCredentials, Long> {

    Optional<TenantCredentials> findByTenantId(Long tenantId);

    Optional<TenantCredentials> findByWhatsappPhoneNumberId(String phoneNumberId);
}
