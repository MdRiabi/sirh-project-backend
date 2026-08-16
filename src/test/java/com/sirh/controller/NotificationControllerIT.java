package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.repository.UtilisateurRepository;
import com.sirh.model.entity.Notification;
import com.sirh.model.enums.NotificationType;
import com.sirh.model.repository.NotificationRepository;
import com.sirh.model.entity.Employe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 11 : GET/PUT .../lue strictement scopés au destinataire authentifié
 * (cahier-technique.md §4.8, "tout rôle authentifié"). Les notifications ne
 * sont jamais créées via l'API (seulement par les déclencheurs Lot 5/CRON),
 * donc insérées directement via NotificationRepository ici.
 */
class NotificationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    private String login(String username) {
        return login(username, FIXTURE_PASSWORD).accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Long utilisateurIdDe(String username) {
        return utilisateurRepository.findByUsernameAndDeletedAtIsNull(username).orElseThrow().getId();
    }

    @Test
    void list_neRenvoieQueLesNotificationsDuDestinataireAuthentifie() {
        String username = createFixtureUser(Role.ROLE_MANAGER);
        Long destinataireId = utilisateurIdDe(username);
        Long autreDestinataireId = utilisateurIdDe(createFixtureUser(Role.ROLE_RH));
        notificationRepository.save(Notification.builder().destinataireId(destinataireId)
                .type(NotificationType.ALERTE_POINTAGE).message("Pour moi").lue(false).build());
        notificationRepository.save(Notification.builder().destinataireId(autreDestinataireId)
                .type(NotificationType.ALERTE_POINTAGE).message("Pas pour moi").lue(false).build());

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/notifications"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(login(username))), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) reponse.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(((Map) content.get(0)).get("message")).isEqualTo("Pour moi");
    }

    @Test
    void marquerLue_proprietaire_passeLueATrueEtRenvoie200() {
        String username = createFixtureUser(Role.ROLE_EMPLOYE);
        Long destinataireId = utilisateurIdDe(username);
        Notification notif = notificationRepository.save(Notification.builder().destinataireId(destinataireId)
                .type(NotificationType.ALERTE_POINTAGE).message("A lire").lue(false).build());

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/notifications/" + notif.getReference() + "/lue"),
                HttpMethod.PUT, new HttpEntity<>(authHeaders(login(username))), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) reponse.getBody().get("data")).get("lue")).isEqualTo(true);
    }

    @Test
    void marquerLue_notificationDunAutreDestinataire_estRefuseeAvec403() {
        Long autreDestinataireId = utilisateurIdDe(createFixtureUser(Role.ROLE_RH));
        Notification notif = notificationRepository.save(Notification.builder().destinataireId(autreDestinataireId)
                .type(NotificationType.ALERTE_POINTAGE).message("Pas pour moi").lue(false).build());
        String usurpateur = createFixtureUser(Role.ROLE_MANAGER);

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/notifications/" + notif.getReference() + "/lue"),
                HttpMethod.PUT, new HttpEntity<>(authHeaders(login(usurpateur))), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Bout en bout de l'architecture événementielle (plan Lot 8) : la création
    // d'une autorisation par un Manager (statut EN_ATTENTE_RH) publie un
    // DemandeCreeEvent, consommé après commit par NotificationEventListener
    // (@Async) — poll court plutôt que Thread.sleep fixe, le traitement passe
    // par un pool dédié séparé du thread de la requête HTTP.
    @Test
    void creationAutorisationParManager_notifieLeRhDeFaconAsynchrone() throws InterruptedException {
        String rhUsername = createFixtureUser(Role.ROLE_RH);
        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Employe employe = createFixtureEmploye();
        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "type", "RETARD_JUSTIFIE",
                "dateAutorisation", LocalDate.now().toString(), "heureDebut", "08:00:00",
                "dureeMinutes", 30, "avisManagerStatut", "FAVORABLE");

        ResponseEntity<Map> creation = restTemplate.exchange(baseUrl("/autorisations"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(managerToken)), Map.class);
        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        List<Notification> notifications = List.of();
        for (int tentative = 0; tentative < 30 && notifications.isEmpty(); tentative++) {
            Thread.sleep(100);
            notifications = notificationRepository.findByDestinataireIdAndDeletedAtIsNullOrderByCreatedAtDesc(utilisateurIdDe(rhUsername));
        }

        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.DEMANDE_A_VALIDER);
        assertThat(notifications.get(0).getEntiteType()).isEqualTo("AUTORISATION");
    }
}
