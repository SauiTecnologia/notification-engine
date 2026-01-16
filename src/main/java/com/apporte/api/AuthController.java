package com.apporte.api;

import com.apporte.infrastructure.security.KeycloakUserContext;
import com.apporte.infrastructure.security.UserContext;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para endpoints de autenticação e informações do usuário.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {
    
    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);
    
    private final KeycloakUserContext keycloakUserContext;
    
    @Inject
    public AuthController(KeycloakUserContext keycloakUserContext) {
        this.keycloakUserContext = keycloakUserContext;
    }
    
    /**
     * Retorna informações do usuário autenticado.
     */
    @GET
    @Path("/me")
    @Authenticated
    public Response getUserInfo() {
        Optional<UserContext> userOpt = keycloakUserContext.getCurrentUser();
        
        if (userOpt.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "User not authenticated"))
                    .build();
        }
        
        UserContext user = userOpt.get();
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.userId());
        userInfo.put("username", user.username());
        userInfo.put("email", user.email());
        userInfo.put("name", user.name());
        userInfo.put("roles", user.roles());
        userInfo.put("isAdmin", user.isAdmin());
        
        LOG.info("User info requested for: {}", user.username());
        
        return Response.ok(userInfo).build();
    }
    
    /**
     * Health check (sem autenticação).
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
                "service", "notification-engine",
                "auth", "keycloak",
                "status", "healthy"
        )).build();
    }
    
    /**
     * Endpoint de teste - apenas admins.
     */
    @GET
    @Path("/admin-only")
    @RolesAllowed("notification-admin")
    public Response adminOnly() {
        return Response.ok(Map.of(
                "message", "You are an admin!",
                "timestamp", System.currentTimeMillis()
        )).build();
    }
}
