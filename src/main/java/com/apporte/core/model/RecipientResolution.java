package com.apporte.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Representa um destinatário resolvido para receber uma notificação.
 * Contém todas as informações necessárias para enviar mensagens via diferentes canais.
 */
public class RecipientResolution {
    
    private String userId;
    private String email;
    private String phone;
    private String name;
    private String recipientType; // project_owner, evaluator, admin, workflow_participant, etc.
    private Map<String, Object> metadata;
    
    // Constructor
    public RecipientResolution() {}
    
    public RecipientResolution(String userId, String email, String name, String recipientType) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.name = name;
        this.recipientType = Objects.requireNonNull(recipientType, "recipientType cannot be null");
    }
    
    // Getters
    public String getUserId() { 
        return userId; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public String getPhone() { 
        return phone; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public String getRecipientType() { 
        return recipientType; 
    }
    
    public Map<String, Object> getMetadata() { 
        return metadata; 
    }
    
    // Setters with validation
    public void setUserId(String userId) { 
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
    }
    
    public void setEmail(String email) { 
        this.email = Objects.requireNonNull(email, "email cannot be null");
    }
    
    public void setPhone(String phone) { 
        this.phone = phone;
    }
    
    public void setName(String name) { 
        this.name = name;
    }
    
    public void setRecipientType(String recipientType) { 
        this.recipientType = Objects.requireNonNull(recipientType, "recipientType cannot be null");
    }
    
    public void setMetadata(Map<String, Object> metadata) { 
        this.metadata = metadata;
    }
    
    // Helper methods
    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }
    
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }
    
    public boolean isValid() {
        return userId != null && email != null && recipientType != null;
    }
    
    @Override
    public String toString() {
        return String.format("Recipient[userId=%s, name=%s, email=%s, recipientType=%s]", 
                            userId, name, email, recipientType);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipientResolution that)) return false;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(email, that.email) &&
               Objects.equals(recipientType, that.recipientType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, email, recipientType);
    }
}