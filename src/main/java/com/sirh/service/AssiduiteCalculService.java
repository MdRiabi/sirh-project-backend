package com.sirh.service;

import com.sirh.model.dto.CalculMensuelEmployeDto;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Planning;
import com.sirh.model.entity.PointageBrut;
import com.sirh.model.enums.PlanningStatut;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.Role;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.PlanningRepository;
import com.sirh.model.repository.PointageBrutRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moteur de calcul mensuel (Module 3, cahier-fonctionnel.md) : croise pointages_bruts (heures
 * réelles) et plannings (heures théoriques) pour produire, par employé, la segmentation heures
 * normales/nuit/majorées, le taux de présence, le taux de prime d'assiduité (paliers d'incidents)
 * et la pénalité (déficit théorique/réel). Aucun seuil n'est codé en dur — tous lus depuis
 * parametres_globaux (Lot 5).
 */
@Service
@RequiredArgsConstructor
public class AssiduiteCalculService {

  private final EmployeRepository employeRepository;
  private final PointageBrutRepository pointageBrutRepository;
  private final PlanningRepository planningRepository;
  private final ParametreGlobalService parametreGlobalService;

  // MANAGER restreint à son propre département (departementId client ignoré) —
  // même convention que les autres endpoints de listing scopés département.
  @Transactional(readOnly = true)
  public List<CalculMensuelEmployeDto> calculerMoisAvecScope(
      YearMonth mois, Role role, Long principalDepartementId, Long departementIdParam) {
    Long departementEffectif =
        role == Role.ROLE_MANAGER ? principalDepartementId : departementIdParam;
    return calculerMois(mois, departementEffectif);
  }

  @Transactional(readOnly = true)
  public List<CalculMensuelEmployeDto> calculerMois(YearMonth mois, Long departementId) {
    List<Employe> employes =
        departementId != null
            ? employeRepository.findByDepartement_IdAndDeletedAtIsNull(departementId)
            : employeRepository.findAll();

    LocalTime heureDebutNuit =
        LocalTime.parse(parametreGlobalService.findByCle("HEURE_DEBUT_NUIT").getValeur());
    LocalTime heureFinNuit =
        LocalTime.parse(parametreGlobalService.findByCle("HEURE_FIN_NUIT").getValeur());
    int seuilMajoreesMinutes =
        parametreGlobalService
            .valeurDecimale("SEUIL_HEURES_MAJOREES_JOUR")
            .multiply(BigDecimal.valueOf(60))
            .intValue();
    int seuilPrime100 = parametreGlobalService.valeurDecimale("SEUIL_PRIME_100").intValue();
    int seuilPrime50 = parametreGlobalService.valeurDecimale("SEUIL_PRIME_50").intValue();
    int seuilPrime33 = parametreGlobalService.valeurDecimale("SEUIL_PRIME_33").intValue();

    LocalDateTime debut = mois.atDay(1).atStartOfDay();
    LocalDateTime fin = mois.atEndOfMonth().plusDays(1).atStartOfDay();

    return employes.stream()
        .map(
            employe ->
                calculerEmploye(
                    employe,
                    debut,
                    fin,
                    heureDebutNuit,
                    heureFinNuit,
                    seuilMajoreesMinutes,
                    seuilPrime100,
                    seuilPrime50,
                    seuilPrime33))
        .toList();
  }

