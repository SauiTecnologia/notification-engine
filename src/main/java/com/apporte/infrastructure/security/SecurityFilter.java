package com.apporte.infrastructure.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityFilter implements ContainerRequestFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(SecurityFilter.class);
    
    @Context
    UriInfo uriInfo;
    
    @Inject
    JsonWebToken jwt; // Pode ser null em testes!
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = uriInfo.getPath();
        
        boolean isJwtValid = false;
        String username = null;
        
        try {
            // Método SEGURO para verificar se é JWT válido
            if (jwt != null) {
                // Tenta acessar uma claim que sempre existe em JWT válidos
                Object rawToken = jwt.getRawToken();
                if (rawToken != null && !rawToken.toString().isEmpty()) {
                    isJwtValid = true;
                    username = jwt.getClaim("preferred_username");
                    if (username == null) {
                        username = jwt.getClaim("sub");
                    }
                }
            }
        } catch (Exception e) {
            isJwtValid = false;
        }
        
        if (isJwtValid && username != null) {
            LOG.debug("Authenticated JWT request from {} to {}", username, path);
            // Adicione informações JWT se necessário
            requestContext.setProperty("jwtUser", username);
        } else {
            LOG.debug("Non-JWT request to {} (test environment or basic auth)", path);
        }
    }
}