package com.apporte.core.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Record para dados de template WhatsApp.
 * Fornece métodos auxiliares para acesso seguro ao contexto.
 */
public record WhatsAppTemplateData(
    String name,
    String eventType,
    String entityType,
    String entityId,
    LocalDateTime date,
    String systemUrl,
    Map<String, Object> context
) {
    
    /**
     * Construtor compacto com defaults para date e systemUrl.
     */
    public WhatsAppTemplateData(String name, String eventType, String entityType, 
                                String entityId, Map<String, Object> context) {
        this(name, eventType, entityType, entityId, LocalDateTime.now(),
             System.getProperty("app.system.url", "https://app.apporte.com"), 
             context);
    }
    
    // Métodos auxiliares para templates
    public String getProjectId() { 
        return entityId; 
    }
    
    public String getProjectTitle() {
        return contextValue("projectTitle", entityId);
    }
    
    public String getFromColumn() {
        return contextValue("fromColumn", "Coluna Anterior");
    }
    
    public String getToColumn() {
        return contextValue("toColumn", "Coluna Atual");
    }
    
    public String getTaskTitle() { 
        return contextValue("title", entityId);
    }
    
    public String getPriority() {
        return contextValue("priority", "Normal");
    }
    
    public String getDeadline() {
        return contextValue("deadline", "Não especificado");
    }
    
    public String getItemName() {
        return contextValue("itemName", entityId);
    }
    
    public String getDaysLeft() {
        return contextValue("daysLeft", "0");
    }
    
    public String getOldStatus() {
        return contextValue("oldStatus", "Não especificado");
    }
    
    public String getNewStatus() {
        return contextValue("newStatus", "Não especificado");
    }
    
    public String getEventDescription() {
        return contextValue("description", "Um item requer sua atenção");
    }
    
    public String getTaskDescription() {
        return contextValue("taskDescription", null);
    }
    
    public String getUpdatedBy() {
        return contextValue("updatedBy", "Sistema");
    }
    
    public String getFinalStatus() {
        return contextValue("finalStatus", "Concluído");
    }
    
    public String getFeedback() {
        return contextValue("feedback", null);
    }
    
    public String getNextSteps() {
        return contextValue("nextSteps", null);
    }
    
    public String getRating() {
        return contextValue("rating", null);
    }
    
    public String getUrgency() {
        return contextValue("urgency", "LOW");
    }
    
    public String getComments() {
        return contextValue("comments", null);
    }
    
    /**
     * Extrai valor do contexto com padrão.
     */
    private String contextValue(String key, String defaultValue) {
        if (context != null && context.containsKey(key)) {
            Object value = context.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        return defaultValue;
    }
    
    /**
     * Valida se os dados essenciais estão presentes.
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               eventType != null && !eventType.trim().isEmpty() &&
               entityId != null && !entityId.trim().isEmpty();
    }
}
