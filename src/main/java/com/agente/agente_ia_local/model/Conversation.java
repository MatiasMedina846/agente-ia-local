package com.agente.agente_ia_local.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    public enum Status { ACTIVE, BOT, HUMAN, CLOSED }
    public enum Channel { WHATSAPP, WEB }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_phone", nullable = false)
    private String userPhone;

    @Column(name = "user_name")
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel = Channel.WHATSAPP;

    @Column(name = "handoff_active", nullable = false)
    private boolean handoffActive = false;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Conversation() {}

    public Conversation(Long tenantId, String userPhone, Channel channel) {
        this.tenantId = tenantId;
        this.userPhone = userPhone;
        this.channel = channel;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public boolean isHandoffActive() { return handoffActive; }
    public void setHandoffActive(boolean handoffActive) { this.handoffActive = handoffActive; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
