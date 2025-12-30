package com.apporte.core.service;

import com.apporte.core.dto.WhatsAppTemplateData;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class WhatsAppTemplateService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppTemplateService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    private final Template project_approval;
    private final Template task_assignment;
    private final Template deadline_reminder;
    private final Template status_update;
    private final Template project_completed;
    private final Template default_template;
    
    public WhatsAppTemplateService(
            @Location("whatsapp/project_approval.html") Template project_approval,
            @Location("whatsapp/task_assignment.html") Template task_assignment,
            @Location("whatsapp/deadline_reminder.html") Template deadline_reminder,
            @Location("whatsapp/status_update.html") Template status_update,
            @Location("whatsapp/project_completed.html") Template project_completed,
            @Location("whatsapp/default.html") Template default_template) {
        this.project_approval = project_approval;
        this.task_assignment = task_assignment;
        this.deadline_reminder = deadline_reminder;
        this.status_update = status_update;
        this.project_completed = project_completed;
        this.default_template = default_template;
    }
    
    public String renderTemplate(WhatsAppTemplateData data) {
        try {
            if (!data.isValid()) {
                LOG.warn("Invalid template data received: name={}, eventType={}, entityId={}", 
                        data.name(), data.eventType(), data.entityId());
                return fallbackMessage(data);
            }
            
            TemplateInstance template = selectTemplate(data.eventType());
            
            String rendered = template
                .data("data", data)
                .data("name", data.name())
                .data("eventType", data.eventType())
                .data("entityType", data.entityType())
                .data("entityId", data.entityId())
                .data("date", data.date().format(DATE_FORMATTER))
                .data("year", data.date().getYear())
                .data("systemUrl", data.systemUrl())
                .data("context", data.context())
                .data("projectId", data.getProjectId())
                .data("projectTitle", data.getProjectTitle())
                .data("fromColumn", data.getFromColumn())
                .data("toColumn", data.getToColumn())
                .data("taskTitle", data.getTaskTitle())
                .data("priority", data.getPriority())
                .data("deadline", data.getDeadline())
                .data("itemName", data.getItemName())
                .data("daysLeft", data.getDaysLeft())
                .data("oldStatus", data.getOldStatus())
                .data("newStatus", data.getNewStatus())
                .data("eventDescription", data.getEventDescription())
                .data("taskDescription", data.getTaskDescription())
                .data("updatedBy", data.getUpdatedBy())
                .data("finalStatus", data.getFinalStatus())
                .data("feedback", data.getFeedback())
                .data("nextSteps", data.getNextSteps())
                .data("rating", data.getRating())
                .data("urgency", data.getUrgency())
                .data("comments", data.getComments())
                .render();
            
            LOG.debug("Template rendered successfully for event type: {}", data.eventType());
            return cleanWhatsAppMessage(rendered);
            
        } catch (Exception e) {
            LOG.error("Error rendering WhatsApp template for event type: {}", data.eventType(), e);
            return fallbackMessage(data);
        }
    }
    
    private TemplateInstance selectTemplate(String eventType) {
        if (eventType == null) {
            LOG.debug("Event type is null, using default template");
            return default_template.instance();
        }
        
        String normalizedEvent = eventType.toUpperCase().trim();
        LOG.debug("Selecting template for event type: {}", normalizedEvent);
        
        switch (normalizedEvent) {
            case "PROJECT_APPROVAL":
            case "PROJECT_READY":
            case "PROJECT_READY_REVIEW":
                LOG.debug("Selected project_approval template");
                return project_approval.instance();
                
            case "TASK_ASSIGNMENT":
            case "NEW_TASK":
            case "TASK_ASSIGNED":
                LOG.debug("Selected task_assignment template");
                return task_assignment.instance();
                
            case "DEADLINE_REMINDER":
            case "DEADLINE_APPROACHING":
            case "DEADLINE_WARNING":
                LOG.debug("Selected deadline_reminder template");
                return deadline_reminder.instance();
                
            case "STATUS_UPDATE":
            case "STATUS_CHANGE":
            case "STATUS_ALTERED":
                LOG.debug("Selected status_update template");
                return status_update.instance();
                
            case "PROJECT_COMPLETED":
            case "PROJECT_FINISHED":
            case "PROJECT_DONE":
                LOG.debug("Selected project_completed template");
                return project_completed.instance();
                
            default:
                LOG.warn("Unknown event type '{}', using default template", normalizedEvent);
                return default_template.instance();
        }
    }
    
    private String cleanWhatsAppMessage(String rendered) {
        // Remove tags HTML mantendo a formatação WhatsApp
        String cleaned = rendered
            .replaceAll("<[^>]*>", "")           // Remove tags HTML
            .replaceAll("\\s*\n\\s*\n+", "\n\n") // Normaliza linhas em branco
            .replaceAll("\\s+", " ")             // Remove espaços múltiplos
            .replaceAll("^\\s+", "")             // Remove espaços no início
            .replaceAll("\\s+$", "")             // Remove espaços no final
            .trim();
        
        // Garante que a mensagem não exceda o limite do WhatsApp
        if (cleaned.length() > 4096) {
            LOG.warn("WhatsApp message truncated from {} to 4096 characters", cleaned.length());
            cleaned = cleaned.substring(0, 4090) + "\n[...]";
        }
        
        LOG.debug("Message cleaned successfully. Final length: {} characters", cleaned.length());
        return cleaned;
    }
    
    private String fallbackMessage(WhatsAppTemplateData data) {
        return String.format(
            "*NOTIFICACAO DO APPORTE*\n\n" +
            "Ola %s,\n\n" +
            "Voce recebeu uma notificacao:\n" +
            "* Tipo: %s\n" +
            "* Referencia: %s\n" +
            "* Data: %s\n\n" +
            "Acesse o sistema para mais detalhes:\n" +
            "%s\n\n" +
            "---\n" +
            "Mensagem automatica - Apporte",
            data.name() != null ? data.name() : "Usuario",
            data.eventType(),
            data.entityId(),
            data.date().format(DATE_FORMATTER),
            data.systemUrl()
        );
    }
    
    // Método para pré-validação de templates
    public boolean validateTemplate(String eventType) {
        try {
            TemplateInstance template = selectTemplate(eventType);
            boolean isValid = template != null;
            LOG.debug("Template validation for event '{}': {}", eventType, isValid ? "VALID" : "INVALID");
            return isValid;
        } catch (Exception e) {
            LOG.error("Template validation failed for event type: {}", eventType, e);
            return false;
        }
    }
    
    // Método para obter lista de eventos suportados
    public String[] getSupportedEvents() {
        return new String[] {
            "PROJECT_APPROVAL",
            "TASK_ASSIGNMENT", 
            "DEADLINE_REMINDER",
            "STATUS_UPDATE",
            "PROJECT_COMPLETED"
        };
    }
}