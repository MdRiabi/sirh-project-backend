package com.sirh.model.repository;

import com.sirh.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataireIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long destinataireId);

    long countByDestinataireIdAndLueFalseAndDeletedAtIsNull(Long destinataireId);
}
