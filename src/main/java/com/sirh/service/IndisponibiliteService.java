package com.sirh.service;

import com.sirh.service.CloturePaieService;
import com.sirh.annotation.Audited;
import com.sirh.model.enums.TypeJourFerie;
import com.sirh.model.repository.JourFerieRepository;
import com.sirh.model.dto.IndisponibiliteDto;
import com.sirh.model.entity.HistoriqueSolde;
import com.sirh.model.entity.Indisponibilite;
import com.sirh.exception.DemandeStatutInvalideException;
import com.sirh.exception.IndisponibiliteNotFoundException;
import com.sirh.exception.SoldeInsuffisantException;
import com.sirh.model.repository.HistoriqueSoldeRepository;
import com.sirh.model.repository.IndisponibiliteRepository;
import com.sirh.event.DemandeCreeEvent;
import com.sirh.model.entity.Employe;
import com.sirh.exception.EmployeNotFoundException;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.enums.PlanningStatut;
import com.sirh.service.PlanningService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Workflow d'indisponibilité / congé (Module 8) : saisie Manager avec avis
 * (EN_ATTENTE_RH) ou saisie RH d'urgence (VALIDE direct), déduction du solde
 * en jours ouvrés (week-ends + jours fériés exclus, cf. Lot 4 jours_feries)
 * à la validation, avec forçage RH possible sur solde insuffisant, et mise à
 * jour du planning (source de vérité unique, décision d'architecture Lot 5).
 */
@Service
public class IndisponibiliteService {

    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_RH = "RH";
    private static final String STATUT_EN_ATTENTE_RH = "EN_ATTENTE_RH";
    private static final String STATUT_VALIDE = "VALIDE";
    private static final String STATUT_REFUSE = "REFUSE";
    private static final String TYPE_CONGE_PAYE = "CONGE_PAYE";
    private static final String TYPE_CONGE_SANS_SOLDE = "CONGE_SANS_SOLDE";

    private final IndisponibiliteRepository indisponibiliteRepository;
    private final EmployeRepository employeRepository;
    private final HistoriqueSoldeRepository historiqueSoldeRepository;
    private final JourFerieRepository jourFerieRepository;
    private final PlanningService planningService;
    private final CloturePaieService cloturePaieService;
    private final ApplicationEventPublisher eventPublisher;

    public IndisponibiliteService(IndisponibiliteRepository indisponibiliteRepository, EmployeRepository employeRepository,
                                   HistoriqueSoldeRepository historiqueSoldeRepository, JourFerieRepository jourFerieRepository,
                                   PlanningService planningService, CloturePaieService cloturePaieService,
                                   ApplicationEventPublisher eventPublisher) {
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.employeRepository = employeRepository;
        this.historiqueSoldeRepository = historiqueSoldeRepository;
        this.jourFerieRepository = jourFerieRepository;
        this.planningService = planningService;
        this.cloturePaieService = cloturePaieService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Indisponibilite creer(UUID employeId, String typeIndisponibilite, LocalDate dateDebut, LocalDate dateFin,
                                  String commentaire, Long pieceJointeId, UUID creeParId, String creeParRole,
                                  String avisManagerStatut, String avisManagerCommentaire, boolean forcer) {
        employeRepository.findById(employeId).orElseThrow(() -> new EmployeNotFoundException(employeId));
        // Cas exceptionnel Module 3 : idem AutorisationService.creer — vérifie les
        // deux bornes, une indisponibilité peut chevaucher deux mois calendaires.
        cloturePaieService.verifierMoisOuvert(dateDebut);
        cloturePaieService.verifierMoisOuvert(dateFin);

        boolean urgenceRh = ROLE_RH.equals(creeParRole);
        Indisponibilite indisponibilite = Indisponibilite.builder()
                .employeId(employeId)
                .typeIndisponibilite(typeIndisponibilite)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .commentaire(commentaire)
                .pieceJointeId(pieceJointeId)
                .statut(urgenceRh ? STATUT_VALIDE : STATUT_EN_ATTENTE_RH)
                .creeParId(creeParId)
                .creeParRole(creeParRole)
                .avisManagerStatut(ROLE_MANAGER.equals(creeParRole) ? avisManagerStatut : null)
                .avisManagerCommentaire(ROLE_MANAGER.equals(creeParRole) ? avisManagerCommentaire : null)
                .build();
        indisponibilite = indisponibiliteRepository.save(indisponibilite);

        // Workflow d'urgence (Module 8) : le RH valide instantanément, donc les
        // effets d'une validation (déduction de solde, mise à jour du planning)
        // doivent s'appliquer immédiatement — sans cet appel, la demande restait
        // VALIDE en base sans jamais avoir déduit de solde ni généré de créneau
        // planning (bug trouvé en vérification manuelle docker compose + curl).
        if (urgenceRh) {
            appliquerImpactValidation(indisponibilite, forcer);
        } else {
            eventPublisher.publishEvent(new DemandeCreeEvent("INDISPONIBILITE", indisponibilite.getId(),
                    indisponibilite.getEmployeId(), indisponibilite.getTypeIndisponibilite()));
        }
        return indisponibilite;
    }

    @Audited(action = "VALIDER_INDISPONIBILITE", tableCible = "indisponibilites")
    @Transactional
    public Indisponibilite valider(Long id, String bascule, boolean forcer, UUID valideParRhId) {
        Indisponibilite indisponibilite = findById(id);
        if (!STATUT_EN_ATTENTE_RH.equals(indisponibilite.getStatut())) {
            throw new DemandeStatutInvalideException(
                    "Seule une indisponibilité EN_ATTENTE_RH peut être validée (statut actuel : " + indisponibilite.getStatut() + ").");
        }
        if (bascule != null && !bascule.isBlank()) {
            indisponibilite.setTypeIndisponibilite(bascule);
        }

        appliquerImpactValidation(indisponibilite, forcer);

        indisponibilite.setStatut(STATUT_VALIDE);
        indisponibilite.setValideParRhId(valideParRhId);
        return indisponibiliteRepository.save(indisponibilite);
    }

    // Partagé entre creer() (workflow d'urgence RH, statut VALIDE dès la
    // création) et valider() (workflow normal) : les deux chemins produisent le
    // même impact — déduction du solde si congé payé, mise à jour du planning.
    private void appliquerImpactValidation(Indisponibilite indisponibilite, boolean forcer) {
        if (TYPE_CONGE_PAYE.equals(indisponibilite.getTypeIndisponibilite())) {
            deduireSolde(indisponibilite, forcer);
        }
        PlanningStatut statutPlanning = estUnConge(indisponibilite.getTypeIndisponibilite()) ? PlanningStatut.CONGE : PlanningStatut.ABSENCE;
        planningService.ecraserCreneaux(indisponibilite.getEmployeId(),
                indisponibilite.getDateDebut().atStartOfDay(), indisponibilite.getDateFin().plusDays(1).atStartOfDay(),
                statutPlanning, "Indisponibilité #" + indisponibilite.getId() + " (" + indisponibilite.getTypeIndisponibilite() + ")");
    }

    private boolean estUnConge(String type) {
        return TYPE_CONGE_PAYE.equals(type) || TYPE_CONGE_SANS_SOLDE.equals(type);
    }

    private void deduireSolde(Indisponibilite indisponibilite, boolean forcer) {
        Employe employe = employeRepository.findById(indisponibilite.getEmployeId())
                .orElseThrow(() -> new EmployeNotFoundException(indisponibilite.getEmployeId()));
        BigDecimal soldeAvant = employe.getSoldeCongesJours() != null ? employe.getSoldeCongesJours() : BigDecimal.ZERO;

        int joursOuvres = joursOuvres(indisponibilite.getDateDebut(), indisponibilite.getDateFin());
        BigDecimal soldeApres = soldeAvant.subtract(BigDecimal.valueOf(joursOuvres));

        if (soldeApres.signum() < 0 && !forcer) {
            throw new SoldeInsuffisantException(soldeAvant);
        }

        employe.setSoldeCongesJours(soldeApres);
        employeRepository.save(employe);

        historiqueSoldeRepository.save(HistoriqueSolde.builder()
                .employeId(employe.getId())
                .dateMouvement(indisponibilite.getDateDebut())
                .deltaJours(BigDecimal.valueOf(-joursOuvres))
                .soldeApres(soldeApres)
                .motif(soldeApres.signum() < 0 ? "FORCAGE_RH" : "VALIDATION_CONGE")
                .sourceType("INDISPONIBILITE")
                .sourceId(indisponibilite.getId())
                .build());
    }

    // Exclut week-ends et jours fériés légaux (type FERIE uniquement — une
    // FERMETURE_COLLECTIVE reste une décision RH interne, cf. Lot 4) de la
    // période [dateDebut, dateFin] inclusive.
    private int joursOuvres(LocalDate dateDebut, LocalDate dateFin) {
        Set<LocalDate> feries = jourFerieRepository.findByDateBetweenAndDeletedAtIsNullOrderByDate(dateDebut, dateFin).stream()
                .filter(j -> j.getType() == TypeJourFerie.FERIE)
                .map(com.sirh.model.entity.JourFerie::getDate)
                .collect(Collectors.toSet());

        int compte = 0;
        for (LocalDate jour = dateDebut; !jour.isAfter(dateFin); jour = jour.plusDays(1)) {
            DayOfWeek jourSemaine = jour.getDayOfWeek();
            if (jourSemaine != DayOfWeek.SATURDAY && jourSemaine != DayOfWeek.SUNDAY && !feries.contains(jour)) {
                compte++;
            }
        }
        return compte;
    }

    @Audited(action = "REFUSER_INDISPONIBILITE", tableCible = "indisponibilites")
    @Transactional
    public Indisponibilite refuser(Long id) {
        Indisponibilite indisponibilite = findById(id);
        if (!STATUT_EN_ATTENTE_RH.equals(indisponibilite.getStatut())) {
            throw new DemandeStatutInvalideException(
                    "Seule une indisponibilité EN_ATTENTE_RH peut être refusée (statut actuel : " + indisponibilite.getStatut() + ").");
        }
        indisponibilite.setStatut(STATUT_REFUSE);
        return indisponibiliteRepository.save(indisponibilite);
    }

    @Transactional(readOnly = true)
    public Indisponibilite findById(Long id) {
        return indisponibiliteRepository.findById(id).orElseThrow(() -> new IndisponibiliteNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public BigDecimal solde(UUID employeId) {
        Employe employe = employeRepository.findById(employeId).orElseThrow(() -> new EmployeNotFoundException(employeId));
        return employe.getSoldeCongesJours() != null ? employe.getSoldeCongesJours() : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<Indisponibilite> search(String statut, UUID employeId, Long departementId, LocalDate dateDebut, LocalDate dateFin) {
        List<UUID> employeIdsDepartement = departementId != null
                ? employeRepository.findByDepartement_IdAndDeletedAtIsNull(departementId).stream().map(Employe::getId).toList()
                : null;

        Specification<Indisponibilite> spec = (root, query, cb) -> {
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
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateFin"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateDebut"), dateFin));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return indisponibiliteRepository.findAll(spec);
    }

    // Cf. AutorisationService.searchPourManager : même garde-fou sans
    // département (liste vide plutôt qu'une recherche non filtrée).
    @Transactional(readOnly = true)
    public List<Indisponibilite> searchPourManager(Long departementIdManager, String statut, LocalDate dateDebut, LocalDate dateFin) {
        if (departementIdManager == null) {
            return List.of();
        }
        return search(statut, null, departementIdManager, dateDebut, dateFin);
    }

    @Transactional(readOnly = true)
    public List<IndisponibiliteDto> enrichir(List<Indisponibilite> indisponibilites) {
        Map<UUID, Employe> employesParId = employeRepository
                .findAllById(indisponibilites.stream().map(Indisponibilite::getEmployeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, e -> e));
        return indisponibilites.stream()
                .map(i -> {
                    Employe employe = employesParId.get(i.getEmployeId());
                    return IndisponibiliteDto.from(i, employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null, null);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public IndisponibiliteDto enrichirAvecDecompte(Indisponibilite indisponibilite) {
        Employe employe = employeRepository.findById(indisponibilite.getEmployeId()).orElse(null);
        Integer joursOuvresDecomptes = TYPE_CONGE_PAYE.equals(indisponibilite.getTypeIndisponibilite())
                ? joursOuvres(indisponibilite.getDateDebut(), indisponibilite.getDateFin())
                : null;
        return IndisponibiliteDto.from(indisponibilite, employe != null ? employe.getNom() : null,
                employe != null ? employe.getPrenom() : null, joursOuvresDecomptes);
    }
}
