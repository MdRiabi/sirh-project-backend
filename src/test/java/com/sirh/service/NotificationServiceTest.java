package com.sirh.service;

import com.sirh.model.entity.Notification;
import com.sirh.model.enums.NotificationType;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Module 11 : scope strict au destinataire authentifié (cahier-technique.md
 * §4.8, "tout rôle authentifié, scope = ses propres notifications").
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository);
        lenient().when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Notification notification(Long destinataireId) {
        return Notification.builder().id(1L).destinataireId(destinataireId)
                .type(NotificationType.DEMANDE_A_VALIDER).message("Test").lue(false).build();
    }

    @Test
    void marquerLue_proprietaire_passeLueATrue() {
        UUID reference = UUID.randomUUID();
        Notification notif = notification(42L);
        notif.setReference(reference);
        when(notificationRepository.findByReferenceAndDeletedAtIsNull(reference)).thenReturn(Optional.of(notif));

        var resultat = service.marquerNotificationLueByReference(reference, 42L);

        assertThat(resultat.lue()).isTrue();
    }

    @Test
    void marquerLue_autreDestinataire_estRefuse() {
        UUID reference = UUID.randomUUID();
        Notification notif = notification(42L);
        notif.setReference(reference);
        when(notificationRepository.findByReferenceAndDeletedAtIsNull(reference)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> service.marquerNotificationLueByReference(reference, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void marquerLue_inexistante_leve404() {
        UUID reference = UUID.randomUUID();
        when(notificationRepository.findByReferenceAndDeletedAtIsNull(reference)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marquerNotificationLueByReference(reference, 42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
