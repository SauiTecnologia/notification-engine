package com.apporte.core.service;

import com.apporte.infrastructure.client.dto.KeycloakTokenResponse;
import com.apporte.infrastructure.client.dto.KeycloakUserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class KeycloakService {
    
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakService.class);
    
    @ConfigProperty(name = "app.keycloak.admin.server-url")
    String keycloakAdminUrl;
    
    @ConfigProperty(name = "app.keycloak.admin.username")
    String adminUsername;
    
    @ConfigProperty(name = "app.keycloak.admin.password")
    String adminPassword;
    
    @ConfigProperty(name = "app.keycloak.admin.client-id", defaultValue = "admin-cli")
    String clientId;
    
    private KeycloakTokenResponse cachedToken;
    private long tokenExpiry;
    
    public Optional<KeycloakUserResponse> getUserById(String userId) {
        String token = getAdminToken();
        
        if (token == null) {
            LOG.warn("Cannot get admin token for Keycloak");
            return Optional.empty();
        }
        
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(keycloakAdminUrl + "/users/" + userId)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .get();
            
            if (response.getStatus() == 200) {
                KeycloakUserResponse user = response.readEntity(KeycloakUserResponse.class);
                return Optional.ofNullable(user);
            } else {
                LOG.warn("Failed to get user from Keycloak: HTTP {}", response.getStatus());
            }
        } catch (Exception e) {
            LOG.error("Error fetching user from Keycloak: {}", e.getMessage(), e);
        }
        
        return Optional.empty();
    }
    
    public Optional<String> getUserEmail(String userId) {
        return getUserById(userId)
                .map(KeycloakUserResponse::getEmail);
    }
    
    public Optional<String> getUserPhone(String userId) {
        return getUserById(userId)
                .map(KeycloakUserResponse::getPhoneNumber);
    }
    
    public Optional<String> getUserFullName(String userId) {
        return getUserById(userId)
                .map(KeycloakUserResponse::getFullName);
    }
    
    public List<KeycloakUserResponse> getUsersByRole(String role) {
        // Implementar busca de usuários por role
        LOG.info("Getting users by role: {}", role);
        return Collections.emptyList();
    }
    
    public boolean validateUserToken(String token) {
        // Validação básica do token
        return token != null && token.startsWith("eyJ");
    }
    
    private String getAdminToken() {
        // Cache do token por 5 minutos
        if (cachedToken != null && cachedToken.isValid() && System.currentTimeMillis() < tokenExpiry) {
            return cachedToken.getAccessToken();
        }
        
        try (Client client = ClientBuilder.newClient()) {
            Form form = new Form()
                    .param("client_id", clientId)
                    .param("username", adminUsername)
                    .param("password", adminPassword)
                    .param("grant_type", "password");
            
            String tokenUrl = keycloakAdminUrl.replace("/admin/", "/protocol/openid-connect/token");
            Response response = client.target(tokenUrl)
                    .request(MediaType.APPLICATION_FORM_URLENCODED)
                    .post(Entity.form(form));
            
            if (response.getStatus() == 200) {
                KeycloakTokenResponse tokenResponse = response.readEntity(KeycloakTokenResponse.class);
                
                if (tokenResponse != null && tokenResponse.isValid()) {
                    cachedToken = tokenResponse;
                    // Calcular expiração (menos 1 minuto para margem de segurança)
                    tokenExpiry = System.currentTimeMillis() + 
                                 (tokenResponse.getExpiresIn() * 1000L) - 60000;
                    return cachedToken.getAccessToken();
                } else {
                    LOG.error("Invalid token response from Keycloak");
                }
            } else {
                LOG.error("Failed to get admin token: HTTP {}", response.getStatus());
                // Log do corpo da resposta para debug
                if (response.hasEntity()) {
                    String errorBody = response.readEntity(String.class);
                    LOG.error("Error response: {}", errorBody);
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting admin token: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    // Método para limpar cache (útil para testes ou quando o token é revogado)
    public void clearTokenCache() {
        cachedToken = null;
        tokenExpiry = 0;
        LOG.info("Keycloak token cache cleared");
    }
}