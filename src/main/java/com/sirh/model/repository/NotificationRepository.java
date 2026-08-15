package com.sirh.model.repository;

import com.sirh.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByReferenceAndDeletedAtIsNull(UUID reference);

    List<Notification> findByDestinataireIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long destinataireId);

    long countByDestinataireIdAndLueFalseAndDeletedAtIsNull(Long destinataireId);
}
