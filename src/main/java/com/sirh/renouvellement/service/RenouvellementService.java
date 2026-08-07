package com.sirh.renouvellement.service;

import com.sirh.audit.annotation.Audited;
import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.repository.UtilisateurRepository;
import com.sirh.notifications.entity.NotificationType;
import com.sirh.notifications.service.NotificationService;
import com.sirh.parametres.service.ParametreGlobalService;
import com.sirh.personnel.dto.ContratRequest;
import com.sirh.personnel.entity.Contrat;
import com.sirh.personnel.entity.ContratStatut;
import com.sirh.personnel.entity.ContratType;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.entity.MotifFin;
import com.sirh.personnel.repository.ContratRepository;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.personnel.service.ContratService;
import com.sirh.renouvellement.dto.TacheRenouvellementDto;
import com.sirh.renouvellement.entity.TacheRenouvellement;
import com.sirh.renouvellement.entity.TacheRenouvellementStatut;
import com.sirh.renouvellement.exception.TacheRenouvellementNotFoundException;
import com.sirh.renouvellement.exception.TacheRenouvellementStatutInvalideException;
import com.sirh.renouvellement.repository.TacheRenouvellementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Module 12 (Gestion du Renouvellement des Contrats) : génération M+5/M+11 et
 * bascule URGENT_EXPIRE isolées du @Scheduled (règle d'or déjà appliquée au
 * Lot 8 — RenouvellementScheduler n'orchestre que l'appel quotidien).
 * Périmètre restreint aux contrats CDD (arbitrage Lot 9) : un CDI n'a pas de
 * terme à anticiper.
 */
@Service
public class RenouvellementService {

    private static final String TYPE_M5 = "M+5";
    private static final String TYPE_M11 = "M+11";
    private static final String CLE_MOIS_M5 = "MOIS_ECHEANCE_M5";
    private static final String CLE_MOIS_M11 = "MOIS_ECHEANCE_M11";
    private static final String CLE_DUREE_RENOUVELLEMENT = "DUREE_RENOUVELLEMENT_CDD_MOIS";
    private static final List<TacheRenouvellementStatut> STATUTS_ACTIONNABLES =
            List.of(TacheRenouvellementStatut.EN_COURS, TacheRenouvellementStatut.URGENT_EXPIRE);

    private final TacheRenouvellementRepository tacheRenouvellementRepository;
    private final ContratRepository contratRepository;
    private final ContratService contratService;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreGlobalService parametreGlobalService;
    private final NotificationService notificationService;

