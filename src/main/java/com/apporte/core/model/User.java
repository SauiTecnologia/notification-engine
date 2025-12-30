package com.apporte.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidade que representa um usuário em cache sincronizado com Keycloak.
 * Mantém informações básicas do usuário para resolução de destinatários de notificações.
 */
@Entity
@Table(name = "users_cache")
public class User extends PanacheEntityBase {
    
    @Id
    private String id;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "roles_json", columnDefinition = "TEXT")
    private String rolesJson;
    
    @Column(name = "last_sync")
    private Instant lastSync;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    // Constructors
    public User() {}
    
    public User(String id, String email, String name) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.name = name;
        this.createdAt = Instant.now();
        this.lastSync = Instant.now();
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getRolesJson() {
        return rolesJson;
    }
    
    public Instant getLastSync() {
        return lastSync;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    // Setters
    public void setId(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }
    
    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "email cannot be null");
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public void setRolesJson(String rolesJson) {
        this.rolesJson = rolesJson;
    }
    
    public void setLastSync(Instant lastSync) {
        this.lastSync = Objects.requireNonNull(lastSync, "lastSync cannot be null");
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }
    
    public void updateLastSync() {
        this.lastSync = Instant.now();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}