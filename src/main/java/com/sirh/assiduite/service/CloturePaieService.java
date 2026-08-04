package com.sirh.assiduite.service;

import com.sirh.assiduite.entity.CloturePaie;
import com.sirh.assiduite.entity.DemandeDeverrouillage;
import com.sirh.assiduite.exception.ClotureRefuseeException;
import com.sirh.assiduite.exception.DemandeDeverrouillageNotFoundException;
import com.sirh.assiduite.exception.DemandeDeverrouillageStatutInvalideException;
import com.sirh.assiduite.exception.MoisDejaClotureException;
import com.sirh.assiduite.exception.MoisNonClotureException;
import com.sirh.assiduite.dto.DemandeDeverrouillageDto;
import com.sirh.assiduite.repository.CloturePaieRepository;
import com.sirh.assiduite.repository.DemandeDeverrouillageRepository;
import com.sirh.audit.annotation.Audited;
import com.sirh.demandes.repository.AutorisationRepository;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.parametres.service.ParametreGlobalService;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.repository.PointageBrutRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Clôture de paie irréversible (sauf déverrouillage exceptionnel) et workflow
 * de déverrouillage (cahier-fonctionnel.md Module 3). Clôture globale par
 * mois (décision d'architecture Lot 6, pas par département) : préconditions
 * bloquantes — taux d'anomalies justifiées ≥ SEUIL_ANOMALIES_CLOTURE,
 * aucune demande EN_ATTENTE_RH sur le mois.
 */
@Service
public class CloturePaieService {

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

    public CloturePaieService(CloturePaieRepository cloturePaieRepository,
                               DemandeDeverrouillageRepository demandeDeverrouillageRepository,
                               AutorisationRepository autorisationRepository,
                               IndisponibiliteRepository indisponibiliteRepository,
                               PointageBrutRepository pointageBrutRepository,
                               ParametreGlobalService parametreGlobalService,
                               EmployeRepository employeRepository) {
        this.cloturePaieRepository = cloturePaieRepository;
        this.demandeDeverrouillageRepository = demandeDeverrouillageRepository;
        this.autorisationRepository = autorisationRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.pointageBrutRepository = pointageBrutRepository;
        this.parametreGlobalService = parametreGlobalService;
        this.employeRepository = employeRepository;
    }

    // GET /assiduite/cloture-paie : lecture du statut sans forcer la clôture ni
    // exiger qu'une ligne existe déjà (mois jamais clôturé -> OUVERT par
    // défaut, objet transitoire non persisté).
    @Transactional(readOnly = true)
    public CloturePaie trouverOuDefaut(YearMonth moisParam) {
        LocalDate mois = moisParam.atDay(1);
        return cloturePaieRepository.findByMois(mois).orElseGet(() -> CloturePaie.builder().mois(mois).build());
    }

    // Appelée avant toute écriture manuelle sur pointages_bruts (correction),
    // autorisations ou indisponibilites (création) — cf. PointageBrutService,
    // AutorisationService, IndisponibiliteService.
    @Transactional(readOnly = true)
    public void verifierMoisOuvert(LocalDate date) {
        LocalDate mois = date.withDayOfMonth(1);
        cloturePaieRepository.findByMois(mois)
                .filter(c -> STATUT_CLOTURE.equals(c.getStatut()))
                .ifPresent(c -> {
                    throw new MoisDejaClotureException(mois);
                });
    }

    @Audited(action = "CLOTURER_PAIE", tableCible = "cloture_paie")
    @Transactional
    public CloturePaie cloturer(YearMonth moisParam, UUID clotureParId) {
        LocalDate mois = moisParam.atDay(1);
        LocalDate finMois = moisParam.atEndOfMonth();
        CloturePaie cloture = cloturePaieRepository.findByMois(mois).orElseGet(() -> CloturePaie.builder().mois(mois).build());
        if (STATUT_CLOTURE.equals(cloture.getStatut())) {
            throw new MoisDejaClotureException(mois);
        }

        BigDecimal tauxAnomaliesJustifiees = tauxAnomaliesJustifiees(mois, finMois);
        BigDecimal seuil = parametreGlobalService.valeurDecimale("SEUIL_ANOMALIES_CLOTURE");
        if (tauxAnomaliesJustifiees.compareTo(seuil) < 0) {
            throw new ClotureRefuseeException(String.format(
                    "Taux d'anomalies justifiées insuffisant (%s%% < %s%% requis).", tauxAnomaliesJustifiees, seuil));
        }

        boolean demandesEnAttente = !autorisationRepository.findByStatutAndDateAutorisationBetween(STATUT_EN_ATTENTE_RH, mois, finMois).isEmpty()
                || !indisponibiliteRepository.findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(STATUT_EN_ATTENTE_RH, finMois, mois).isEmpty();
        if (demandesEnAttente) {
            throw new ClotureRefuseeException("Des demandes (autorisations/indisponibilités) sont encore EN_ATTENTE_RH sur ce mois.");
        }

        cloture.setStatut(STATUT_CLOTURE);
        cloture.setDateCloture(LocalDateTime.now());
        cloture.setClotureParId(clotureParId);
        cloture.setTauxAnomaliesJustifiees(tauxAnomaliesJustifiees);
        return cloturePaieRepository.save(cloture);
    }

    private BigDecimal tauxAnomaliesJustifiees(LocalDate mois, LocalDate finMois) {
        LocalDateTime debut = mois.atStartOfDay();
        LocalDateTime fin = finMois.plusDays(1).atStartOfDay();
        long justifiees = pointageBrutRepository.countByTimestampPointageBetweenAndStatut(debut, fin, PointageStatut.JUSTIFIE);
        long nonJustifiees = pointageBrutRepository.countByTimestampPointageBetweenAndStatut(debut, fin, PointageStatut.ANOMALIE_NON_JUSTIFIEE);
        long total = justifiees + nonJustifiees;
        return total == 0
                ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(justifiees * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public DemandeDeverrouillage demanderDeverrouillage(YearMonth moisParam, String motif, UUID demandeParId) {
        LocalDate mois = moisParam.atDay(1);
        boolean moisCloture = cloturePaieRepository.findByMois(mois)
                .map(c -> STATUT_CLOTURE.equals(c.getStatut()))
                .orElse(false);
        if (!moisCloture) {
            throw new MoisNonClotureException();
        }
        return demandeDeverrouillageRepository.save(DemandeDeverrouillage.builder()
                .mois(mois).motif(motif).demandeParId(demandeParId).build());
    }

    @Audited(action = "TRAITER_DEMANDE_DEVERROUILLAGE", tableCible = "demandes_deverrouillage")
    @Transactional
    public DemandeDeverrouillage traiterDemande(Long id, String statut, UUID traiteParId) {
        DemandeDeverrouillage demande = findDemandeById(id);
        if (!STATUT_EN_ATTENTE.equals(demande.getStatut())) {
            throw new DemandeDeverrouillageStatutInvalideException(demande.getStatut());
        }

        LocalDate mois = demande.getMois();
        demande.setStatut(statut);
        demande.setTraiteParId(traiteParId);
        demande.setDateTraitement(LocalDateTime.now());
        DemandeDeverrouillage sauvegardee = demandeDeverrouillageRepository.save(demande);

        if (STATUT_APPROUVEE.equals(statut)) {
            CloturePaie cloture = cloturePaieRepository.findByMois(mois)
                    .orElseThrow(() -> new IllegalStateException("Clôture introuvable pour le mois " + mois));
            cloture.setStatut(STATUT_OUVERT);
            cloturePaieRepository.save(cloture);
        }
        return sauvegardee;
    }

    @Transactional(readOnly = true)
    public DemandeDeverrouillage findDemandeById(Long id) {
        return demandeDeverrouillageRepository.findById(id).orElseThrow(() -> new DemandeDeverrouillageNotFoundException(id));
    }

    // Filtré par mois/statut et, indirectement, par département : le
    // demandeur (demande_par_id) est résolu vers son département via
    // EmployeRepository (cloture_paie/demandes_deverrouillage restent
    // globales par mois, aucune colonne département sur la table) — même
    // principe que AutorisationService.search (chaîne employeId -> departementId).
    @Transactional(readOnly = true)
    public List<DemandeDeverrouillage> listerDemandes(YearMonth moisParam, Long departementId, String statut) {
        LocalDate mois = moisParam != null ? moisParam.atDay(1) : null;
        List<UUID> employeIdsDepartement = departementId != null
                ? employeRepository.findByDepartement_Id(departementId).stream().map(Employe::getId).toList()
                : null;

        Specification<DemandeDeverrouillage> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (mois != null) {
                predicates.add(cb.equal(root.get("mois"), mois));
            }
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            if (employeIdsDepartement != null) {
                predicates.add(employeIdsDepartement.isEmpty()
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
    public List<DemandeDeverrouillage> listerDemandesPourManager(YearMonth mois, Long departementIdManager, String statut) {
        if (departementIdManager == null) {
            return List.of();
        }
        return listerDemandes(mois, departementIdManager, statut);
    }

    @Transactional(readOnly = true)
    public List<DemandeDeverrouillageDto> enrichir(List<DemandeDeverrouillage> demandes) {
        Map<UUID, Employe> employesParId = employeRepository
                .findAllById(demandes.stream().map(DemandeDeverrouillage::getDemandeParId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, e -> e));
        return demandes.stream()
                .map(d -> {
                    Employe employe = employesParId.get(d.getDemandeParId());
                    return DemandeDeverrouillageDto.from(d,
                            employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null,
                            employe != null ? employe.getDepartementId() : null);
                })
                .toList();
    }
}
