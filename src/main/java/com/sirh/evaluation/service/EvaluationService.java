package com.sirh.evaluation.service;

import com.sirh.audit.annotation.Audited;
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
import com.sirh.personnel.entity.EvenementCategorie;
import com.sirh.personnel.entity.EvenementHistorique;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.entity.StatutBadge;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.personnel.repository.EvenementHistoriqueRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable : " + employeId));
        verifierMemeDepartement(employe, managerDepartementId);

        int annee = Year.now().getValue();
        if (evaluationRepository.existsByEmployeIdAndAnnee(employeId, annee)) {
            throw new EvaluationStatutInvalideException(
                    "Une évaluation existe déjà pour cet employé sur l'année " + annee + ".");
        }

        Evaluation evaluation = evaluationRepository.save(Evaluation.builder()
                .employeId(employeId)
                .managerId(managerId)
                .annee(annee)
                .statut(EvaluationStatut.EN_COURS)
                .build());

        List<EvaluationCompetence> lignes = enregistrerCompetences(evaluation.getId(), competences);
        return EvaluationDto.from(evaluation, employe.getNom(), employe.getPrenom(), lignes, List.of());
    }

    @Transactional
    public EvaluationDto mettreAJour(Long evaluationId, UUID managerId, List<EvaluationCompetenceRequest> competences) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));
        if (!evaluation.getManagerId().equals(managerId)) {
            throw new AccessDeniedException("Seul le Manager ayant créé la grille peut la compléter/corriger.");
        }
        if (evaluation.getStatut() != EvaluationStatut.EN_COURS) {
            throw new EvaluationStatutInvalideException(
                    "Grille déjà validée par le RH, elle ne peut plus être modifiée (id " + evaluationId + ").");
        }

        List<EvaluationCompetence> lignes = enregistrerCompetences(evaluation.getId(), competences);

        List<String> manquantes = lignes.stream()
                .filter(c -> c.getNote() == null)
                .map(EvaluationCompetence::getCompetence)
                .toList();
        if (!manquantes.isEmpty()) {
            throw new EvaluationIncompleteException(manquantes);
        }

        Employe employe = employeRepository.findById(evaluation.getEmployeId()).orElse(null);
        return EvaluationDto.from(evaluation, employe != null ? employe.getNom() : null,
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
    public EvaluationDto valider(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));
        if (evaluation.getStatut() != EvaluationStatut.EN_COURS) {
            throw new EvaluationStatutInvalideException(
                    "Seule une évaluation EN_COURS peut être validée (id " + evaluationId + ").");
        }

        List<EvaluationCompetence> lignes = competenceRepository.findByEvaluationId(evaluationId);
        List<String> manquantes = lignes.stream().filter(c -> c.getNote() == null).map(EvaluationCompetence::getCompetence).toList();
        if (!manquantes.isEmpty()) {
            throw new EvaluationIncompleteException(manquantes);
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

        return EvaluationDto.from(evaluation, employe != null ? employe.getNom() : null,
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
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable : " + employeId));
        if (restreintAuDepartement) {
            verifierMemeDepartement(employe, principalDepartementId);
        }
        List<Evaluation> evaluations = evaluationRepository.findByEmployeIdOrderByAnneeDesc(employeId);
        return enrichir(evaluations, Map.of(employeId, employe));
    }

    @Transactional(readOnly = true)
    public List<EvaluationDto> search(EvaluationStatut statut, Integer annee, Long principalDepartementId, boolean restreintAuDepartement) {
        List<Evaluation> evaluations;
        if (statut != null && annee != null) {
            evaluations = evaluationRepository.findByStatutAndAnneeOrderByCreatedAtDesc(statut, annee);
        } else if (statut != null) {
            evaluations = evaluationRepository.findByStatutOrderByAnneeDescCreatedAtDesc(statut);
        } else {
            evaluations = evaluationRepository.findAllByOrderByAnneeDescCreatedAtDesc();
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
        Map<Long, List<EvaluationCompetence>> competencesParEvaluation = competenceRepository.findByEvaluationIdIn(ids)
                .stream().collect(Collectors.groupingBy(EvaluationCompetence::getEvaluationId));
        Map<Long, List<PlanDeveloppement>> pdiParEvaluation = planDeveloppementRepository.findByEvaluationIdIn(ids)
                .stream().collect(Collectors.groupingBy(PlanDeveloppement::getEvaluationId));

        return evaluations.stream()
                .map(e -> {
                    Employe employe = employesParId.get(e.getEmployeId());
                    return EvaluationDto.from(e, employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null,
                            competencesParEvaluation.getOrDefault(e.getId(), List.of()),
                            pdiParEvaluation.getOrDefault(e.getId(), List.of()));
                })
                .toList();
    }
}
