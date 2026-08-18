package com.sirh.service;

import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.NotificationDto;
import com.sirh.model.entity.Notification;
import com.sirh.model.enums.NotificationType;
import com.sirh.model.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module 11 (Gestion des Notifications) : création par les déclencheurs (événements Lot 5, tâches
 * CRON) et lecture/marquage strictement scopés au destinataire authentifié — tout rôle peut avoir
 * des notifications, aucun filtre par rôle ici (cf. cahier-technique.md §4.8).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final ModelMapper modelMapper;

  @Transactional
  public Notification creer(
      Long destinataireId,
      NotificationType type,
      String message,
      String entiteType,
      Long entiteId) {
    return notificationRepository.save(
        Notification.builder()
            .destinataireId(destinataireId)
            .type(type)
            .message(message)
            .entiteType(entiteType)
            .entiteId(entiteId)
            .build());
  }

  @Transactional(readOnly = true)
  public List<NotificationDto> mesNotifications(Long destinataireId) {
    return notificationRepository
        .findByDestinataireIdAndDeletedAtIsNullOrderByCreatedAtDesc(destinataireId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public NotificationDto marquerNotificationLueByReference(UUID reference, Long destinataireId) {
    Notification notification =
        notificationRepository
            .findByReferenceAndDeletedAtIsNull(reference)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ErrorCode.NOTIFICATION_NOT_FOUND, new Object[] {reference}));
    if (!notification.getDestinataireId().equals(destinataireId)) {
      throw new AccessDeniedException(
          "Cette notification n'appartient pas à l'utilisateur authentifié.");
    }
    notification.setLue(true);
    return toDto(notificationRepository.save(notification));
  }

  private NotificationDto toDto(Notification n) {
    return modelMapper.map(n, NotificationDto.class);
  }
}
