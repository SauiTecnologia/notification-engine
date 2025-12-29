package com.apporte.core.repository;

import com.apporte.core.model.Project;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class ProjectRepository implements PanacheRepository<Project> {
    
    public Optional<Project> findProjectById(String projectId) {
        return find("id", projectId).firstResultOptional();
    }
    
    public Optional<String> findProjectOwnerId(String projectId) {
        return find("id", projectId)
                .firstResultOptional()
                .map(project -> project.ownerId);
    }
    
    public Optional<String> findProjectOwnerEmail(String projectId) {
        return find("id", projectId)
                .firstResultOptional()
                .map(project -> project.ownerEmail);
    }
}