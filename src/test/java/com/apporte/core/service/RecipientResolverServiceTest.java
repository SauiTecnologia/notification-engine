package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.model.Project;
import com.apporte.core.model.User;
import com.apporte.core.model.RecipientResolution;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.TestInstance;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;

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
        adminUser.setId("user-123");
        adminUser.setEmail("admin@apporte.com");
        adminUser.setName("Administrador");
        adminUser.setRolesJson("[\"admin\", \"notification-admin\"]");
        adminUser.setCreatedAt(Instant.now());
        adminUser.setLastSync(Instant.now());
        adminUser.persist();
        
        // Criar projeto
        Project project = new Project();
        project.setId("proj-123");
        project.setOwnerId("user-123");
        project.setOwnerEmail("admin@apporte.com");
        project.setOwnerName("Administrador");
        project.persist();
    }
    
    @Test
    @TestTransaction
    public void testResolveRecipients_ProjectOwner() {
        WorkflowNotificationRequest request = new WorkflowNotificationRequest(
            "PROJECT_READY_REVIEW",
            "project",
            "proj-123",
            List.of("email"),
            List.of("project_owner"),
            Map.of()
        );
        
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
        WorkflowNotificationRequest request = new WorkflowNotificationRequest(
            "SYSTEM_ALERT",
            "system",
            "system",
            List.of("email"),
            List.of("admins"),
            Map.of()
        );
        
        List<RecipientResolution> recipients = recipientResolverService.resolveRecipients(request);
        
        assertNotNull(recipients);
        assertFalse(recipients.isEmpty());
        
        boolean foundAdmin = recipients.stream()
            .anyMatch(r -> "admin@apporte.com".equals(r.getEmail()));
        assertTrue(foundAdmin, "Should find admin user");
    }
}