package com.sirh.reporting.service;

import com.sirh.assiduite.entity.CloturePaie;
import com.sirh.assiduite.repository.CloturePaieRepository;
import com.sirh.demandes.entity.Indisponibilite;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.evaluation.entity.EvaluationStatut;
import com.sirh.evaluation.repository.EvaluationRepository;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.repository.PointageBrutRepository;
import com.sirh.renouvellement.entity.TacheRenouvellement;
import com.sirh.renouvellement.entity.TacheRenouvellementStatut;
import com.sirh.renouvellement.repository.TacheRenouvellementRepository;
import com.sirh.reporting.dto.ChartsDto;
import com.sirh.reporting.dto.KpiDto;
import com.sirh.reporting.dto.PointDto;
import com.sirh.reporting.dto.RepartitionDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Module reporting (ARCHITECTURE.md §3) : consolide en lecture seule les
// données des Lots 1/2/5/6/9/10 sans nouvelle FK ni couplage service-à-service
// — uniquement des requêtes de comptage/regroupement sur les repositories déjà
// exposés par chaque module. Le scope département (MANAGER) est appliqué en
// mémoire par filtrage d'un ensemble d'employeId, même convention que
// EvaluationService#search et RenouvellementService#enrichir (les entités
// congés/renouvellement/évaluation n'ont qu'un employeId à plat, pas de
// relation JPA vers departements).
@Service
public class DashboardService {

    private static final int SEMAINES_GRAPHIQUE = 8;

    private final EmployeRepository employeRepository;
    private final TacheRenouvellementRepository tacheRenouvellementRepository;
    private final IndisponibiliteRepository indisponibiliteRepository;
    private final PointageBrutRepository pointageBrutRepository;
    private final CloturePaieRepository cloturePaieRepository;
    private final EvaluationRepository evaluationRepository;

    public DashboardService(EmployeRepository employeRepository,
                             TacheRenouvellementRepository tacheRenouvellementRepository,
                             IndisponibiliteRepository indisponibiliteRepository,
                             PointageBrutRepository pointageBrutRepository,
                             CloturePaieRepository cloturePaieRepository,
                             EvaluationRepository evaluationRepository) {
        this.employeRepository = employeRepository;
        this.tacheRenouvellementRepository = tacheRenouvellementRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.pointageBrutRepository = pointageBrutRepository;
        this.cloturePaieRepository = cloturePaieRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Cacheable(value = "dashboardKpi", key = "#departementId != null ? #departementId : 'global'")
    public KpiDto kpi(Long departementId) {
        long effectifActif = departementId == null
                ? employeRepository.countByActifTrue()
                : employeRepository.countByActifTrueAndDepartement_Id(departementId);
        long effectifTotal = departementId == null
                ? employeRepository.count()
                : employeRepository.countByDepartement_Id(departementId);

        List<BigDecimal> evolution = extraireValeurs(employeRepository.evolutionEffectifsParMois(departementId));
        List<BigDecimal> departs = extraireValeurs(employeRepository.departsParMois(departementId));
        BigDecimal tauxTurnoverMensuel = tauxDuDernierMois(evolution, departs);

        Set<UUID> employeIds = departementId == null ? null : employeIdsDuDepartement(departementId);

        long congesEnCours = indisponibiliteRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        "VALIDE", LocalDate.now(), LocalDate.now())
                .stream()
                .filter(i -> employeIds == null || employeIds.contains(i.getEmployeId()))
                .count();

        long renouvellementsUrgents = tacheRenouvellementRepository
                .findByStatut(TacheRenouvellementStatut.URGENT_EXPIRE)
                .stream()
                .filter(t -> employeIds == null || employeIds.contains(t.getEmployeId()))
                .count();

        int anneeCourante = LocalDate.now().getYear();
        long evaluationsEnCours = evaluationRepository
                .findByStatutAndAnneeOrderByCreatedAtDesc(EvaluationStatut.EN_COURS, anneeCourante)
                .stream()
                .filter(e -> employeIds == null || employeIds.contains(e.getEmployeId()))
                .count();
        long evaluationsValidees = evaluationRepository
                .findByStatutAndAnneeOrderByCreatedAtDesc(EvaluationStatut.VALIDEE_RH, anneeCourante)
                .stream()
                .filter(e -> employeIds == null || employeIds.contains(e.getEmployeId()))
                .count();

        long alertesPointageNonJustifiees = alertesPointage(30, employeIds);

        BigDecimal tauxAnomaliesJustifieesDernierMois = cloturePaieRepository.findTopByOrderByMoisDesc()
                .map(CloturePaie::getTauxAnomaliesJustifiees)
                .orElse(null);

        return new KpiDto(effectifActif, effectifTotal, tauxTurnoverMensuel, congesEnCours,
                alertesPointageNonJustifiees, renouvellementsUrgents, evaluationsEnCours, evaluationsValidees,
                tauxAnomaliesJustifieesDernierMois);
    }

