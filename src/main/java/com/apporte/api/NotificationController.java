package com.apporte.api;

import com.apporte.api.dto.*;
import com.apporte.api.util.ResponseBuilder;
import com.apporte.core.model.Notification;
import com.apporte.core.service.NotificationService;
import com.apporte.infrastructure.security.KeycloakUserContext;
import com.apporte.infrastructure.security.UserContext;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * API REST para gerenciamento de notificações.
 * Responsável por receber requisições e coordenar o envio de notificações.
 */
@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class NotificationController {
    
    private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);
    
    private final NotificationService notificationService;
    private final KeycloakUserContext keycloakUserContext;
    
    public NotificationController(NotificationService notificationService, KeycloakUserContext keycloakUserContext) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService cannot be null");
        this.keycloakUserContext = Objects.requireNonNull(keycloakUserContext, "keycloakUserContext cannot be null");
    }
    
    @GET
    @io.quarkus.security.PermitAll
    public Response healthCheck() {
        return ResponseBuilder.ok(Map.of(
            "message", "Notification Engine is running!",
            "status", "healthy",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    @POST
    @Path("/from-workflow")
    @RolesAllowed({"notification-sender", "notification-admin", "system-admin"})
    public Response processWorkflowNotification(@Valid WorkflowNotificationRequest request) {
        UserContext user = keycloakUserContext.getCurrentUser()
            .orElseThrow(() -> new WebApplicationException("User not authenticated", Response.Status.UNAUTHORIZED));
        LOG.info("Received workflow notification: {} from user: {}", request.eventType(), user.email());
        String requestId = ResponseBuilder.generateRequestId();
        
        try {
            notificationService.processWorkflowNotification(request);
            
            return ResponseBuilder.accepted(
                "accepted",
                "Workflow notification processing started",
                Map.of(
                    "eventType", request.eventType(),
                    "entityId", request.entityId(),
                    "requestId", requestId
                )
            );
            
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid workflow notification request: {}", e.getMessage());
            return ResponseBuilder.badRequest(e.getMessage());
            
        } catch (Exception e) {
            LOG.error("Error processing workflow notification: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to process notification", e);
        }
    }
    
    @POST
    @Path("/send")
    @RolesAllowed({"notification-sender", "notification-admin", "system-admin"})
    public Response sendNotification(@Valid SimpleNotificationRequest request) {
        UserContext user = keycloakUserContext.getCurrentUser()
            .orElseThrow(() -> new WebApplicationException("User not authenticated", Response.Status.UNAUTHORIZED));
        LOG.info("Manual notification: {} to {} from user: {}", request.eventType(), request.recipientId(), user.email());
        String requestId = ResponseBuilder.generateRequestId();
        
        try {
            WorkflowNotificationRequest workflowRequest = convertToWorkflowRequest(request);
            notificationService.processWorkflowNotification(workflowRequest);
            
            return ResponseBuilder.accepted(
                "accepted",
                "Notification accepted for processing",
                Map.of(
                    "eventType", request.eventType(),
                    "recipientId", request.recipientId(),
                    "requestId", requestId
                )
            );
            
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid notification request: {}", e.getMessage());
            return ResponseBuilder.badRequest(e.getMessage());
            
        } catch (Exception e) {
            LOG.error("Error sending notification: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to send notification", e);
        }
    }
    
    @POST
    @Path("/batch")
    @RolesAllowed({"notification-sender", "notification-admin", "system-admin"})
    public Response sendBatchNotifications(@Valid BatchNotificationRequest batchRequest) {
        UserContext user = keycloakUserContext.getCurrentUser()
            .orElseThrow(() -> new WebApplicationException("User not authenticated", Response.Status.UNAUTHORIZED));
        LOG.info("Batch notification with {} items from user: {}", batchRequest.notifications().size(), user.email());
        
        if (batchRequest.notifications().isEmpty()) {
            return ResponseBuilder.badRequest("Batch cannot be empty");
        }
        
        int successCount = 0;
        int errorCount = 0;
        var results = new ArrayList<Map<String, Object>>();
        
        for (var request : batchRequest.notifications()) {
            try {
                WorkflowNotificationRequest workflowRequest = convertToWorkflowRequest(request);
                notificationService.processWorkflowNotification(workflowRequest);
                successCount++;
                
                results.add(Map.of(
                    "eventType", request.eventType(),
                    "recipientId", request.recipientId(),
                    "status", "success"
                ));
                
            } catch (Exception e) {
                errorCount++;
                LOG.warn("Error processing batch item: {}", e.getMessage());
                results.add(Map.of(
                    "eventType", request.eventType(),
                    "recipientId", request.recipientId(),
                    "status", "error",
                    "error", e.getMessage()
                ));
            }
        }
        
        return ResponseBuilder.ok(Map.of(
            "status", "batch_processed",
            "message", String.format("Processed %d notifications: %d success, %d errors",
                    batchRequest.notifications().size(), successCount, errorCount),
            "data", Map.of(
                "total", batchRequest.notifications().size(),
                "success", successCount,
                "errors", errorCount,
                "results", results
            )
        ));
    }
    
    @GET
    @Path("/status/{id}")
    @RolesAllowed({"notification-viewer", "notification-sender", "notification-admin", "system-admin"})
    public Response getNotificationStatus(@PathParam("id") String id) {
        try {
            Long notificationId = parseId(id);
            if (notificationId == null) {
                return ResponseBuilder.badRequest("Invalid notification ID format");
            }
            
            Notification notification = notificationService.findNotificationById(notificationId);
            
            if (notification == null) {
                return ResponseBuilder.notFound("Notification not found: " + id);
            }
            
            return ResponseBuilder.ok(buildNotificationStatusResponse(notification));
            
        } catch (Exception e) {
            LOG.error("Error getting notification status: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Error retrieving notification", e);
        }
    }
    
    @GET
    @Path("/user/{userId}")
    @RolesAllowed({"notification-viewer", "notification-sender", "notification-admin", "system-admin"})
    public Response getUserNotifications(
            @PathParam("userId") String userId,
            @QueryParam("status") String status,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        
        UserContext user = keycloakUserContext.getCurrentUser()
            .orElseThrow(() -> new WebApplicationException("User not authenticated", Response.Status.UNAUTHORIZED));
        
        // Usuários só podem ver suas próprias notificações, exceto admins
        if (!user.isAdmin() && !user.userId().equals(userId)) {
            LOG.warn("User {} tried to access notifications of user {}", user.email(), userId);
            return ResponseBuilder.forbidden("You can only access your own notifications");
        }
        
        Objects.requireNonNull(userId, "userId cannot be null");
        
        if (limit <= 0 || limit > 100) {
            return ResponseBuilder.badRequest("Limit must be between 1 and 100");
        }
        
        try {
            List<Notification> notifications = notificationService.getUserNotifications(userId, status, limit);
            
            return ResponseBuilder.ok(Map.of(
                "userId", userId,
                "status", status != null ? status : "all",
                "notifications", notifications,
                "count", notifications.size()
            ));
            
        } catch (Exception e) {
            LOG.error("Error getting user notifications: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Error retrieving notifications", e);
        }
    }
    
    // ========== Métodos auxiliares privados ==========
    
    /**
     * Converte SimpleNotificationRequest para WorkflowNotificationRequest.
     */
    private WorkflowNotificationRequest convertToWorkflowRequest(SimpleNotificationRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(request.recipientId(), "recipientId cannot be null");
        Objects.requireNonNull(request.channel(), "channel cannot be null");
        Objects.requireNonNull(request.eventType(), "eventType cannot be null");
        
        return new WorkflowNotificationRequest(
            request.eventType(),
            "user",
            request.recipientId(),
            List.of(request.channel()),
            List.of("manual"),
            request.context()
        );
    }
    
    /**
     * Parsa String como Long, retornando null se inválido.
     */
    private Long parseId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            LOG.debug("Invalid ID format: {}", id);
            return null;
        }
    }
    
    /**
     * Constrói resposta com status da notificação.
     */
    private Map<String, Object> buildNotificationStatusResponse(Notification notification) {
        return Map.of(
            "id", notification.getId(),
            "eventType", notification.getEventType(),
            "channel", notification.getChannel(),
            "userId", notification.getUserId(),
            "status", notification.getStatus(),
            "createdAt", notification.getCreatedAt(),
            "sentAt", notification.getSentAt(),
            "errorMessage", notification.getErrorMessage() != null ? notification.getErrorMessage() : ""
        );
    }
}
