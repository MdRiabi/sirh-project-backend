package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.event.DemandeCreeEvent;
import com.sirh.exception.ConflictException;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.AutorisationDto;
import com.sirh.model.entity.Autorisation;
import com.sirh.model.entity.Employe;
import com.sirh.model.enums.PlanningStatut;
import com.sirh.model.enums.Role;
import com.sirh.model.repository.AutorisationRepository;
import com.sirh.model.repository.EmployeRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workflow d'autorisation d'absence courte (Module 13) : saisie Manager avec avis (statut
 * EN_ATTENTE_RH) ou saisie RH d'urgence (statut VALIDE direct), délai de régularisation 72h, quota
 * mensuel 4h, justification automatique des anomalies de pointage et mise à jour du planning à la
 * validation.
 */
@Service
@RequiredArgsConstructor
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
  private final ModelMapper modelMapper;

  @Transactional
  public AutorisationDto creer(
      UUID employeId,
      String type,
      LocalDate dateAutorisation,
      LocalTime heureDebut,
      LocalTime heureFin,
      Integer dureeMinutes,
      String motif,
      UUID creeParId,
      Role creePar,
      String avisManagerStatut,
      String avisManagerCommentaire) {
    // cree_par_role (schéma) n'accepte que MANAGER/RH — le SUPERUSER, qui a les
    // mêmes droits que le RH (cahier-technique.md §2.1), est tracé comme "RH".
    String creeParRole = creePar == Role.ROLE_MANAGER ? ROLE_MANAGER : ROLE_RH;
    employeRepository
        .findById(employeId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ErrorCode.EMPLOYE_NOT_FOUND, new Object[] {employeId}));
    // Cas exceptionnel Module 3 : une nouvelle autorisation ne peut pas être
    // saisie sur un mois déjà clôturé, y compris via le workflow d'urgence RH.
    cloturePaieService.verifierMoisOuvert(dateAutorisation);

    if (ROLE_MANAGER.equals(creeParRole)) {
      LocalDateTime reference = LocalDateTime.of(dateAutorisation, heureDebut);
      if (Duration.between(reference, LocalDateTime.now()).toHours()
          > DELAI_REGULARISATION_HEURES) {
        throw new ConflictException(ErrorCode.DELAI_REGULARISATION_DEPASSE);
      }
    }

    boolean urgenceRh = ROLE_RH.equals(creeParRole);
    Autorisation autorisation =
        Autorisation.builder()
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
            .avisManagerCommentaire(
                ROLE_MANAGER.equals(creeParRole) ? avisManagerCommentaire : null)
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
      eventPublisher.publishEvent(
          new DemandeCreeEvent(
              "AUTORISATION",
              autorisation.getId(),
              autorisation.getEmployeId(),
              autorisation.getType()));
    }
    return enrichirAvecQuota(autorisation);
  }

  @Audited(action = "VALIDER_AUTORISATION", tableCible = "autorisations")
  @Transactional
  public AutorisationDto validerAutorisationByReference(
      UUID reference, String traitementDepassement, UUID valideParRhId) {
    Autorisation autorisation = getAutorisationByReference(reference);
    if (!STATUT_EN_ATTENTE_RH.equals(autorisation.getStatut())) {
      throw new ConflictException(
          ErrorCode.AUTORISATION_VALIDATION_STATUT_INVALIDE,
          new Object[] {autorisation.getStatut()});
    }

    int cumulMinutes =
        cumulMinutesMois(autorisation.getEmployeId(), autorisation.getDateAutorisation());
    if (cumulMinutes > QUOTA_MENSUEL_MINUTES
        && (traitementDepassement == null || traitementDepassement.isBlank())) {
      throw new ConflictException(
          ErrorCode.QUOTA_AUTORISATION_DEPASSE,
          new Object[] {String.valueOf(cumulMinutes - QUOTA_MENSUEL_MINUTES)});
    }

    autorisation.setStatut(STATUT_VALIDE);
    autorisation.setValideParRhId(valideParRhId);
    autorisation = autorisationRepository.save(autorisation);

    appliquerImpactValidation(autorisation);

    return enrichirAvecQuota(autorisation);
  }

  // Partagé entre creer() (workflow d'urgence RH, statut VALIDE dès la
  // création) et valider() (workflow normal) : les deux chemins produisent le
  // même impact — justification des anomalies de pointage, mise à jour du planning.
  private void appliquerImpactValidation(Autorisation autorisation) {
    pointageBrutService.justifierAnomaliesDuJour(
        autorisation.getEmployeId(), autorisation.getDateAutorisation(), autorisation.getId());

    LocalDateTime debut =
        LocalDateTime.of(autorisation.getDateAutorisation(), autorisation.getHeureDebut());
    LocalDateTime fin =
        autorisation.getHeureFin() != null
            ? LocalDateTime.of(autorisation.getDateAutorisation(), autorisation.getHeureFin())
            : debut.plusMinutes(
                autorisation.getDureeMinutes() != null ? autorisation.getDureeMinutes() : 0);
    planningService.ecraserCreneaux(
        autorisation.getEmployeId(),
        debut,
        fin,
        PlanningStatut.ABSENCE,
        "Autorisation #" + autorisation.getId() + " (" + autorisation.getType() + ")");
  }

  @Audited(action = "REFUSER_AUTORISATION", tableCible = "autorisations")
  @Transactional
  public AutorisationDto refuserAutorisationByReference(UUID reference) {
    Autorisation autorisation = getAutorisationByReference(reference);
    if (!STATUT_EN_ATTENTE_RH.equals(autorisation.getStatut())) {
      throw new ConflictException(
          ErrorCode.AUTORISATION_REFUS_STATUT_INVALIDE, new Object[] {autorisation.getStatut()});
    }
    autorisation.setStatut(STATUT_REFUSE);
    return enrichirAvecQuota(autorisationRepository.save(autorisation));
  }

  @Transactional(readOnly = true)
  public Autorisation getAutorisationByReference(UUID reference) {
    return autorisationRepository
        .findByReferenceAndDeletedAtIsNull(reference)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ErrorCode.AUTORISATION_NOT_FOUND, new Object[] {reference}));
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
  public List<AutorisationDto> search(
      String statut, UUID employeId, Long departementId, LocalDate dateDebut, LocalDate dateFin) {
    return enrichir(searchEntities(statut, employeId, departementId, dateDebut, dateFin));
  }

  private List<Autorisation> searchEntities(
      String statut, UUID employeId, Long departementId, LocalDate dateDebut, LocalDate dateFin) {
    List<UUID> employeIdsDepartement =
        departementId != null
            ? employeRepository.findByDepartement_IdAndDeletedAtIsNull(departementId).stream()
                .map(Employe::getId)
                .toList()
            : null;

    Specification<Autorisation> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (statut != null) {
            predicates.add(cb.equal(root.get("statut"), statut));
          }
          if (employeId != null) {
            predicates.add(cb.equal(root.get("employeId"), employeId));
          }
          if (employeIdsDepartement != null) {
            predicates.add(
                employeIdsDepartement.isEmpty()
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
  public List<AutorisationDto> searchPourManager(
      Long departementIdManager, String statut, LocalDate dateDebut, LocalDate dateFin) {
    if (departementIdManager == null) {
      return List.of();
    }
    return search(statut, null, departementIdManager, dateDebut, dateFin);
  }

  @Transactional(readOnly = true)
  public List<AutorisationDto> searchAvecScope(
      Role role,
      Long principalDepartementId,
      String statut,
      UUID employeId,
      Long departementId,
      LocalDate dateDebut,
      LocalDate dateFin) {
    return role == Role.ROLE_MANAGER
        ? searchPourManager(principalDepartementId, statut, dateDebut, dateFin)
        : search(statut, employeId, departementId, dateDebut, dateFin);
  }

  private List<AutorisationDto> enrichir(List<Autorisation> autorisations) {
    Map<UUID, Employe> employesParId =
        employeRepository
            .findAllById(autorisations.stream().map(Autorisation::getEmployeId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Employe::getId, e -> e));
    return autorisations.stream()
        .map(
            a -> {
              Employe employe = employesParId.get(a.getEmployeId());
              return toDto(
                  a,
                  employe != null ? employe.getNom() : null,
                  employe != null ? employe.getPrenom() : null,
                  null);
            })
        .toList();
  }

  private AutorisationDto enrichirAvecQuota(Autorisation autorisation) {
    Employe employe = employeRepository.findById(autorisation.getEmployeId()).orElse(null);
    int cumul = cumulMinutesMois(autorisation.getEmployeId(), autorisation.getDateAutorisation());
    Integer depassement = cumul > QUOTA_MENSUEL_MINUTES ? cumul - QUOTA_MENSUEL_MINUTES : null;
    return toDto(
        autorisation,
        employe != null ? employe.getNom() : null,
        employe != null ? employe.getPrenom() : null,
        depassement);
  }

  private AutorisationDto toDto(
      Autorisation a, String employeNom, String employePrenom, Integer quotaDepasseMinutes) {
    AutorisationDto dto = modelMapper.map(a, AutorisationDto.class);
    dto.setEmployeNom(employeNom);
    dto.setEmployePrenom(employePrenom);
    dto.setQuotaDepasseMinutes(quotaDepasseMinutes);
    return dto;
  }
}
