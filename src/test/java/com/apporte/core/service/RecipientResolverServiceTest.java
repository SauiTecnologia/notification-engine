package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.model.Project;
import com.apporte.core.model.User;
import com.apporte.core.model.RecipientResolution;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class RecipientResolverServiceTest {
    
    @Inject
    RecipientResolverService recipientResolverService;
    
    @BeforeEach
    @Transactional
    public void setupTestData() {
        // Limpar dados
            Project.deleteAll();
            User.deleteAll();
        
        // Criar usuário admin
        User adminUser = new User();
        adminUser.id = "user-123";
        adminUser.email = "admin@apporte.com";
        adminUser.name = "Administrador";
        adminUser.rolesJson = "[\"admin\", \"notification-admin\"]";
        adminUser.createdAt = Instant.now();
        adminUser.lastSync = Instant.now();
        adminUser.persist();
        
        // Criar projeto
        Project project = new Project();
        project.id = "proj-123";
        project.ownerId = "user-123";
        project.ownerEmail = "admin@apporte.com";
        project.ownerName = "Administrador";
        project.persist();
    }
    
    @Test
    @TestTransaction
    public void testResolveRecipients_ProjectOwner() {
        WorkflowNotificationRequest request = new WorkflowNotificationRequest();
        request.setEventType("PROJECT_READY_REVIEW");
        request.setEntityId("proj-123");
        request.setEntityType("project");
        request.setRecipients(List.of("project_owner"));
        request.setChannels(List.of("email"));
        
        List<RecipientResolution> recipients = recipientResolverService.resolveRecipients(request);
        
        assertNotNull(recipients);
        assertEquals(1, recipients.size());
        
        RecipientResolution recipient = recipients.get(0);
        assertEquals("user-123", recipient.getUserId());
        assertEquals("admin@apporte.com", recipient.getEmail());
        assertEquals("Administrador", recipient.getName());
        assertEquals("project_owner", recipient.getRecipientType());
    }
    
    @Test
    @TestTransaction
    public void testResolveRecipients_Admins() {
        WorkflowNotificationRequest request = new WorkflowNotificationRequest();
        request.setEventType("SYSTEM_ALERT");
        request.setEntityId("system");
        request.setEntityType("system");
        request.setRecipients(List.of("admins"));
        request.setChannels(List.of("email"));
        
        List<RecipientResolution> recipients = recipientResolverService.resolveRecipients(request);
        
        assertNotNull(recipients);
        assertFalse(recipients.isEmpty());
        
        boolean foundAdmin = recipients.stream()
            .anyMatch(r -> "admin@apporte.com".equals(r.getEmail()));
        assertTrue(foundAdmin, "Should find admin user");
    }
}