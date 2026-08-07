package com.sirh.notifications.service;

import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.repository.UtilisateurRepository;
import com.sirh.demandes.entity.Indisponibilite;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.demandes.repository.IndisponibiliteSpecifications;
import com.sirh.notifications.entity.NotificationType;
import com.sirh.parametres.service.ParametreGlobalService;
import com.sirh.personnel.entity.Contrat;
import com.sirh.personnel.entity.ContratStatut;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.ContratRepository;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.repository.PointageBrutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Module 11, "Alerte Sous-activité (Mensuelle)" : calcul isolé du @Scheduled
 * (règle d'or du plan Lot 8). Fenêtre de 30 jours glissants se terminant à la
 * dateReference (dernier jour du mois en usage normal), contrats EN_COURS
 * uniquement, suspension légale de contrat exclue via IndisponibiliteSpecifications.
 */
@Service
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

    public SousActiviteService(ContratRepository contratRepository, IndisponibiliteRepository indisponibiliteRepository,
                                EmployeRepository employeRepository, PointageBrutRepository pointageBrutRepository,
                                UtilisateurRepository utilisateurRepository, ParametreGlobalService parametreGlobalService,
                                NotificationService notificationService) {
        this.contratRepository = contratRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.employeRepository = employeRepository;
        this.pointageBrutRepository = pointageBrutRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.parametreGlobalService = parametreGlobalService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void detecterSousActivite(LocalDate dateReference) {
        int seuil = parametreGlobalService.valeurDecimale(CLE_SEUIL).intValue();
        List<String> typesSuspensionLegale = List.of(parametreGlobalService.findByCle(CLE_TYPES_SUSPENSION).getValeur().split(","));
        LocalDate debutFenetre = dateReference.minusDays(FENETRE_JOURS - 1);

        List<UUID> employesContratActif = contratRepository.findByStatut(ContratStatut.EN_COURS).stream()
                .map(Contrat::getEmployeId).distinct().toList();
        if (employesContratActif.isEmpty()) {
            return;
        }

        Set<UUID> exclusSuspensionLegale = indisponibiliteRepository
                .findAll(IndisponibiliteSpecifications.suspensionLegaleActiveSur(debutFenetre, dateReference, typesSuspensionLegale))
                .stream().map(Indisponibilite::getEmployeId).collect(Collectors.toSet());

        List<UUID> candidats = employesContratActif.stream().filter(id -> !exclusSuspensionLegale.contains(id)).toList();
        if (candidats.isEmpty()) {
            return;
        }

        Map<UUID, Employe> employesParId = employeRepository.findAllById(candidats).stream()
                .collect(Collectors.toMap(Employe::getId, e -> e));
        List<Utilisateur> destinatairesRh = utilisateurRepository.findByRoleIn(List.of(Role.ROLE_RH, Role.ROLE_SUPERUSER));
        LocalDateTime debut = debutFenetre.atStartOfDay();
        LocalDateTime fin = dateReference.plusDays(1).atStartOfDay();

        for (UUID employeId : candidats) {
            long joursTravailles = pointageBrutRepository.findByEmployeIdAndTimestampPointageBetween(employeId, debut, fin).stream()
                    .filter(p -> p.getStatut() != PointageStatut.IGNORE_DOUBLON)
                    .map(p -> p.getTimestampPointage().toLocalDate())
                    .distinct()
                    .count();
            if (joursTravailles > seuil) {
                continue;
            }
            Employe employe = employesParId.get(employeId);
            String nom = employe != null ? employe.getNom() + " " + employe.getPrenom() : employeId.toString();
            String message = "Risque de carence : %s n'a travaillé que %d jour(s) sur les 30 derniers jours."
                    .formatted(nom, joursTravailles);
            for (Utilisateur destinataire : destinatairesRh) {
                notificationService.creer(destinataire.getId(), NotificationType.ALERTE_SOUS_ACTIVITE, message, null, null);
            }
        }
    }
}
