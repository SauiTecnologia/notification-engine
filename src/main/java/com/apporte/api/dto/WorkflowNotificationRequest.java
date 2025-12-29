package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowNotificationRequest {
    
    @NotNull(message = "Event type cannot be null")
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    @NotNull(message = "Entity type cannot be null")
    @NotBlank(message = "Entity type is required")
    private String entityType;
    
    @NotNull(message = "Entity ID cannot be null")
    @NotBlank(message = "Entity ID is required")
    private String entityId;
    
    @NotNull(message = "Channels cannot be null")
    @NotEmpty(message = "At least one channel is required")
    private List<String> channels;
    
    @NotNull(message = "Recipients cannot be null")
    @NotEmpty(message = "At least one recipient type is required")
    private List<String> recipients;
    
    private Map<String, Object> context;
    
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
}