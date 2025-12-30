package com.apporte.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Entidade que representa um projeto para resolver proprietários em notificações.
 * Mantém informações de proprietário em cache para performance.
 */
@Entity
@Table(name = "projects")
public class Project extends PanacheEntityBase {
    
    @Id
    private String id;
    
    @Column(name = "owner_id", nullable = false)
    private String ownerId;
    
    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;
    
    @Column(name = "owner_name")
    private String ownerName;
    
    // Constructors
    public Project() {}
    
    public Project(String id, String ownerId, String ownerEmail, String ownerName) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId cannot be null");
        this.ownerEmail = Objects.requireNonNull(ownerEmail, "ownerEmail cannot be null");
        this.ownerName = ownerName;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public String getOwnerEmail() {
        return ownerEmail;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    // Setters
    public void setId(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId cannot be null");
    }
    
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = Objects.requireNonNull(ownerEmail, "ownerEmail cannot be null");
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", ownerName='" + ownerName + '\'' +
                '}';
    }
}