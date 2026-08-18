package com.sirh.service;

import com.sirh.model.entity.Contrat;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Indisponibilite;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.NotificationType;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.Role;
import com.sirh.model.repository.ContratRepository;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.IndisponibiliteRepository;
import com.sirh.model.repository.IndisponibiliteSpecifications;
import com.sirh.model.repository.PointageBrutRepository;
import com.sirh.model.repository.UtilisateurRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module 11, "Alerte Sous-activité (Mensuelle)" : calcul isolé du @Scheduled (règle d'or du plan
 * Lot 8). Fenêtre de 30 jours glissants se terminant à la dateReference (dernier jour du mois en
 * usage normal), contrats EN_COURS uniquement, suspension légale de contrat exclue via
 * IndisponibiliteSpecifications.
 */
@Service
@RequiredArgsConstructor
public class SousActiviteService {

  private static final String CLE_SEUIL = "SEUIL_JOURS_TRAVAILLES_SOUS_ACTIVITE";
  private static final String CLE_TYPES_SUSPENSION = "TYPES_SUSPENSION_LEGALE";
  private static final int FENETRE_JOURS = 30;

  private final ContratRepository contratRepository;
  private final IndisponibiliteRepository indisponibiliteRepository;
  private final EmployeRepository employeRepository;
  private final PointageBrutRepository pointageBrutRepository;
  private final UtilisateurRepository utilisateurRepository;
  private final ParametreGlobalService parametreGlobalService;
  private final NotificationService notificationService;

  @Transactional
  public void detecterSousActivite(LocalDate dateReference) {
    int seuil = parametreGlobalService.valeurDecimale(CLE_SEUIL).intValue();
    List<String> typesSuspensionLegale =
        List.of(parametreGlobalService.findByCle(CLE_TYPES_SUSPENSION).getValeur().split(","));
    LocalDate debutFenetre = dateReference.minusDays(FENETRE_JOURS - 1);

    List<UUID> employesContratActif =
        contratRepository.findByStatutAndDeletedAtIsNull(ContratStatut.EN_COURS).stream()
            .map(Contrat::getEmployeId)
            .distinct()
            .toList();
    if (employesContratActif.isEmpty()) {
      return;
    }

    Set<UUID> exclusSuspensionLegale =
        indisponibiliteRepository
            .findAll(
                IndisponibiliteSpecifications.suspensionLegaleActiveSur(
                    debutFenetre, dateReference, typesSuspensionLegale))
            .stream()
            .map(Indisponibilite::getEmployeId)
            .collect(Collectors.toSet());

    List<UUID> candidats =
        employesContratActif.stream().filter(id -> !exclusSuspensionLegale.contains(id)).toList();
    if (candidats.isEmpty()) {
      return;
    }

    Map<UUID, Employe> employesParId =
        employeRepository.findAllById(candidats).stream()
            .collect(Collectors.toMap(Employe::getId, e -> e));
    List<Utilisateur> destinatairesRh =
        utilisateurRepository.findByRoleInAndDeletedAtIsNull(
            List.of(Role.ROLE_RH, Role.ROLE_SUPERUSER));
    LocalDateTime debut = debutFenetre.atStartOfDay();
    LocalDateTime fin = dateReference.plusDays(1).atStartOfDay();

    for (UUID employeId : candidats) {
      long joursTravailles =
          pointageBrutRepository
              .findByEmployeIdAndTimestampPointageBetweenAndDeletedAtIsNull(employeId, debut, fin)
              .stream()
              .filter(p -> p.getStatut() != PointageStatut.IGNORE_DOUBLON)
              .map(p -> p.getTimestampPointage().toLocalDate())
              .distinct()
              .count();
      if (joursTravailles > seuil) {
        continue;
      }
      Employe employe = employesParId.get(employeId);
      String nom =
          employe != null ? employe.getNom() + " " + employe.getPrenom() : employeId.toString();
      String message =
          "Risque de carence : %s n'a travaillé que %d jour(s) sur les 30 derniers jours."
              .formatted(nom, joursTravailles);
      for (Utilisateur destinataire : destinatairesRh) {
        notificationService.creerNotification(
            destinataire.getId(), NotificationType.ALERTE_SOUS_ACTIVITE, message, null, null);
      }
    }
  }
}
