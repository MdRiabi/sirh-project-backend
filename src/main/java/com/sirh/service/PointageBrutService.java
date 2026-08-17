package com.sirh.service;

import com.sirh.exception.ResourceNotFoundException;
import com.sirh.exception.ErrorCode;

import com.sirh.service.CloturePaieService;
import com.sirh.model.dto.PointageBrutDto;
import com.sirh.model.entity.AuditLog;
import com.sirh.model.repository.AuditLogRepository;
import com.sirh.model.dto.CorrectionPointageRequest;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.entity.PointageBrut;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.entity.Pointeuse;
import com.sirh.model.repository.PointeuseRepository;
import com.sirh.model.enums.SourceImport;
import com.sirh.model.enums.TypeEvenement;
import com.sirh.model.repository.PointageBrutRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Point d'entrée unique pour toute insertion de pointage brut (webhook
 * Hikvision, import Excel, correction manuelle) : c'est ici, et uniquement
 * ici, que la règle de détection des doublons et l'anomalie "nombre impair
 * de pointages" du jour sont appliquées (cahier-fonctionnel.md Module 1,
 * cahier-technique.md §3.7).
 */
@Service
public class PointageBrutService {

    private static final int FENETRE_DOUBLON_MINUTES = 5;

    private final PointageBrutRepository pointageBrutRepository;
    private final AuditLogRepository auditLogRepository;
    private final CloturePaieService cloturePaieService;
    private final EmployeRepository employeRepository;
    private final PointeuseRepository pointeuseRepository;

    public PointageBrutService(PointageBrutRepository pointageBrutRepository, AuditLogRepository auditLogRepository,
                                CloturePaieService cloturePaieService, EmployeRepository employeRepository,
                                PointeuseRepository pointeuseRepository) {
        this.pointageBrutRepository = pointageBrutRepository;
        this.auditLogRepository = auditLogRepository;
        this.cloturePaieService = cloturePaieService;
        this.employeRepository = employeRepository;
        this.pointeuseRepository = pointeuseRepository;
    }

    @Transactional
    public PointageBrut enregistrer(UUID employeId, Long pointeuseId, LocalDateTime timestamp,
                                     TypeEvenement type, MethodeValidation methode, SourceImport source) {
        PointageBrut nouveau = PointageBrut.builder()
                .employeId(employeId)
                .pointeuseId(pointeuseId)
                .timestampPointage(timestamp)
                .typeEvenement(type)
                .methodeValidation(methode)
                .sourceImport(source)
                .statut(PointageStatut.OK)
                .build();
        nouveau = pointageBrutRepository.save(nouveau);

        resoudreDoublon(nouveau, employeId, pointeuseId, timestamp);
        detecterAnomalieImpaire(employeId, timestamp);

        return nouveau;
    }

    // Portée : même employé + même pointeuse (deux lecteurs différents dans la
    // même fenêtre ne sont pas des doublons). Les deux pointages sont conservés
    // en base ; seul le statut change. Le premier chronologiquement (ou, à
    // égalité stricte de timestamp, celui déjà en base — inséré avant, donc
    // d'ID plus petit) reste/redevient OK.
    private void resoudreDoublon(PointageBrut nouveau, UUID employeId, Long pointeuseId, LocalDateTime timestamp) {
        if (pointeuseId == null) {
            return;
        }
        LocalDateTime debut = timestamp.minusMinutes(FENETRE_DOUBLON_MINUTES);
        LocalDateTime fin = timestamp.plusMinutes(FENETRE_DOUBLON_MINUTES);
        List<PointageBrut> fenetre = pointageBrutRepository
                .findByEmployeIdAndPointeuseIdAndTimestampPointageBetweenAndDeletedAtIsNull(employeId, pointeuseId, debut, fin);

        PointageBrut plusAncienOk = fenetre.stream()
                .filter(p -> !p.getId().equals(nouveau.getId()))
                .filter(p -> p.getStatut() != PointageStatut.IGNORE_DOUBLON)
                .min(Comparator.comparing(PointageBrut::getTimestampPointage))
                .orElse(null);
        if (plusAncienOk == null) {
            return;
        }

        boolean nouveauEstAnterieur = nouveau.getTimestampPointage().isBefore(plusAncienOk.getTimestampPointage());
        if (nouveauEstAnterieur) {
            plusAncienOk.setStatut(PointageStatut.IGNORE_DOUBLON);
            pointageBrutRepository.save(plusAncienOk);
        } else {
            nouveau.setStatut(PointageStatut.IGNORE_DOUBLON);
            pointageBrutRepository.save(nouveau);
        }
    }

