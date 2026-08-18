package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.exception.ConflictException;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.exception.TechnicalException;
import com.sirh.model.dto.ClotureDto;
import com.sirh.model.dto.DemandeDeverrouillageDto;
import com.sirh.model.entity.CloturePaie;
import com.sirh.model.entity.DemandeDeverrouillage;
import com.sirh.model.entity.Employe;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.Role;
import com.sirh.model.repository.AutorisationRepository;
import com.sirh.model.repository.CloturePaieRepository;
import com.sirh.model.repository.DemandeDeverrouillageRepository;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.IndisponibiliteRepository;
import com.sirh.model.repository.PointageBrutRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clôture de paie irréversible (sauf déverrouillage exceptionnel) et workflow de déverrouillage
 * (cahier-fonctionnel.md Module 3). Clôture globale par mois (décision d'architecture Lot 6, pas
 * par département) : préconditions bloquantes — taux d'anomalies justifiées ≥
 * SEUIL_ANOMALIES_CLOTURE, aucune demande EN_ATTENTE_RH sur le mois.
 */
@Service
@RequiredArgsConstructor
public class CloturePaieService {

  private static final Logger log = LoggerFactory.getLogger(CloturePaieService.class);

  private static final String STATUT_OUVERT = "OUVERT";
  private static final String STATUT_CLOTURE = "CLOTURE";
  private static final String STATUT_EN_ATTENTE_RH = "EN_ATTENTE_RH";
  private static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
  private static final String STATUT_APPROUVEE = "APPROUVEE";

  private final CloturePaieRepository cloturePaieRepository;
  private final DemandeDeverrouillageRepository demandeDeverrouillageRepository;
  private final AutorisationRepository autorisationRepository;
  private final IndisponibiliteRepository indisponibiliteRepository;
  private final PointageBrutRepository pointageBrutRepository;
  private final ParametreGlobalService parametreGlobalService;
  private final EmployeRepository employeRepository;
  private final ModelMapper modelMapper;

  // GET /assiduite/cloture-paie : lecture du statut sans forcer la clôture ni
  // exiger qu'une ligne existe déjà (mois jamais clôturé -> OUVERT par
  // défaut, objet transitoire non persisté).
  @Transactional(readOnly = true)
  public ClotureDto trouverOuDefaut(YearMonth moisParam) {
    LocalDate mois = moisParam.atDay(1);
    return toDto(
        cloturePaieRepository
            .findByMoisAndDeletedAtIsNull(mois)
            .orElseGet(() -> CloturePaie.builder().mois(mois).build()));
  }

  // Appelée avant toute écriture manuelle sur pointages_bruts (correction),
  // autorisations ou indisponibilites (création) — cf. PointageBrutService,
  // AutorisationService, IndisponibiliteService.
  @Transactional(readOnly = true)
  public void verifierMoisOuvert(LocalDate date) {
    LocalDate mois = date.withDayOfMonth(1);
    cloturePaieRepository
        .findByMoisAndDeletedAtIsNull(mois)
        .filter(c -> STATUT_CLOTURE.equals(c.getStatut()))
        .ifPresent(
            c -> {
              throw new ConflictException(
                  ErrorCode.MOIS_DEJA_CLOTURE, new Object[] {formatMois(mois)});
            });
  }

  private static String formatMois(LocalDate mois) {
    return mois.getYear() + "-" + String.format("%02d", mois.getMonthValue());
  }

