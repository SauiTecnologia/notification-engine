package com.apporte.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchNotificationRequest {
    
    @NotNull(message = "Notifications list cannot be null")
    @NotEmpty(message = "At least one notification is required")
    private List<SimpleNotificationRequest> notifications;
    
    // Getters e Setters
    public List<SimpleNotificationRequest> getNotifications() { return notifications; }
    public void setNotifications(List<SimpleNotificationRequest> notifications) { 
        this.notifications = notifications; 
    }
}