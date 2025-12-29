package com.apporte.core.repository;

import com.apporte.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    
    public Optional<User> findByKeycloakId(String keycloakId) {
        return find("id", keycloakId).firstResultOptional();
    }
    
    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
    
    public List<User> findUsersByRole(String role) {
        // Correção: usar query JPQL correta
        return find("rolesJson like ?1", "%\"" + role + "\"%").list();
    }
    
    public List<User> findAdmins() {
        // Correção: query mais simples
        return find("""
            rolesJson like ?1 or rolesJson like ?2 or rolesJson like ?3
            """, "%\"admin\"%", "%\"notification-admin\"%", "%\"supervisor\"%")
            .list();
    }
    
    // Método alternativo usando PanacheQuery
    public List<User> findAdminsAlternative() {
        return list("FROM User u WHERE " +
                   "u.rolesJson LIKE '%\"admin\"%' OR " +
                   "u.rolesJson LIKE '%\"notification-admin\"%' OR " +
                   "u.rolesJson LIKE '%\"supervisor\"%'");
    }
}