package com.sirh.assiduite.service;

import com.sirh.assiduite.dto.CalculMensuelEmployeDto;
import com.sirh.parametres.entity.ParametreGlobal;
import com.sirh.parametres.service.ParametreGlobalService;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.planning.entity.Planning;
import com.sirh.planning.entity.PlanningStatut;
import com.sirh.planning.repository.PlanningRepository;
import com.sirh.pointages.entity.MethodeValidation;
import com.sirh.pointages.entity.PointageBrut;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.entity.SourceImport;
import com.sirh.pointages.entity.TypeEvenement;
import com.sirh.pointages.repository.PointageBrutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Couvre le moteur de calcul mensuel (Module 3, Lot 6) : segmentation
 * heures normales/nuit/majorées (aucun montant, cf. CalculMensuelEmployeDto),
 * taux de prime par paliers d'incidents, taux de présence — tous les seuils
 * viennent de parametres_globaux (Lot 5), jamais codés en dur.
 */
@ExtendWith(MockitoExtension.class)
class AssiduiteCalculServiceTest {

    private static final YearMonth JUILLET_2026 = YearMonth.of(2026, 7);

    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private PointageBrutRepository pointageBrutRepository;
    @Mock
    private PlanningRepository planningRepository;
    @Mock
    private ParametreGlobalService parametreGlobalService;

    private AssiduiteCalculService service;
    private Employe employe;

    @BeforeEach
    void setUp() {
        service = new AssiduiteCalculService(employeRepository, pointageBrutRepository, planningRepository, parametreGlobalService);
        employe = Employe.builder().id(UUID.randomUUID()).nom("Test").prenom("User").build();
        lenient().when(employeRepository.findAll()).thenReturn(List.of(employe));

        lenient().when(parametreGlobalService.findByCle("HEURE_DEBUT_NUIT"))
                .thenReturn(ParametreGlobal.builder().valeur("21:00").build());
        lenient().when(parametreGlobalService.findByCle("HEURE_FIN_NUIT"))
                .thenReturn(ParametreGlobal.builder().valeur("06:00").build());
        lenient().when(parametreGlobalService.valeurDecimale("SEUIL_HEURES_MAJOREES_JOUR")).thenReturn(BigDecimal.valueOf(8));
        lenient().when(parametreGlobalService.valeurDecimale("SEUIL_PRIME_100")).thenReturn(BigDecimal.valueOf(0));
        lenient().when(parametreGlobalService.valeurDecimale("SEUIL_PRIME_50")).thenReturn(BigDecimal.valueOf(2));
        lenient().when(parametreGlobalService.valeurDecimale("SEUIL_PRIME_33")).thenReturn(BigDecimal.valueOf(4));

        lenient().when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetweenAndStatut(
                any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(planningRepository.findByEmployeIdAndDateDebutBetweenOrderByDateDebut(any(), any(), any()))
                .thenReturn(List.of());
    }

    private PointageBrut pointage(LocalDateTime timestamp, TypeEvenement type) {
        return PointageBrut.builder().employeId(employe.getId()).timestampPointage(timestamp).typeEvenement(type)
                .methodeValidation(MethodeValidation.BADGE).sourceImport(SourceImport.HIKVISION_PUSH)
                .statut(PointageStatut.OK).build();
    }

    @Test
    void calculerMois_journeeDe10h30_segmenteNormalesEtMajoreesSansChevaucherLaNuit() {
        // 08:00-18:30 (10h30 = 630min), seuil 8h/480min -> 2h30 majorées, 8h normales.
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(any(), any(), any())).thenReturn(List.of(
                pointage(LocalDateTime.of(2026, 7, 6, 8, 0), TypeEvenement.ENTREE),
                pointage(LocalDateTime.of(2026, 7, 6, 18, 30), TypeEvenement.SORTIE)));

        CalculMensuelEmployeDto dto = service.calculerMois(JUILLET_2026, null).get(0);

        assertThat(dto.heuresNormales()).isEqualByComparingTo("8.00");
        assertThat(dto.heuresMajorees()).isEqualByComparingTo("2.50");
        assertThat(dto.heuresNuit()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculerMois_sessionChevauchantMinuit_compteLesHeuresDeNuitSurLaFenetre21h_06h() {
        // 20:00 -> 05:00 le lendemain (9h) ; nuit [21:00-06:00[ => 21:00->05:00 = 8h de nuit.
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(any(), any(), any())).thenReturn(List.of(
                pointage(LocalDateTime.of(2026, 7, 7, 20, 0), TypeEvenement.ENTREE),
                pointage(LocalDateTime.of(2026, 7, 8, 5, 0), TypeEvenement.SORTIE)));

        CalculMensuelEmployeDto dto = service.calculerMois(JUILLET_2026, null).get(0);

        assertThat(dto.heuresNuit()).isEqualByComparingTo("8.00");
        assertThat(dto.heuresMajorees()).isEqualByComparingTo("1.00"); // 9h - 8h de seuil
        assertThat(dto.heuresNormales()).isEqualByComparingTo("8.00");
    }

    @Test
    void calculerMois_excluLesPointagesIgnoreDoublonDeLAppariement() {
        UUID id = employe.getId();
        PointageBrut entreeDoublon = pointage(LocalDateTime.of(2026, 7, 6, 8, 1), TypeEvenement.ENTREE);
        entreeDoublon.setStatut(PointageStatut.IGNORE_DOUBLON);
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(any(), any(), any())).thenReturn(List.of(
                pointage(LocalDateTime.of(2026, 7, 6, 8, 0), TypeEvenement.ENTREE),
                entreeDoublon,
                pointage(LocalDateTime.of(2026, 7, 6, 12, 0), TypeEvenement.SORTIE)));

        CalculMensuelEmployeDto dto = service.calculerMois(JUILLET_2026, null).get(0);

        // Seule la paire (08:00, 12:00) doit compter : 4h, sous le seuil de majoration.
        assertThat(dto.heuresNormales()).isEqualByComparingTo("4.00");
        assertThat(dto.heuresMajorees()).isEqualByComparingTo("0.00");
        assertThat(dto.employeId()).isEqualTo(id);
    }

