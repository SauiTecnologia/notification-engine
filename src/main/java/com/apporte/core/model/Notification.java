package com.apporte.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidade que representa uma notificação enviada ou pendente.
 * Mantém histórico de envios e controla status e erros.
 */
@Entity
@Table(name = "notifications")
public class Notification extends PanacheEntity {
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "channel", nullable = false)
    private String channel;
    
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;
    
    @Column(name = "status", nullable = false)
    private String status; // pending, sent, error, retrying
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "sent_at")
    private Instant sentAt;
    
    // Constructors
    public Notification() {
        this.status = "pending";
    }
    
    public Notification(String userId, String eventType, String channel) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
        this.status = "pending";
        this.createdAt = Instant.now();
    }
    
    // Getters
    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public String getPayloadJson() {
        return payloadJson;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getSentAt() {
        return sentAt;
    }
    
    // Setters
    public void setUserId(String userId) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
    }
    
    public void setEventType(String eventType) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
    }
    
    public void setChannel(String channel) {
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
    }
    
    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
    
    public void setStatus(String status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }
    
    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
    
    public void markAsSent() {
        this.status = "sent";
        this.sentAt = Instant.now();
        this.errorMessage = null;
    }
    
    public void markAsError(String errorMessage) {
        this.status = "error";
        this.errorMessage = errorMessage;
    }
    
    public void markAsRetrying() {
        this.status = "retrying";
        this.errorMessage = null;
    }
    
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
    
    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", channel='" + channel + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", sentAt=" + sentAt +
                '}';
    }
}