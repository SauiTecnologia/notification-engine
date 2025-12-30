package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Record para requisição de notificação genérica.
 * Imutável por natureza e com equals/hashCode/toString automáticos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationRequest(
    @NotNull(message = "Event type cannot be null")
    @NotBlank(message = "Event type is required and cannot be empty")
    @Size(min = 1, max = 100, message = "Event type must be between 1 and 100 characters")
    String eventType,
    
    @NotNull(message = "Entity type cannot be null")
    @NotBlank(message = "Entity type is required and cannot be empty")
    @Size(min = 1, max = 50, message = "Entity type must be between 1 and 50 characters")
    String entityType,
    
    @NotNull(message = "Entity ID cannot be null")
    @NotBlank(message = "Entity ID is required and cannot be empty")
    @Size(min = 1, max = 100, message = "Entity ID must be between 1 and 100 characters")
    String entityId,
    
    @NotNull(message = "Channels cannot be null")
    @NotEmpty(message = "At least one channel is required")
    @Size(min = 1, max = 5, message = "You must specify between 1 and 5 channels")
    List<@NotBlank(message = "Channel cannot be blank") 
         @Size(min = 1, max = 20, message = "Channel name must be between 1 and 20 characters") 
         String> channels,
    
    @NotNull(message = "Recipients cannot be null")
    @NotEmpty(message = "At least one recipient type is required")
    @Size(min = 1, max = 10, message = "You must specify between 1 and 10 recipient types")
    List<@NotBlank(message = "Recipient type cannot be blank") 
         @Size(min = 1, max = 50, message = "Recipient type must be between 1 and 50 characters") 
         String> recipients,
    
    Map<String, Object> context
) {
    /**
     * Valida se os canais especificados são suportados.
     */
    public boolean hasValidChannels() {
        if (channels == null) return false;
        List<String> supportedChannels = List.of("email", "whatsapp", "in_app", "sms", "push");
        return channels.stream()
                .allMatch(channel -> supportedChannels.contains(channel.toLowerCase()));
    }
}
