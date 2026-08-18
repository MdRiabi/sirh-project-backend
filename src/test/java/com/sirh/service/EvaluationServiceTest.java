package com.sirh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirh.config.ModelMapperConfig;
import com.sirh.exception.ConflictException;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.EvaluationCompetenceRequest;
import com.sirh.model.dto.EvaluationDto;
import com.sirh.model.entity.Departement;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Evaluation;
import com.sirh.model.entity.EvaluationCompetence;
import com.sirh.model.entity.PlanDeveloppement;
import com.sirh.model.enums.EvaluationStatut;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.EvaluationCompetenceRepository;
import com.sirh.model.repository.EvaluationRepository;
import com.sirh.model.repository.EvenementHistoriqueRepository;
import com.sirh.model.repository.PlanDeveloppementRepository;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Module 9 : blocage de soumission si une compétence obligatoire n'est pas notée (au moment de PUT,
 * pas à la création — la grille peut être enregistrée incomplète, cf. cas exceptionnel du CDF) et
 * génération automatique du PDI pour toute compétence sous son seuil d'exigence lors de la
 * validation RH.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

  @Mock private EvaluationRepository evaluationRepository;
  @Mock private EvaluationCompetenceRepository competenceRepository;
  @Mock private PlanDeveloppementRepository planDeveloppementRepository;
  @Mock private EmployeRepository employeRepository;
  @Mock private EvenementHistoriqueRepository evenementRepository;

  @Captor private ArgumentCaptor<List<EvaluationCompetence>> competencesCaptor;
  @Captor private ArgumentCaptor<PlanDeveloppement> pdiCaptor;

  private EvaluationService service;

  @BeforeEach
  void setUp() {
    service =
        new EvaluationService(
            evaluationRepository,
            competenceRepository,
            planDeveloppementRepository,
            employeRepository,
            evenementRepository,
            new ModelMapperConfig().modelMapper());
  }

  private Employe employeDansDepartement(UUID id, Long departementId) {
    Departement departement =
        departementId != null ? Departement.builder().id(departementId).build() : null;
    return Employe.builder().id(id).nom("Doe").prenom("Jane").departement(departement).build();
  }

  @Test
  void creer_employeHorsDepartementDuManager_estRefuseAvecAccessDenied() {
    UUID employeId = UUID.randomUUID();
    when(employeRepository.findById(employeId))
        .thenReturn(Optional.of(employeDansDepartement(employeId, 2L)));

    assertThatThrownBy(
            () ->
                service.creer(
                    employeId,
                    UUID.randomUUID(),
                    1L,
                    List.of(
                        new EvaluationCompetenceRequest("Rigueur", null, BigDecimal.TEN, null))))
        .isInstanceOf(AccessDeniedException.class);

    verify(evaluationRepository, never()).save(any());
  }

  @Test
  void creer_evaluationDejaExistantePourLAnnee_estRefusee() {
    UUID employeId = UUID.randomUUID();
    when(employeRepository.findById(employeId))
        .thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));
    when(evaluationRepository.existsByEmployeIdAndAnneeAndDeletedAtIsNull(
            employeId, Year.now().getValue()))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                service.creer(
                    employeId,
                    UUID.randomUUID(),
                    1L,
                    List.of(
                        new EvaluationCompetenceRequest("Rigueur", null, BigDecimal.TEN, null))))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void creer_grilleAvecNotesPartielles_estAccepteeSansBlocage() {
    UUID employeId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    when(employeRepository.findById(employeId))
        .thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));
    when(evaluationRepository.existsByEmployeIdAndAnneeAndDeletedAtIsNull(any(), any()))
        .thenReturn(false);
    when(evaluationRepository.save(any()))
        .thenAnswer(
            inv -> {
              Evaluation e = inv.getArgument(0);
              e.setId(1L);
              return e;
            });
    when(competenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    EvaluationDto dto =
        service.creer(
            employeId,
            managerId,
            1L,
            List.of(
                new EvaluationCompetenceRequest(
                    "Rigueur", BigDecimal.valueOf(12), BigDecimal.TEN, null),
                new EvaluationCompetenceRequest("Esprit d'équipe", null, BigDecimal.TEN, null)));

    assertThat(dto.getCompetencesManquantes()).containsExactly("Esprit d'équipe");
    assertThat(dto.getStatut()).isEqualTo(EvaluationStatut.EN_COURS);
  }

  @Test
  void mettreAJour_parUnAutreManagerQueLeCreateur_estRefuseAvecAccessDenied() {
    UUID reference = UUID.randomUUID();
    Evaluation evaluation =
        Evaluation.builder()
            .id(1L)
            .managerId(UUID.randomUUID())
            .statut(EvaluationStatut.EN_COURS)
            .build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));

    assertThatThrownBy(
            () ->
                service.mettreAJourEvaluationByReference(
                    reference,
                    UUID.randomUUID(),
                    List.of(
                        new EvaluationCompetenceRequest(
                            "Rigueur", BigDecimal.TEN, BigDecimal.TEN, null))))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void mettreAJour_grilleDejaValideeRh_estRefuseeAvecStatutInvalide() {
    UUID reference = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    Evaluation evaluation =
        Evaluation.builder()
            .id(1L)
            .managerId(managerId)
            .statut(EvaluationStatut.VALIDEE_RH)
            .build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));

    assertThatThrownBy(
            () ->
                service.mettreAJourEvaluationByReference(
                    reference,
                    managerId,
                    List.of(
                        new EvaluationCompetenceRequest(
                            "Rigueur", BigDecimal.TEN, BigDecimal.TEN, null))))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void
      mettreAJour_avecUneCompetenceNonNotee_bloqueLaSoumissionAvec400EtListeLaCompetenceManquante() {
    UUID reference = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    UUID employeId = UUID.randomUUID();
    Evaluation evaluation =
        Evaluation.builder()
            .id(1L)
            .employeId(employeId)
            .managerId(managerId)
            .statut(EvaluationStatut.EN_COURS)
            .build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));
    when(competenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThatThrownBy(
            () ->
                service.mettreAJourEvaluationByReference(
                    reference,
                    managerId,
                    List.of(
                        new EvaluationCompetenceRequest(
                            "Rigueur", BigDecimal.TEN, BigDecimal.TEN, null),
                        new EvaluationCompetenceRequest(
                            "Esprit d'équipe", null, BigDecimal.TEN, null))))
        .isInstanceOfSatisfying(
            ConflictException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVALUATION_INCOMPLETE);
              assertThat((String) ex.getArgs()[0]).isEqualTo("Esprit d'équipe");
            });

    // La saisie est tout de même persistée malgré le rejet, pour ne pas perdre le travail du
    // Manager.
    verify(competenceRepository, times(1)).saveAll(any());
  }

  @Test
  void mettreAJour_grilleComplete_reussitSansException() {
    UUID reference = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    UUID employeId = UUID.randomUUID();
    Evaluation evaluation =
        Evaluation.builder()
            .id(1L)
            .employeId(employeId)
            .managerId(managerId)
            .statut(EvaluationStatut.EN_COURS)
            .build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));
    when(competenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    when(employeRepository.findById(employeId))
        .thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));

    EvaluationDto dto =
        service.mettreAJourEvaluationByReference(
            reference,
            managerId,
            List.of(
                new EvaluationCompetenceRequest(
                    "Rigueur", BigDecimal.valueOf(15), BigDecimal.TEN, null)));

    assertThat(dto.getCompetencesManquantes()).isEmpty();
  }

  @Test
  void valider_grilleIncomplete_estRefuseeEnFiletDeSecurite() {
    UUID reference = UUID.randomUUID();
    Evaluation evaluation = Evaluation.builder().id(1L).statut(EvaluationStatut.EN_COURS).build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));
    when(competenceRepository.findByEvaluationIdAndDeletedAtIsNull(1L))
        .thenReturn(
            List.of(
                EvaluationCompetence.builder()
                    .competence("Rigueur")
                    .note(null)
                    .seuilExigence(BigDecimal.TEN)
                    .build()));

    assertThatThrownBy(() -> service.validerEvaluationByReference(reference))
        .isInstanceOf(ConflictException.class);
    verify(evaluationRepository, never()).save(any());
  }

  @Test
  void valider_deuxFois_estRefuseeAvecStatutInvalide() {
    UUID reference = UUID.randomUUID();
    Evaluation evaluation = Evaluation.builder().id(1L).statut(EvaluationStatut.VALIDEE_RH).build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));

    assertThatThrownBy(() -> service.validerEvaluationByReference(reference))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void valider_evaluationInexistante_lance404() {
    UUID reference = UUID.randomUUID();
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.validerEvaluationByReference(reference))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void valider_competenceSousLeSeuil_genereUnPdiEtPousseUnEvenement360() {
    UUID reference = UUID.randomUUID();
    UUID employeId = UUID.randomUUID();
    Evaluation evaluation =
        Evaluation.builder()
            .id(1L)
            .employeId(employeId)
            .annee(2026)
            .statut(EvaluationStatut.EN_COURS)
            .build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));
    when(competenceRepository.findByEvaluationIdAndDeletedAtIsNull(1L))
        .thenReturn(
            List.of(
                EvaluationCompetence.builder()
                    .competence("Rigueur")
                    .note(BigDecimal.valueOf(8))
                    .seuilExigence(BigDecimal.TEN)
                    .build(),
                EvaluationCompetence.builder()
                    .competence("Autonomie")
                    .note(BigDecimal.valueOf(15))
                    .seuilExigence(BigDecimal.TEN)
                    .build()));
    when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(planDeveloppementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(employeRepository.findById(employeId))
        .thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));

    EvaluationDto dto = service.validerEvaluationByReference(reference);

    assertThat(dto.getStatut()).isEqualTo(EvaluationStatut.VALIDEE_RH);
    assertThat(dto.getPlansDeveloppement()).hasSize(1);
    verify(planDeveloppementRepository, times(1)).save(pdiCaptor.capture());
    assertThat(pdiCaptor.getValue().getCompetence()).isEqualTo("Rigueur");
    verify(evenementRepository, times(1)).save(any());
  }

  @Test
  void valider_toutesLesCompetencesAuNiveau_neGenereAucunPdi() {
    UUID reference = UUID.randomUUID();
    UUID employeId = UUID.randomUUID();
    Evaluation evaluation =
        Evaluation.builder()
            .id(1L)
            .employeId(employeId)
            .annee(2026)
            .statut(EvaluationStatut.EN_COURS)
            .build();
    evaluation.setReference(reference);
    when(evaluationRepository.findByReferenceAndDeletedAtIsNull(reference))
        .thenReturn(Optional.of(evaluation));
    when(competenceRepository.findByEvaluationIdAndDeletedAtIsNull(1L))
        .thenReturn(
            List.of(
                EvaluationCompetence.builder()
                    .competence("Rigueur")
                    .note(BigDecimal.TEN)
                    .seuilExigence(BigDecimal.TEN)
                    .build()));
    when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(employeRepository.findById(employeId))
        .thenReturn(Optional.of(employeDansDepartement(employeId, 1L)));

    EvaluationDto dto = service.validerEvaluationByReference(reference);

    assertThat(dto.getPlansDeveloppement()).isEmpty();
    verify(planDeveloppementRepository, never()).save(any());
  }

  @Test
  void parEmploye_managerHorsDepartement_estRefuseAvecAccessDenied() {
    UUID employeId = UUID.randomUUID();
    when(employeRepository.findById(employeId))
        .thenReturn(Optional.of(employeDansDepartement(employeId, 2L)));

    assertThatThrownBy(() -> service.parEmploye(employeId, UUID.randomUUID(), 1L, true))
        .isInstanceOf(AccessDeniedException.class);
  }
}
