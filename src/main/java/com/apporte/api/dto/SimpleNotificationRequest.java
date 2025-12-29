package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimpleNotificationRequest {
    
    @NotNull(message = "Event type cannot be null")
    @NotBlank(message = "Event type is required and cannot be empty")
    private String eventType;
    
    @NotNull(message = "Channel cannot be null")
    @NotBlank(message = "Channel is required and cannot be empty")
    private String channel;
    
    @NotNull(message = "Recipient ID cannot be null")
    @NotBlank(message = "Recipient ID is required and cannot be empty")
    private String recipientId;
    
    private Map<String, Object> context;
    
    // Getters e Setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}