package com.sirh.service;

import com.sirh.model.entity.Notification;
import com.sirh.model.enums.NotificationType;
import com.sirh.exception.NotificationNotFoundException;
import com.sirh.model.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

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
        Notification notif = notification(42L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        Notification resultat = service.marquerLue(1L, 42L);

        assertThat(resultat.getLue()).isTrue();
    }

    @Test
    void marquerLue_autreDestinataire_estRefuse() {
        Notification notif = notification(42L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> service.marquerLue(1L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void marquerLue_inexistante_leve404() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marquerLue(1L, 42L))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