  @Audited(action = "CLOTURER_PAIE", tableCible = "cloture_paie")
  @Transactional
  public ClotureDto cloturer(YearMonth moisParam, UUID clotureParId) {
    LocalDate mois = moisParam.atDay(1);
    LocalDate finMois = moisParam.atEndOfMonth();
    CloturePaie cloture =
        cloturePaieRepository
            .findByMoisAndDeletedAtIsNull(mois)
            .orElseGet(() -> CloturePaie.builder().mois(mois).build());
    if (STATUT_CLOTURE.equals(cloture.getStatut())) {
      throw new ConflictException(ErrorCode.MOIS_DEJA_CLOTURE, new Object[] {formatMois(mois)});
    }

    BigDecimal tauxAnomaliesJustifiees = tauxAnomaliesJustifiees(mois, finMois);
    BigDecimal seuil = parametreGlobalService.valeurDecimale("SEUIL_ANOMALIES_CLOTURE");
    if (tauxAnomaliesJustifiees.compareTo(seuil) < 0) {
      throw new ConflictException(
          ErrorCode.CLOTURE_TAUX_ANOMALIES_INSUFFISANT,
          new Object[] {
            tauxAnomaliesJustifiees.stripTrailingZeros().toPlainString(),
            seuil.stripTrailingZeros().toPlainString()
          });
    }

    boolean demandesEnAttente =
        !autorisationRepository
                .findByStatutAndDateAutorisationBetweenAndDeletedAtIsNull(
                    STATUT_EN_ATTENTE_RH, mois, finMois)
                .isEmpty()
            || !indisponibiliteRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualAndDeletedAtIsNull(
                    STATUT_EN_ATTENTE_RH, finMois, mois)
                .isEmpty();
    if (demandesEnAttente) {
      throw new ConflictException(ErrorCode.CLOTURE_DEMANDES_EN_ATTENTE);
    }

    cloture.setStatut(STATUT_CLOTURE);
    cloture.setDateCloture(LocalDateTime.now());
    cloture.setClotureParId(clotureParId);
    cloture.setTauxAnomaliesJustifiees(tauxAnomaliesJustifiees);
    return toDto(cloturePaieRepository.save(cloture));
  }

