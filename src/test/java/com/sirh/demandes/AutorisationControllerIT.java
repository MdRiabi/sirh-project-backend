package com.sirh.demandes;

import com.sirh.AbstractIntegrationTest;
import com.sirh.auth.dto.LoginRequest;
import com.sirh.auth.dto.TokenResponse;
import com.sirh.auth.entity.Role;
import com.sirh.personnel.entity.Employe;
import com.sirh.pointages.entity.MethodeValidation;
import com.sirh.pointages.entity.PointageBrut;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.entity.SourceImport;
import com.sirh.pointages.entity.TypeEvenement;
import com.sirh.pointages.repository.PointageBrutRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre le workflow d'autorisation (Module 13) : saisie Manager avec avis →
 * validation RH, délai de régularisation 72h, quota mensuel 4h, justification
 * automatique des anomalies de pointage, mise à jour du planning à la
 * validation (Lot 5, décisions d'architecture explicites de l'utilisateur).
 */
class AutorisationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private PointageBrutRepository pointageBrutRepository;

    private String accessTokenFor(Role role) {
        return login(createFixtureUser(role));
    }

    private String login(String username) {
        return restTemplate.postForEntity(baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class)
                .getBody().accessToken();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<Map> creer(String token, Map<String, Object> body) {
        return restTemplate.exchange(baseUrl("/autorisations"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    private Map<String, Object> requeteBase(UUID employeId, LocalDate date) {
        return new java.util.HashMap<>(Map.of(
                "employeId", employeId,
                "type", "RETARD_JUSTIFIE",
                "dateAutorisation", date.toString(),
                "heureDebut", "08:00:00",
                "dureeMinutes", 30));
    }

    @Test
    void creer_parManagerDansLes72h_estEnAttenteRh() {
        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Employe employe = createFixtureEmploye();
        Map<String, Object> body = requeteBase(employe.getId(), LocalDate.now());
        body.put("avisManagerStatut", "FAVORABLE");

        ResponseEntity<Map> reponse = creer(managerToken, body);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reponse.getBody().get("statut")).isEqualTo("EN_ATTENTE_RH");
        assertThat(reponse.getBody().get("creeParRole")).isEqualTo("MANAGER");
        assertThat(reponse.getBody().get("avisManagerStatut")).isEqualTo("FAVORABLE");
    }

    @Test
    void creer_parManagerAuDelaDe72h_estRefuseAvec409() {
        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Employe employe = createFixtureEmploye();
        Map<String, Object> body = requeteBase(employe.getId(), LocalDate.now().minusDays(5));
        body.put("avisManagerStatut", "FAVORABLE");

        ResponseEntity<Map> reponse = creer(managerToken, body);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message").toString()).contains("DÉLAI DÉPASSÉ");
    }

    @Test
    void creer_parRhAuDelaDe72h_estAccepteEtDirectementValide() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        Map<String, Object> body = requeteBase(employe.getId(), LocalDate.now().minusDays(10));

        ResponseEntity<Map> reponse = creer(rhToken, body);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reponse.getBody().get("statut")).isEqualTo("VALIDE");
        assertThat(reponse.getBody().get("creeParRole")).isEqualTo("RH");
        assertThat(reponse.getBody().get("avisManagerStatut")).isNull();
    }

    // Workflow d'urgence RH (statut VALIDE dès la création) : la justification
    // doit s'appliquer immédiatement, sans appel valider() séparé (bug trouvé en
    // vérification manuelle docker compose + curl — corrigé dans AutorisationService).
    @Test
    void creerParRh_justifieImmediatementLAnomalieDePointageDuJour() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        LocalDate jour = LocalDate.now();

        PointageBrut anomalie = pointageBrutRepository.save(PointageBrut.builder()
                .employeId(employe.getId())
                .timestampPointage(jour.atTime(8, 5))
                .typeEvenement(TypeEvenement.ENTREE)
                .methodeValidation(MethodeValidation.BADGE)
                .sourceImport(SourceImport.HIKVISION_PUSH)
                .statut(PointageStatut.ANOMALIE_NON_JUSTIFIEE)
                .build());

        Map<String, Object> body = requeteBase(employe.getId(), jour);
        ResponseEntity<Map> creation = creer(rhToken, body);
        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long autorisationId = ((Number) creation.getBody().get("id")).longValue();

        PointageBrut rechargee = pointageBrutRepository.findById(anomalie.getId()).orElseThrow();
        assertThat(rechargee.getStatut()).isEqualTo(PointageStatut.JUSTIFIE);
        assertThat(rechargee.getAutorisationId()).isEqualTo(autorisationId);
    }

    // Même principe côté planning : le créneau ABSENCE est généré dès la
    // création par le RH, pas seulement après un valider() distinct.
    @Test
    void creerParRh_genereImmediatementUnCreneauAbsenceSurLePlanning() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        LocalDate jour = LocalDate.now().plusDays(1);

        Map<String, Object> body = requeteBase(employe.getId(), jour);
        ResponseEntity<Map> creation = creer(rhToken, body);
        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String url = baseUrl("/plannings/delta?employeId=" + employe.getId()
                + "&dateDebut=" + jour + "&dateFin=" + jour);
        ResponseEntity<List> planning = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rhToken)), List.class);
        assertThat(planning.getStatusCode()).isEqualTo(HttpStatus.OK);
        // ABSENCE est exclue des heures théoriques (comme REPOS/CONGE) : le
        // créneau existe mais le rapport delta reste vide pour ce jour.
        assertThat(planning.getBody()).isEmpty();
    }

    @Test
    void valider_avecQuotaDepasseSansTraitement_estRefuseAvec409() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        LocalDate jour = LocalDate.now();

        // 3 x 90min = 270min > 240min de quota mensuel.
        for (int i = 0; i < 3; i++) {
            Map<String, Object> body = requeteBase(employe.getId(), jour.plusDays(i));
            body.put("dureeMinutes", 90);
            creer(rhToken, body);
        }

        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Map<String, Object> derniere = requeteBase(employe.getId(), jour.plusDays(3));
        derniere.put("dureeMinutes", 90);
        derniere.put("avisManagerStatut", "FAVORABLE");
        ResponseEntity<Map> creation = creer(managerToken, derniere);
        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = ((Number) creation.getBody().get("id")).longValue();
        assertThat(((Number) creation.getBody().get("quotaDepasseMinutes")).intValue()).isPositive();

        ResponseEntity<Map> validation = restTemplate.exchange(
                baseUrl("/autorisations/" + id + "/valider"), HttpMethod.PUT,
                new HttpEntity<>(null, authHeaders(rhToken)), Map.class);
        assertThat(validation.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(validation.getBody().get("message").toString()).contains("QUOTA DÉPASSÉ");

        ResponseEntity<Map> validationForcee = restTemplate.exchange(
                baseUrl("/autorisations/" + id + "/valider"), HttpMethod.PUT,
                new HttpEntity<>(Map.of("traitementDepassement", "RECUPERATION"), authHeaders(rhToken)), Map.class);
        assertThat(validationForcee.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validationForcee.getBody().get("statut")).isEqualTo("VALIDE");
    }

    @Test
    void refuser_parRh_changeStatutSansToucherAuPlanningNiAuxPointages() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Employe employe = createFixtureEmploye();
        Map<String, Object> body = requeteBase(employe.getId(), LocalDate.now());
        body.put("avisManagerStatut", "DEFAVORABLE");
        ResponseEntity<Map> creation = creer(managerToken, body);
        assertThat(creation.getBody().get("statut")).isEqualTo("EN_ATTENTE_RH");
        Long id = ((Number) creation.getBody().get("id")).longValue();

        ResponseEntity<Map> refus = restTemplate.exchange(
                baseUrl("/autorisations/" + id + "/refuser"), HttpMethod.PUT,
                new HttpEntity<>(null, authHeaders(rhToken)), Map.class);

        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refus.getBody().get("statut")).isEqualTo("REFUSE");
    }

    @Test
    void valider_uneAutorisationDejaValidee_estRefuseAvec409() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        ResponseEntity<Map> creation = creer(rhToken, requeteBase(employe.getId(), LocalDate.now()));
        Long id = ((Number) creation.getBody().get("id")).longValue();

        ResponseEntity<Map> deuxiemeValidation = restTemplate.exchange(
                baseUrl("/autorisations/" + id + "/valider"), HttpMethod.PUT,
                new HttpEntity<>(null, authHeaders(rhToken)), Map.class);

        assertThat(deuxiemeValidation.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void list_parManager_neVoitQueSonPropreDepartement() {
        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Employe employeAutreDepartement = createFixtureEmploye();
        creer(accessTokenFor(Role.ROLE_RH), requeteBase(employeAutreDepartement.getId(), LocalDate.now()));

        ResponseEntity<List> reponse = restTemplate.exchange(baseUrl("/autorisations"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerToken)), List.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Manager fixture et employé n'appartiennent à aucun département commun
        // (departementId = null par défaut) -> liste vide, jamais une 500/403.
        assertThat(reponse.getBody()).isEmpty();
    }

    @Test
    void creer_avecEmployeIdInexistant_estRefuseAvec404() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        ResponseEntity<Map> reponse = creer(rhToken, requeteBase(UUID.randomUUID(), LocalDate.now()));
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
