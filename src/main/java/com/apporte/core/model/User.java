package com.apporte.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users_cache")
public class User extends PanacheEntityBase {
    
    @Id
    public String id;
    
    @Column(name = "email", nullable = false, unique = true)
    public String email;
    
    @Column(name = "name")
    public String name;
    
    @Column(name = "phone")
    public String phone;
    
    @Column(name = "roles_json", columnDefinition = "TEXT")
    public String rolesJson;
    
    @Column(name = "last_sync")
    public Instant lastSync;
    
    @Column(name = "created_at")
    public Instant createdAt;
    
    public User() {}
    
    public User(String id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.createdAt = Instant.now();
        this.lastSync = Instant.now();
    }
}