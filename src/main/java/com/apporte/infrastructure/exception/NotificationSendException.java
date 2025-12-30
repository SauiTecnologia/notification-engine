package com.apporte.infrastructure.exception;

/**
 * Exceção para erros de envio de notificações.
 */
public class NotificationSendException extends NotificationException {
    
    private final String channel;
    private final String recipientId;
    
    public NotificationSendException(String message) {
        super("SEND_ERROR", message);
        this.channel = null;
        this.recipientId = null;
    }
    
    public NotificationSendException(String message, Throwable cause) {
        super("SEND_ERROR", message, cause);
        this.channel = null;
        this.recipientId = null;
    }
    
    public NotificationSendException(String channel, String recipientId, String message) {
        super("SEND_ERROR", message);
        this.channel = channel;
        this.recipientId = recipientId;
    }
    
    public NotificationSendException(String channel, String recipientId, String message, Throwable cause) {
        super("SEND_ERROR", message, cause);
        this.channel = channel;
        this.recipientId = recipientId;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public String getRecipientId() {
        return recipientId;
    }
}
