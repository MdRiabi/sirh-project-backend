package com.sirh.evaluation.service;

import com.sirh.evaluation.dto.EvaluationCompetenceRequest;
import com.sirh.evaluation.dto.EvaluationDto;
import com.sirh.evaluation.entity.Evaluation;
import com.sirh.evaluation.entity.EvaluationCompetence;
import com.sirh.evaluation.entity.EvaluationStatut;
import com.sirh.evaluation.entity.PlanDeveloppement;
import com.sirh.evaluation.exception.EvaluationIncompleteException;
import com.sirh.evaluation.exception.EvaluationNotFoundException;
import com.sirh.evaluation.exception.EvaluationStatutInvalideException;
import com.sirh.evaluation.repository.EvaluationCompetenceRepository;
import com.sirh.evaluation.repository.EvaluationRepository;
import com.sirh.evaluation.repository.PlanDeveloppementRepository;
import com.sirh.personnel.entity.Departement;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.personnel.repository.EvenementHistoriqueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Module 9 : blocage de soumission si une compétence obligatoire n'est pas
 * notée (au moment de PUT, pas à la création — la grille peut être enregistrée
 * incomplète, cf. cas exceptionnel du CDF) et génération automatique du PDI
 * pour toute compétence sous son seuil d'exigence lors de la validation RH.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private EvaluationCompetenceRepository competenceRepository;
    @Mock
    private PlanDeveloppementRepository planDeveloppementRepository;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private EvenementHistoriqueRepository evenementRepository;

    @Captor
    private ArgumentCaptor<List<EvaluationCompetence>> competencesCaptor;
    @Captor
    private ArgumentCaptor<PlanDeveloppement> pdiCaptor;

    private EvaluationService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(evaluationRepository, competenceRepository, planDeveloppementRepository,
                employeRepository, evenementRepository);
    }

    private Employe employeDansDepartement(UUID id, Long departementId) {
        Departement departement = departementId != null ? Departement.builder().id(departementId).build() : null;
        return Employe.builder().id(id).nom("Doe").prenom("Jane").departement(departement).build();
    }

    @Test
    void creer_employeHorsDepartementDuManager_estRefuseAvecAccessDenied() {
        UUID employeId = UUID.randomUUID();
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employeDansDepartement(employeId, 2L)));

        assertThatThrownBy(() -> service.creer(employeId, UUID.randomUUID(), 1L, List.of(
                new EvaluationCompetenceRequest("Rigueur", null, BigDecimal.TEN, null))))
                .isInstanceOf(AccessDeniedException.class);

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void creer_evaluationDejaExistantePourLAnnee_estRefusee() {
        UUID employeId = UUID.randomUUID();
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));
        when(evaluationRepository.existsByEmployeIdAndAnnee(employeId, Year.now().getValue())).thenReturn(true);

        assertThatThrownBy(() -> service.creer(employeId, UUID.randomUUID(), 1L, List.of(
                new EvaluationCompetenceRequest("Rigueur", null, BigDecimal.TEN, null))))
                .isInstanceOf(EvaluationStatutInvalideException.class);
    }

    @Test
    void creer_grilleAvecNotesPartielles_estAccepteeSansBlocage() {
        UUID employeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));
        when(evaluationRepository.existsByEmployeIdAndAnnee(any(), any())).thenReturn(false);
        when(evaluationRepository.save(any())).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(competenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        EvaluationDto dto = service.creer(employeId, managerId, 1L, List.of(
                new EvaluationCompetenceRequest("Rigueur", BigDecimal.valueOf(12), BigDecimal.TEN, null),
                new EvaluationCompetenceRequest("Esprit d'équipe", null, BigDecimal.TEN, null)));

        assertThat(dto.competencesManquantes()).containsExactly("Esprit d'équipe");
        assertThat(dto.statut()).isEqualTo(EvaluationStatut.EN_COURS);
    }

    @Test
    void mettreAJour_parUnAutreManagerQueLeCreateur_estRefuseAvecAccessDenied() {
        Evaluation evaluation = Evaluation.builder().id(1L).managerId(UUID.randomUUID())
                .statut(EvaluationStatut.EN_COURS).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThatThrownBy(() -> service.mettreAJour(1L, UUID.randomUUID(), List.of(
                new EvaluationCompetenceRequest("Rigueur", BigDecimal.TEN, BigDecimal.TEN, null))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void mettreAJour_grilleDejaValideeRh_estRefuseeAvecStatutInvalide() {
        UUID managerId = UUID.randomUUID();
        Evaluation evaluation = Evaluation.builder().id(1L).managerId(managerId)
                .statut(EvaluationStatut.VALIDEE_RH).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThatThrownBy(() -> service.mettreAJour(1L, managerId, List.of(
                new EvaluationCompetenceRequest("Rigueur", BigDecimal.TEN, BigDecimal.TEN, null))))
                .isInstanceOf(EvaluationStatutInvalideException.class);
    }

    @Test
    void mettreAJour_avecUneCompetenceNonNotee_bloqueLaSoumissionAvec400EtListeLaCompetenceManquante() {
        UUID managerId = UUID.randomUUID();
        UUID employeId = UUID.randomUUID();
        Evaluation evaluation = Evaluation.builder().id(1L).employeId(employeId).managerId(managerId)
                .statut(EvaluationStatut.EN_COURS).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(competenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.mettreAJour(1L, managerId, List.of(
                new EvaluationCompetenceRequest("Rigueur", BigDecimal.TEN, BigDecimal.TEN, null),
                new EvaluationCompetenceRequest("Esprit d'équipe", null, BigDecimal.TEN, null))))
                .isInstanceOf(EvaluationIncompleteException.class)
                .satisfies(ex -> assertThat(((EvaluationIncompleteException) ex).getCompetencesManquantes())
                        .containsExactly("Esprit d'équipe"));

        // La saisie est tout de même persistée malgré le rejet, pour ne pas perdre le travail du Manager.
        verify(competenceRepository, times(1)).saveAll(any());
    }

    @Test
    void mettreAJour_grilleComplete_reussitSansException() {
        UUID managerId = UUID.randomUUID();
        UUID employeId = UUID.randomUUID();
        Evaluation evaluation = Evaluation.builder().id(1L).employeId(employeId).managerId(managerId)
                .statut(EvaluationStatut.EN_COURS).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(competenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));

        EvaluationDto dto = service.mettreAJour(1L, managerId, List.of(
                new EvaluationCompetenceRequest("Rigueur", BigDecimal.valueOf(15), BigDecimal.TEN, null)));

        assertThat(dto.competencesManquantes()).isEmpty();
    }

    @Test
    void valider_grilleIncomplete_estRefuseeEnFiletDeSecurite() {
        Evaluation evaluation = Evaluation.builder().id(1L).statut(EvaluationStatut.EN_COURS).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(competenceRepository.findByEvaluationId(1L)).thenReturn(List.of(
                EvaluationCompetence.builder().competence("Rigueur").note(null).seuilExigence(BigDecimal.TEN).build()));

        assertThatThrownBy(() -> service.valider(1L)).isInstanceOf(EvaluationIncompleteException.class);
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void valider_deuxFois_estRefuseeAvecStatutInvalide() {
        Evaluation evaluation = Evaluation.builder().id(1L).statut(EvaluationStatut.VALIDEE_RH).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThatThrownBy(() -> service.valider(1L)).isInstanceOf(EvaluationStatutInvalideException.class);
    }

    @Test
    void valider_evaluationInexistante_lance404() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.valider(99L)).isInstanceOf(EvaluationNotFoundException.class);
    }

    @Test
    void valider_competenceSousLeSeuil_genereUnPdiEtPousseUnEvenement360() {
        UUID employeId = UUID.randomUUID();
        Evaluation evaluation = Evaluation.builder().id(1L).employeId(employeId).annee(2026)
                .statut(EvaluationStatut.EN_COURS).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(competenceRepository.findByEvaluationId(1L)).thenReturn(List.of(
                EvaluationCompetence.builder().competence("Rigueur").note(BigDecimal.valueOf(8)).seuilExigence(BigDecimal.TEN).build(),
                EvaluationCompetence.builder().competence("Autonomie").note(BigDecimal.valueOf(15)).seuilExigence(BigDecimal.TEN).build()));
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planDeveloppementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));

        EvaluationDto dto = service.valider(1L);

        assertThat(dto.statut()).isEqualTo(EvaluationStatut.VALIDEE_RH);
        assertThat(dto.plansDeveloppement()).hasSize(1);
        verify(planDeveloppementRepository, times(1)).save(pdiCaptor.capture());
        assertThat(pdiCaptor.getValue().getCompetence()).isEqualTo("Rigueur");
        verify(evenementRepository, times(1)).save(any());
    }

    @Test
    void valider_toutesLesCompetencesAuNiveau_neGenereAucunPdi() {
        UUID employeId = UUID.randomUUID();
        Evaluation evaluation = Evaluation.builder().id(1L).employeId(employeId).annee(2026)
                .statut(EvaluationStatut.EN_COURS).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(competenceRepository.findByEvaluationId(1L)).thenReturn(List.of(
                EvaluationCompetence.builder().competence("Rigueur").note(BigDecimal.TEN).seuilExigence(BigDecimal.TEN).build()));
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));

        EvaluationDto dto = service.valider(1L);

        assertThat(dto.plansDeveloppement()).isEmpty();
        verify(planDeveloppementRepository, never()).save(any());
    }

    @Test
    void parEmploye_managerHorsDepartement_estRefuseAvecAccessDenied() {
        UUID employeId = UUID.randomUUID();
        when(employeRepository.findById(employeId)).thenReturn(Optional.of(employeDansDepartement(employeId, 2L)));

        assertThatThrownBy(() -> service.parEmploye(employeId, UUID.randomUUID(), 1L, true))
                .isInstanceOf(AccessDeniedException.class);
    }
}
