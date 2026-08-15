package com.sirh.service;

import com.sirh.service.CloturePaieService;
import com.sirh.annotation.Audited;
import com.sirh.model.dto.AutorisationDto;
import com.sirh.model.entity.Autorisation;
import com.sirh.exception.AutorisationNotFoundException;
import com.sirh.exception.DelaiRegularisationDepasseException;
import com.sirh.exception.DemandeStatutInvalideException;
import com.sirh.exception.QuotaAutorisationDepasseException;
import com.sirh.model.repository.AutorisationRepository;
import com.sirh.event.DemandeCreeEvent;
import com.sirh.model.entity.Employe;
import com.sirh.exception.EmployeNotFoundException;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.enums.PlanningStatut;
import com.sirh.service.PlanningService;
import com.sirh.service.PointageBrutService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Workflow d'autorisation d'absence courte (Module 13) : saisie Manager avec
 * avis (statut EN_ATTENTE_RH) ou saisie RH d'urgence (statut VALIDE direct),
 * délai de régularisation 72h, quota mensuel 4h, justification automatique
 * des anomalies de pointage et mise à jour du planning à la validation.
 */
@Service
public class AutorisationService {

    private static final long DELAI_REGULARISATION_HEURES = 72;
    private static final int QUOTA_MENSUEL_MINUTES = 240;
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_RH = "RH";
    private static final String STATUT_EN_ATTENTE_RH = "EN_ATTENTE_RH";
    private static final String STATUT_VALIDE = "VALIDE";
    private static final String STATUT_REFUSE = "REFUSE";

    private final AutorisationRepository autorisationRepository;
    private final EmployeRepository employeRepository;
    private final PointageBrutService pointageBrutService;
    private final PlanningService planningService;
    private final CloturePaieService cloturePaieService;
    private final ApplicationEventPublisher eventPublisher;

