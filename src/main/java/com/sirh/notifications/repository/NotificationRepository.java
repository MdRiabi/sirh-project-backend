package com.sirh.notifications.repository;

import com.sirh.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataireIdOrderByCreatedAtDesc(Long destinataireId);

    long countByDestinataireIdAndLueFalse(Long destinataireId);
}
