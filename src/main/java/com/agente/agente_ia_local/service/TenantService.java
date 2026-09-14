package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.model.TenantCredentials;
import com.agente.agente_ia_local.repository.TenantCredentialsRepository;
import com.agente.agente_ia_local.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantCredentialsRepository credentialsRepository;

    public TenantService(TenantRepository tenantRepository, TenantCredentialsRepository credentialsRepository) {
        this.tenantRepository = tenantRepository;
        this.credentialsRepository = credentialsRepository;
    }

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> findById(Long id) {
        return tenantRepository.findById(id);
    }

    public Optional<Tenant> findBySlug(String slug) {
        return tenantRepository.findBySlug(slug);
    }

    @Transactional
    public Tenant create(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant update(Long id, Tenant updated) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setName(updated.getName());
        tenant.setBusinessName(updated.getBusinessName());
        tenant.setLogoUrl(updated.getLogoUrl());
        tenant.setPrimaryColor(updated.getPrimaryColor());
        tenant.setSecondaryColor(updated.getSecondaryColor());
        tenant.setToneOfVoice(updated.getToneOfVoice());
        tenant.setBusinessHours(updated.getBusinessHours());
        tenant.setSystemPrompt(updated.getSystemPrompt());
        tenant.setWelcomeMessage(updated.getWelcomeMessage());
        tenant.setOfflineMessage(updated.getOfflineMessage());
        tenant.setActive(updated.isActive());
        return tenantRepository.save(tenant);
    }

    public Optional<TenantCredentials> getCredentials(Long tenantId) {
        return credentialsRepository.findByTenantId(tenantId);
    }

    public Optional<TenantCredentials> getCredentialsByPhoneNumberId(String phoneNumberId) {
        return credentialsRepository.findByWhatsappPhoneNumberId(phoneNumberId);
    }

    @Transactional
    public TenantCredentials saveCredentials(TenantCredentials credentials) {
        Optional<TenantCredentials> existing = credentialsRepository.findByTenantId(credentials.getTenantId());
        if (existing.isPresent()) {
            TenantCredentials creds = existing.get();
            creds.setWhatsappApiKey(credentials.getWhatsappApiKey());
            creds.setWhatsappPhoneNumberId(credentials.getWhatsappPhoneNumberId());
            creds.setWhisperApiKey(credentials.getWhisperApiKey());
            creds.setOpenaiApiKey(credentials.getOpenaiApiKey());
            creds.setWhatsappVerifyToken(credentials.getWhatsappVerifyToken());
            return credentialsRepository.save(creds);
        }
        return credentialsRepository.save(credentials);
    }
}