    // "identifie les anomalies (nombre impair de pointages...)" — signale le
    // dernier pointage OK du jour comme non apparié ; la résolution (correction
    // ou justification via autorisation_id) reste une action RH explicite,
    // jamais un ré-équilibrage automatique silencieux.
    private void detecterAnomalieImpaire(UUID employeId, LocalDateTime timestamp) {
        LocalDate jour = timestamp.toLocalDate();
        LocalDateTime debutJour = jour.atStartOfDay();
        LocalDateTime finJour = debutJour.plusDays(1);

        List<PointageBrut> pointagesDuJour = pointageBrutRepository
                .findByEmployeIdAndTimestampPointageBetweenAndDeletedAtIsNull(employeId, debutJour, finJour);

        long compteValide = pointagesDuJour.stream()
                .filter(p -> p.getStatut() != PointageStatut.IGNORE_DOUBLON)
                .count();

        if (compteValide % 2 != 0) {
            pointagesDuJour.stream()
                    .filter(p -> p.getStatut() == PointageStatut.OK)
                    .max(Comparator.comparing(PointageBrut::getTimestampPointage))
                    .ifPresent(dernier -> {
                        dernier.setStatut(PointageStatut.ANOMALIE_NON_JUSTIFIEE);
                        pointageBrutRepository.save(dernier);
                    });
        }
    }

