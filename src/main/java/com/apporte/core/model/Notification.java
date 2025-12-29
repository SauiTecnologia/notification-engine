package com.apporte.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification extends PanacheEntity {
    
    @Column(name = "user_id", nullable = false)
    public String userId;
    
    @Column(name = "event_type", nullable = false)
    public String eventType;
    
    @Column(name = "channel", nullable = false)
    public String channel;
    
    @Column(name = "payload_json", columnDefinition = "jsonb")
    public String payloadJson;
    
    @Column(name = "status", nullable = false)
    public String status; // pending, sent, error
    
    @Column(name = "error_message")
    public String errorMessage;
    
    @Column(name = "created_at")
    public Instant createdAt;
    
    @Column(name = "sent_at")
    public Instant sentAt;
    
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}