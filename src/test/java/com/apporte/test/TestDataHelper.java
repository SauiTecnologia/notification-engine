package com.apporte.test;

import com.apporte.core.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TestDataHelper {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
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
        user.setId(userId);
        user.setEmail(email);
        user.setName(name);
        user.setCreatedAt(Instant.now());
        user.setLastSync(Instant.now());
        
        if (roles != null && roles.length > 0) {
            StringBuilder rolesJson = new StringBuilder("[");
            for (int i = 0; i < roles.length; i++) {
                if (i > 0) rolesJson.append(",");
                rolesJson.append("\"").append(roles[i]).append("\"");
            }
            rolesJson.append("]");
            user.setRolesJson(rolesJson.toString());
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
        project.setId(projectId);
        project.setOwnerId(ownerId);
        project.setOwnerEmail(ownerEmail);
        project.setOwnerName(ownerName);
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
        notification.setUserId(userId);
        notification.setEventType(eventType);
        notification.setChannel(channel);
        notification.setStatus(status);
        notification.setCreatedAt(Instant.now());
        
        if ("sent".equals(status)) {
            notification.setSentAt(Instant.now());
        }
        
        if ("error".equals(status)) {
            notification.setErrorMessage("Test error message");
        }
        
        notification.setPayloadJson(createTestPayload(userId, eventType, channel));
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
        notification.setUserId(userId);
        notification.setEventType(eventType);
        notification.setChannel(channel);
        notification.setStatus("error");
        notification.setCreatedAt(Instant.now().minusSeconds(300)); // 5 minutos atrás
        notification.setErrorMessage("SMTP connection failed");
        notification.setPayloadJson(createTestPayload(userId, eventType, channel));
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
            // Use Jackson ObjectMapper para serializar corretamente
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test payload: " + e.getMessage(), e);
        }
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
        sentNotification.setUserId("user-123");
        sentNotification.setEventType("PROJECT_READY_REVIEW");
        sentNotification.setChannel("email");
        sentNotification.setStatus("sent");
        sentNotification.setCreatedAt(Instant.now());
        sentNotification.setSentAt(Instant.now());
        sentNotification.setPayloadJson("{\"recipient\":{\"userId\":\"user-123\",\"email\":\"admin@apporte.com\",\"name\":\"Administrador\",\"recipientType\":\"project_owner\"},\"event\":{\"type\":\"PROJECT_READY_REVIEW\",\"entityType\":\"project\",\"entityId\":\"proj-123\"}}");
        sentNotification.persist();
        
        Notification errorNotification = new Notification();
        errorNotification.setUserId("user-999");
        errorNotification.setEventType("TEST_EVENT");
        errorNotification.setChannel("email");
        errorNotification.setStatus("error");
        errorNotification.setErrorMessage("SMTP connection failed");
        errorNotification.setCreatedAt(Instant.now().minusSeconds(300)); // 5 minutos atrás
        errorNotification.setPayloadJson("{\"recipient\":{\"userId\":\"user-999\",\"email\":\"user-999@test.example.com\",\"name\":\"Test User 999\",\"recipientType\":\"manual\",\"phone\":\"+5511999999999\"},\"event\":{\"type\":\"TEST_EVENT\",\"entityType\":\"user\",\"entityId\":\"user-999\"}}");
        errorNotification.persist();
        
        Notification pendingNotification = new Notification();
        pendingNotification.setUserId("user-456");
        pendingNotification.setEventType("TASK_ASSIGNED");
        pendingNotification.setChannel("whatsapp");
        pendingNotification.setStatus("pending");
        pendingNotification.setCreatedAt(Instant.now());
        pendingNotification.setPayloadJson("{\"recipient\":{\"userId\":\"user-456\",\"email\":\"user@example.com\",\"name\":\"Regular User\",\"phone\":\"+5511999999999\",\"recipientType\":\"task_assignee\"},\"event\":{\"type\":\"TASK_ASSIGNED\",\"entityType\":\"task\",\"entityId\":\"task-123\"}}");
        pendingNotification.persist();
        
        return new TestData(adminUser, regularUser, project, 
                        sentNotification, errorNotification, pendingNotification);
    }
}