    public AutorisationService(AutorisationRepository autorisationRepository, EmployeRepository employeRepository,
                                PointageBrutService pointageBrutService, PlanningService planningService,
                                CloturePaieService cloturePaieService, ApplicationEventPublisher eventPublisher) {
        this.autorisationRepository = autorisationRepository;
        this.employeRepository = employeRepository;
        this.pointageBrutService = pointageBrutService;
        this.planningService = planningService;
        this.cloturePaieService = cloturePaieService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Autorisation creer(UUID employeId, String type, LocalDate dateAutorisation, LocalTime heureDebut,
                               LocalTime heureFin, Integer dureeMinutes, String motif, UUID creeParId,
                               String creeParRole, String avisManagerStatut, String avisManagerCommentaire) {
        employeRepository.findById(employeId).orElseThrow(() -> new EmployeNotFoundException(employeId));
        // Cas exceptionnel Module 3 : une nouvelle autorisation ne peut pas être
        // saisie sur un mois déjà clôturé, y compris via le workflow d'urgence RH.
        cloturePaieService.verifierMoisOuvert(dateAutorisation);

        if (ROLE_MANAGER.equals(creeParRole)) {
            LocalDateTime reference = LocalDateTime.of(dateAutorisation, heureDebut);
            if (Duration.between(reference, LocalDateTime.now()).toHours() > DELAI_REGULARISATION_HEURES) {
                throw new DelaiRegularisationDepasseException();
            }
        }

        boolean urgenceRh = ROLE_RH.equals(creeParRole);
        Autorisation autorisation = Autorisation.builder()
                .employeId(employeId)
                .type(type)
                .dateAutorisation(dateAutorisation)
                .heureDebut(heureDebut)
                .heureFin(heureFin)
                .dureeMinutes(dureeMinutes)
                .motif(motif)
                .statut(urgenceRh ? STATUT_VALIDE : STATUT_EN_ATTENTE_RH)
                .creeParId(creeParId)
                .creeParRole(creeParRole)
                .avisManagerStatut(ROLE_MANAGER.equals(creeParRole) ? avisManagerStatut : null)
                .avisManagerCommentaire(ROLE_MANAGER.equals(creeParRole) ? avisManagerCommentaire : null)
                .build();
        autorisation = autorisationRepository.save(autorisation);

        // Workflow d'urgence (Module 13) : le RH valide instantanément, donc la
        // justification des anomalies de pointage et la mise à jour du planning
        // doivent s'appliquer immédiatement (même bug que IndisponibiliteService,
        // trouvé en vérification manuelle). Le contrôle de quota bloquant reste
        // volontairement réservé à la validation RH explicite en deux temps
        // (workflow Manager) : bloquer l'urgence RH sur un total mensuel sans
        // rapport avec l'absence à traiter irait à l'encontre de l'objectif même
        // de ce chemin — traiter vite un cas réellement urgent.
        if (urgenceRh) {
            appliquerImpactValidation(autorisation);
        } else {
            eventPublisher.publishEvent(new DemandeCreeEvent("AUTORISATION", autorisation.getId(),
                    autorisation.getEmployeId(), autorisation.getType()));
        }
        return autorisation;
    }

    @Audited(action = "VALIDER_AUTORISATION", tableCible = "autorisations")
    @Transactional
    public Autorisation valider(Long id, String traitementDepassement, UUID valideParRhId) {
        Autorisation autorisation = findById(id);
        if (!STATUT_EN_ATTENTE_RH.equals(autorisation.getStatut())) {
            throw new DemandeStatutInvalideException(
                    "Seule une autorisation EN_ATTENTE_RH peut être validée (statut actuel : " + autorisation.getStatut() + ").");
        }

        int cumulMinutes = cumulMinutesMois(autorisation.getEmployeId(), autorisation.getDateAutorisation());
        if (cumulMinutes > QUOTA_MENSUEL_MINUTES && (traitementDepassement == null || traitementDepassement.isBlank())) {
            throw new QuotaAutorisationDepasseException(cumulMinutes - QUOTA_MENSUEL_MINUTES);
        }

        autorisation.setStatut(STATUT_VALIDE);
        autorisation.setValideParRhId(valideParRhId);
        autorisation = autorisationRepository.save(autorisation);

        appliquerImpactValidation(autorisation);

        return autorisation;
    }

    // Partagé entre creer() (workflow d'urgence RH, statut VALIDE dès la
    // création) et valider() (workflow normal) : les deux chemins produisent le
    // même impact — justification des anomalies de pointage, mise à jour du planning.
    private void appliquerImpactValidation(Autorisation autorisation) {
        pointageBrutService.justifierAnomaliesDuJour(autorisation.getEmployeId(), autorisation.getDateAutorisation(), autorisation.getId());

        LocalDateTime debut = LocalDateTime.of(autorisation.getDateAutorisation(), autorisation.getHeureDebut());
        LocalDateTime fin = autorisation.getHeureFin() != null
                ? LocalDateTime.of(autorisation.getDateAutorisation(), autorisation.getHeureFin())
                : debut.plusMinutes(autorisation.getDureeMinutes() != null ? autorisation.getDureeMinutes() : 0);
        planningService.ecraserCreneaux(autorisation.getEmployeId(), debut, fin, PlanningStatut.ABSENCE,
                "Autorisation #" + autorisation.getId() + " (" + autorisation.getType() + ")");
    }

    @Audited(action = "REFUSER_AUTORISATION", tableCible = "autorisations")
    @Transactional
    public Autorisation refuser(Long id) {
        Autorisation autorisation = findById(id);
        if (!STATUT_EN_ATTENTE_RH.equals(autorisation.getStatut())) {
            throw new DemandeStatutInvalideException(
                    "Seule une autorisation EN_ATTENTE_RH peut être refusée (statut actuel : " + autorisation.getStatut() + ").");
        }
        autorisation.setStatut(STATUT_REFUSE);
        return autorisationRepository.save(autorisation);
    }

    @Transactional(readOnly = true)
    public Autorisation findById(Long id) {
        return autorisationRepository.findById(id).orElseThrow(() -> new AutorisationNotFoundException(id));
    }

    // Somme des durées EN_ATTENTE_RH + VALIDE sur le mois calendaire de
    // dateReference (inclut l'autorisation en cours de validation, déjà en base).
    private int cumulMinutesMois(UUID employeId, LocalDate dateReference) {
        LocalDate debutMois = dateReference.withDayOfMonth(1);
        LocalDate finMois = debutMois.plusMonths(1).minusDays(1);
        return autorisationRepository
                .findByEmployeIdAndDateAutorisationBetweenAndStatutInAndDeletedAtIsNull(
                        employeId, debutMois, finMois, List.of(STATUT_EN_ATTENTE_RH, STATUT_VALIDE))
                .stream()
                .mapToInt(a -> a.getDureeMinutes() != null ? a.getDureeMinutes() : 0)
                .sum();
    }

    @Transactional(readOnly = true)
    public List<Autorisation> search(String statut, UUID employeId, Long departementId, LocalDate dateDebut, LocalDate dateFin) {
        List<UUID> employeIdsDepartement = departementId != null
                ? employeRepository.findByDepartement_IdAndDeletedAtIsNull(departementId).stream().map(Employe::getId).toList()
                : null;

        Specification<Autorisation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            if (employeId != null) {
                predicates.add(cb.equal(root.get("employeId"), employeId));
            }
            if (employeIdsDepartement != null) {
                predicates.add(employeIdsDepartement.isEmpty()
                        ? cb.disjunction()
                        : root.get("employeId").in(employeIdsDepartement));
            }
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateAutorisation"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateAutorisation"), dateFin));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return autorisationRepository.findAll(spec);
    }

    // Manager : restreint strictement à son propre département, sans jamais
    // faire confiance à un employeId/departementId fourni par le client — même
    // principe que MonPlanningController (filtre serveur, pas client). Un
    // Manager sans département (fixture par défaut, régression déjà couverte
    // au Lot 3 pour /plannings/departement) ne doit jamais retomber sur une
    // recherche non filtrée : liste vide plutôt qu'une fuite vers "tout voir".
    @Transactional(readOnly = true)
    public List<Autorisation> searchPourManager(Long departementIdManager, String statut, LocalDate dateDebut, LocalDate dateFin) {
        if (departementIdManager == null) {
            return List.of();
        }
        return search(statut, null, departementIdManager, dateDebut, dateFin);
    }

    @Transactional(readOnly = true)
    public List<AutorisationDto> enrichir(List<Autorisation> autorisations) {
        Map<UUID, Employe> employesParId = employeRepository
                .findAllById(autorisations.stream().map(Autorisation::getEmployeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, e -> e));
        return autorisations.stream()
                .map(a -> {
                    Employe employe = employesParId.get(a.getEmployeId());
                    return AutorisationDto.from(a, employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null, null);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AutorisationDto enrichirAvecQuota(Autorisation autorisation) {
        Employe employe = employeRepository.findById(autorisation.getEmployeId()).orElse(null);
        int cumul = cumulMinutesMois(autorisation.getEmployeId(), autorisation.getDateAutorisation());
        Integer depassement = cumul > QUOTA_MENSUEL_MINUTES ? cumul - QUOTA_MENSUEL_MINUTES : null;
        return AutorisationDto.from(autorisation, employe != null ? employe.getNom() : null,
                employe != null ? employe.getPrenom() : null, depassement);
    }
}
