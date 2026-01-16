package com.apporte.infrastructure.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Serviço para extrair contexto do usuário autenticado via Keycloak JWT.
 */
@ApplicationScoped
public class KeycloakUserContext {
    
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakUserContext.class);
    
    private final SecurityIdentity securityIdentity;
    private final JsonWebToken jwt;
    
    @Inject
    public KeycloakUserContext(SecurityIdentity securityIdentity, JsonWebToken jwt) {
        this.securityIdentity = Objects.requireNonNull(securityIdentity, "securityIdentity cannot be null");
        this.jwt = jwt; // Pode ser null em requisições não autenticadas
    }
    
    /**
     * Retorna o contexto do usuário autenticado atual.
     * @return Optional com UserContext se autenticado, empty caso contrário
     */
    public Optional<UserContext> getCurrentUser() {
        if (!securityIdentity.isAnonymous() && jwt != null) {
            try {
                return Optional.of(extractUserContext());
            } catch (Exception e) {
                LOG.error("Failed to extract user context from JWT", e);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Extrai o contexto do usuário do JWT.
     */
    private UserContext extractUserContext() {
        String userId = getClaimAsString("sub");
        String username = getClaimAsString("preferred_username", "email", "sub");
        String email = getClaimAsString("email");
        String name = getClaimAsString("name");
        Set<String> roles = extractRoles();
        
        return new UserContext(userId, username, email, name, roles);
    }
    
    /**
     * Extrai as roles do claim 'realm_access.roles'.
     */
    private Set<String> extractRoles() {
        try {
            Object realmAccess = jwt.getClaim("realm_access");
            
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
                Object rolesObj = realmAccessMap.get("roles");
                
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) rolesObj;
                    return new HashSet<>(rolesList);
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not extract roles from JWT: {}", e.getMessage());
        }
        
        return Collections.emptySet();
    }
    
    /**
     * Obtém um claim do JWT, tentando múltiplos nomes de claim.
     */
    private String getClaimAsString(String... claimNames) {
        for (String claimName : claimNames) {
            try {
                Object claimValue = jwt.getClaim(claimName);
                if (claimValue != null) {
                    String value = claimValue.toString();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            } catch (Exception e) {
                LOG.trace("Could not get claim '{}': {}", claimName, e.getMessage());
            }
        }
        return null;
    }
}
