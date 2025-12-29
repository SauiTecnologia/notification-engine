package com.apporte.test;

import com.apporte.core.model.*;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TestDataHelper {
    
    /**
     * Limpa todos os dados de teste
     */
    @Transactional
    public static void cleanAllTestData() {
        // Notificações
        List<Notification> notifications = Notification.listAll();
        notifications.forEach(n -> n.delete());
        
        // Projetos
        List<Project> projects = Project.listAll();
        projects.forEach(p -> p.delete());
        
        // Usuários
        List<User> users = User.listAll();
        users.forEach(u -> u.delete());
    }
    
    /**
     * Cria um usuário de teste
     */
    @Transactional
    public static User createTestUser(String userId, String email, String name, String... roles) {
        User user = new User();
        user.id = userId;
        user.email = email;
        user.name = name;
        user.createdAt = Instant.now();
        user.lastSync = Instant.now();
        
        if (roles != null && roles.length > 0) {
            StringBuilder rolesJson = new StringBuilder("[");
            for (int i = 0; i < roles.length; i++) {
                if (i > 0) rolesJson.append(",");
                rolesJson.append("\"").append(roles[i]).append("\"");
            }
            rolesJson.append("]");
            user.rolesJson = rolesJson.toString();
        }
        
        user.persist();
        return user;
    }
    
    /**
     * Cria um projeto de teste
     */
    @Transactional
    public static Project createTestProject(String projectId, String ownerId, 
                                           String ownerEmail, String ownerName) {
        Project project = new Project();
        project.id = projectId;
        project.ownerId = ownerId;
        project.ownerEmail = ownerEmail;
        project.ownerName = ownerName;
        project.persist();
        return project;
    }
    
    /**
     * Cria uma notificação de teste
     */
    @Transactional
    public static Notification createTestNotification(String userId, String eventType, 
                                                     String channel, String status) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.eventType = eventType;
        notification.channel = channel;
        notification.status = status;
        notification.createdAt = Instant.now();
        
        if ("sent".equals(status)) {
            notification.sentAt = Instant.now();
        }
        
        if ("error".equals(status)) {
            notification.errorMessage = "Test error message";
        }
        
        notification.payloadJson = createTestPayload(userId, eventType, channel);
        notification.persist();
        
        return notification;
    }
    
    /**
     * Cria uma notificação com erro para testes de retry
     */
    @Transactional
    public static Notification createErrorNotificationForRetry(String userId, String eventType, 
                                                              String channel) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.eventType = eventType;
        notification.channel = channel;
        notification.status = "error";
        notification.createdAt = Instant.now().minusSeconds(300); // 5 minutos atrás
        notification.errorMessage = "SMTP connection failed";
        notification.payloadJson = createTestPayload(userId, eventType, channel);
        notification.persist();
        
        return notification;
    }
    
    /**
     * Cria múltiplas notificações para testes
     */
    @Transactional
    public static void createMultipleNotifications(int count, String userIdPrefix, 
                                                  String eventType, String channel) {
        for (int i = 0; i < count; i++) {
            String userId = userIdPrefix + "-" + (i + 1);
            createTestNotification(userId, eventType, channel, 
                                 i % 3 == 0 ? "sent" : i % 3 == 1 ? "pending" : "error");
        }
    }
    
    /**
     * Busca a última notificação criada
     */
    public static Notification getLastNotification() {
        return Notification.find("order by createdAt desc").firstResult();
    }
    
    /**
     * Conta notificações por status
     */
    public static long countNotificationsByStatus(String status) {
        return Notification.count("status", status);
    }
    
    /**
     * Cria payload JSON para notificação de teste
     */
    private static String createTestPayload(String userId, String eventType, String channel) {
        Map<String, Object> payload = Map.of(
            "recipient", Map.of(
                "userId", userId,
                "email", userId + "@test.example.com",
                "name", "Test User " + userId,
                "recipientType", "manual",
                "phone", "+5511999999999"
            ),
            "event", Map.of(
                "type", eventType,
                "entityType", "user",
                "entityId", userId,
                "context", Map.of(
                    "test", true,
                    "channel", channel
                )
            ),
            "createdAt", Instant.now().toString(),
            "retryAttempts", 0
        );
        
        try {
            // Se você tiver Jackson disponível nos testes
            // return new ObjectMapper().writeValueAsString(payload);
            return formatJsonManually(payload);
        } catch (Exception e) {
            return formatJsonManually(payload);
        }
    }
    
    /**
     * Formata JSON manualmente (fallback)
     */
    private static String formatJsonManually(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) json.append(",");
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> innerMap = (Map<String, Object>) entry.getValue();
                json.append(formatJsonManually(innerMap));
            } else if (entry.getValue() instanceof Boolean) {
                json.append(entry.getValue());
            } else {
                json.append("\"").append(entry.getValue()).append("\"");
            }
            
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Setup básico para testes: usuário, projeto e algumas notificações
     */
    @Transactional
    public static TestData setupBasicTestData() {
        cleanAllTestData();
        
        // Criar usuário admin
        User adminUser = createTestUser("user-123", "admin@apporte.com", "Administrador", 
                                       "admin", "notification-admin");
        
        // Criar usuário regular
        User regularUser = createTestUser("user-456", "user@example.com", "Regular User");
        
        // Criar projeto
        Project project = createTestProject("proj-123", "user-123", 
                                          "admin@apporte.com", "Administrador");
        
        // Criar algumas notificações
        Notification sentNotification = createTestNotification("user-123", "PROJECT_READY_REVIEW", 
                                                             "email", "sent");
        
        Notification errorNotification = createErrorNotificationForRetry("user-999", 
                                                                       "TEST_EVENT", "email");
        
        Notification pendingNotification = createTestNotification("user-456", "TASK_ASSIGNED", 
                                                                "whatsapp", "pending");
        
        return new TestData(adminUser, regularUser, project, 
                           sentNotification, errorNotification, pendingNotification);
    }
    
    /**
     * Classe para retornar todos os dados de teste
     */
    public static class TestData {
        public final User adminUser;
        public final User regularUser;
        public final Project project;
        public final Notification sentNotification;
        public final Notification errorNotification;
        public final Notification pendingNotification;
        
        public TestData(User adminUser, User regularUser, Project project,
                       Notification sentNotification, Notification errorNotification,
                       Notification pendingNotification) {
            this.adminUser = adminUser;
            this.regularUser = regularUser;
            this.project = project;
            this.sentNotification = sentNotification;
            this.errorNotification = errorNotification;
            this.pendingNotification = pendingNotification;
        }
    }

    @Transactional
    public static TestData setupAdminControllerTestData() {
        cleanAllTestData();
        
        // Criar usuário admin
        User adminUser = createTestUser("user-123", "admin@apporte.com", "Administrador", 
                                    "admin", "notification-admin");
        
        // Criar usuário regular
        User regularUser = createTestUser("user-456", "user@example.com", "Regular User");
        
        // Criar projeto
        Project project = createTestProject("proj-123", "user-123", 
                                        "admin@apporte.com", "Administrador");
        
        // Criar notificações com dados específicos para os testes
        Notification sentNotification = new Notification();
        sentNotification.userId = "user-123";
        sentNotification.eventType = "PROJECT_READY_REVIEW";
        sentNotification.channel = "email";
        sentNotification.status = "sent";
        sentNotification.createdAt = Instant.now();
        sentNotification.sentAt = Instant.now();
        sentNotification.payloadJson = "{\"recipient\":{\"userId\":\"user-123\"}}";
        sentNotification.persist();
        
        Notification errorNotification = new Notification();
        errorNotification.userId = "user-999";
        errorNotification.eventType = "TEST_EVENT";
        errorNotification.channel = "email";
        errorNotification.status = "error";
        errorNotification.errorMessage = "SMTP connection failed";
        errorNotification.createdAt = Instant.now().minusSeconds(300); // 5 minutos atrás
        errorNotification.payloadJson = "{\"recipient\":{\"userId\":\"user-999\"}}";
        errorNotification.persist();
        
        Notification pendingNotification = new Notification();
        pendingNotification.userId = "user-456";
        pendingNotification.eventType = "TASK_ASSIGNED";
        pendingNotification.channel = "whatsapp";
        pendingNotification.status = "pending";
        pendingNotification.createdAt = Instant.now();
        pendingNotification.payloadJson = "{\"recipient\":{\"userId\":\"user-456\"}}";
        pendingNotification.persist();
        
        return new TestData(adminUser, regularUser, project, 
                        sentNotification, errorNotification, pendingNotification);
    }
}