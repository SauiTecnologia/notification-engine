package com.apporte.infrastructure.security;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Contexto do usuário autenticado.
 * Record imutável que encapsula informações extraídas do JWT.
 * 
 * @param userId ID único do usuário (claim 'sub')
 * @param username Username/email (claim 'preferred_username' ou 'email')
 * @param email Email do usuário
 * @param name Nome completo do usuário
 * @param roles Conjunto de roles do usuário (claim 'realm_access.roles')
 */
public record UserContext(
    String userId,
    String username,
    String email,
    String name,
    Set<String> roles
) {
    
    /**
     * Construtor compacto com validação.
     */
    public UserContext {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        
        // Garantir que roles nunca seja null
        if (roles == null) {
            roles = Collections.emptySet();
        }
        
        // Criar cópia imutável do Set
        roles = Set.copyOf(roles);
    }
    
    /**
     * Verifica se o usuário tem a role especificada.
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Verifica se o usuário tem alguma das roles especificadas.
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verifica se o usuário é administrador.
     */
    public boolean isAdmin() {
        return hasRole("notification-admin") || hasRole("admin");
    }
}
