package com.sirh.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirh.model.entity.Contrat;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Indisponibilite;
import com.sirh.model.entity.ParametreGlobal;
import com.sirh.model.entity.PointageBrut;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.NotificationType;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.Role;
import com.sirh.model.enums.TypeEvenement;
import com.sirh.model.repository.ContratRepository;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.IndisponibiliteRepository;
import com.sirh.model.repository.PointageBrutRepository;
import com.sirh.model.repository.UtilisateurRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Module 11, "Alerte Sous-activité (Mensuelle)" : <= SEUIL_JOURS_TRAVAILLES_ SOUS_ACTIVITE sur 30
 * jours glissants pour un contrat EN_COURS, sauf suspension légale de contrat active (faux positif
 * congé maternité/arrêt maladie longue durée, cahier-fonctionnel.md Module 11).
 */
@ExtendWith(MockitoExtension.class)
class SousActiviteServiceTest {

  @Mock private ContratRepository contratRepository;
  @Mock private IndisponibiliteRepository indisponibiliteRepository;
  @Mock private EmployeRepository employeRepository;
  @Mock private PointageBrutRepository pointageBrutRepository;
  @Mock private UtilisateurRepository utilisateurRepository;
  @Mock private ParametreGlobalService parametreGlobalService;
  @Mock private NotificationService notificationService;

  private SousActiviteService service;
  private final LocalDate dateReference = LocalDate.of(2026, 8, 31);

  @BeforeEach
  void setUp() {
    service =
        new SousActiviteService(
            contratRepository,
            indisponibiliteRepository,
            employeRepository,
            pointageBrutRepository,
            utilisateurRepository,
            parametreGlobalService,
            notificationService);
    lenient()
        .when(parametreGlobalService.valeurDecimale("SEUIL_JOURS_TRAVAILLES_SOUS_ACTIVITE"))
        .thenReturn(BigDecimal.valueOf(5));
    lenient()
        .when(parametreGlobalService.findByCle("TYPES_SUSPENSION_LEGALE"))
        .thenReturn(ParametreGlobal.builder().valeur("MALADIE,MATERNITE").build());
    lenient()
        .when(
            utilisateurRepository.findByRoleInAndDeletedAtIsNull(
                List.of(Role.ROLE_RH, Role.ROLE_SUPERUSER)))
        .thenReturn(List.of(Utilisateur.builder().id(1L).build()));
  }

  private Contrat contratActif(UUID employeId) {
    return Contrat.builder().employeId(employeId).statut(ContratStatut.EN_COURS).build();
  }

  private PointageBrut pointage(LocalDateTime timestamp) {
    return PointageBrut.builder()
        .timestampPointage(timestamp)
        .statut(PointageStatut.OK)
        .typeEvenement(TypeEvenement.ENTREE)
        .build();
  }

  @Test
  void detecter_contratActifSansSuspensionEtSousLeSeuil_notifieRhEtSuperuser() {
    UUID employeId = UUID.randomUUID();
    when(contratRepository.findByStatutAndDeletedAtIsNull(ContratStatut.EN_COURS))
        .thenReturn(List.of(contratActif(employeId)));
    when(indisponibiliteRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class)))
        .thenReturn(List.of());
    when(employeRepository.findAllById(List.of(employeId)))
        .thenReturn(List.of(Employe.builder().id(employeId).nom("Ali").prenom("Ben").build()));
    when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetweenAndDeletedAtIsNull(
            eq(employeId), any(), any()))
        .thenReturn(List.of(pointage(dateReference.minusDays(1).atTime(9, 0))));

    service.detecterSousActivite(dateReference);

    verify(notificationService, times(1))
        .creer(eq(1L), eq(NotificationType.ALERTE_SOUS_ACTIVITE), any(), eq(null), eq(null));
  }

  @Test
  void detecter_employeEnSuspensionLegale_nEstPasNotifie() {
    UUID employeId = UUID.randomUUID();
    when(contratRepository.findByStatutAndDeletedAtIsNull(ContratStatut.EN_COURS))
        .thenReturn(List.of(contratActif(employeId)));
    when(indisponibiliteRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class)))
        .thenReturn(
            List.of(
                Indisponibilite.builder()
                    .employeId(employeId)
                    .typeIndisponibilite("MALADIE")
                    .build()));

    service.detecterSousActivite(dateReference);

    verify(notificationService, never()).creer(anyLong(), any(), any(), any(), any());
  }

  @Test
  void detecter_joursTravaillesAuDessusDuSeuil_nEstPasNotifie() {
    UUID employeId = UUID.randomUUID();
    when(contratRepository.findByStatutAndDeletedAtIsNull(ContratStatut.EN_COURS))
        .thenReturn(List.of(contratActif(employeId)));
    when(indisponibiliteRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class)))
        .thenReturn(List.of());
    when(employeRepository.findAllById(List.of(employeId)))
        .thenReturn(List.of(Employe.builder().id(employeId).nom("Ali").prenom("Ben").build()));
    List<PointageBrut> sixJoursDistincts =
        List.of(
            pointage(dateReference.minusDays(1).atTime(9, 0)),
            pointage(dateReference.minusDays(2).atTime(9, 0)),
            pointage(dateReference.minusDays(3).atTime(9, 0)),
            pointage(dateReference.minusDays(4).atTime(9, 0)),
            pointage(dateReference.minusDays(5).atTime(9, 0)),
            pointage(dateReference.minusDays(6).atTime(9, 0)));
    when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetweenAndDeletedAtIsNull(
            eq(employeId), any(), any()))
        .thenReturn(sixJoursDistincts);

    service.detecterSousActivite(dateReference);

    verify(notificationService, never()).creer(anyLong(), any(), any(), any(), any());
  }
}