    public RenouvellementService(TacheRenouvellementRepository tacheRenouvellementRepository,
                                  ContratRepository contratRepository, ContratService contratService,
                                  EmployeRepository employeRepository, UtilisateurRepository utilisateurRepository,
                                  ParametreGlobalService parametreGlobalService, NotificationService notificationService) {
        this.tacheRenouvellementRepository = tacheRenouvellementRepository;
        this.contratRepository = contratRepository;
        this.contratService = contratService;
        this.employeRepository = employeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.parametreGlobalService = parametreGlobalService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void genererEtEscalader(LocalDate reference) {
        int moisM5 = parametreGlobalService.valeurDecimale(CLE_MOIS_M5).intValue();
        int moisM11 = parametreGlobalService.valeurDecimale(CLE_MOIS_M11).intValue();

        for (Contrat contrat : contratRepository.findByStatutAndType(ContratStatut.EN_COURS, ContratType.CDD)) {
            genererSiEcheanceAtteinte(contrat, TYPE_M5, moisM5, reference);
            genererSiEcheanceAtteinte(contrat, TYPE_M11, moisM11, reference);
        }
        escaladerTachesExpirees(reference);
    }

    private void genererSiEcheanceAtteinte(Contrat contrat, String typeEcheance, int moisOffset, LocalDate reference) {
        LocalDate dateEcheance = contrat.getDateDebut().plusMonths(moisOffset);
        if (reference.isBefore(dateEcheance)
                || tacheRenouvellementRepository.existsByContratIdAndTypeEcheance(contrat.getId(), typeEcheance)) {
            return;
        }
        TacheRenouvellement tache = tacheRenouvellementRepository.save(TacheRenouvellement.builder()
                .employeId(contrat.getEmployeId())
                .contratId(contrat.getId())
                .typeEcheance(typeEcheance)
                .dateEcheance(dateEcheance)
                .statut(TacheRenouvellementStatut.EN_COURS)
                .build());
        notifierRh("Renouvellement à préparer (%s) pour le contrat #%d.".formatted(typeEcheance, contrat.getId()), tache.getId());
    }

    // "le contrat n'est pas clôturé automatiquement" (cahier-fonctionnel.md
    // Module 12) : seule la tâche bascule, jamais le contrat lui-même.
    private void escaladerTachesExpirees(LocalDate reference) {
        for (TacheRenouvellement tache : tacheRenouvellementRepository.findByStatut(TacheRenouvellementStatut.EN_COURS)) {
            Contrat contrat = tache.getContratId() != null ? contratRepository.findById(tache.getContratId()).orElse(null) : null;
            if (contrat == null || contrat.getDateFin() == null || reference.isBefore(contrat.getDateFin())) {
                continue;
            }
            tache.setStatut(TacheRenouvellementStatut.URGENT_EXPIRE);
            tacheRenouvellementRepository.save(tache);
            notifierRh("URGENT : tâche de renouvellement #%d expirée sans décision RH.".formatted(tache.getId()), tache.getId());
        }
    }

    private void notifierRh(String message, Long tacheId) {
        for (Utilisateur destinataire : utilisateurRepository.findByRoleIn(List.of(Role.ROLE_RH, Role.ROLE_SUPERUSER))) {
            notificationService.creer(destinataire.getId(), NotificationType.RENOUVELLEMENT_CONTRAT, message,
                    "TACHE_RENOUVELLEMENT", tacheId);
        }
    }

    @Transactional(readOnly = true)
    public List<TacheRenouvellementDto> search(TacheRenouvellementStatut statut) {
        List<TacheRenouvellement> taches = statut != null
                ? tacheRenouvellementRepository.findByStatutOrderByDateEcheanceAsc(statut)
                : tacheRenouvellementRepository.findAllByOrderByDateEcheanceAsc();
        return enrichir(taches);
    }

    @Audited(action = "TRAITER_TACHE_RENOUVELLEMENT", tableCible = "taches_renouvellement")
    @Transactional
    public TacheRenouvellementDto traiter(Long id, TacheRenouvellementStatut statutDemande, LocalDate dateFin, Long traiteParId) {
        if (statutDemande != TacheRenouvellementStatut.VALIDE && statutDemande != TacheRenouvellementStatut.NON_RENOUVELE) {
            throw new IllegalArgumentException("Seuls les statuts VALIDE ou NON_RENOUVELE sont acceptés en traitement.");
        }
        TacheRenouvellement tache = tacheRenouvellementRepository.findById(id)
                .orElseThrow(() -> new TacheRenouvellementNotFoundException(id));
        if (!STATUTS_ACTIONNABLES.contains(tache.getStatut())) {
            throw new TacheRenouvellementStatutInvalideException(
                    "Seule une tâche EN_COURS ou URGENT_EXPIRE peut être traitée (statut actuel : " + tache.getStatut() + ").");
        }
        Contrat contrat = contratRepository.findById(tache.getContratId())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour la tâche de renouvellement " + id));

        if (statutDemande == TacheRenouvellementStatut.VALIDE) {
            valider(tache, contrat, dateFin, traiteParId);
        } else {
            nonRenouveler(contrat);
        }

        tache.setStatut(statutDemande);
        tache.setDateTraitement(LocalDateTime.now());
        tache.setTraiteParId(traiteParId);
        tache = tacheRenouvellementRepository.save(tache);
        return enrichir(List.of(tache)).get(0);
    }

    // VALIDE "prolonge la base active" (cahier-technique.md §4.9) : le contrat
    // en cours passe RENOUVELE (jamais TERMINE — il a bien été honoré) et une
    // nouvelle ligne contrats prend le relais, réutilisant ContratService pour
    // rester cohérent avec la création manuelle (sync employes.type_contrat/
    // date_fin_contrat + timeline 360°).
    private void valider(TacheRenouvellement tache, Contrat contrat, LocalDate dateFinDemandee, Long traiteParId) {
        contrat.setStatut(ContratStatut.RENOUVELE);
        contratRepository.save(contrat);

        LocalDate dateDebutNouveau = contrat.getDateFin() != null ? contrat.getDateFin() : LocalDate.now();
        LocalDate dateFinNouveau = dateFinDemandee != null
                ? dateFinDemandee
                : dateDebutNouveau.plusMonths(parametreGlobalService.valeurDecimale(CLE_DUREE_RENOUVELLEMENT).intValue());

        contratService.create(tache.getEmployeId(),
                new ContratRequest(contrat.getType(), dateDebutNouveau, dateFinNouveau, ContratStatut.EN_COURS, null),
                traiteParId);
    }

    // NON_RENOUVELE déclenche l'offboarding : contrat TERMINE + employé
    // inactif (Module 12).
    private void nonRenouveler(Contrat contrat) {
        contrat.setStatut(ContratStatut.TERMINE);
        contrat.setMotifFin(MotifFin.FIN_CDD);
        contratRepository.save(contrat);

        Employe employe = employeRepository.findById(contrat.getEmployeId())
                .orElseThrow(() -> new IllegalStateException("Employé introuvable pour le contrat " + contrat.getId()));
        employe.setActif(false);
        employeRepository.save(employe);
    }

    private List<TacheRenouvellementDto> enrichir(List<TacheRenouvellement> taches) {
        Map<UUID, Employe> employesParId = employeRepository.findAllById(
                taches.stream().map(TacheRenouvellement::getEmployeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, e -> e));
        return taches.stream()
                .map(t -> {
                    Employe employe = employesParId.get(t.getEmployeId());
                    return TacheRenouvellementDto.from(t, employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null);
                })
                .toList();
    }
}
