package com.apporte.core.model;

import java.util.Map;

public class RecipientResolution {
    private String userId;
    private String email;
    private String phone;
    private String name;
    private String recipientType; // project_owner, evaluator, admin, workflow_participant, etc.
    private Map<String, Object> metadata;
    
    // Getters e Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Helper methods
    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }
    
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("Recipient[%s, %s, %s]", name, email, recipientType);
    }
}