package com.sirh.notifications.service;

import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.repository.UtilisateurRepository;
import com.sirh.notifications.entity.NotificationType;
import com.sirh.parametres.service.ParametreGlobalService;
import com.sirh.personnel.entity.Departement;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.repository.PointageBrutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Module 11, "Alerte Pointage (Hebdomadaire)" : >= SEUIL_OUBLIS_BADGEAGE_HEBDO
 * (jamais codé en dur), notifié au manager du département de l'employé
 * concerné, jamais à l'employé lui-même.
 */
@ExtendWith(MockitoExtension.class)
class AlertePointageServiceTest {

    @Mock
    private PointageBrutRepository pointageBrutRepository;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ParametreGlobalService parametreGlobalService;
    @Mock
    private NotificationService notificationService;

    private AlertePointageService service;

    private Employe employeAvecManager(UUID employeId, UUID managerId) {
        Departement departement = Departement.builder().id(1L).managerId(managerId).build();
        return Employe.builder().id(employeId).nom("Ali").prenom("Ben").departement(departement).build();
    }

    private void mockSeuil(int seuil) {
        when(parametreGlobalService.valeurDecimale("SEUIL_OUBLIS_BADGEAGE_HEBDO")).thenReturn(BigDecimal.valueOf(seuil));
    }

    @Test
    void detecter_seuilAtteint_notifieLeManagerDuDepartement() {
        service = new AlertePointageService(pointageBrutRepository, employeRepository, utilisateurRepository,
                parametreGlobalService, notificationService);
        mockSeuil(3);
        UUID employeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        Employe employe = employeAvecManager(employeId, managerId);
        Utilisateur manager = Utilisateur.builder().id(77L).build();

        when(pointageBrutRepository.compterAnomaliesParEmploye(any(), any(), eq(PointageStatut.ANOMALIE_NON_JUSTIFIEE)))
                .thenReturn(List.<Object[]>of(new Object[]{employeId, 3L}));
        when(employeRepository.findAllById(List.of(employeId))).thenReturn(List.of(employe));
        when(utilisateurRepository.findByEmployeId(managerId)).thenReturn(Optional.of(manager));

        service.detecterOublisBadgeageSemaine(LocalDate.of(2026, 8, 9));

        verify(notificationService).creer(eq(77L), eq(NotificationType.ALERTE_POINTAGE), any(), eq(null), eq(null));
    }

    @Test
    void detecter_sousLeSeuil_neNotifiePersonne() {
        service = new AlertePointageService(pointageBrutRepository, employeRepository, utilisateurRepository,
                parametreGlobalService, notificationService);
        mockSeuil(3);
        UUID employeId = UUID.randomUUID();

        when(pointageBrutRepository.compterAnomaliesParEmploye(any(), any(), eq(PointageStatut.ANOMALIE_NON_JUSTIFIEE)))
                .thenReturn(List.<Object[]>of(new Object[]{employeId, 2L}));

        service.detecterOublisBadgeageSemaine(LocalDate.of(2026, 8, 9));

        verify(notificationService, never()).creer(anyLong(), any(), any(), any(), any());
    }

    @Test
    void detecter_departementSansManager_neLevePasEtIgnoreLEmploye() {
        service = new AlertePointageService(pointageBrutRepository, employeRepository, utilisateurRepository,
                parametreGlobalService, notificationService);
        mockSeuil(3);
        UUID employeId = UUID.randomUUID();
        Employe employe = Employe.builder().id(employeId).nom("Ali").prenom("Ben")
                .departement(Departement.builder().id(1L).managerId(null).build()).build();

        when(pointageBrutRepository.compterAnomaliesParEmploye(any(), any(), eq(PointageStatut.ANOMALIE_NON_JUSTIFIEE)))
                .thenReturn(List.<Object[]>of(new Object[]{employeId, 5L}));
        when(employeRepository.findAllById(List.of(employeId))).thenReturn(List.of(employe));

        service.detecterOublisBadgeageSemaine(LocalDate.of(2026, 8, 9));

        verify(notificationService, never()).creer(anyLong(), any(), any(), any(), any());
    }
}
