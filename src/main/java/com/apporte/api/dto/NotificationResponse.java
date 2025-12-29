package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    
    private String status;
    private String message;
    private Map<String, Object> data;
    private Long timestamp;
    
    public NotificationResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public NotificationResponse(String status, String message, Map<String, Object> data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}