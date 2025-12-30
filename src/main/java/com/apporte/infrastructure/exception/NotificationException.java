package com.apporte.infrastructure.exception;

/**
 * Exceção base para operações de notificação.
 */
public class NotificationException extends RuntimeException {
    
    private final String errorCode;
    private final String details;
    
    public NotificationException(String message) {
        super(message);
        this.errorCode = "NOTIFICATION_ERROR";
        this.details = null;
    }
    
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NOTIFICATION_ERROR";
        this.details = null;
    }
    
    public NotificationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public NotificationException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public NotificationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getDetails() {
        return details;
    }
}
