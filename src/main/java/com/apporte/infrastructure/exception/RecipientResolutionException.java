package com.apporte.infrastructure.exception;

/**
 * Exceção para erros na resolução de destinatários.
 */
public class RecipientResolutionException extends NotificationException {
    
    public RecipientResolutionException(String message) {
        super("RECIPIENT_RESOLUTION_ERROR", message);
    }
    
    public RecipientResolutionException(String message, Throwable cause) {
        super("RECIPIENT_RESOLUTION_ERROR", message, cause);
    }
    
    public RecipientResolutionException(String message, String details) {
        super("RECIPIENT_RESOLUTION_ERROR", message, details);
    }
}
