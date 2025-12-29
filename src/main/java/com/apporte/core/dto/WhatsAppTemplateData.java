package com.apporte.core.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class WhatsAppTemplateData {
    private String name;
    private String eventType;
    private String entityType;
    private String entityId;
    private LocalDateTime date;
    private String systemUrl;
    private Map<String, Object> context;
    
    // Construtor
    public WhatsAppTemplateData(String name, String eventType, String entityType, 
                                String entityId, Map<String, Object> context) {
        this.name = name;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.date = LocalDateTime.now();
        this.systemUrl = System.getProperty("app.system.url", "https://app.apporte.com");
        this.context = context;
    }
    
    // Getters básicos
    public String getName() { return name; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public LocalDateTime getDate() { return date; }
    public String getSystemUrl() { return systemUrl; }
    public Map<String, Object> getContext() { return context; }
    
    // Métodos auxiliares para templates
    public String getProjectId() { return entityId; }
    
    public String getProjectTitle() {
        return context != null && context.containsKey("projectTitle") 
            ? context.get("projectTitle").toString() 
            : entityId;
    }
    
    public String getFromColumn() {
        return context != null && context.containsKey("fromColumn") 
            ? context.get("fromColumn").toString() 
            : "Coluna Anterior";
    }
    
    public String getToColumn() {
        return context != null && context.containsKey("toColumn") 
            ? context.get("toColumn").toString() 
            : "Coluna Atual";
    }
    
    public String getTaskTitle() { 
        return context != null && context.containsKey("title") 
            ? context.get("title").toString() 
            : entityId; 
    }
    
    public String getPriority() {
        return context != null && context.containsKey("priority") 
            ? context.get("priority").toString() 
            : "Normal";
    }
    
    public String getDeadline() {
        return context != null && context.containsKey("deadline") 
            ? context.get("deadline").toString() 
            : "Não especificado";
    }
    
    public String getItemName() {
        return context != null && context.containsKey("itemName") 
            ? context.get("itemName").toString() 
            : entityId;
    }
    
    public String getDaysLeft() {
        return context != null && context.containsKey("daysLeft") 
            ? context.get("daysLeft").toString() 
            : "0";
    }
    
    public String getOldStatus() {
        return context != null && context.containsKey("oldStatus") 
            ? context.get("oldStatus").toString() 
            : "Não especificado";
    }
    
    public String getNewStatus() {
        return context != null && context.containsKey("newStatus") 
            ? context.get("newStatus").toString() 
            : "Não especificado";
    }
    
    public String getEventDescription() {
        return context != null && context.containsKey("description") 
            ? context.get("description").toString() 
            : "Um item requer sua atenção";
    }
    
    public String getTaskDescription() {
        return context != null && context.containsKey("taskDescription") 
            ? context.get("taskDescription").toString() 
            : null;
    }
    
    public String getUpdatedBy() {
        return context != null && context.containsKey("updatedBy") 
            ? context.get("updatedBy").toString() 
            : "Sistema";
    }
    
    public String getFinalStatus() {
        return context != null && context.containsKey("finalStatus") 
            ? context.get("finalStatus").toString() 
            : "Concluído";
    }
    
    public String getFeedback() {
        return context != null && context.containsKey("feedback") 
            ? context.get("feedback").toString() 
            : null;
    }
    
    public String getNextSteps() {
        return context != null && context.containsKey("nextSteps") 
            ? context.get("nextSteps").toString() 
            : null;
    }
    
    public String getRating() {
        return context != null && context.containsKey("rating") 
            ? context.get("rating").toString() 
            : null;
    }
    
    public String getUrgency() {
        return context != null && context.containsKey("urgency") 
            ? context.get("urgency").toString() 
            : "LOW";
    }
    
    public String getComments() {
        return context != null && context.containsKey("comments") 
            ? context.get("comments").toString() 
            : null;
    }
    
    // Método para validação
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               eventType != null && !eventType.trim().isEmpty() &&
               entityId != null && !entityId.trim().isEmpty();
    }
}