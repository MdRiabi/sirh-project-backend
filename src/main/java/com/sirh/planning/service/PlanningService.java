package com.sirh.planning.service;

import com.sirh.demandes.entity.Indisponibilite;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.exception.EmployeNotFoundException;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.planning.dto.DeltaJourDto;
import com.sirh.planning.entity.Planning;
import com.sirh.planning.entity.PlanningStatut;
import com.sirh.planning.entity.Shift;
import com.sirh.planning.exception.PlanningConflictException;
import com.sirh.planning.exception.ShiftNotFoundException;
import com.sirh.planning.repository.PlanningRepository;
import com.sirh.planning.repository.ShiftRepository;
import com.sirh.pointages.entity.PointageBrut;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.repository.PointageBrutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Affectation de planning avec contrôle bloquant du repos légal (11h
 * consécutives) et du chevauchement avec un congé validé
 * (cahier-fonctionnel.md Module 2).
 */
@Service
public class PlanningService {

    private static final long REPOS_LEGAL_HEURES = 11;

    private final PlanningRepository planningRepository;
    private final ShiftRepository shiftRepository;
    private final IndisponibiliteRepository indisponibiliteRepository;
    private final PointageBrutRepository pointageBrutRepository;
    private final EmployeRepository employeRepository;

    public PlanningService(PlanningRepository planningRepository, ShiftRepository shiftRepository,
                            IndisponibiliteRepository indisponibiliteRepository,
                            PointageBrutRepository pointageBrutRepository, EmployeRepository employeRepository) {
        this.planningRepository = planningRepository;
        this.shiftRepository = shiftRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.pointageBrutRepository = pointageBrutRepository;
        this.employeRepository = employeRepository;
    }

    @Transactional
    public Planning affecter(UUID employeId, Long shiftId, LocalDateTime dateDebut, LocalDateTime dateFin,
                              PlanningStatut statut, String commentaire) {
        if (!dateDebut.isBefore(dateFin)) {
            throw new IllegalArgumentException("dateDebut doit être strictement avant dateFin");
        }
        Shift shift = shiftRepository.findById(shiftId).orElseThrow(() -> new ShiftNotFoundException(shiftId));

        verifierReposLegalEtChevauchement(employeId, dateDebut, dateFin);
        verifierIndisponibilite(employeId, dateDebut, dateFin);

        Planning planning = Planning.builder()
                .employeId(employeId)
                .shiftId(shift.getId())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .statut(statut != null ? statut : PlanningStatut.PROVISOIRE)
                .commentaire(commentaire)
                .build();
        return planningRepository.save(planning);
    }

    // Fenêtre élargie de 11h de part et d'autre : au-delà, aucun chevauchement
    // ni violation de repos n'est possible.
    private void verifierReposLegalEtChevauchement(UUID employeId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        LocalDateTime fenetreDebut = dateDebut.minusHours(REPOS_LEGAL_HEURES);
        LocalDateTime fenetreFin = dateFin.plusHours(REPOS_LEGAL_HEURES);
        List<Planning> voisins = planningRepository
                .findByEmployeIdAndDateFinAfterAndDateDebutBefore(employeId, fenetreDebut, fenetreFin);

        for (Planning existant : voisins) {
            boolean chevauchementDirect = existant.getDateDebut().isBefore(dateFin) && dateDebut.isBefore(existant.getDateFin());
            if (chevauchementDirect) {
                throw new PlanningConflictException(
                        "Affectation refusée : l'employé a déjà une affectation sur cette période.");
            }

            Duration ecart = !existant.getDateFin().isAfter(dateDebut)
                    ? Duration.between(existant.getDateFin(), dateDebut)
                    : Duration.between(dateFin, existant.getDateDebut());

            if (ecart.compareTo(Duration.ofHours(REPOS_LEGAL_HEURES)) < 0) {
                throw new PlanningConflictException(String.format(
                        "[VIOLATION LÉGALE] Repos consécutif insuffisant (%dh au lieu de %dh minimum). Affectation refusée.",
                        ecart.toHours(), REPOS_LEGAL_HEURES));
            }
        }
    }

