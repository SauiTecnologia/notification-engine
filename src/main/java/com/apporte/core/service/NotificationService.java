package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.model.Notification;
import com.apporte.core.model.RecipientResolution;
import com.apporte.core.repository.NotificationRepository;
import com.apporte.infrastructure.exception.NotificationSendException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Serviço responsável por processar notificações de workflow.
 * Coordena resolução de destinatários, envio por diferentes canais e persistência.
 */
@ApplicationScoped
public class NotificationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    
    private final RecipientResolverService recipientResolver;
    private final EmailService emailService;
    private final WhatsAppService whatsappService;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    
    public NotificationService(RecipientResolverService recipientResolver, 
                             EmailService emailService,
                             WhatsAppService whatsappService,
                             NotificationRepository notificationRepository) {
        this.recipientResolver = Objects.requireNonNull(recipientResolver, "recipientResolver cannot be null");
        this.emailService = Objects.requireNonNull(emailService, "emailService cannot be null");
        this.whatsappService = Objects.requireNonNull(whatsappService, "whatsappService cannot be null");
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository cannot be null");
        this.objectMapper = new ObjectMapper();
    }
    
    @Transactional
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(5000)
    public void processWorkflowNotification(WorkflowNotificationRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        
        LOG.info("Processing workflow notification: {} for entity: {}", request.eventType(), request.entityId());
        
        try {
            List<RecipientResolution> resolvedRecipients = recipientResolver.resolveRecipients(request);
            LOG.debug("Resolved {} recipients for notification", resolvedRecipients.size());
            
            for (RecipientResolution recipient : resolvedRecipients) {
                if (!recipient.isValid()) {
                    LOG.warn("Skipping invalid recipient: {}", recipient);
                    continue;
                }
                
                for (String channel : request.channels()) {
                    sendNotificationForChannel(recipient, request, channel);
                }
            }
            
            LOG.info("Workflow notification {} processed successfully", request.eventType());
            
        } catch (Exception e) {
            LOG.error("Error processing workflow notification {}: {}", request.eventType(), e.getMessage(), e);
            throw new RuntimeException("Failed to process notification: " + e.getMessage(), e);
        }
    }
    
    private void sendNotificationForChannel(RecipientResolution recipient, WorkflowNotificationRequest request, String channel) {
        Notification notification = createNotification(recipient, request, channel);
        
        try {
            sendNotification(notification, recipient, request);
            notification.markAsSent();
            LOG.info("Notification sent successfully: {} to {} via {}", 
                     notification.getEventType(), recipient.getEmail(), channel);
            
        } catch (Exception e) {
            notification.markAsError(e.getMessage());
            LOG.error("Failed to send {} notification to {}: {}", 
                      channel, recipient.getEmail(), e.getMessage(), e);
        } finally {
            notificationRepository.persist(notification);
            LOG.debug("Notification persisted: {}", notification.getId());
        }
    }
    
    @Transactional
    public void retryNotification(Notification notification) {
        Objects.requireNonNull(notification, "notification cannot be null");
        
        LOG.info("Retrying notification ID: {}", notification.getId());
        
        try {
            notification.markAsRetrying();
            notification = notificationRepository.getEntityManager().merge(notification);
            
            Map<String, Object> payload = parseJson(notification.getPayloadJson());
            
            if (!payload.containsKey("recipient")) {
                throw new IllegalArgumentException("Invalid notification payload: missing recipient data");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> recipientData = (Map<String, Object>) payload.get("recipient");
            
            if (recipientData == null) {
                throw new IllegalArgumentException("Recipient data is null in notification payload");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) payload.get("event");
            
            if (eventData == null) {
                eventData = new HashMap<>();
            }
            
            RecipientResolution recipient = reconstructRecipient(recipientData, notification);
            WorkflowNotificationRequest request = reconstructRequest(eventData, notification);
            
            sendNotification(notification, recipient, request);
            
            notification.markAsSent();
            notification = notificationRepository.getEntityManager().merge(notification);
            
            LOG.info("Notification {} retried successfully", notification.getId());
            
        } catch (Exception e) {
            LOG.error("Retry failed for notification {}: {}", notification.getId(), e.getMessage(), e);
            
            notification.markAsError("Retry failed: " + e.getMessage());
            notificationRepository.getEntityManager().merge(notification);
            
            throw new RuntimeException("Failed to retry notification: " + e.getMessage(), e);
        }
    }
    
    private RecipientResolution reconstructRecipient(Map<String, Object> recipientData, Notification notification) {
        RecipientResolution recipient = new RecipientResolution();
        
        String userId = (String) recipientData.getOrDefault("userId", notification.getUserId());
        recipient.setUserId(userId);
        recipient.setEmail((String) recipientData.get("email"));
        recipient.setName((String) recipientData.get("name"));
        recipient.setRecipientType((String) recipientData.get("recipientType"));
        recipient.setPhone((String) recipientData.get("phone"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) recipientData.get("metadata");
        recipient.setMetadata(metadata);
        
        return recipient;
    }
    
    private WorkflowNotificationRequest reconstructRequest(Map<String, Object> eventData, Notification notification) {
        String eventType = notification.getEventType();
        String entityType = (String) eventData.getOrDefault("entityType", "user");
        String entityId = (String) eventData.getOrDefault("entityId", notification.getUserId());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) eventData.get("context");
        
        return new WorkflowNotificationRequest(
            eventType,
            entityType,
            entityId,
            List.of(notification.getChannel()),
            List.of("manual"),
            context != null ? context : new HashMap<>()
        );
    }
    
    private void sendNotification(Notification notification, RecipientResolution recipient, 
                                 WorkflowNotificationRequest request) {
        String channel = notification.getChannel();
        
        LOG.debug("Sending {} notification to {} via {}", 
                 request.eventType(), recipient.getEmail(), channel);
        
        try {
            switch (channel.toLowerCase()) {
                case "email":
                    emailService.sendEmail(recipient, request);
                    break;
                case "whatsapp":
                    if (!recipient.hasPhone()) {
                        throw new NotificationSendException(channel, recipient.getUserId(), 
                                "Recipient has no phone number for WhatsApp");
                    }
                    whatsappService.sendMessage(recipient, request);
                    break;
                case "in_app":
                    LOG.info("In-app notification would be sent to: {}", recipient.getEmail());
                    break;
                case "sms":
                    LOG.info("SMS notification would be sent to: {}", recipient.getPhone());
                    break;
                default:
                    throw new NotificationSendException(channel, recipient.getUserId(), 
                            "Unsupported channel: " + channel);
            }
        } catch (Exception e) {
            throw new NotificationSendException(channel, recipient.getUserId(), 
                    "Failed to send via " + channel + ": " + e.getMessage(), e);
        }
    }
    
    private Notification createNotification(RecipientResolution recipient, 
                                          WorkflowNotificationRequest request, 
                                          String channel) {
        Notification notification = new Notification(
            recipient.getUserId(),
            request.eventType(),
            channel
        );
        
        notification.setPayloadJson(createJsonPayload(recipient, request));
        
        LOG.debug("Created notification for user {}", recipient.getUserId());
        return notification;
    }
    
    private String createJsonPayload(RecipientResolution recipient, WorkflowNotificationRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            
            Map<String, Object> recipientMap = new HashMap<>();
            recipientMap.put("userId", recipient.getUserId());
            recipientMap.put("email", recipient.getEmail());
            recipientMap.put("name", recipient.getName());
            recipientMap.put("recipientType", recipient.getRecipientType());
            recipientMap.put("phone", recipient.getPhone());
            recipientMap.put("metadata", recipient.getMetadata());
            
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("type", request.eventType());
            eventMap.put("entityType", request.entityType());
            eventMap.put("entityId", request.entityId());
            eventMap.put("context", request.context());
            eventMap.put("timestamp", Instant.now().toString());
            
            payload.put("recipient", recipientMap);
            payload.put("event", eventMap);
            payload.put("retryAttempts", 0);
            payload.put("createdAt", Instant.now().toString());
            
            return objectMapper.writeValueAsString(payload);
            
        } catch (Exception e) {
            LOG.error("Error creating JSON payload: {}", e.getMessage());
            return createFallbackPayload(recipient, request);
        }
    }
    
    private String createFallbackPayload(RecipientResolution recipient, WorkflowNotificationRequest request) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"recipient\": {");
        json.append("\"userId\": \"").append(recipient.getUserId()).append("\",");
        json.append("\"email\": \"").append(recipient.getEmail()).append("\",");
        json.append("\"name\": \"").append(recipient.getName()).append("\",");
        json.append("\"recipientType\": \"").append(recipient.getRecipientType()).append("\"");
        
        if (recipient.getPhone() != null) {
            json.append(",\"phone\": \"").append(recipient.getPhone()).append("\"");
        }
        
        json.append("},");
        json.append("\"event\": {");
        json.append("\"type\": \"").append(request.eventType()).append("\",");
        json.append("\"entityType\": \"").append(request.entityType()).append("\",");
        json.append("\"entityId\": \"").append(request.entityId()).append("\"");
        json.append("},");
        json.append("\"retryAttempts\": 0,");
        json.append("\"createdAt\": \"").append(Instant.now().toString()).append("\"");
        json.append("}");
        
        return json.toString();
    }
    
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            LOG.warn("Cannot parse null or empty JSON payload");
            throw new IllegalArgumentException("JSON payload is null or empty");
        }
        
        try {
            // Se o JSON começar e terminar com aspas, removê-las
            String actualJson = json;
            if (json.startsWith("\"") && json.endsWith("\"")) {
                LOG.warn("JSON payload has outer quotes, removing them");
                actualJson = json.substring(1, json.length() - 1);
                // Unescape escaped quotes
                actualJson = actualJson.replace("\\\"", "\"");
            }
            
            return objectMapper.readValue(actualJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            LOG.error("Error parsing JSON payload: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse notification payload: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public void cleanupOldNotifications(int daysToKeep) {
        Instant cutoffDate = Instant.now().minusSeconds((long) daysToKeep * 24 * 60 * 60);
        
        long deletedCount = notificationRepository.delete(
            "createdAt < ?1 and status in ('sent', 'error')", 
            cutoffDate
        );
        
        LOG.info("Cleaned up {} old notifications older than {} days", deletedCount, daysToKeep);
    }
    
    @io.quarkus.cache.CacheResult(cacheName = "notification-status-cache")
    public String getNotificationStatusWithCache(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId);
        return notification != null ? notification.getStatus() : "not_found";
    }
    
    public Notification findNotificationById(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return notificationRepository.findById(id);
    }
    
    public List<Notification> listNotifications(
            int page, 
            int size,
            String status,
            String channel,
            String eventType,
            Instant startDate,
            Instant endDate) {
        
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();
        
        if (status != null && !status.isEmpty()) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        
        if (channel != null && !channel.isEmpty()) {
            query.append(" and channel = :channel");
            params.put("channel", channel);
        }
        
        if (eventType != null && !eventType.isEmpty()) {
            query.append(" and eventType = :eventType");
            params.put("eventType", eventType);
        }
        
        if (startDate != null) {
            query.append(" and createdAt >= :startDate");
            params.put("startDate", startDate);
        }
        
        if (endDate != null) {
            query.append(" and createdAt <= :endDate");
            params.put("endDate", endDate);
        }
        
        return notificationRepository.find(query.toString(), 
                io.quarkus.panache.common.Sort.descending("createdAt"), params)
                .page(page, size)
                .list();
    }
    
    public long countNotifications(
            String status,
            String channel,
            String eventType,
            Instant startDate,
            Instant endDate) {
        
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();
        
        if (status != null && !status.isEmpty()) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        
        if (channel != null && !channel.isEmpty()) {
            query.append(" and channel = :channel");
            params.put("channel", channel);
        }
        
        if (eventType != null && !eventType.isEmpty()) {
            query.append(" and eventType = :eventType");
            params.put("eventType", eventType);
        }
        
        if (startDate != null) {
            query.append(" and createdAt >= :startDate");
            params.put("startDate", startDate);
        }
        
        if (endDate != null) {
            query.append(" and createdAt <= :endDate");
            params.put("endDate", endDate);
        }
        
        return notificationRepository.find(query.toString(), params).count();
    }
    
    @Transactional
    public void deleteNotification(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        notificationRepository.deleteById(id);
    }
    
    public List<Notification> getUserNotifications(String userId, String status, int limit) {
        Objects.requireNonNull(userId, "userId cannot be null");
        
        StringBuilder query = new StringBuilder("userId = :userId");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        
        if (status != null && !status.isEmpty()) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        
        return notificationRepository.find(query.toString(), params)
                .range(0, limit)
                .list();
    }
    
    public Map<String, Object> getStatistics(int days) {
        Instant since = Instant.now().minusSeconds((long) days * 24 * 60 * 60);
        
        long total = notificationRepository.count("createdAt >= ?1", since);
        long sent = notificationRepository.count("status = ?1 and createdAt >= ?2", "sent", since);
        long error = notificationRepository.count("status = ?1 and createdAt >= ?2", "error", since);
        long pending = notificationRepository.count("status = ?1 and createdAt >= ?2", "pending", since);
        
        Map<String, Long> byChannel = new HashMap<>();
        String[] channels = {"email", "whatsapp", "sms", "in_app"};
        for (String channel : channels) {
            long count = notificationRepository.count("channel = ?1 and createdAt >= ?2", channel, since);
            if (count > 0) {
                byChannel.put(channel, count);
            }
        }
        
        Map<String, Long> byEventType = new HashMap<>();
        try {
            var eventResults = notificationRepository.getEntityManager()
                    .createQuery("SELECT n.eventType, COUNT(n) FROM Notification n WHERE n.createdAt >= :since GROUP BY n.eventType", Object[].class)
                    .setParameter("since", since)
                    .setMaxResults(5)
                    .getResultList();
            
            for (Object[] row : eventResults) {
                byEventType.put((String) row[0], (Long) row[1]);
            }
        } catch (Exception e) {
            LOG.warn("Could not get event type statistics: {}", e.getMessage());
        }
        
        Map<String, Long> byDay = new HashMap<>();
        try {
            var dailyResults = notificationRepository.getEntityManager()
                    .createNativeQuery("SELECT DATE_FORMAT(created_at, '%Y-%m-%d'), COUNT(*) FROM notifications WHERE created_at >= ? GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')")
                    .setParameter(1, since)
                    .getResultList();
            
            for (Object row : dailyResults) {
                Object[] columns = (Object[]) row;
                byDay.put(columns[0].toString(), ((Number) columns[1]).longValue());
            }
        } catch (Exception e) {
            LOG.warn("Could not get daily statistics (falling back to basic): {}", e.getMessage());
            var allNotifications = notificationRepository.find("createdAt >= ?1", since).list();
            for (var notif : allNotifications) {
                String date = notif.getCreatedAt().toString().substring(0, 10);
                byDay.put(date, byDay.getOrDefault(date, 0L) + 1);
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("periodDays", days);
        stats.put("since", since.toString());
        stats.put("total", total);
        stats.put("sent", sent);
        stats.put("error", error);
        stats.put("pending", pending);
        stats.put("successRate", total > 0 ? 
            String.format("%.1f%%", (double) sent / total * 100) : "0%");
        stats.put("byChannel", byChannel);
        stats.put("byEventType", byEventType);
        stats.put("byDay", byDay);
        
        return stats;
    }
    
    public Map<String, Object> getHealthStatus() {
        try {
            long total = notificationRepository.count();
            long pending = notificationRepository.count("status = ?1", "pending");
            long error = notificationRepository.count("status = ?1", "error");
            long sent = notificationRepository.count("status = ?1", "sent");
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", Instant.now().toString());
            health.put("database", "connected");
            health.put("totalNotifications", total);
            health.put("pendingNotifications", pending);
            health.put("errorNotifications", error);
            health.put("sentNotifications", sent);
            
            return health;
        } catch (Exception e) {
            LOG.error("Health check failed: {}", e.getMessage(), e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("timestamp", Instant.now().toString());
            health.put("error", e.getMessage());
            
            return health;
        }
    }
}
