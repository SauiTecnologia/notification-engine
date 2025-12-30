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
import java.util.Optional;

/**
 * Filtro de segurança que valida JWT tokens em requisições.
 * Integrado com Quarkus OIDC para autenticação via Keycloak.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityFilter implements ContainerRequestFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(SecurityFilter.class);
    
    @Context
    UriInfo uriInfo;
    
    @Inject
    JsonWebToken jwt; // Pode ser null em testes ou requisições não-autenticadas
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = uriInfo.getPath();
        
        String username = extractUsername();
        
        if (username != null) {
            LOG.debug("Authenticated request from {} to {}", username, path);
            requestContext.setProperty("jwtUser", username);
            requestContext.setProperty("authenticated", true);
        } else {
            LOG.debug("Unauthenticated or non-JWT request to {}", path);
            requestContext.setProperty("authenticated", false);
        }
    }
    
    /**
     * Extrai o nome do usuário do JWT.
     * Tenta usar 'preferred_username' primeiro, depois 'sub' como fallback.
     */
    private String extractUsername() {
        if (jwt == null) {
            return null;
        }
        
        try {
            // Validar se o JWT é válido
            if (!isJwtValid()) {
                return null;
            }
            
            // Tentar obter preferred_username primeiro
            String username = getClaimAsString("preferred_username");
            if (username != null && !username.isEmpty()) {
                return username;
            }
            
            // Fallback para 'sub' (subject)
            return getClaimAsString("sub");
            
        } catch (Exception e) {
            LOG.debug("Could not extract username from JWT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtém um claim do JWT como String.
     */
    private String getClaimAsString(String claimName) {
        try {
            Object claimValue = jwt.getClaim(claimName);
            if (claimValue != null) {
                return claimValue.toString();
            }
        } catch (Exception e) {
            LOG.trace("Could not get claim '{}': {}", claimName, e.getMessage());
        }
        return null;
    }
    
    /**
     * Valida se o JWT é válido e ativo.
     */
    private boolean isJwtValid() {
        try {
            // Verificar se o token bruto não é nulo
            Object rawToken = jwt.getRawToken();
            if (rawToken == null) {
                return false;
            }
            
            String tokenStr = rawToken.toString();
            
            // Validação básica: JWT tem 3 partes separadas por ponto
            if (tokenStr.isEmpty() || tokenStr.split("\\.").length != 3) {
                return false;
            }
            
            // Token é válido se passou pelas validações básicas
            return true;
            
        } catch (Exception e) {
            LOG.trace("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Retorna o usuário autenticado atual, se houver.
     */
    public Optional<String> getCurrentUser() {
        return Optional.ofNullable(extractUsername());
    }
}
