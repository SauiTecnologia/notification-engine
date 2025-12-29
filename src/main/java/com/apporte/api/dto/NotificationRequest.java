package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL) // Não inclui campos nulos no JSON
public class NotificationRequest {
    
    @NotNull(message = "Event type cannot be null")
    @NotBlank(message = "Event type is required and cannot be empty")
    @Size(min = 1, max = 100, message = "Event type must be between 1 and 100 characters")
    private String eventType;
    
    @NotNull(message = "Entity type cannot be null")
    @NotBlank(message = "Entity type is required and cannot be empty")
    @Size(min = 1, max = 50, message = "Entity type must be between 1 and 50 characters")
    private String entityType;
    
    @NotNull(message = "Entity ID cannot be null")
    @NotBlank(message = "Entity ID is required and cannot be empty")
    @Size(min = 1, max = 100, message = "Entity ID must be between 1 and 100 characters")
    private String entityId;
    
    @NotNull(message = "Channels cannot be null")
    @NotEmpty(message = "At least one channel is required")
    @Size(min = 1, max = 5, message = "You must specify between 1 and 5 channels")
    private List<@NotBlank(message = "Channel cannot be blank") 
                 @Size(min = 1, max = 20, message = "Channel name must be between 1 and 20 characters") 
                 String> channels;
    
    @NotNull(message = "Recipients cannot be null")
    @NotEmpty(message = "At least one recipient type is required")
    @Size(min = 1, max = 10, message = "You must specify between 1 and 10 recipient types")
    private List<@NotBlank(message = "Recipient type cannot be blank") 
                 @Size(min = 1, max = 50, message = "Recipient type must be between 1 and 50 characters") 
                 String> recipients;
                 
    private Map<@NotBlank(message = "Context key cannot be blank") String, Object> context;
    
    // Construtores
    public NotificationRequest() {}
    
    public NotificationRequest(String eventType, String entityType, String entityId, 
                               List<String> channels, List<String> recipients) {
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.channels = channels;
        this.recipients = recipients;
    }
    
    // Getters e Setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }
    
    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    // Método auxiliar para validação adicional
    public boolean hasValidChannels() {
        if (channels == null) return false;
        
        // Lista de canais suportados
        List<String> supportedChannels = List.of("email", "whatsapp", "in_app", "sms", "push");
        
        return channels.stream()
                .allMatch(channel -> supportedChannels.contains(channel.toLowerCase()));
    }
}