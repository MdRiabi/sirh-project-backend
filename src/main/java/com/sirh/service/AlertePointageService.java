package com.sirh.service;

import com.sirh.model.entity.Departement;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.enums.NotificationType;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.PointageBrutRepository;
import com.sirh.model.repository.UtilisateurRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module 11, "Alerte Pointage (Hebdomadaire)" : calcul isolé du @Scheduled (règle d'or du plan Lot
 * 8) — la tâche CRON n'orchestre que l'appel.
 */
@Service
@RequiredArgsConstructor
public class AlertePointageService {

  private static final Logger log = LoggerFactory.getLogger(AlertePointageService.class);
  private static final String CLE_SEUIL = "SEUIL_OUBLIS_BADGEAGE_HEBDO";

  private final PointageBrutRepository pointageBrutRepository;
  private final EmployeRepository employeRepository;
  private final UtilisateurRepository utilisateurRepository;
  private final ParametreGlobalService parametreGlobalService;
  private final NotificationService notificationService;

  // "cumule 3 oublis... COUNT(oublis) = 3" (cahier-fonctionnel.md Module 11) —
  // interprété en >= seuil (comme SEUIL_ANOMALIES_CLOTURE, SEUIL_PRIME_*,
  // etc.) : un employé à 4 oublis reste alerté, jamais exempté par un
  // dépassement strict du seuil.
  @Transactional
  public void detecterOublisBadgeageSemaine(LocalDate finSemaine) {
    int seuil = parametreGlobalService.valeurDecimale(CLE_SEUIL).intValue();
    LocalDate debutSemaine = finSemaine.with(DayOfWeek.MONDAY);
    LocalDateTime debut = debutSemaine.atStartOfDay();
    LocalDateTime fin = finSemaine.plusDays(1).atStartOfDay();

    Map<UUID, Long> anomaliesParEmploye =
        pointageBrutRepository
            .compterAnomaliesParEmploye(debut, fin, PointageStatut.ANOMALIE_NON_JUSTIFIEE)
            .stream()
            .collect(Collectors.toMap(ligne -> (UUID) ligne[0], ligne -> (Long) ligne[1]));

    List<UUID> employesEnAlerte =
        anomaliesParEmploye.entrySet().stream()
            .filter(entree -> entree.getValue() >= seuil)
            .map(Map.Entry::getKey)
            .toList();
    if (employesEnAlerte.isEmpty()) {
      return;
    }

    Map<UUID, Employe> employesParId =
        employeRepository.findAllById(employesEnAlerte).stream()
            .collect(Collectors.toMap(Employe::getId, e -> e));

    for (UUID employeId : employesEnAlerte) {
      Employe employe = employesParId.get(employeId);
      Departement departement = employe != null ? employe.getDepartement() : null;
      if (departement == null || departement.getManagerId() == null) {
        log.warn(
            "Alerte pointage : employé {} sans manager de département résolvable, notification"
                + " ignorée.",
            employeId);
        continue;
      }
      Utilisateur manager =
          utilisateurRepository
              .findByEmployeIdAndDeletedAtIsNull(departement.getManagerId())
              .orElse(null);
      if (manager == null) {
        log.warn(
            "Alerte pointage : manager {} sans compte utilisateur, notification ignorée.",
            departement.getManagerId());
        continue;
      }
      String message =
          "%s %s cumule %d oublis de badgeage cette semaine."
              .formatted(employe.getNom(), employe.getPrenom(), anomaliesParEmploye.get(employeId));
      notificationService.creer(
          manager.getId(), NotificationType.ALERTE_POINTAGE, message, null, null);
    }
  }
}
