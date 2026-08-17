package com.sirh.service;

import com.sirh.exception.ResourceNotFoundException;
import com.sirh.exception.ErrorCode;

import com.sirh.annotation.Audited;
import com.sirh.model.dto.EvaluationCompetenceDto;
import com.sirh.model.dto.EvaluationCompetenceRequest;
import com.sirh.model.dto.EvaluationDto;
import com.sirh.model.dto.PlanDeveloppementDto;
import com.sirh.model.entity.Evaluation;
import com.sirh.model.entity.EvaluationCompetence;
import com.sirh.model.enums.EvaluationStatut;
import com.sirh.model.entity.PlanDeveloppement;
import com.sirh.exception.ConflictException;
import com.sirh.model.repository.EvaluationCompetenceRepository;
import com.sirh.model.repository.EvaluationRepository;
import com.sirh.model.repository.PlanDeveloppementRepository;
import com.sirh.model.enums.EvenementCategorie;
import com.sirh.model.entity.EvenementHistorique;
import com.sirh.model.entity.Employe;
import com.sirh.model.enums.StatutBadge;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.EvenementHistoriqueRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Module 9 (Évaluation Annuelle) : dématérialisation de la grille de notation
 * et génération automatique du Plan de Développement Individuel (PDI).
 * "Son équipe" (cahier-technique.md §4.10) est résolu par département,
 * comme le reste du codebase (AutorisationService/IndisponibiliteService) —
 * il n'existe pas de hiérarchie manager→employé indépendante du département.
 */
