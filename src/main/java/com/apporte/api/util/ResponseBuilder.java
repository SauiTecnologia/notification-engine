package com.apporte.api.util;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Classe utilitária para construção de respostas HTTP.
 * Elimina código duplicado nos controllers.
 */
public class ResponseBuilder {
    
    private ResponseBuilder() {
        // Utility class
    }
    
    public static Response ok(Object data) {
        return Response.ok(data).build();
    }
    
    public static Response accepted(String status, String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        if (data != null) {
            response.putAll(data);
        }
        return Response.accepted(response).build();
    }
    
    public static Response badRequest(String message) {
        return errorResponse(Response.Status.BAD_REQUEST, "INVALID_REQUEST", message, null);
    }
    
    public static Response badRequest(String message, String details) {
        return errorResponse(Response.Status.BAD_REQUEST, "INVALID_REQUEST", message, details);
    }
    
    public static Response notFound(String message) {
        return errorResponse(Response.Status.NOT_FOUND, "NOT_FOUND", message, null);
    }
    
    public static Response conflict(String message) {
        return errorResponse(Response.Status.CONFLICT, "CONFLICT", message, null);
    }
    
    public static Response internalServerError(String message) {
        return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message, null);
    }
    
    public static Response internalServerError(String message, Throwable cause) {
        String details = cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : null;
        return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message, details);
    }
    
    private static Response errorResponse(Response.Status status, String code, String message, String details) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (details != null) {
            error.put("details", details);
        }
        error.put("timestamp", System.currentTimeMillis());
        return Response.status(status).entity(error).build();
    }
    
    /**
     * Parsa uma string de data no formato ISO 8601.
     */
    public static Instant parseInstant(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateStr).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use: YYYY-MM-DDTHH:MM:SS", e);
        }
    }
    
    /**
     * Gera um ID de requisição único para rastreamento.
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Cria um mapa de paginação.
     */
    public static Map<String, Object> createPaginationMap(int page, int size, int total) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("total", total);
        pagination.put("totalPages", (int) Math.ceil((double) total / size));
        return pagination;
    }
}