    private void verifierIndisponibilite(UUID employeId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Indisponibilite> chevauchements = indisponibiliteRepository
                .findByEmployeIdAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        employeId, "VALIDE", dateFin.toLocalDate(), dateDebut.toLocalDate());
        if (!chevauchements.isEmpty()) {
            throw new PlanningConflictException(
                    "Affectation refusée : l'employé a un congé validé sur cette période. Veuillez sélectionner un remplaçant.");
        }
    }

    @Transactional(readOnly = true)
    public List<Planning> findByEmploye(UUID employeId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        return planningRepository.findByEmployeIdAndDateDebutBetweenOrderByDateDebut(employeId, dateDebut, dateFin);
    }

    @Transactional(readOnly = true)
    public List<Planning> findByDepartement(Long departementId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<UUID> employeIds = employeRepository.findByDepartement_Id(departementId).stream()
                .map(Employe::getId)
                .toList();
        if (employeIds.isEmpty()) {
            return List.of();
        }
        return planningRepository.findByEmployeIdInAndDateDebutBetweenOrderByDateDebut(employeIds, dateDebut, dateFin);
    }

    @Transactional(readOnly = true)
    public List<DeltaJourDto> rapportDeltaEmploye(UUID employeId, LocalDate dateDebut, LocalDate dateFin) {
        Employe employe = employeRepository.findById(employeId).orElseThrow(() -> new EmployeNotFoundException(employeId));
        return calculerDelta(employe, dateDebut, dateFin);
    }

    @Transactional(readOnly = true)
    public List<DeltaJourDto> rapportDeltaDepartement(Long departementId, LocalDate dateDebut, LocalDate dateFin) {
        return employeRepository.findByDepartement_Id(departementId).stream()
                .flatMap(e -> calculerDelta(e, dateDebut, dateFin).stream())
                .toList();
    }

    private List<DeltaJourDto> calculerDelta(Employe employe, LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

        Map<LocalDate, BigDecimal> theoriquesParJour = planningRepository
                .findByEmployeIdAndDateDebutBetweenOrderByDateDebut(employe.getId(), debut, fin).stream()
                .collect(Collectors.groupingBy(p -> p.getDateDebut().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, p -> heuresEntre(p.getDateDebut(), p.getDateFin()), BigDecimal::add)));

        Map<LocalDate, BigDecimal> reellesParJour = heuresReellesParJour(employe.getId(), debut, fin);

        var jours = new TreeSet<LocalDate>();
        jours.addAll(theoriquesParJour.keySet());
        jours.addAll(reellesParJour.keySet());

        return jours.stream()
                .map(jour -> {
                    BigDecimal theorique = theoriquesParJour.getOrDefault(jour, BigDecimal.ZERO);
                    BigDecimal reelle = reellesParJour.getOrDefault(jour, BigDecimal.ZERO);
                    return new DeltaJourDto(employe.getId(), employe.getNom(), employe.getPrenom(), jour, theorique, reelle,
                            reelle.subtract(theorique));
                })
                .toList();
    }

    // Apparie les pointages consécutifs (Entrée, Sortie) par jour, en excluant
    // les doublons ignorés — même règle que le moteur d'assiduité (Module 3,
    // cahier-technique.md §3.7).
    private Map<LocalDate, BigDecimal> heuresReellesParJour(UUID employeId, LocalDateTime debut, LocalDateTime fin) {
        List<PointageBrut> pointages = pointageBrutRepository
                .findByEmployeIdAndTimestampPointageBetween(employeId, debut, fin).stream()
                .filter(p -> p.getStatut() != PointageStatut.IGNORE_DOUBLON)
                .sorted(Comparator.comparing(PointageBrut::getTimestampPointage))
                .toList();

        Map<LocalDate, List<PointageBrut>> parJour = pointages.stream()
                .collect(Collectors.groupingBy(p -> p.getTimestampPointage().toLocalDate()));

        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (var entry : parJour.entrySet()) {
            List<PointageBrut> jourPointages = entry.getValue();
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i + 1 < jourPointages.size(); i += 2) {
                total = total.add(heuresEntre(jourPointages.get(i).getTimestampPointage(), jourPointages.get(i + 1).getTimestampPointage()));
            }
            result.put(entry.getKey(), total);
        }
        return result;
    }

    private static BigDecimal heuresEntre(LocalDateTime debut, LocalDateTime fin) {
        return BigDecimal.valueOf(Duration.between(debut, fin).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}