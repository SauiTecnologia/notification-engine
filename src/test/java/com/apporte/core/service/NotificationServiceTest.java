package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.model.*;
import com.apporte.core.repository.NotificationRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationServiceTest {

    @Inject
    NotificationService notificationService;

    @InjectMock
    RecipientResolverService recipientResolver;

    @InjectMock
    EmailService emailService;

    @InjectMock
    WhatsAppService whatsappService;

    @InjectMock
    NotificationRepository notificationRepository;

    private WorkflowNotificationRequest createTestRequest() {
        WorkflowNotificationRequest request = new WorkflowNotificationRequest();
        request.setEventType("PROJECT_READY_REVIEW");
        request.setEntityType("project");
        request.setEntityId("proj-123");
        request.setChannels(Arrays.asList("email", "whatsapp"));
        request.setRecipients(Arrays.asList("project_owner", "evaluators"));
        
        Map<String, Object> context = new HashMap<>();
        context.put("projectTitle", "Meu Projeto");
        context.put("fromColumn", "Em Análise");
        context.put("toColumn", "Pronto para Revisão");
        request.setContext(context);
        
        return request;
    }

    private RecipientResolution createTestRecipient() {
        RecipientResolution recipient = new RecipientResolution();
        recipient.setUserId("user-123");
        recipient.setEmail("usuario@empresa.com");
        recipient.setName("João Silva");
        recipient.setPhone("+5511999999999");
        recipient.setRecipientType("project_owner");
        return recipient;
    }

    @BeforeEach
    public void setup() {
        // Configurar comportamento padrão dos mocks
        when(recipientResolver.resolveRecipients(any(WorkflowNotificationRequest.class)))
            .thenReturn(Arrays.asList(createTestRecipient()));
        
        doNothing().when(emailService).sendEmail(any(RecipientResolution.class), any(WorkflowNotificationRequest.class));
        doNothing().when(whatsappService).sendMessage(any(RecipientResolution.class), any(WorkflowNotificationRequest.class));
    }

    @Test
    public void testProcessWorkflowNotification_Success() {
        // Arrange
        WorkflowNotificationRequest request = createTestRequest();

        // Act
        notificationService.processWorkflowNotification(request);

        // Assert
        // Verificar que recipients foram resolvidos
        verify(recipientResolver, times(1)).resolveRecipients(request);
        
        // Verificar que email foi enviado (1 recipient × 1 canal email)
        verify(emailService, times(1)).sendEmail(any(RecipientResolution.class), eq(request));
        
        // Verificar que WhatsApp foi enviado (1 recipient × 1 canal whatsapp)
        verify(whatsappService, times(1)).sendMessage(any(RecipientResolution.class), eq(request));
    }

    @Test
    public void testProcessWorkflowNotification_SingleChannel() {
        // Arrange
        WorkflowNotificationRequest request = createTestRequest();
        request.setChannels(Arrays.asList("email")); // Apenas email
        
        // Act
        notificationService.processWorkflowNotification(request);

        // Assert
        verify(emailService, times(1)).sendEmail(any(RecipientResolution.class), eq(request));
        verify(whatsappService, never()).sendMessage(any(RecipientResolution.class), any());
    }

    @Test
    public void testProcessWorkflowNotification_NoRecipients() {
        // Arrange
        WorkflowNotificationRequest request = createTestRequest();
        when(recipientResolver.resolveRecipients(any(WorkflowNotificationRequest.class)))
            .thenReturn(Arrays.asList()); // Nenhum recipient

        // Act
        notificationService.processWorkflowNotification(request);

        // Assert
        verify(emailService, never()).sendEmail(any(), any());
        verify(whatsappService, never()).sendMessage(any(), any());
    }

    @Test
    public void testProcessWorkflowNotification_EmailFailure() {
        // Arrange
        WorkflowNotificationRequest request = createTestRequest();
        
        // Simular falha no email
        doThrow(new RuntimeException("SMTP error"))
            .when(emailService).sendEmail(any(RecipientResolution.class), any(WorkflowNotificationRequest.class));

        // Act
        notificationService.processWorkflowNotification(request);

        // Assert
        // WhatsApp ainda deve ser enviado
        verify(whatsappService, times(1)).sendMessage(any(RecipientResolution.class), eq(request));
    }
}