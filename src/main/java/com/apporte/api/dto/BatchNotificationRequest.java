package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Record para requisição de notificação em lote.
 * Padrão imutável com validação automática.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchNotificationRequest(
    @NotNull(message = "Notifications list cannot be null")
    @NotEmpty(message = "At least one notification is required")
    List<SimpleNotificationRequest> notifications
) {}