  private BigDecimal tauxAnomaliesJustifiees(LocalDate mois, LocalDate finMois) {
    LocalDateTime debut = mois.atStartOfDay();
    LocalDateTime fin = finMois.plusDays(1).atStartOfDay();
    long justifiees =
        pointageBrutRepository.countByTimestampPointageBetweenAndStatutAndDeletedAtIsNull(
            debut, fin, PointageStatut.JUSTIFIE);
    long nonJustifiees =
        pointageBrutRepository.countByTimestampPointageBetweenAndStatutAndDeletedAtIsNull(
            debut, fin, PointageStatut.ANOMALIE_NON_JUSTIFIEE);
    long total = justifiees + nonJustifiees;
    return total == 0
        ? BigDecimal.valueOf(100)
        : BigDecimal.valueOf(justifiees * 100)
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  @Transactional
  public DemandeDeverrouillageDto demanderDeverrouillage(
      YearMonth moisParam, String motif, UUID demandeParId) {
    LocalDate mois = moisParam.atDay(1);
    boolean moisCloture =
        cloturePaieRepository
            .findByMoisAndDeletedAtIsNull(mois)
            .map(c -> STATUT_CLOTURE.equals(c.getStatut()))
            .orElse(false);
    if (!moisCloture) {
      throw new ConflictException(ErrorCode.MOIS_NON_CLOTURE);
    }
    DemandeDeverrouillage demande =
        demandeDeverrouillageRepository.save(
            DemandeDeverrouillage.builder()
                .mois(mois)
                .motif(motif)
                .demandeParId(demandeParId)
                .build());
    return enrichir(List.of(demande)).get(0);
  }

  @Audited(action = "TRAITER_DEMANDE_DEVERROUILLAGE", tableCible = "demandes_deverrouillage")
  @Transactional
  public DemandeDeverrouillageDto traiterDemandeByReference(
      UUID reference, String statut, UUID traiteParId) {
    DemandeDeverrouillage demande = getDemandeByReference(reference);
    if (!STATUT_EN_ATTENTE.equals(demande.getStatut())) {
      throw new ConflictException(
          ErrorCode.DEMANDE_DEVERROUILLAGE_STATUT_INVALIDE, new Object[] {demande.getStatut()});
    }

    LocalDate mois = demande.getMois();
    demande.setStatut(statut);
    demande.setTraiteParId(traiteParId);
    demande.setDateTraitement(LocalDateTime.now());
    DemandeDeverrouillage sauvegardee = demandeDeverrouillageRepository.save(demande);

    if (STATUT_APPROUVEE.equals(statut)) {
      CloturePaie cloture =
          cloturePaieRepository
              .findByMoisAndDeletedAtIsNull(mois)
              .orElseThrow(
                  () -> {
                    log.error(
                        "Clôture introuvable pour le mois {} lors de l'approbation d'une demande de"
                            + " déverrouillage — incohérence de données.",
                        mois);
                    return new TechnicalException(ErrorCode.SYSTEM_ERROR);
                  });
      cloture.setStatut(STATUT_OUVERT);
      cloturePaieRepository.save(cloture);
    }
    return enrichir(List.of(sauvegardee)).get(0);
  }

  @Transactional(readOnly = true)
  public DemandeDeverrouillage getDemandeByReference(UUID reference) {
    return demandeDeverrouillageRepository
        .findByReferenceAndDeletedAtIsNull(reference)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ErrorCode.DEMANDE_DEVERROUILLAGE_NOT_FOUND, new Object[] {reference}));
  }

  // Filtré par mois/statut et, indirectement, par département : le
  // demandeur (demande_par_id) est résolu vers son département via
  // EmployeRepository (cloture_paie/demandes_deverrouillage restent
  // globales par mois, aucune colonne département sur la table) — même
  // principe que AutorisationService.search (chaîne employeId -> departementId).
  @Transactional(readOnly = true)
  public List<DemandeDeverrouillageDto> listerDemandes(
      YearMonth moisParam, Long departementId, String statut) {
    return enrichir(listerDemandesEntities(moisParam, departementId, statut));
  }

  private List<DemandeDeverrouillage> listerDemandesEntities(
      YearMonth moisParam, Long departementId, String statut) {
    LocalDate mois = moisParam != null ? moisParam.atDay(1) : null;
    List<UUID> employeIdsDepartement =
        departementId != null
            ? employeRepository.findByDepartement_IdAndDeletedAtIsNull(departementId).stream()
                .map(Employe::getId)
                .toList()
            : null;

    Specification<DemandeDeverrouillage> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (mois != null) {
            predicates.add(cb.equal(root.get("mois"), mois));
          }
          if (statut != null) {
            predicates.add(cb.equal(root.get("statut"), statut));
          }
          if (employeIdsDepartement != null) {
            predicates.add(
                employeIdsDepartement.isEmpty()
                    ? cb.disjunction()
                    : root.get("demandeParId").in(employeIdsDepartement));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return demandeDeverrouillageRepository.findAll(spec);
  }

  // Manager : restreint strictement à son propre département — même
  // garde-fou que AutorisationService.searchPourManager (liste vide plutôt
  // qu'une recherche non filtrée si le compte n'a pas de département).
  @Transactional(readOnly = true)
  public List<DemandeDeverrouillageDto> listerDemandesPourManager(
      YearMonth mois, Long departementIdManager, String statut) {
    if (departementIdManager == null) {
      return List.of();
    }
    return listerDemandes(mois, departementIdManager, statut);
  }

  @Transactional(readOnly = true)
  public List<DemandeDeverrouillageDto> listerDemandesAvecScope(
      Role role,
      Long principalDepartementId,
      YearMonth moisParam,
      Long departementIdParam,
      String statut) {
    return role == Role.ROLE_MANAGER
        ? listerDemandesPourManager(moisParam, principalDepartementId, statut)
        : listerDemandes(moisParam, departementIdParam, statut);
  }

  private List<DemandeDeverrouillageDto> enrichir(List<DemandeDeverrouillage> demandes) {
    Map<UUID, Employe> employesParId =
        employeRepository
            .findAllById(
                demandes.stream().map(DemandeDeverrouillage::getDemandeParId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Employe::getId, e -> e));
    return demandes.stream()
        .map(
            d -> {
              Employe employe = employesParId.get(d.getDemandeParId());
              DemandeDeverrouillageDto dto = modelMapper.map(d, DemandeDeverrouillageDto.class);
              dto.setDemandeParNom(employe != null ? employe.getNom() : null);
              dto.setDemandeParPrenom(employe != null ? employe.getPrenom() : null);
              dto.setDepartementId(employe != null ? employe.getDepartementId() : null);
              return dto;
            })
        .toList();
  }

  private ClotureDto toDto(CloturePaie cloture) {
    return modelMapper.map(cloture, ClotureDto.class);
  }
}
