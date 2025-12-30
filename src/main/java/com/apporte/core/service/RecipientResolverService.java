package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.model.RecipientResolution;
import com.apporte.core.model.User;
import com.apporte.core.model.Project;
import com.apporte.core.repository.UserRepository;
import com.apporte.core.repository.ProjectRepository;
import com.apporte.infrastructure.client.dto.KeycloakUserResponse;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class RecipientResolverService {
    
    private static final Logger LOG = LoggerFactory.getLogger(RecipientResolverService.class);
    
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final KeycloakService keycloakService;
    
    public RecipientResolverService(UserRepository userRepository,
                                   ProjectRepository projectRepository,
                                   KeycloakService keycloakService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.keycloakService = keycloakService;
    }
    
    @CacheResult(cacheName = "recipients-cache")
    public List<RecipientResolution> resolveRecipients(WorkflowNotificationRequest request) {
        LOG.debug("Resolving recipients for event: {}, entity: {}", 
                 request.eventType(), request.entityId());
        
        List<RecipientResolution> recipients = new ArrayList<>();
        
        for (String recipientType : request.recipients()) {
            LOG.debug("Processing recipient type: {}", recipientType);
            
            switch (recipientType) {
                case "project_owner":
                    recipients.addAll(resolveProjectOwner(request.entityId()));
                    break;
                case "admins":
                    recipients.addAll(resolveAdmins());
                    break;
                case "workflow_participants":
                    recipients.addAll(resolveWorkflowParticipants(request.context()));
                    break;
                case "specific_users":
                    recipients.addAll(resolveSpecificUsers(request.context()));
                    break;
                case "manual":
                    recipients.addAll(resolveManualRecipient(request.entityId(), request.context()));
                    break;
                default:
                    LOG.warn("Unknown recipient type: {}", recipientType);
            }
        }
        
        LOG.info("Resolved {} total recipients for event {}", recipients.size(), request.eventType());
        return recipients;
    }
    
    private List<RecipientResolution> resolveProjectOwner(String projectId) {
        LOG.debug("Resolving project owner for project: {}", projectId);
        List<RecipientResolution> result = new ArrayList<>();
        
        try {
            Optional<Project> projectOpt = projectRepository.findProjectById(projectId);
            
            if (projectOpt.isEmpty()) {
                LOG.warn("Project not found: {}", projectId);
                return result;
            }
            
            Project project = projectOpt.get();
            LOG.debug("Found project owner: id={}, email={}, name={}", 
                     project.getOwnerId(), project.getOwnerEmail(), project.getOwnerName());
            
            // Buscar ou criar cache do usuário
            User user = getOrCreateUser(project.getOwnerId(), project.getOwnerEmail(), project.getOwnerName());
            
            RecipientResolution rr = new RecipientResolution();
            rr.setUserId(project.getOwnerId());
            rr.setEmail(user.getEmail());
            rr.setName(user.getName());
            rr.setPhone(user.getPhone());
            rr.setRecipientType("project_owner");
            rr.setMetadata(Map.of("project_id", projectId));
            
            result.add(rr);
            LOG.info("Resolved project owner: {} <{}>", rr.getName(), rr.getEmail());
            
        } catch (Exception e) {
            LOG.error("Error resolving project owner for project {}: {}", projectId, e.getMessage(), e);
        }
        
        return result;
    }
    
    private List<RecipientResolution> resolveAdmins() {
        LOG.debug("Resolving admins");
        List<RecipientResolution> result = new ArrayList<>();
        
        try {
            List<User> adminUsers = userRepository.findAdmins();
            
            for (User user : adminUsers) {
                RecipientResolution rr = new RecipientResolution();
                rr.setUserId(user.getId());
                rr.setEmail(user.getEmail());
                rr.setName(user.getName());
                rr.setPhone(user.getPhone());
                rr.setRecipientType("admin");
                rr.setMetadata(Map.of("role", "admin"));
                
                result.add(rr);
            }
            
            LOG.info("Resolved {} admins", result.size());
            
        } catch (Exception e) {
            LOG.error("Error resolving admins: {}", e.getMessage(), e);
            result.addAll(resolveAdminsFallback());
        }
        
        return result;
    }
    
    private List<RecipientResolution> resolveAdminsFallback() {
        List<RecipientResolution> result = new ArrayList<>();
        
        // Fallback para testes
        User adminUser = getOrCreateUser("admin-001", "admin@apporte.com", "Administrador");
        
        RecipientResolution rr = new RecipientResolution();
        rr.setUserId("admin-001");
        rr.setEmail(adminUser.getEmail());
        rr.setName(adminUser.getName());
        rr.setPhone(adminUser.getPhone());
        rr.setRecipientType("admin");
        rr.setMetadata(Map.of("role", "admin", "source", "fallback"));
        
        result.add(rr);
        LOG.warn("Using fallback admin user");
        return result;
    }
    
    private List<RecipientResolution> resolveWorkflowParticipants(Map<String, Object> context) {
        LOG.debug("Resolving workflow participants from context: {}", context);
        List<RecipientResolution> result = new ArrayList<>();
        
        if (context == null || context.isEmpty()) {
            return result;
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<String> participantIds = (List<String>) context.get("participant_ids");
            
            if (participantIds != null && !participantIds.isEmpty()) {
                for (String participantId : participantIds) {
                    Optional<User> userOpt = userRepository.findByKeycloakId(participantId);
                    
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        
                        RecipientResolution rr = new RecipientResolution();
                        rr.setUserId(user.getId());
                        rr.setEmail(user.getEmail());
                        rr.setName(user.getName());
                        rr.setPhone(user.getPhone());
                        rr.setRecipientType("workflow_participant");
                        rr.setMetadata(Map.of(
                            "context", "workflow",
                            "source", "context"
                        ));
                        
                        result.add(rr);
                    }
                }
            }
            
            LOG.info("Resolved {} workflow participants from context", result.size());
            
        } catch (Exception e) {
            LOG.error("Error resolving workflow participants: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    private List<RecipientResolution> resolveSpecificUsers(Map<String, Object> context) {
        LOG.debug("Resolving specific users from context");
        List<RecipientResolution> result = new ArrayList<>();
        
        if (context == null) {
            return result;
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<String> userEmails = (List<String>) context.get("user_emails");
            
            if (userEmails != null && !userEmails.isEmpty()) {
                for (String email : userEmails) {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        
                        RecipientResolution rr = new RecipientResolution();
                        rr.setUserId(user.getId());
                        rr.setEmail(user.getEmail());
                        rr.setName(user.getName());
                        rr.setPhone(user.getPhone());
                        rr.setRecipientType("specific_user");
                        rr.setMetadata(Map.of(
                            "source", "database",
                            "email_provided", email
                        ));
                        
                        result.add(rr);
                    }
                }
            }
            
        } catch (Exception e) {
            LOG.error("Error resolving specific users: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    private List<RecipientResolution> resolveManualRecipient(String entityId, Map<String, Object> context) {
        LOG.debug("Resolving manual recipient for entity: {}", entityId);
        List<RecipientResolution> result = new ArrayList<>();
        
        try {
            // Para recipient type "manual", o entityId é o userId do destinatário
            // Primeiro tenta encontrar no banco
            Optional<User> userOpt = userRepository.findByKeycloakId(entityId);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                RecipientResolution rr = new RecipientResolution();
                rr.setUserId(user.getId());
                rr.setEmail(user.getEmail());
                rr.setName(user.getName());
                rr.setPhone(user.getPhone());
                rr.setRecipientType("manual");
                rr.setMetadata(Map.of(
                    "source", "database",
                    "entityId", entityId
                ));
                
                result.add(rr);
                LOG.info("Resolved manual recipient from database: {} <{}>", rr.getName(), rr.getEmail());
            } else {
                // Se não encontrar, verifica se há dados no contexto
                if (context != null && context.containsKey("recipient")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recipientData = (Map<String, Object>) context.get("recipient");
                    
                    if (recipientData != null) {
                        RecipientResolution rr = new RecipientResolution();
                        rr.setUserId((String) recipientData.getOrDefault("userId", entityId));
                        rr.setEmail((String) recipientData.getOrDefault("email", entityId + "@example.com"));
                        rr.setName((String) recipientData.getOrDefault("name", "User " + entityId));
                        rr.setPhone((String) recipientData.get("phone"));
                        rr.setRecipientType("manual");
                        rr.setMetadata(Map.of(
                            "source", "context",
                            "entityId", entityId
                        ));
                        
                        result.add(rr);
                        LOG.info("Resolved manual recipient from context: {} <{}>", rr.getName(), rr.getEmail());
                    }
                } else {
                    // Fallback básico para testes
                    RecipientResolution rr = new RecipientResolution();
                    rr.setUserId(entityId);
                    rr.setEmail(entityId + "@example.com");
                    rr.setName("User " + entityId);
                    rr.setRecipientType("manual");
                    rr.setMetadata(Map.of(
                        "source", "fallback",
                        "entityId", entityId
                    ));
                    
                    result.add(rr);
                    LOG.warn("Using fallback manual recipient for: {}", entityId);
                }
            }
            
        } catch (Exception e) {
            LOG.error("Error resolving manual recipient: {}", e.getMessage(), e);
            
            // Fallback mínimo em caso de erro
            RecipientResolution rr = new RecipientResolution();
            rr.setUserId(entityId);
            rr.setEmail(entityId + "@error.example.com");
            rr.setName("Error Recipient");
            rr.setRecipientType("manual");
            rr.setMetadata(Map.of("source", "error_fallback"));
            
            result.add(rr);
        }
        
        return result;
    }
    
    private User getOrCreateUser(String keycloakId, String fallbackEmail, String fallbackName) {
        Optional<User> existing = userRepository.findByKeycloakId(keycloakId);
        
        if (existing.isPresent()) {
            User user = existing.get();
            // Atualizar se necessário (cache de 1 hora)
            if (user.getLastSync().isBefore(Instant.now().minusSeconds(3600))) {
                updateUserFromKeycloak(user, keycloakId, fallbackEmail, fallbackName);
            }
            return user;
        } else {
            return createUserFromKeycloak(keycloakId, fallbackEmail, fallbackName);
        }
    }
    
    private void updateUserFromKeycloak(User user, String keycloakId, String fallbackEmail, String fallbackName) {
        LOG.debug("Updating user from Keycloak: {}", keycloakId);
        
        Optional<KeycloakUserResponse> keycloakUser = keycloakService.getUserById(keycloakId);
        
        if (keycloakUser.isPresent()) {
            KeycloakUserResponse kcUser = keycloakUser.get();
            user.setEmail(kcUser.email() != null ? kcUser.email() : fallbackEmail);
            user.setName(kcUser.getFullName() != null ? kcUser.getFullName() : fallbackName);
            user.setPhone(kcUser.getPhoneNumber());
            
            // Atualizar roles se disponível
            if (kcUser.realmRoles() != null && !kcUser.realmRoles().isEmpty()) {
                try {
                    String rolesJson = "[\"" + String.join("\",\"", kcUser.realmRoles()) + "\"]";
                    user.setRolesJson(rolesJson);
                } catch (Exception e) {
                    LOG.warn("Failed to serialize roles for user {}: {}", keycloakId, e.getMessage());
                }
            }
        } else {
            // Usar fallback se Keycloak não responder
            user.setEmail(fallbackEmail);
            user.setName(fallbackName);
            LOG.warn("Could not fetch user {} from Keycloak, using fallback data", keycloakId);
        }
        
        user.setLastSync(Instant.now());
        userRepository.persist(user);
        LOG.debug("User updated: {} <{}>", user.getName(), user.getEmail());
    }
    
    private User createUserFromKeycloak(String keycloakId, String fallbackEmail, String fallbackName) {
        LOG.debug("Creating new user from Keycloak: {}", keycloakId);
        
        Optional<KeycloakUserResponse> keycloakUser = keycloakService.getUserById(keycloakId);
        
        User user = new User();
        user.setId(keycloakId);
        
        if (keycloakUser.isPresent()) {
            KeycloakUserResponse kcUser = keycloakUser.get();
            user.setEmail(kcUser.email() != null ? kcUser.email() : fallbackEmail);
            user.setName(kcUser.getFullName() != null ? kcUser.getFullName() : fallbackName);
            user.setPhone(kcUser.getPhoneNumber());
            
            if (kcUser.realmRoles() != null && !kcUser.realmRoles().isEmpty()) {
                try {
                    String rolesJson = "[\"" + String.join("\",\"", kcUser.realmRoles()) + "\"]";
                    user.setRolesJson(rolesJson);
                } catch (Exception e) {
                    LOG.warn("Failed to serialize roles for new user {}: {}", keycloakId, e.getMessage());
                }
            }
            
            LOG.debug("Created user from Keycloak: {} <{}>", user.getName(), user.getEmail());
        } else {
            // Criar com dados básicos se Keycloak não responder
            user.setEmail(fallbackEmail);
            user.setName(fallbackName);
            LOG.info("Created user with fallback data: {} <{}>", fallbackName, fallbackEmail);
        }
        
        user.setCreatedAt(Instant.now());
        user.setLastSync(Instant.now());
        
        userRepository.persist(user);
        return user;
    }
}