    @Transactional(readOnly = true)
    public PointageBrut getPointageByReference(UUID reference) {
        return pointageBrutRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POINTAGE_NOT_FOUND, new Object[]{reference}));
    }

    // Appelé par com.sirh.demandes.AutorisationService à la validation RH d'une
    // autorisation (cahier-fonctionnel.md Module 13, "l'anomalie correspondante
    // ... est automatiquement effacée/justifiée") : toutes les anomalies non
    // justifiées de l'employé sur le jour de l'autorisation basculent en
    // JUSTIFIE, tracées vers l'autorisation qui les couvre.
    @Transactional
    public List<PointageBrut> justifierAnomaliesDuJour(UUID employeId, LocalDate jour, Long autorisationId) {
        LocalDateTime debut = jour.atStartOfDay();
        LocalDateTime fin = debut.plusDays(1);
        List<PointageBrut> anomalies = pointageBrutRepository
                .findByEmployeIdAndTimestampPointageBetweenAndStatutAndDeletedAtIsNull(employeId, debut, fin, PointageStatut.ANOMALIE_NON_JUSTIFIEE);
        anomalies.forEach(p -> {
            p.setStatut(PointageStatut.JUSTIFIE);
            p.setAutorisationId(autorisationId);
        });
        return pointageBrutRepository.saveAll(anomalies);
    }

    @Transactional(readOnly = true)
    public List<PointageBrutDto> search(UUID employeId, LocalDateTime debut, LocalDateTime fin, PointageStatut statut) {
        Specification<PointageBrut> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeId != null) {
                predicates.add(cb.equal(root.get("employeId"), employeId));
            }
            if (debut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestampPointage"), debut));
            }
            if (fin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestampPointage"), fin));
            }
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return enrichir(pointageBrutRepository.findAll(spec, Sort.by("timestampPointage").descending()));
    }

    private List<PointageBrutDto> enrichir(List<PointageBrut> pointages) {
        Map<UUID, Employe> employesParId = employeRepository
                .findAllById(pointages.stream().map(PointageBrut::getEmployeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, Function.identity()));
        Map<Long, Pointeuse> pointeusesParId = pointeuseRepository
                .findAllById(pointages.stream().map(PointageBrut::getPointeuseId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Pointeuse::getId, Function.identity()));

        return pointages.stream()
                .map(p -> {
                    Employe employe = employesParId.get(p.getEmployeId());
                    Pointeuse pointeuse = p.getPointeuseId() != null ? pointeusesParId.get(p.getPointeuseId()) : null;
                    return toDto(p, employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null,
                            pointeuse != null ? pointeuse.getNom() : null);
                })
                .toList();
    }

    private PointageBrutDto toDto(PointageBrut p, String employeNom, String employePrenom, String pointeuseNom) {
        return new PointageBrutDto(
                p.getReference(),
                p.getEmployeId(),
                employeNom,
                employePrenom,
                p.getPointeuseId(),
                pointeuseNom,
                p.getTimestampPointage(),
                p.getTypeEvenement(),
                p.getMethodeValidation(),
                p.getSourceImport(),
                p.getStatut(),
                p.getAutorisationId(),
                p.getCreatedAt()
        );
    }

    // Pas de @Audited ici : le mécanisme générique (AuditAspect, @AfterReturning)
    // ne capture que les arguments de la méthode dans nouvelles_valeurs, jamais
    // un état "avant" — insuffisant pour l'exigence explicite du Module 1
    // ("Qui, Quand, Valeur précédente"). Écriture directe et complète de
    // l'AuditLog ici plutôt qu'un contournement partiel de l'annotation.
    @Transactional
    public PointageBrutDto corrigerPointageByReference(UUID reference, CorrectionPointageRequest request, Long utilisateurId, String adresseIp) {
        PointageBrut pointage = getPointageByReference(reference);
        // Cas exceptionnel Module 3 : "un Manager tente d'ajouter une autorisation
        // ou un pointage sur un mois dont la paie a déjà été validée" — vérifie à
        // la fois le mois d'origine du pointage et celui de la nouvelle date
        // demandée (une correction peut déplacer le pointage vers un autre mois).
        cloturePaieService.verifierMoisOuvert(pointage.getTimestampPointage().toLocalDate());
        cloturePaieService.verifierMoisOuvert(request.timestampPointage().toLocalDate());

        String ancienneValeur = String.format(
                "{\"timestampPointage\":\"%s\",\"typeEvenement\":\"%s\",\"statut\":\"%s\"}",
                pointage.getTimestampPointage(), pointage.getTypeEvenement(), pointage.getStatut());

        pointage.setTimestampPointage(request.timestampPointage());
        pointage.setTypeEvenement(request.typeEvenement());
        pointage.setMethodeValidation(MethodeValidation.MANUEL);
        pointage.setStatut(PointageStatut.JUSTIFIE);
        pointage = pointageBrutRepository.save(pointage);

        String nouvelleValeur = String.format(
                "{\"timestampPointage\":\"%s\",\"typeEvenement\":\"%s\",\"statut\":\"%s\",\"motif\":\"%s\"}",
                pointage.getTimestampPointage(), pointage.getTypeEvenement(), pointage.getStatut(),
                request.motif().replace("\"", "'"));

        auditLogRepository.save(AuditLog.builder()
                .utilisateurId(utilisateurId)
                .action("CORRECTION_MANUELLE_POINTAGE")
                .tableCible("pointages_bruts")
                .anciennesValeurs(ancienneValeur)
                .nouvellesValeurs(nouvelleValeur)
                .adresseIp(adresseIp)
                .build());

        return enrichir(List.of(pointage)).get(0);
    }
}