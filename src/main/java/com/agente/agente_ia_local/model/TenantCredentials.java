package com.agente.agente_ia_local.model;

import com.agente.agente_ia_local.encryption.EncryptionUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_credentials")
public class TenantCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", unique = true, nullable = false)
    private Long tenantId;

    @Convert(converter = EncryptionUtil.class)
    @Column(name = "whatsapp_api_key", columnDefinition = "TEXT")
    private String whatsappApiKey;

    @Column(name = "whatsapp_phone_number_id")
    private String whatsappPhoneNumberId;

    @Convert(converter = EncryptionUtil.class)
    @Column(name = "whisper_api_key", columnDefinition = "TEXT")
    private String whisperApiKey;

    @Convert(converter = EncryptionUtil.class)
    @Column(name = "openai_api_key", columnDefinition = "TEXT")
    private String openaiApiKey;

    @Column(name = "whatsapp_verify_token")
    private String whatsappVerifyToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TenantCredentials() {}

    public TenantCredentials(Long tenantId) {
        this.tenantId = tenantId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getWhatsappApiKey() { return whatsappApiKey; }
    public void setWhatsappApiKey(String whatsappApiKey) { this.whatsappApiKey = whatsappApiKey; }
    public String getWhatsappPhoneNumberId() { return whatsappPhoneNumberId; }
    public void setWhatsappPhoneNumberId(String whatsappPhoneNumberId) { this.whatsappPhoneNumberId = whatsappPhoneNumberId; }
    public String getWhisperApiKey() { return whisperApiKey; }
    public void setWhisperApiKey(String whisperApiKey) { this.whisperApiKey = whisperApiKey; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
    public String getWhatsappVerifyToken() { return whatsappVerifyToken; }
    public void setWhatsappVerifyToken(String whatsappVerifyToken) { this.whatsappVerifyToken = whatsappVerifyToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
