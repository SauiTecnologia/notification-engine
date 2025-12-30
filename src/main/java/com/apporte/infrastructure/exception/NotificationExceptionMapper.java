package com.apporte.infrastructure.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapeador de exceções para respostas HTTP uniformes.
 * Converte exceções em respostas JSON apropriadas.
 */
@Provider
public class NotificationExceptionMapper implements ExceptionMapper<Exception> {
    
    private static final Logger LOG = LoggerFactory.getLogger(NotificationExceptionMapper.class);
    
    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Handling exception: {}", exception.getClass().getSimpleName(), exception);
        
        if (exception instanceof NotificationException notifEx) {
            return handleNotificationException(notifEx);
        }
        
        return handleGenericException(exception);
    }
    
    private Response handleNotificationException(NotificationException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", exception.getErrorCode());
        error.put("message", exception.getMessage());
        error.put("details", exception.getDetails());
        error.put("timestamp", System.currentTimeMillis());
        error.put("type", exception.getClass().getSimpleName());
        
        int statusCode = getStatusCode(exception);
        return Response.status(statusCode).entity(error).build();
    }
    
    private Response handleGenericException(Exception exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "UNKNOWN_ERROR");
        error.put("message", exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred");
        error.put("timestamp", System.currentTimeMillis());
        error.put("type", exception.getClass().getSimpleName());
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }
    
    private int getStatusCode(NotificationException exception) {
        return switch (exception.getErrorCode()) {
            case "RECIPIENT_RESOLUTION_ERROR" -> Response.Status.BAD_REQUEST.getStatusCode();
            case "SEND_ERROR" -> Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
            case "VALIDATION_ERROR" -> Response.Status.BAD_REQUEST.getStatusCode();
            default -> Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        };
    }
}
