package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.model.RecipientResolution;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

@ApplicationScoped
public class EmailService {
    
    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);
    
    private final Mailer mailer;
    private final Template projectReadyReview;
    
    public EmailService(Mailer mailer, 
                       @Location("emails/project-ready-review.html") Template projectReadyReview) {
        this.mailer = mailer;
        this.projectReadyReview = projectReadyReview;
    }
    
    public void sendEmail(RecipientResolution recipient, WorkflowNotificationRequest request) {
        try {
            String subject = getEmailSubject(request.eventType(), recipient.getName(), request.entityId());
            String htmlContent = renderEmailTemplate(recipient, request);
            
            Mail mail = Mail.withHtml(recipient.getEmail(), subject, htmlContent);
            mailer.send(mail);
            
            LOG.info("Email sent successfully to {} for event: {}", 
                     recipient.getEmail(), request.eventType());
            
        } catch (Exception e) {
            LOG.error("Failed to send email to {}: {}", 
                      recipient.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    private String getEmailSubject(String eventType, String recipientName, String entityId) {
        switch (eventType.toUpperCase()) {
            case "PROJECT_APPROVAL":
            case "PROJECT_READY_REVIEW":
                return "Seu projeto está pronto para avaliação - " + entityId;
            case "TASK_ASSIGNMENT":
                return "Nova tarefa atribuída - " + entityId;
            case "DEADLINE_REMINDER":
                return "Lembrete de prazo - " + entityId;
            case "STATUS_UPDATE":
                return "Atualização de status - " + entityId;
            default:
                return "Notificação do Apporte - " + entityId;
        }
    }
    
    private String renderEmailTemplate(RecipientResolution recipient, WorkflowNotificationRequest request) {
        try {
            Map<String, Object> data = Map.of(
                "nome", recipient.getName() != null ? recipient.getName() : "Colaborador",
                "projectTitle", request.context() != null && request.context().containsKey("projectTitle") 
                    ? request.context().get("projectTitle").toString() 
                    : request.entityId(),
                "fromColumn", request.context() != null && request.context().containsKey("fromColumn") 
                    ? request.context().get("fromColumn").toString() 
                    : "Coluna Anterior",
                "toColumn", request.context() != null && request.context().containsKey("toColumn") 
                    ? request.context().get("toColumn").toString() 
                    : "Coluna Atual",
                "projectUrl", System.getProperty("app.system.url", "https://app.apporte.com") + 
                    "/projects/" + request.entityId(),
                "year", LocalDateTime.now().getYear()
            );
            
            TemplateInstance template = projectReadyReview.instance();
            return template.data(data).render();
            
        } catch (Exception e) {
            LOG.error("Error rendering email template: {}", e.getMessage(), e);
            // Fallback simples
            return String.format(
                "<html><body>" +
                "<h1>Notificação do Apporte</h1>" +
                "<p>Olá %s,</p>" +
                "<p>Você recebeu uma notificação: %s</p>" +
                "<p>ID: %s</p>" +
                "<p>Acesse o sistema para mais detalhes.</p>" +
                "</body></html>",
                recipient.getName() != null ? recipient.getName() : "Colaborador",
                request.eventType(),
                request.entityId()
            );
        }
    }
}