package com.apporte.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project extends PanacheEntityBase {
    
    @Id
    public String id;
    
    @Column(name = "owner_id", nullable = false)
    public String ownerId;
    
    @Column(name = "owner_email", nullable = false)
    public String ownerEmail;
    
    @Column(name = "owner_name")
    public String ownerName;
    
    // Construtor padrão necessário
    public Project() {}
    
    // Construtor conveniente
    public Project(String id, String ownerId, String ownerEmail, String ownerName) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
    }
}