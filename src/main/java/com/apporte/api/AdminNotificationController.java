package com.apporte.api;

import com.apporte.core.service.NotificationService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Path("/api/admin/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin - Notifications", description = "API de administração de notificações")
@RolesAllowed({"admin", "notification-admin"})
public class AdminNotificationController {
    
    private static final Logger LOG = LoggerFactory.getLogger(AdminNotificationController.class);
    
    private final NotificationService notificationService;
    
    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @GET
    @Operation(summary = "Lista todas as notificações")
    public Response listNotifications(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status,
            @QueryParam("channel") String channel,
            @QueryParam("eventType") String eventType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        
        LOG.debug("Listing notifications - page: {}, size: {}, status: {}, channel: {}, eventType: {}",
                 page, size, status, channel, eventType);
        
        try {
            Instant start = null;
            Instant end = null;
            
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    start = LocalDateTime.parse(startDate).toInstant(ZoneOffset.UTC);
                } catch (Exception e) {
                    LOG.warn("Invalid start date format: {}", startDate);
                    return Response.status(Response.Status.BAD_REQUEST)
                                  .entity(Map.of("error", "Invalid start date format. Use: YYYY-MM-DDTHH:MM:SS"))
                                  .build();
                }
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    end = LocalDateTime.parse(endDate).toInstant(ZoneOffset.UTC);
                } catch (Exception e) {
                    LOG.warn("Invalid end date format: {}", endDate);
                    return Response.status(Response.Status.BAD_REQUEST)
                                  .entity(Map.of("error", "Invalid end date format. Use: YYYY-MM-DDTHH:MM:SS"))
                                  .build();
                }
            }
            
            var notifications = notificationService.listNotifications(
                    page, size, status, channel, eventType, start, end);
            
            long count = notificationService.countNotifications(
                    status, channel, eventType, start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications);
            response.put("page", page);
            response.put("size", size);
            response.put("total", count);
            response.put("totalPages", (int) Math.ceil((double) count / size));
            
            LOG.info("Returning {} notifications (page {} of {})", 
                    notifications.size(), page, (int) Math.ceil((double) count / size));
            
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error listing notifications: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(Map.of("error", "Failed to list notifications: " + e.getMessage()))
                          .build();
        }
    }
    
    @GET
    @Path("/{id}")
    @Operation(summary = "Busca notificação por ID")
    public Response getNotification(@PathParam("id") Long id) {
        LOG.debug("Getting notification by ID: {}", id);
        
        try {
            var notification = notificationService.findNotificationById(id);
            
            if (notification == null) {
                LOG.warn("Notification not found: {}", id);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity(Map.of("error", "Notification not found: " + id))
                              .build();
            }
            
            LOG.info("Found notification: {} - {}", id, notification.getEventType());
            return Response.ok(notification).build();
        } catch (Exception e) {
            LOG.error("Error getting notification: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(Map.of("error", "Failed to get notification: " + e.getMessage()))
                          .build();
        }
    }
    
    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Operation(summary = "Remove notificação (apenas admin)")
    public Response deleteNotification(@PathParam("id") Long id) {
        LOG.debug("Deleting notification: {}", id);
        
        try {
            var notification = notificationService.findNotificationById(id);
            
            if (notification == null) {
                LOG.warn("Notification not found for deletion: {}", id);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity(Map.of("error", "Notification not found: " + id))
                              .build();
            }
            
            notificationService.deleteNotification(id);
            LOG.info("Notification deleted successfully: {}", id);
            
            return Response.ok(Map.of(
                "message", "Notification deleted successfully",
                "id", id
            )).build();
            
        } catch (Exception e) {
            LOG.error("Error deleting notification {}: {}", id, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(Map.of("error", "Failed to delete notification: " + e.getMessage()))
                          .build();
        }
    }
    
    @POST
    @Path("/{id}/retry")
    @Operation(summary = "Reprocessa notificação com erro")
    public Response retryNotification(@PathParam("id") Long id) {
        LOG.debug("Retrying notification: {}", id);
        
        try {
            var notification = notificationService.findNotificationById(id);
            
            if (notification == null) {
                LOG.warn("Notification not found for retry: {}", id);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity(Map.of("error", "Notification not found"))
                              .build();
            }
            
            if (!"error".equals(notification.getStatus())) {
                LOG.warn("Notification {} is not in error state: {}", id, notification.getStatus());
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(Map.of("error", "Notification is not in error state"))
                              .build();
            }
            
            notificationService.retryNotification(notification);
            
            LOG.info("Notification {} scheduled for retry", id);
            
            return Response.ok(Map.of(
                "message", "Notification scheduled for retry",
                "originalId", id,
                "newStatus", "retrying"
            )).build();
            
        } catch (Exception e) {
            LOG.error("Error retrying notification {}: {}", id, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(Map.of("error", "Failed to retry notification: " + e.getMessage()))
                          .build();
        }
    }
    
    @GET
    @Path("/stats")
    @Operation(summary = "Estatísticas de notificações")
    public Response getStatistics(
            @QueryParam("days") @DefaultValue("7") int days) {
        
        LOG.debug("Getting statistics for last {} days", days);
        
        try {
            Map<String, Object> stats = notificationService.getStatistics(days);
            
            LOG.info("Statistics generated for {} days: total={}, sent={}, error={}", 
                    days, stats.get("total"), stats.get("sent"), stats.get("error"));
            
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            LOG.error("Error getting statistics: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(Map.of("error", "Failed to get statistics: " + e.getMessage()))
                          .build();
        }
    }
    
    @GET
    @Path("/health")
    @Operation(summary = "Health check do serviço de notificações")
    public Response healthCheck() {
        LOG.debug("Health check requested");
        
        Map<String, Object> health = notificationService.getHealthStatus();
        
        if ("UP".equals(health.get("status"))) {
            LOG.info("Health check: status UP");
            return Response.ok(health).build();
        } else {
            LOG.error("Health check: status DOWN - {}", health.get("error"));
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                          .entity(health)
                          .build();
        }
    }
}