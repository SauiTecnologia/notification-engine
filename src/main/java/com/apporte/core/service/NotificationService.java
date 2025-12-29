package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.model.Notification;
import com.apporte.core.model.RecipientResolution;
import com.apporte.core.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NotificationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    
    @Inject
    RecipientResolverService recipientResolver;
    
    @Inject
    EmailService emailService;
    
    @Inject
    WhatsAppService whatsappService;
    
    @Inject
    NotificationRepository notificationRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(5000)
    public void processWorkflowNotification(WorkflowNotificationRequest request) {
        LOG.info("Processing workflow notification: {}", request.getEventType());
        
        // 1. Resolver destinatários
        List<RecipientResolution> resolvedRecipients = recipientResolver.resolveRecipients(request);
        
        // Log para debug
        LOG.debug("Resolved {} recipients for notification", resolvedRecipients.size());
        
        // 2. Para cada destinatário e canal
        for (RecipientResolution recipient : resolvedRecipients) {
            for (String channel : request.getChannels()) {
                Notification notification = createNotification(recipient, request, channel);
                
                try {
                    // 3. Enviar notificação
                    sendNotification(notification, recipient, request);
                    
                    // 4. Marcar como enviada
                    notification.status = "sent";
                    notification.sentAt = Instant.now();
                    LOG.info("Notification sent successfully: {} to {}", 
                             notification.eventType, recipient.getEmail());
                    
                } catch (Exception e) {
                    notification.status = "error";
                    notification.errorMessage = e.getMessage();
                    LOG.error("Failed to send notification to {}: {}", 
                              recipient.getEmail(), e.getMessage(), e);
                }
                
                // 5. Persistir
                notificationRepository.persist(notification);
                LOG.debug("Notification persisted: {}", notification.id);
            }
        }
    }
    
    @Transactional
    public void retryNotification(Notification notification) {
        LOG.info("Retrying notification ID: {}", notification.id);
        
        try {
            // Limpar erro anterior
            notification.status = "retrying";
            notification.errorMessage = null;
            notificationRepository.persist(notification);
            
            // Parse do payload JSON
            Map<String, Object> payload = parseJson(notification.payloadJson);
            
            // Verificar se o payload tem a estrutura esperada
            if (payload == null || !payload.containsKey("recipient")) {
                throw new RuntimeException("Invalid notification payload: missing recipient data");
            }
            
            // Extrair dados do payload com verificações de null
            @SuppressWarnings("unchecked")
            Map<String, Object> recipientData = (Map<String, Object>) payload.get("recipient");
            
            if (recipientData == null) {
                throw new RuntimeException("Recipient data is null in notification payload");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) payload.get("event");
            
            if (eventData == null) {
                eventData = new HashMap<>(); // Criar mapa vazio se não existir
            }
            
            // Reconstruir o recipient com verificações
            RecipientResolution recipient = new RecipientResolution();
            
            String userId = (String) recipientData.get("userId");
            if (userId == null) {
                userId = notification.userId; // Fallback para o userId da notificação
            }
            recipient.setUserId(userId);
            
            recipient.setEmail((String) recipientData.get("email"));
            recipient.setName((String) recipientData.get("name"));
            recipient.setRecipientType((String) recipientData.get("recipientType"));
            recipient.setPhone((String) recipientData.get("phone"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) recipientData.get("metadata");
            recipient.setMetadata(metadata);
            
            // Reconstruir a request como WorkflowNotificationRequest
            WorkflowNotificationRequest request = new WorkflowNotificationRequest();
            request.setEventType(notification.eventType);
            
            String entityType = (String) eventData.get("entityType");
            if (entityType == null) {
                entityType = "user"; // Default para retry
            }
            request.setEntityType(entityType);
            
            String entityId = (String) eventData.get("entityId");
            if (entityId == null) {
                entityId = notification.userId; // Usar userId como fallback
            }
            request.setEntityId(entityId);
            
            request.setChannels(List.of(notification.channel));
            request.setRecipients(List.of(recipient.getRecipientType() != null ? 
                                         recipient.getRecipientType() : "manual"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>) eventData.get("context");
            request.setContext(context != null ? context : new HashMap<>());
            
            // Reenviar a notificação
            sendNotification(notification, recipient, request);
            
            // Atualizar status
            notification.status = "sent";
            notification.sentAt = Instant.now();
            notificationRepository.persist(notification);
            
            LOG.info("Notification {} retried successfully", notification.id);
            
        } catch (Exception e) {
            LOG.error("Retry failed for notification {}: {}", notification.id, e.getMessage(), e);
            
            notification.status = "error";
            notification.errorMessage = "Retry failed: " + e.getMessage();
            notificationRepository.persist(notification);
            
            throw new RuntimeException("Failed to retry notification: " + e.getMessage(), e);
        }
    }
    
    // Os métodos auxiliares continuam os mesmos, mas agora usam WorkflowNotificationRequest do api.dto
    private void sendNotification(Notification notification, RecipientResolution recipient, 
                                 WorkflowNotificationRequest request) {
        String channel = notification.channel;
        
        LOG.debug("Sending {} notification to {} via {}", 
                 request.getEventType(), recipient.getEmail(), channel);
        
        switch (channel.toLowerCase()) {
            case "email":
                emailService.sendEmail(recipient, request);
                break;
            case "whatsapp":
                if (recipient.getPhone() != null && !recipient.getPhone().isEmpty()) {
                    whatsappService.sendMessage(recipient, request);
                } else {
                    LOG.warn("Cannot send WhatsApp to {}: no phone number", recipient.getEmail());
                    throw new IllegalArgumentException("Recipient has no phone number for WhatsApp");
                }
                break;
            case "in_app":
                // Implementar notificação in-app
                LOG.info("In-app notification would be sent to: {}", recipient.getEmail());
                break;
            case "sms":
                LOG.info("SMS notification would be sent to: {}", recipient.getPhone());
                break;
            default:
                throw new IllegalArgumentException("Unsupported channel: " + channel);
        }
    }
    
    private Notification createNotification(RecipientResolution recipient, 
                                          WorkflowNotificationRequest request, 
                                          String channel) {
        Notification notification = new Notification();
        
        notification.userId = recipient.getUserId();
        notification.eventType = request.getEventType();
        notification.channel = channel;
        notification.status = "pending";
        notification.createdAt = Instant.now();
        
        // Criar payload JSON usando Jackson
        notification.payloadJson = createJsonPayload(recipient, request);
        
        LOG.debug("Created notification for user {}", recipient.getUserId());
        return notification;
    }
    
    private String createJsonPayload(RecipientResolution recipient, WorkflowNotificationRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            
            // Recipient data
            Map<String, Object> recipientMap = new HashMap<>();
            recipientMap.put("userId", recipient.getUserId());
            recipientMap.put("email", recipient.getEmail());
            recipientMap.put("name", recipient.getName());
            recipientMap.put("recipientType", recipient.getRecipientType());
            recipientMap.put("phone", recipient.getPhone());
            recipientMap.put("metadata", recipient.getMetadata());
            
            // Event data
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("type", request.getEventType());
            eventMap.put("entityType", request.getEntityType());
            eventMap.put("entityId", request.getEntityId());
            eventMap.put("context", request.getContext());
            eventMap.put("timestamp", Instant.now().toString());
            
            payload.put("recipient", recipientMap);
            payload.put("event", eventMap);
            payload.put("retryAttempts", 0);
            payload.put("createdAt", Instant.now().toString());
            
            return objectMapper.writeValueAsString(payload);
            
        } catch (Exception e) {
            LOG.error("Error creating JSON payload: {}", e.getMessage());
            // Fallback para método manual
            return createFallbackPayload(recipient, request);
        }
    }
    
    private String createFallbackPayload(RecipientResolution recipient, WorkflowNotificationRequest request) {
        // Método fallback se o Jackson falhar
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
        
        if (recipient.getMetadata() != null && !recipient.getMetadata().isEmpty()) {
            json.append(",\"metadata\": {");
            boolean first = true;
            for (var entry : recipient.getMetadata().entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\": ");
                if (entry.getValue() instanceof String) {
                    json.append("\"").append(entry.getValue()).append("\"");
                } else {
                    json.append(entry.getValue());
                }
                first = false;
            }
            json.append("}");
        }
        
        json.append("},");
        json.append("\"event\": {");
        json.append("\"type\": \"").append(request.getEventType()).append("\",");
        json.append("\"entityType\": \"").append(request.getEntityType()).append("\",");
        json.append("\"entityId\": \"").append(request.getEntityId()).append("\"");
        
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            json.append(",\"context\": {");
            boolean first = true;
            for (var entry : request.getContext().entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\": ");
                if (entry.getValue() instanceof String) {
                    json.append("\"").append(entry.getValue()).append("\"");
                } else {
                    json.append(entry.getValue());
                }
                first = false;
            }
            json.append("}");
        }
        
        json.append("},");
        json.append("\"retryAttempts\": 0,");
        json.append("\"createdAt\": \"").append(Instant.now().toString()).append("\"");
        json.append("}");
        
        return json.toString();
    }
    
    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            LOG.error("Error parsing JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Transactional
    public void cleanupOldNotifications(int daysToKeep) {
        Instant cutoffDate = Instant.now().minusSeconds(daysToKeep * 24 * 60 * 60L);
        
        long deletedCount = notificationRepository.delete("createdAt < ?1 and status in ('sent', 'error')", 
                                                         cutoffDate);
        
        LOG.info("Cleaned up {} old notifications older than {} days", deletedCount, daysToKeep);
    }
    
    // Método alternativo com cache (retorna algo para poder usar @CacheResult)
    @io.quarkus.cache.CacheResult(cacheName = "notification-status-cache")
    public String getNotificationStatusWithCache(Long notificationId) {
        var notification = notificationRepository.findById(notificationId);
        return notification != null ? notification.status : "not_found";
    }
}