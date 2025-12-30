package com.apporte.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Record para resposta de token do Keycloak.
 * Desserializa automaticamente de JSON com mapeamento de nomes de campos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakTokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    
    @JsonProperty("expires_in")
    Integer expiresIn,
    
    @JsonProperty("refresh_expires_in")
    Integer refreshExpiresIn,
    
    @JsonProperty("refresh_token")
    String refreshToken,
    
    @JsonProperty("token_type")
    String tokenType,
    
    @JsonProperty("id_token")
    String idToken,
    
    @JsonProperty("not-before-policy")
    Integer notBeforePolicy,
    
    @JsonProperty("session_state")
    String sessionState,
    
    @JsonProperty("scope")
    String scope
) {
    
    /**
     * Verifica se o token é válido e ativo.
     */
    public boolean isValid() {
        return accessToken != null && !accessToken.isEmpty() && 
               expiresIn != null && expiresIn > 0;
    }
}
