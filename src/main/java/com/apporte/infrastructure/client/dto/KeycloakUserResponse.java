package com.apporte.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("emailVerified")
    private Boolean emailVerified;
    
    @JsonProperty("attributes")
    private Map<String, List<String>> attributes;
    
    @JsonProperty("realmRoles")
    private List<String> realmRoles;
    
    @JsonProperty("clientRoles")
    private Map<String, List<String>> clientRoles;
    
    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public Map<String, List<String>> getAttributes() { return attributes; }
    public void setAttributes(Map<String, List<String>> attributes) { this.attributes = attributes; }
    
    public List<String> getRealmRoles() { return realmRoles; }
    public void setRealmRoles(List<String> realmRoles) { this.realmRoles = realmRoles; }
    
    public Map<String, List<String>> getClientRoles() { return clientRoles; }
    public void setClientRoles(Map<String, List<String>> clientRoles) { this.clientRoles = clientRoles; }
    
    // Helper methods
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
    
    public String getPhoneNumber() {
        if (attributes != null && attributes.containsKey("phone")) {
            List<String> phones = attributes.get("phone");
            return phones != null && !phones.isEmpty() ? phones.get(0) : null;
        }
        return null;
    }
}