  private CalculMensuelEmployeDto calculerEmploye(
      Employe employe,
      LocalDateTime debut,
      LocalDateTime fin,
      LocalTime heureDebutNuit,
      LocalTime heureFinNuit,
      int seuilMajoreesMinutes,
      int seuilPrime100,
      int seuilPrime50,
      int seuilPrime33) {
    List<PointageBrut> pointages =
        pointageBrutRepository
            .findByEmployeIdAndTimestampPointageBetweenAndDeletedAtIsNull(
                employe.getId(), debut, fin)
            .stream()
            .filter(p -> p.getStatut() != PointageStatut.IGNORE_DOUBLON)
            .sorted(Comparator.comparing(PointageBrut::getTimestampPointage))
            .toList();

    Map<LocalDate, Integer> minutesReellesParJour = new HashMap<>();
    int minutesNuitTotal = 0;
    for (int i = 0; i + 1 < pointages.size(); i += 2) {
      LocalDateTime debutSession = pointages.get(i).getTimestampPointage();
      LocalDateTime finSession = pointages.get(i + 1).getTimestampPointage();
      int minutesSession = (int) Duration.between(debutSession, finSession).toMinutes();
      minutesReellesParJour.merge(debutSession.toLocalDate(), minutesSession, Integer::sum);
      minutesNuitTotal += minutesNuit(debutSession, finSession, heureDebutNuit, heureFinNuit);
    }

    int minutesMajoreesTotal =
        minutesReellesParJour.values().stream()
            .mapToInt(minutes -> Math.max(0, minutes - seuilMajoreesMinutes))
            .sum();
    int minutesTotalTravail =
        minutesReellesParJour.values().stream().mapToInt(Integer::intValue).sum();
    int minutesNormalesTotal = minutesTotalTravail - minutesMajoreesTotal;

    int incidents =
        pointageBrutRepository
            .findByEmployeIdAndTimestampPointageBetweenAndStatutAndDeletedAtIsNull(
                employe.getId(), debut, fin, PointageStatut.ANOMALIE_NON_JUSTIFIEE)
            .size();
    BigDecimal tauxPrime = tauxPrime(incidents, seuilPrime100, seuilPrime50, seuilPrime33);

    Map<LocalDate, Integer> minutesTheoriquesParJour =
        planningRepository
            .findByEmployeIdAndDateDebutBetweenAndDeletedAtIsNullOrderByDateDebut(
                employe.getId(), debut, fin)
            .stream()
            .filter(
                p ->
                    p.getStatut() != PlanningStatut.REPOS
                        && p.getStatut() != PlanningStatut.CONGE
                        && p.getStatut() != PlanningStatut.ABSENCE)
            .collect(
                java.util.stream.Collectors.groupingBy(
                    p -> p.getDateDebut().toLocalDate(),
                    java.util.stream.Collectors.summingInt(this::minutesPlanning)));

    long joursTravailles =
        minutesTheoriquesParJour.keySet().stream()
            .filter(jour -> minutesReellesParJour.getOrDefault(jour, 0) > 0)
            .count();
    BigDecimal tauxPresence =
        minutesTheoriquesParJour.isEmpty()
            ? BigDecimal.valueOf(100)
            : BigDecimal.valueOf(joursTravailles * 100)
                .divide(
                    BigDecimal.valueOf(minutesTheoriquesParJour.size()), 2, RoundingMode.HALF_UP);

    int minutesPenalite =
        minutesTheoriquesParJour.entrySet().stream()
            .mapToInt(
                entree ->
                    Math.max(
                        0,
                        entree.getValue() - minutesReellesParJour.getOrDefault(entree.getKey(), 0)))
            .sum();

    return new CalculMensuelEmployeDto(
        employe.getId(),
        employe.getNom(),
        employe.getPrenom(),
        minutes(minutesNormalesTotal),
        minutes(minutesNuitTotal),
        minutes(minutesMajoreesTotal),
        tauxPresence,
        tauxPrime,
        incidents,
        minutes(minutesPenalite));
  }

  private int minutesPlanning(Planning planning) {
    return (int) Duration.between(planning.getDateDebut(), planning.getDateFin()).toMinutes();
  }

  private BigDecimal tauxPrime(
      int incidents, int seuilPrime100, int seuilPrime50, int seuilPrime33) {
    if (incidents <= seuilPrime100) {
      return BigDecimal.valueOf(100);
    }
    if (incidents <= seuilPrime50) {
      return BigDecimal.valueOf(50);
    }
    if (incidents <= seuilPrime33) {
      return BigDecimal.valueOf(33);
    }
    return BigDecimal.valueOf(25);
  }

  // Anchor du créneau de nuit sur chaque jour couvert par la session (veille
  // incluse, pour capter un créneau qui a démarré la veille et chevauche
  // minuit, ex. 21:00-06:00) : deux créneaux ancrés sur des jours consécutifs
  // ne se chevauchent jamais entre eux, donc sommer les recouvrements
  // individuels ne double-compte rien.
  private int minutesNuit(
      LocalDateTime debutSession,
      LocalDateTime finSession,
      LocalTime heureDebutNuit,
      LocalTime heureFinNuit) {
    int total = 0;
    for (LocalDate jour = debutSession.toLocalDate().minusDays(1);
        !jour.isAfter(finSession.toLocalDate());
        jour = jour.plusDays(1)) {
      LocalDateTime nuitDebut = jour.atTime(heureDebutNuit);
      LocalDateTime nuitFin =
          !heureFinNuit.isAfter(heureDebutNuit)
              ? jour.plusDays(1).atTime(heureFinNuit)
              : jour.atTime(heureFinNuit);

      LocalDateTime chevauchementDebut = debutSession.isAfter(nuitDebut) ? debutSession : nuitDebut;
      LocalDateTime chevauchementFin = finSession.isBefore(nuitFin) ? finSession : nuitFin;
      if (chevauchementDebut.isBefore(chevauchementFin)) {
        total += (int) Duration.between(chevauchementDebut, chevauchementFin).toMinutes();
      }
    }
    return total;
  }

  private static BigDecimal minutes(int minutes) {
    return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
  }
}
