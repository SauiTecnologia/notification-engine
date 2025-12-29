package com.apporte.core.repository;

import com.apporte.core.model.Notification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {
    // Métodos customizados podem ser adicionados aqui
    // O Panache já fornece: persist(), findById(), listAll(), etc.
}