    @Cacheable(value = "dashboardCharts", key = "#departementId != null ? #departementId : 'global'")
    public ChartsDto charts(Long departementId) {
        List<Object[]> evolutionBrute = employeRepository.evolutionEffectifsParMois(departementId);
        List<Object[]> departsBrute = employeRepository.departsParMois(departementId);

        List<PointDto> evolutionEffectifs = evolutionBrute.stream()
                .map(row -> new PointDto((String) row[0], new BigDecimal(row[1].toString())))
                .toList();

        // ponytail: taux mensuel approximé sur l'effectif du même mois (pas
        // d'effectif "en début de mois" dédié, aucune table d'historique de
        // headcount en base) — à affiner si un historique headcount est ajouté.
        Map<String, BigDecimal> effectifParMois = evolutionEffectifs.stream()
                .collect(Collectors.toMap(PointDto::label, PointDto::valeur));
        List<PointDto> turnoverMensuel = departsBrute.stream()
                .map(row -> {
                    String mois = (String) row[0];
                    BigDecimal departsMois = new BigDecimal(row[1].toString());
                    BigDecimal effectifMois = effectifParMois.getOrDefault(mois, BigDecimal.ONE);
                    BigDecimal taux = effectifMois.signum() == 0 ? BigDecimal.ZERO
                            : departsMois.multiply(BigDecimal.valueOf(100))
                                    .divide(effectifMois, 1, RoundingMode.HALF_UP);
                    return new PointDto(mois, taux);
                })
                .toList();

        List<RepartitionDto> congesEnCours = congesEnCoursParRepartition(departementId);
        List<PointDto> anomaliesPointageHebdo = anomaliesPointageParSemaine(departementId);

        return new ChartsDto(evolutionEffectifs, turnoverMensuel, congesEnCours, anomaliesPointageHebdo);
    }

    private List<BigDecimal> extraireValeurs(List<Object[]> lignes) {
        return lignes.stream().map(row -> new BigDecimal(row[1].toString())).toList();
    }

    private BigDecimal tauxDuDernierMois(List<BigDecimal> evolution, List<BigDecimal> departs) {
        if (departs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal departsDernierMois = departs.get(departs.size() - 1);
        BigDecimal effectifReference = evolution.isEmpty() ? BigDecimal.ONE : evolution.get(evolution.size() - 1);
        if (effectifReference.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return departsDernierMois.multiply(BigDecimal.valueOf(100))
                .divide(effectifReference, 1, RoundingMode.HALF_UP);
    }

    private Set<UUID> employeIdsDuDepartement(Long departementId) {
        return employeRepository.findByDepartement_Id(departementId).stream()
                .map(Employe::getId)
                .collect(Collectors.toSet());
    }

    private long alertesPointage(int dernierNJours, Set<UUID> employeIds) {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = fin.minusDays(dernierNJours);
        List<Object[]> parEmploye = pointageBrutRepository.compterAnomaliesParEmploye(
                debut, fin, PointageStatut.ANOMALIE_NON_JUSTIFIEE);
        return parEmploye.stream()
                .filter(row -> employeIds == null || employeIds.contains(row[0]))
                .mapToLong(row -> (Long) row[1])
                .sum();
    }

    private List<RepartitionDto> congesEnCoursParRepartition(Long departementId) {
        List<Indisponibilite> enCours = indisponibiliteRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        "VALIDE", LocalDate.now(), LocalDate.now());

        if (departementId != null) {
            // Vue Manager (une seule équipe) : répartition par type de congé.
            Set<UUID> employeIds = employeIdsDuDepartement(departementId);
            return enCours.stream()
                    .filter(i -> employeIds.contains(i.getEmployeId()))
                    .collect(Collectors.groupingBy(Indisponibilite::getTypeIndisponibilite, Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> new RepartitionDto(e.getKey(), e.getValue()))
                    .toList();
        }

        // Vue globale (RH/SUPERUSER/DIRECTEUR) : répartition par département,
        // via un lookup batché des employés concernés (même principe que
        // RenouvellementService#enrichir), pas de relation JPA départements
        // sur indisponibilites.
        Set<UUID> employeIdsConcernes = enCours.stream().map(Indisponibilite::getEmployeId).collect(Collectors.toSet());
        Map<UUID, Employe> employesParId = employeRepository.findAllById(employeIdsConcernes).stream()
                .collect(Collectors.toMap(Employe::getId, e -> e));

        Map<String, Long> parDepartement = new HashMap<>();
        for (Indisponibilite i : enCours) {
            Employe employe = employesParId.get(i.getEmployeId());
            String label = employe != null && employe.getDepartement() != null
                    ? employe.getDepartement().getNom()
                    : "Non affecté";
            parDepartement.merge(label, 1L, Long::sum);
        }
        return parDepartement.entrySet().stream()
                .map(e -> new RepartitionDto(e.getKey(), e.getValue()))
                .toList();
    }

    private List<PointDto> anomaliesPointageParSemaine(Long departementId) {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = fin.minusWeeks(SEMAINES_GRAPHIQUE);
        Set<UUID> employeIds = departementId == null ? null : employeIdsDuDepartement(departementId);

        List<Object[]> lignes = pointageBrutRepository.compterAnomaliesParSemaineEtEmploye(
                debut, fin, PointageStatut.ANOMALIE_NON_JUSTIFIEE.name());

        Map<String, Long> parSemaine = new HashMap<>();
        for (Object[] ligne : lignes) {
            String semaine = (String) ligne[0];
            UUID employeId = (UUID) ligne[1];
            long compte = ((Number) ligne[2]).longValue();
            if (employeIds == null || employeIds.contains(employeId)) {
                parSemaine.merge(semaine, compte, Long::sum);
            }
        }

        // Comble les semaines sans anomalie (absentes du GROUP BY) pour une
        // courbe continue sur les 8 dernières semaines.
        List<PointDto> resultat = new java.util.ArrayList<>();
        LocalDate lundiCourant = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        for (int i = SEMAINES_GRAPHIQUE - 1; i >= 0; i--) {
            LocalDate lundi = lundiCourant.minusWeeks(i);
            String cle = lundi.toString();
            resultat.add(new PointDto(cle, BigDecimal.valueOf(parSemaine.getOrDefault(cle, 0L))));
        }
        return resultat;
    }
}
