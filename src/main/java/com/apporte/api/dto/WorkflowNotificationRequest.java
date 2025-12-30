package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Record para requisição de notificação via workflow.
 * Implementa padrão de record imutável com validação automática.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowNotificationRequest(
    @NotNull(message = "Event type cannot be null")
    @NotBlank(message = "Event type is required")
    String eventType,
    
    @NotNull(message = "Entity type cannot be null")
    @NotBlank(message = "Entity type is required")
    String entityType,
    
    @NotNull(message = "Entity ID cannot be null")
    @NotBlank(message = "Entity ID is required")
    String entityId,
    
    @NotNull(message = "Channels cannot be null")
    @NotEmpty(message = "At least one channel is required")
    List<String> channels,
    
    @NotNull(message = "Recipients cannot be null")
    @NotEmpty(message = "At least one recipient type is required")
    List<String> recipients,
    
    Map<String, Object> context
) {}
