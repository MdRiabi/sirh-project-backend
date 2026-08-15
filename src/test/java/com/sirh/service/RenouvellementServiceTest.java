package com.sirh.service;

import com.sirh.model.enums.Role;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.repository.UtilisateurRepository;
import com.sirh.model.enums.NotificationType;
import com.sirh.service.NotificationService;
import com.sirh.service.ParametreGlobalService;
import com.sirh.model.dto.ContratRequest;
import com.sirh.model.entity.Contrat;
import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.ContratType;
import com.sirh.model.entity.Employe;
import com.sirh.model.enums.MotifFin;
import com.sirh.model.repository.ContratRepository;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.service.ContratService;
import com.sirh.model.dto.TacheRenouvellementDto;
import com.sirh.model.entity.TacheRenouvellement;
import com.sirh.model.enums.TacheRenouvellementStatut;
import com.sirh.exception.TacheRenouvellementNotFoundException;
import com.sirh.exception.TacheRenouvellementStatutInvalideException;
import com.sirh.model.repository.TacheRenouvellementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Module 12 : génération M+5/M+11 restreinte aux CDD (arbitrage Lot 9),
 * bascule URGENT_EXPIRE sur le terme réel du contrat (jamais l'échéance de la
 * tâche), et workflow VALIDE/NON_RENOUVELE — cf. RenouvellementService.
 */
@ExtendWith(MockitoExtension.class)
class RenouvellementServiceTest {

    @Mock
    private TacheRenouvellementRepository tacheRenouvellementRepository;
    @Mock
    private ContratRepository contratRepository;
    @Mock
    private ContratService contratService;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ParametreGlobalService parametreGlobalService;
    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<ContratRequest> contratRequestCaptor;

    private RenouvellementService service;
    private final LocalDate reference = LocalDate.of(2026, 8, 7);

    @BeforeEach
    void setUp() {
        service = new RenouvellementService(tacheRenouvellementRepository, contratRepository, contratService,
                employeRepository, utilisateurRepository, parametreGlobalService, notificationService);
    }

    private Contrat contratCdd(Long id, UUID employeId, LocalDate dateDebut) {
        return Contrat.builder().id(id).employeId(employeId).type(ContratType.CDD)
                .statut(ContratStatut.EN_COURS).dateDebut(dateDebut).build();
    }

