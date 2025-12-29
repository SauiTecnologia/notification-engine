package com.apporte.infrastructure.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Provider
@RequiresRole
@Priority(Priorities.AUTHORIZATION)
public class RequiresRoleFilter implements ContainerRequestFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(RequiresRoleFilter.class);
    
    @Inject
    SecurityIdentity securityIdentity;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Buscar annotation no resource method
        var resourceInfo = requestContext.getUriInfo().getMatchedResources();
        if (resourceInfo == null || resourceInfo.isEmpty()) {
            return;
        }
        
        // Simples implementação - em produção, usar reflection mais robusta
        String path = requestContext.getUriInfo().getPath();
        LOG.debug("Checking role requirements for path: {}", path);
        
        // Implementação simplificada - em produção usar framework de segurança do Quarkus
        // Por enquanto, apenas log
        if (securityIdentity != null && securityIdentity.getPrincipal() != null) {
            LOG.debug("User {} accessing {}", securityIdentity.getPrincipal().getName(), path);
        }
    }
    
    public static class ErrorResponse {
        public String code;
        public String message;
        
        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}