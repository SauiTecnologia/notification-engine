package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Record para resposta de notificação.
 * Padrão imutável com timestamp automático.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
    String status,
    String message,
    Map<String, Object> data,
    long timestamp
) {
    /**
     * Construtor compacto com timestamp padrão (momento da criação).
     */
    public NotificationResponse(String status, String message, Map<String, Object> data) {
        this(status, message, data, System.currentTimeMillis());
    }
}
