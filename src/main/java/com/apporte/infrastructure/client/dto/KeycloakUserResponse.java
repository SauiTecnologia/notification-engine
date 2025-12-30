package com.apporte.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Record para resposta de usuário do Keycloak.
 * Desserializa automaticamente de JSON com mapeamento de nomes de campos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUserResponse(
    @JsonProperty("id")
    String id,
    
    @JsonProperty("username")
    String username,
    
    @JsonProperty("email")
    String email,
    
    @JsonProperty("firstName")
    String firstName,
    
    @JsonProperty("lastName")
    String lastName,
    
    @JsonProperty("enabled")
    Boolean enabled,
    
    @JsonProperty("emailVerified")
    Boolean emailVerified,
    
    @JsonProperty("attributes")
    Map<String, List<String>> attributes,
    
    @JsonProperty("realmRoles")
    List<String> realmRoles,
    
    @JsonProperty("clientRoles")
    Map<String, List<String>> clientRoles
) {
    
    /**
     * Retorna o nome completo ou fallback para username.
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }
    
    /**
     * Extrai número de telefone dos atributos.
     */
    public String getPhoneNumber() {
        if (attributes != null && attributes.containsKey("phone")) {
            List<String> phones = attributes.get("phone");
            return phones != null && !phones.isEmpty() ? phones.get(0) : null;
        }
        return null;
    }
}
