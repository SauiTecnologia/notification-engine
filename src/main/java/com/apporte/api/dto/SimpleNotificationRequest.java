package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Record para requisição simples de notificação.
 * Padrão imutável com validação automática.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimpleNotificationRequest(
    @NotNull(message = "Event type cannot be null")
    @NotBlank(message = "Event type is required and cannot be empty")
    String eventType,
    
    @NotNull(message = "Channel cannot be null")
    @NotBlank(message = "Channel is required and cannot be empty")
    String channel,
    
    @NotNull(message = "Recipient ID cannot be null")
    @NotBlank(message = "Recipient ID is required and cannot be empty")
    String recipientId,
    
    Map<String, Object> context
) {}
