package com.apporte.api;

import com.apporte.api.dto.*;
import com.apporte.core.service.NotificationService;
import com.apporte.core.repository.NotificationRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationController {
    
    private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);
    
    @Inject
    NotificationService notificationService;
    
    @Inject
    NotificationRepository notificationRepository;
    
    @GET
    public Response healthCheck() {
        return Response.ok(Map.of(
            "message", "Notification Engine is running!",
            "status", "healthy",
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @POST
    @Path("/from-workflow")
    public Response processWorkflowNotification(@Valid WorkflowNotificationRequest request) {
        LOG.info("Received workflow notification: {}", request.getEventType());
        
        try {
            // O NotificationService espera o core.model.WorkflowNotificationRequest
            // Precisamos converter ou atualizar o service
            notificationService.processWorkflowNotification(request);
            
            return Response.accepted(new NotificationResponse(
                "accepted",
                "Workflow notification processing started",
                Map.of(
                    "eventType", request.getEventType(),
                    "entityId", request.getEntityId(),
                    "requestId", UUID.randomUUID().toString()
                )
            )).build();
            
        } catch (Exception e) {
            LOG.error("Error processing workflow notification", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new NotificationResponse(
                              "error",
                              "Failed to process workflow notification: " + e.getMessage(),
                              null
                          ))
                          .build();
        }
    }
    
    @POST
    @Path("/send")
    public Response sendNotification(@Valid SimpleNotificationRequest request) {
        LOG.info("Manual notification: {} to {}", request.getEventType(), request.getRecipientId());
        
        try {
            // Converter para WorkflowNotificationRequest
            WorkflowNotificationRequest workflowRequest = new WorkflowNotificationRequest();
            workflowRequest.setEventType(request.getEventType());
            workflowRequest.setEntityType("user");
            workflowRequest.setEntityId(request.getRecipientId());
            workflowRequest.setChannels(List.of(request.getChannel()));
            workflowRequest.setRecipients(List.of("manual"));
            workflowRequest.setContext(request.getContext());
            
            notificationService.processWorkflowNotification(workflowRequest);
            
            return Response.accepted(new NotificationResponse(
                "accepted",
                "Notification accepted for processing",
                Map.of(
                    "eventType", request.getEventType(),
                    "recipientId", request.getRecipientId(),
                    "requestId", UUID.randomUUID().toString()
                )
            )).build();
            
        } catch (Exception e) {
            LOG.error("Error sending notification", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new NotificationResponse(
                              "error",
                              "Failed to send notification: " + e.getMessage(),
                              null
                          ))
                          .build();
        }
    }
    
    @POST
    @Path("/batch")
    public Response sendBatchNotifications(@Valid BatchNotificationRequest batchRequest) {
        LOG.info("Batch notification with {} items", batchRequest.getNotifications().size());
        
        int successCount = 0;
        int errorCount = 0;
        var results = new java.util.ArrayList<Map<String, Object>>();
        
        for (var request : batchRequest.getNotifications()) {
            try {
                WorkflowNotificationRequest workflowRequest = new WorkflowNotificationRequest();
                workflowRequest.setEventType(request.getEventType());
                workflowRequest.setEntityType("user");
                workflowRequest.setEntityId(request.getRecipientId());
                workflowRequest.setChannels(List.of(request.getChannel()));
                workflowRequest.setRecipients(List.of("manual"));
                workflowRequest.setContext(request.getContext());
                
                notificationService.processWorkflowNotification(workflowRequest);
                successCount++;
                
                results.add(Map.of(
                    "eventType", request.getEventType(),
                    "recipientId", request.getRecipientId(),
                    "status", "success"
                ));
            } catch (Exception e) {
                errorCount++;
                results.add(Map.of(
                    "eventType", request.getEventType(),
                    "recipientId", request.getRecipientId(),
                    "status", "error",
                    "error", e.getMessage()
                ));
            }
        }
        
        return Response.ok(new NotificationResponse(
            "batch_processed",
            String.format("Processed %d notifications: %d success, %d errors", 
                         batchRequest.getNotifications().size(), successCount, errorCount),
            Map.of(
                "total", batchRequest.getNotifications().size(),
                "success", successCount,
                "errors", errorCount,
                "results", results
            )
        )).build();
    }
    
    @GET
    @Path("/status/{id}")
    public Response getNotificationStatus(@PathParam("id") String id) {
        try {
            // Tenta encontrar por ID (pode ser UUID ou Long)
            var notification = findNotificationById(id);
            
            if (notification == null) {
                return Response.status(Response.Status.NOT_FOUND)
                              .entity(new NotificationResponse(
                                  "error",
                                  "Notification not found: " + id,
                                  Map.of("id", id)
                              ))
                              .build();
            }
            
            return Response.ok(createStatusResponse(notification)).build();
            
        } catch (Exception e) {
            LOG.error("Error getting notification status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new NotificationResponse(
                              "error",
                              "Error retrieving notification: " + e.getMessage(),
                              null
                          ))
                          .build();
        }
    }
    
    @GET
    @Path("/user/{userId}")
    public Response getUserNotifications(
            @PathParam("userId") String userId,
            @QueryParam("status") String status,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        
        var query = "userId = :userId";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        
        if (status != null && !status.isEmpty()) {
            query += " and status = :status";
            params.put("status", status);
        }
        
        var notifications = notificationRepository.find(query, params)
                .range(0, limit)
                .list();
        
        return Response.ok(Map.of(
            "userId", userId,
            "notifications", notifications,
            "count", notifications.size()
        )).build();
    }
    
    // Métodos auxiliares
    private com.apporte.core.model.Notification findNotificationById(String id) {
        try {
            // Tenta como Long primeiro
            Long longId = Long.parseLong(id);
            return notificationRepository.findById(longId);
        } catch (NumberFormatException e1) {
            try {
                // Tenta como UUID
                UUID uuid = UUID.fromString(id);
                return notificationRepository.find("uuid", uuid).firstResult();
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }
    
    private Map<String, Object> createStatusResponse(com.apporte.core.model.Notification notification) {
        Map<String, Object> status = new HashMap<>();
        status.put("id", notification.id);
        status.put("eventType", notification.eventType);
        status.put("channel", notification.channel);
        status.put("userId", notification.userId);
        status.put("status", notification.status);
        status.put("createdAt", notification.createdAt);
        status.put("sentAt", notification.sentAt);
        status.put("errorMessage", notification.errorMessage);
        
        return status;
    }
}