    @Test
    void calculerMois_zeroIncident_donnePrimeA100Pourcent() {
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(any(), any(), any())).thenReturn(List.of());

        CalculMensuelEmployeDto dto = service.calculerMois(JUILLET_2026, null).get(0);

        assertThat(dto.incidents()).isZero();
        assertThat(dto.tauxPrime()).isEqualByComparingTo("100");
    }

    @Test
    void calculerMois_troisIncidents_donnePrimeA33Pourcent() {
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(any(), any(), any())).thenReturn(List.of());
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetweenAndStatut(
                any(), any(), any(), org.mockito.ArgumentMatchers.eq(PointageStatut.ANOMALIE_NON_JUSTIFIEE)))
                .thenReturn(List.of(pointage(LocalDateTime.now(), TypeEvenement.ENTREE),
                        pointage(LocalDateTime.now(), TypeEvenement.ENTREE),
                        pointage(LocalDateTime.now(), TypeEvenement.ENTREE)));

        CalculMensuelEmployeDto dto = service.calculerMois(JUILLET_2026, null).get(0);

        assertThat(dto.incidents()).isEqualTo(3);
        assertThat(dto.tauxPrime()).isEqualByComparingTo("33");
    }

    @Test
    void calculerMois_tauxPresence_ratioDesJoursTheoriquesEffectivementTravailles() {
        UUID id = employe.getId();
        // 2 jours théoriques (planning VALIDE hors REPOS/CONGE/ABSENCE), 1 seul travaillé.
        when(planningRepository.findByEmployeIdAndDateDebutBetweenOrderByDateDebut(any(), any(), any())).thenReturn(List.of(
                Planning.builder().employeId(id).shiftId(1L).statut(PlanningStatut.VALIDE)
                        .dateDebut(LocalDateTime.of(2026, 7, 6, 8, 0)).dateFin(LocalDateTime.of(2026, 7, 6, 16, 0)).build(),
                Planning.builder().employeId(id).shiftId(1L).statut(PlanningStatut.VALIDE)
                        .dateDebut(LocalDateTime.of(2026, 7, 7, 8, 0)).dateFin(LocalDateTime.of(2026, 7, 7, 16, 0)).build()));
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(any(), any(), any())).thenReturn(List.of(
                pointage(LocalDateTime.of(2026, 7, 6, 8, 0), TypeEvenement.ENTREE),
                pointage(LocalDateTime.of(2026, 7, 6, 16, 0), TypeEvenement.SORTIE)));

        CalculMensuelEmployeDto dto = service.calculerMois(JUILLET_2026, null).get(0);

        assertThat(dto.tauxPresence()).isEqualByComparingTo("50.00");
        // Jour non travaillé (07/07, 8h théoriques, 0 réelle) = 8h de pénalité.
        assertThat(dto.penaliteHeures()).isEqualByComparingTo("8.00");
    }

    @Test
    void calculerMois_departementFiltre_interrogeParDepartementPasFindAll() {
        when(employeRepository.findByDepartement_Id(42L)).thenReturn(List.of(employe));
        when(pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(any(), any(), any())).thenReturn(List.of());

        List<CalculMensuelEmployeDto> resultat = service.calculerMois(JUILLET_2026, 42L);

        assertThat(resultat).hasSize(1);
        org.mockito.Mockito.verify(employeRepository).findByDepartement_Id(42L);
        org.mockito.Mockito.verify(employeRepository, org.mockito.Mockito.never()).findAll();
    }
}
