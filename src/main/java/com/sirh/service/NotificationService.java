package com.sirh.service;

import com.sirh.model.entity.Notification;
import com.sirh.model.enums.NotificationType;
import com.sirh.exception.NotificationNotFoundException;
import com.sirh.model.repository.NotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Module 11 (Gestion des Notifications) : création par les déclencheurs
 * (événements Lot 5, tâches CRON) et lecture/marquage strictement scopés au
 * destinataire authentifié — tout rôle peut avoir des notifications, aucun
 * filtre par rôle ici (cf. cahier-technique.md §4.8).
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification creer(Long destinataireId, NotificationType type, String message, String entiteType, Long entiteId) {
        return notificationRepository.save(Notification.builder()
                .destinataireId(destinataireId)
                .type(type)
                .message(message)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .build());
    }

    @Transactional(readOnly = true)
    public List<Notification> mesNotifications(Long destinataireId) {
        return notificationRepository.findByDestinataireIdAndDeletedAtIsNullOrderByCreatedAtDesc(destinataireId);
    }

    @Transactional
    public Notification marquerNotificationLueByReference(UUID reference, Long destinataireId) {
        Notification notification = notificationRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new NotificationNotFoundException(reference));
        if (!notification.getDestinataireId().equals(destinataireId)) {
            throw new AccessDeniedException("Cette notification n'appartient pas à l'utilisateur authentifié.");
        }
        notification.setLue(true);
        return notificationRepository.save(notification);
    }
}