    @Test
    void genererEtEscalader_contratCddAtteignantM5_genereUneTacheEtNotifieRh() {
        UUID employeId = UUID.randomUUID();
        Contrat contrat = contratCdd(1L, employeId, reference.minusMonths(5));
        when(parametreGlobalService.valeurDecimale("MOIS_ECHEANCE_M5")).thenReturn(BigDecimal.valueOf(5));
        when(parametreGlobalService.valeurDecimale("MOIS_ECHEANCE_M11")).thenReturn(BigDecimal.valueOf(11));
        when(contratRepository.findByStatutAndTypeAndDeletedAtIsNull(ContratStatut.EN_COURS, ContratType.CDD)).thenReturn(List.of(contrat));
        when(tacheRenouvellementRepository.existsByContratIdAndTypeEcheanceAndDeletedAtIsNull(1L, "M+5")).thenReturn(false);
        when(tacheRenouvellementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(utilisateurRepository.findByRoleInAndDeletedAtIsNull(List.of(Role.ROLE_RH, Role.ROLE_SUPERUSER)))
                .thenReturn(List.of(Utilisateur.builder().id(1L).build()));
        when(tacheRenouvellementRepository.findByStatutAndDeletedAtIsNull(TacheRenouvellementStatut.EN_COURS)).thenReturn(List.of());

        service.genererEtEscalader(reference);

        ArgumentCaptor<TacheRenouvellement> captor = ArgumentCaptor.forClass(TacheRenouvellement.class);
        verify(tacheRenouvellementRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getTypeEcheance()).isEqualTo("M+5");
        assertThat(captor.getValue().getStatut()).isEqualTo(TacheRenouvellementStatut.EN_COURS);
        verify(notificationService, times(1)).creer(anyLong(), eq(NotificationType.RENOUVELLEMENT_CONTRAT), any(), any(), any());
    }

    @Test
    void genererEtEscalader_tacheDejaExistante_neDupliquePas() {
        UUID employeId = UUID.randomUUID();
        Contrat contrat = contratCdd(1L, employeId, reference.minusMonths(5));
        when(parametreGlobalService.valeurDecimale("MOIS_ECHEANCE_M5")).thenReturn(BigDecimal.valueOf(5));
        when(parametreGlobalService.valeurDecimale("MOIS_ECHEANCE_M11")).thenReturn(BigDecimal.valueOf(11));
        when(contratRepository.findByStatutAndTypeAndDeletedAtIsNull(ContratStatut.EN_COURS, ContratType.CDD)).thenReturn(List.of(contrat));
        when(tacheRenouvellementRepository.existsByContratIdAndTypeEcheanceAndDeletedAtIsNull(1L, "M+5")).thenReturn(true);
        when(tacheRenouvellementRepository.findByStatutAndDeletedAtIsNull(TacheRenouvellementStatut.EN_COURS)).thenReturn(List.of());

        service.genererEtEscalader(reference);

        verify(tacheRenouvellementRepository, never()).save(any());
    }

    @Test
    void genererEtEscalader_tacheEnCoursDontLeContratEstArriveATerme_basculeUrgentExpire() {
        Contrat contrat = Contrat.builder().id(2L).employeId(UUID.randomUUID()).type(ContratType.CDD)
                .statut(ContratStatut.EN_COURS).dateDebut(reference.minusMonths(6)).dateFin(reference.minusDays(1)).build();
        TacheRenouvellement tache = TacheRenouvellement.builder().id(9L).contratId(2L)
                .statut(TacheRenouvellementStatut.EN_COURS).build();
        when(parametreGlobalService.valeurDecimale("MOIS_ECHEANCE_M5")).thenReturn(BigDecimal.valueOf(5));
        when(parametreGlobalService.valeurDecimale("MOIS_ECHEANCE_M11")).thenReturn(BigDecimal.valueOf(11));
        when(contratRepository.findByStatutAndTypeAndDeletedAtIsNull(ContratStatut.EN_COURS, ContratType.CDD)).thenReturn(List.of());
        when(tacheRenouvellementRepository.findByStatutAndDeletedAtIsNull(TacheRenouvellementStatut.EN_COURS)).thenReturn(List.of(tache));
        when(contratRepository.findById(2L)).thenReturn(Optional.of(contrat));
        when(tacheRenouvellementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(utilisateurRepository.findByRoleInAndDeletedAtIsNull(List.of(Role.ROLE_RH, Role.ROLE_SUPERUSER)))
                .thenReturn(List.of(Utilisateur.builder().id(1L).build()));

        service.genererEtEscalader(reference);

        assertThat(tache.getStatut()).isEqualTo(TacheRenouvellementStatut.URGENT_EXPIRE);
        verify(notificationService, times(1)).creer(anyLong(), eq(NotificationType.RENOUVELLEMENT_CONTRAT), any(), any(), any());
    }

    @Test
    void traiter_valideSansDateFin_renouvelleAncienContratEtCreeLeSuivantAvecDureeParDefaut() {
        UUID employeId = UUID.randomUUID();
        LocalDate ancienneFin = LocalDate.of(2026, 12, 31);
        Contrat ancien = Contrat.builder().id(1L).employeId(employeId).type(ContratType.CDD)
                .statut(ContratStatut.EN_COURS).dateFin(ancienneFin).build();
        TacheRenouvellement tache = TacheRenouvellement.builder().id(9L).employeId(employeId).contratId(1L)
                .statut(TacheRenouvellementStatut.EN_COURS).build();
        when(tacheRenouvellementRepository.findById(9L)).thenReturn(Optional.of(tache));
        when(contratRepository.findById(1L)).thenReturn(Optional.of(ancien));
        when(parametreGlobalService.valeurDecimale("DUREE_RENOUVELLEMENT_CDD_MOIS")).thenReturn(BigDecimal.valueOf(6));
        when(tacheRenouvellementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeRepository.findAllById(any())).thenReturn(List.of(Employe.builder().id(employeId).nom("A").prenom("B").build()));

        TacheRenouvellementDto resultat = service.traiter(9L, TacheRenouvellementStatut.VALIDE, null, 42L);

        assertThat(ancien.getStatut()).isEqualTo(ContratStatut.RENOUVELE);
        verify(contratService).create(eq(employeId), contratRequestCaptor.capture(), eq(42L));
        assertThat(contratRequestCaptor.getValue().dateDebut()).isEqualTo(ancienneFin);
        assertThat(contratRequestCaptor.getValue().dateFin()).isEqualTo(ancienneFin.plusMonths(6));
        assertThat(resultat.statut()).isEqualTo(TacheRenouvellementStatut.VALIDE);
        assertThat(resultat.traiteParId()).isEqualTo(42L);
    }

    @Test
    void traiter_valideAvecDateFinExplicite_ignoreLeParametreDynamique() {
        UUID employeId = UUID.randomUUID();
        Contrat ancien = Contrat.builder().id(1L).employeId(employeId).type(ContratType.CDD)
                .statut(ContratStatut.EN_COURS).dateFin(LocalDate.of(2026, 6, 30)).build();
        TacheRenouvellement tache = TacheRenouvellement.builder().id(9L).employeId(employeId).contratId(1L)
                .statut(TacheRenouvellementStatut.URGENT_EXPIRE).build();
        when(tacheRenouvellementRepository.findById(9L)).thenReturn(Optional.of(tache));
        when(contratRepository.findById(1L)).thenReturn(Optional.of(ancien));
        when(tacheRenouvellementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeRepository.findAllById(any())).thenReturn(List.of(Employe.builder().id(employeId).nom("A").prenom("B").build()));
        LocalDate dateFinChoisie = LocalDate.of(2027, 3, 1);

        service.traiter(9L, TacheRenouvellementStatut.VALIDE, dateFinChoisie, 42L);

        verify(contratService).create(eq(employeId), contratRequestCaptor.capture(), eq(42L));
        assertThat(contratRequestCaptor.getValue().dateFin()).isEqualTo(dateFinChoisie);
        verify(parametreGlobalService, never()).valeurDecimale("DUREE_RENOUVELLEMENT_CDD_MOIS");
    }

    @Test
    void traiter_nonRenouvele_termineLeContratEtDesactiveLEmploye() {
        UUID employeId = UUID.randomUUID();
        Contrat contrat = Contrat.builder().id(1L).employeId(employeId).type(ContratType.CDD)
                .statut(ContratStatut.EN_COURS).build();
        TacheRenouvellement tache = TacheRenouvellement.builder().id(9L).employeId(employeId).contratId(1L)
                .statut(TacheRenouvellementStatut.URGENT_EXPIRE).build();
        Employe employe = Employe.builder().id(employeId).nom("A").prenom("B").actif(true).build();
        when(tacheRenouvellementRepository.findById(9L)).thenReturn(Optional.of(tache));
        when(contratRepository.findById(1L)).thenReturn(Optional.of(contrat));
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employe));
        when(tacheRenouvellementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeRepository.findAllById(any())).thenReturn(List.of(employe));

        service.traiter(9L, TacheRenouvellementStatut.NON_RENOUVELE, null, 42L);

        assertThat(contrat.getStatut()).isEqualTo(ContratStatut.TERMINE);
        assertThat(contrat.getMotifFin()).isEqualTo(MotifFin.FIN_CDD);
        assertThat(employe.getActif()).isFalse();
        verify(contratService, never()).create(any(), any(), any());
    }

    @Test
    void traiter_tacheDejaValidee_estRefuseAvecStatutInvalide() {
        TacheRenouvellement tache = TacheRenouvellement.builder().id(9L).statut(TacheRenouvellementStatut.VALIDE).build();
        when(tacheRenouvellementRepository.findById(9L)).thenReturn(Optional.of(tache));

        assertThatThrownBy(() -> service.traiter(9L, TacheRenouvellementStatut.NON_RENOUVELE, null, 42L))
                .isInstanceOf(TacheRenouvellementStatutInvalideException.class);
    }

    @Test
    void traiter_tacheInexistante_lance404() {
        when(tacheRenouvellementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.traiter(99L, TacheRenouvellementStatut.VALIDE, null, 42L))
                .isInstanceOf(TacheRenouvellementNotFoundException.class);
    }

    @Test
    void traiter_statutDemandeInvalide_estRefuseAvecIllegalArgument() {
        assertThatThrownBy(() -> service.traiter(9L, TacheRenouvellementStatut.EN_COURS, null, 42L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