@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCompetenceRepository competenceRepository;
    private final PlanDeveloppementRepository planDeveloppementRepository;
    private final EmployeRepository employeRepository;
    private final EvenementHistoriqueRepository evenementRepository;

    public EvaluationService(EvaluationRepository evaluationRepository,
                              EvaluationCompetenceRepository competenceRepository,
                              PlanDeveloppementRepository planDeveloppementRepository,
                              EmployeRepository employeRepository,
                              EvenementHistoriqueRepository evenementRepository) {
        this.evaluationRepository = evaluationRepository;
        this.competenceRepository = competenceRepository;
        this.planDeveloppementRepository = planDeveloppementRepository;
        this.employeRepository = employeRepository;
        this.evenementRepository = evenementRepository;
    }

    @Transactional
    public EvaluationDto creer(UUID employeId, UUID managerId, Long managerDepartementId, List<EvaluationCompetenceRequest> competences) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMPLOYE_NOT_FOUND, new Object[]{employeId}));
        verifierMemeDepartement(employe, managerDepartementId);

        int annee = Year.now().getValue();
        if (evaluationRepository.existsByEmployeIdAndAnneeAndDeletedAtIsNull(employeId, annee)) {
            throw new ConflictException(ErrorCode.EVALUATION_DEJA_EXISTANTE, new Object[]{String.valueOf(annee)});
        }

        Evaluation evaluation = evaluationRepository.save(Evaluation.builder()
                .employeId(employeId)
                .managerId(managerId)
                .annee(annee)
                .statut(EvaluationStatut.EN_COURS)
                .build());

        List<EvaluationCompetence> lignes = enregistrerCompetences(evaluation.getId(), competences);
        return toDto(evaluation, employe.getNom(), employe.getPrenom(), lignes, List.of());
    }

    @Transactional
    public EvaluationDto mettreAJourEvaluationByReference(UUID reference, UUID managerId, List<EvaluationCompetenceRequest> competences) {
        Evaluation evaluation = evaluationRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EVALUATION_NOT_FOUND, new Object[]{reference}));
        if (!evaluation.getManagerId().equals(managerId)) {
            throw new AccessDeniedException("Seul le Manager ayant créé la grille peut la compléter/corriger.");
        }
        if (evaluation.getStatut() != EvaluationStatut.EN_COURS) {
            throw new ConflictException(ErrorCode.EVALUATION_DEJA_VALIDEE, new Object[]{reference});
        }

        List<EvaluationCompetence> lignes = enregistrerCompetences(evaluation.getId(), competences);

        List<String> manquantes = lignes.stream()
                .filter(c -> c.getNote() == null)
                .map(EvaluationCompetence::getCompetence)
                .toList();
        if (!manquantes.isEmpty()) {
            throw new ConflictException(ErrorCode.EVALUATION_INCOMPLETE, new Object[]{String.join(", ", manquantes)});
        }

        Employe employe = employeRepository.findById(evaluation.getEmployeId()).orElse(null);
        return toDto(evaluation, employe != null ? employe.getNom() : null,
                employe != null ? employe.getPrenom() : null, lignes, List.of());
    }

    // Remplace intégralement la grille (ajout/suppression/correction de
    // compétences) : plus simple et plus sûr qu'un diff ligne à ligne, aucune
    // autre table ne référence evaluation_competences.id.
    private List<EvaluationCompetence> enregistrerCompetences(Long evaluationId, List<EvaluationCompetenceRequest> competences) {
        competenceRepository.deleteByEvaluationId(evaluationId);
        List<EvaluationCompetence> lignes = competences.stream()
                .map(c -> EvaluationCompetence.builder()
                        .evaluationId(evaluationId)
                        .competence(c.competence())
                        .note(c.note())
                        .seuilExigence(c.seuilExigence())
                        .commentaire(c.commentaire())
                        .build())
                .toList();
        return competenceRepository.saveAll(lignes);
    }

    @Audited(action = "VALIDER_EVALUATION", tableCible = "evaluations")
    @Transactional
    public EvaluationDto validerEvaluationByReference(UUID reference) {
        Evaluation evaluation = evaluationRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EVALUATION_NOT_FOUND, new Object[]{reference}));
        if (evaluation.getStatut() != EvaluationStatut.EN_COURS) {
            throw new ConflictException(ErrorCode.EVALUATION_VALIDATION_STATUT_INVALIDE, new Object[]{reference});
        }

        Long evaluationId = evaluation.getId();
        List<EvaluationCompetence> lignes = competenceRepository.findByEvaluationIdAndDeletedAtIsNull(evaluationId);
        List<String> manquantes = lignes.stream().filter(c -> c.getNote() == null).map(EvaluationCompetence::getCompetence).toList();
        if (!manquantes.isEmpty()) {
            throw new ConflictException(ErrorCode.EVALUATION_INCOMPLETE, new Object[]{String.join(", ", manquantes)});
        }

        List<PlanDeveloppement> pdi = lignes.stream()
                .filter(c -> c.getNote().compareTo(c.getSeuilExigence()) < 0)
                .map(c -> planDeveloppementRepository.save(PlanDeveloppement.builder()
                        .evaluationId(evaluationId)
                        .competence(c.getCompetence())
                        .actions("Plan de développement à définir pour la compétence \"%s\" (note %s/%s).".formatted(
                                c.getCompetence(), c.getNote(), c.getSeuilExigence()))
                        .statut("A_TRAITER")
                        .build()))
                .toList();

        evaluation.setStatut(EvaluationStatut.VALIDEE_RH);
        evaluation.setDateValidationRh(LocalDateTime.now());
        evaluation = evaluationRepository.save(evaluation);

        Employe employe = employeRepository.findById(evaluation.getEmployeId()).orElse(null);
        pousserEvenement360(evaluation, pdi.size());

        return toDto(evaluation, employe != null ? employe.getNom() : null,
                employe != null ? employe.getPrenom() : null, lignes, pdi);
    }

    private void pousserEvenement360(Evaluation evaluation, int nombrePdi) {
        evenementRepository.save(EvenementHistorique.builder()
                .employeId(evaluation.getEmployeId())
                .titre("Évaluation annuelle " + evaluation.getAnnee() + " validée")
                .description(nombrePdi > 0
                        ? "Plan de développement individuel généré pour " + nombrePdi + " compétence(s) sous le seuil."
                        : "Toutes les compétences sont au niveau attendu.")
                .categorie(EvenementCategorie.EVALUATION)
                .statutBadge(nombrePdi > 0 ? StatutBadge.WARNING : StatutBadge.SUCCESS)
                .dateEvenement(evaluation.getDateValidationRh())
                .build());
    }

    @Transactional(readOnly = true)
    public List<EvaluationDto> parEmploye(UUID employeId, UUID principalEmployeId, Long principalDepartementId, boolean restreintAuDepartement) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMPLOYE_NOT_FOUND, new Object[]{employeId}));
        if (restreintAuDepartement) {
            verifierMemeDepartement(employe, principalDepartementId);
        }
        List<Evaluation> evaluations = evaluationRepository.findByEmployeIdAndDeletedAtIsNullOrderByAnneeDesc(employeId);
        return enrichir(evaluations, Map.of(employeId, employe));
    }

    @Transactional(readOnly = true)
    public List<EvaluationDto> search(EvaluationStatut statut, Integer annee, Long principalDepartementId, boolean restreintAuDepartement) {
        List<Evaluation> evaluations;
        if (statut != null && annee != null) {
            evaluations = evaluationRepository.findByStatutAndAnneeAndDeletedAtIsNullOrderByCreatedAtDesc(statut, annee);
        } else if (statut != null) {
            evaluations = evaluationRepository.findByStatutAndDeletedAtIsNullOrderByAnneeDescCreatedAtDesc(statut);
        } else {
            evaluations = evaluationRepository.findAllByDeletedAtIsNullOrderByAnneeDescCreatedAtDesc();
        }

        Map<UUID, Employe> employesParId = employeRepository.findAllById(
                        evaluations.stream().map(Evaluation::getEmployeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, e -> e));

        if (restreintAuDepartement) {
            evaluations = evaluations.stream()
                    .filter(e -> {
                        Employe employe = employesParId.get(e.getEmployeId());
                        return employe != null && employe.getDepartementId() != null
                                && employe.getDepartementId().equals(principalDepartementId);
                    })
                    .toList();
        }
        return enrichir(evaluations, employesParId);
    }

    private void verifierMemeDepartement(Employe employe, Long departementId) {
        if (departementId == null || !departementId.equals(employe.getDepartementId())) {
            throw new AccessDeniedException("Évaluation limitée aux employés de son propre département.");
        }
    }

    private List<EvaluationDto> enrichir(List<Evaluation> evaluations, Map<UUID, Employe> employesParId) {
        if (evaluations.isEmpty()) {
            return List.of();
        }
        List<Long> ids = evaluations.stream().map(Evaluation::getId).toList();
        Map<Long, List<EvaluationCompetence>> competencesParEvaluation = competenceRepository.findByEvaluationIdInAndDeletedAtIsNull(ids)
                .stream().collect(Collectors.groupingBy(EvaluationCompetence::getEvaluationId));
        Map<Long, List<PlanDeveloppement>> pdiParEvaluation = planDeveloppementRepository.findByEvaluationIdInAndDeletedAtIsNull(ids)
                .stream().collect(Collectors.groupingBy(PlanDeveloppement::getEvaluationId));

        return evaluations.stream()
                .map(e -> {
                    Employe employe = employesParId.get(e.getEmployeId());
                    return toDto(e, employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null,
                            competencesParEvaluation.getOrDefault(e.getId(), List.of()),
                            pdiParEvaluation.getOrDefault(e.getId(), List.of()));
                })
                .toList();
    }

    private EvaluationDto toDto(Evaluation evaluation, String employeNom, String employePrenom,
                                 List<EvaluationCompetence> competences, List<PlanDeveloppement> plansDeveloppement) {
        List<EvaluationCompetenceDto> competenceDtos = competences.stream().map(this::toDto).toList();
        List<String> manquantes = competences.stream()
                .filter(c -> c.getNote() == null)
                .map(EvaluationCompetence::getCompetence)
                .toList();
        return new EvaluationDto(evaluation.getReference(), evaluation.getEmployeId(), employeNom, employePrenom,
                evaluation.getManagerId(), evaluation.getAnnee(), evaluation.getStatut(), evaluation.getDateEntretien(),
                evaluation.getDateValidationRh(), evaluation.getCreatedAt(), competenceDtos, manquantes,
                plansDeveloppement.stream().map(this::toDto).toList());
    }

    private EvaluationCompetenceDto toDto(EvaluationCompetence c) {
        return new EvaluationCompetenceDto(c.getReference(), c.getCompetence(), c.getNote(), c.getSeuilExigence(), c.getCommentaire());
    }

    private PlanDeveloppementDto toDto(PlanDeveloppement p) {
        return new PlanDeveloppementDto(p.getReference(), p.getCompetence(), p.getActions(), p.getCreatedAt(), p.getStatut());
    }
}
