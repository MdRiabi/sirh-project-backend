package com.sirh.planning;

import com.sirh.AbstractIntegrationTest;
import com.sirh.auth.dto.LoginRequest;
import com.sirh.auth.dto.TokenResponse;
import com.sirh.auth.entity.Role;
import com.sirh.demandes.entity.Indisponibilite;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.personnel.entity.Employe;
import com.sirh.planning.entity.Shift;
import com.sirh.planning.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre les deux règles bloquantes du Module 2 (cahier-fonctionnel.md) :
 * repos légal de 11h et chevauchement avec un congé validé.
 */
class PlanningControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private IndisponibiliteRepository indisponibiliteRepository;

    private String accessTokenFor(Role role) {
        String username = createFixtureUser(role);
        TokenResponse login = restTemplate.postForEntity(
                baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class).getBody();
        return login.accessToken();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Shift creerShift(String nom, LocalTime debut, LocalTime fin) {
        return shiftRepository.save(Shift.builder()
                .nom(nom).typeRotation("FIXE").heureDebut(debut).heureFin(fin).build());
    }

    private ResponseEntity<Map> affecter(String token, UUID employeId, Long shiftId, LocalDateTime debut, LocalDateTime fin) {
        Map<String, Object> body = Map.of(
                "employeId", employeId,
                "shiftId", shiftId,
                "dateDebut", debut.toString(),
                "dateFin", fin.toString());
        return restTemplate.exchange(baseUrl("/plannings/affecter"), org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    @Test
    void affecter_avecMoinsDe11hDeReposConsecutif_estRefuseAvec409() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        Shift soir = creerShift("Soir", LocalTime.of(15, 0), LocalTime.of(23, 0));
        Shift matin = creerShift("Matin", LocalTime.of(6, 0), LocalTime.of(14, 0));

        LocalDate jour = LocalDate.now().plusDays(10);
        ResponseEntity<Map> premiere = affecter(token, employe.getId(), soir.getId(),
                jour.atTime(15, 0), jour.atTime(23, 0));
        assertThat(premiere.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Repos réel : 23h00 -> 06h00 le lendemain = 7h, < 11h minimum.
        ResponseEntity<Map> deuxieme = affecter(token, employe.getId(), matin.getId(),
                jour.plusDays(1).atTime(6, 0), jour.plusDays(1).atTime(14, 0));
        assertThat(deuxieme.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(deuxieme.getBody().get("message").toString()).contains("VIOLATION LÉGALE");
    }

    @Test
    void affecter_avec11hDeReposConsecutif_estAccepte() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        Shift soir = creerShift("Soir", LocalTime.of(14, 0), LocalTime.of(22, 0));
        Shift matin = creerShift("Matin", LocalTime.of(6, 0), LocalTime.of(14, 0));

        LocalDate jour = LocalDate.now().plusDays(11);
        affecter(token, employe.getId(), soir.getId(), jour.atTime(14, 0), jour.atTime(22, 0));

        // Repos réel : 22h00 -> 09h00 le lendemain = 11h pile.
        ResponseEntity<Map> deuxieme = affecter(token, employe.getId(), matin.getId(),
                jour.plusDays(1).atTime(9, 0), jour.plusDays(1).atTime(17, 0));
        assertThat(deuxieme.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void affecter_surUnCongeValide_estRefuseAvec409() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();
        Shift matin = creerShift("Matin", LocalTime.of(6, 0), LocalTime.of(14, 0));

        LocalDate jour = LocalDate.now().plusDays(20);
        indisponibiliteRepository.save(Indisponibilite.builder()
                .employeId(employe.getId())
                .typeIndisponibilite("CONGE_PAYE")
                .dateDebut(jour.minusDays(1))
                .dateFin(jour.plusDays(1))
                .statut("VALIDE")
                .creeParId(employe.getId())
                .creeParRole("RH")
                .build());

        ResponseEntity<Map> reponse = affecter(token, employe.getId(), matin.getId(),
                jour.atTime(6, 0), jour.atTime(14, 0));
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void monPlanning_neRenvoieQueLePlanningDuTitulaireDuJwt() {
        String tokenRh = accessTokenFor(Role.ROLE_RH);
        var fixtureEmploye = createFixtureUserWithEmploye(Role.ROLE_EMPLOYE);
        Shift matin = creerShift("Matin", LocalTime.of(6, 0), LocalTime.of(14, 0));
        LocalDate jour = LocalDate.now().plusDays(30);
        affecter(tokenRh, fixtureEmploye.employeId(), matin.getId(), jour.atTime(6, 0), jour.atTime(14, 0));

        TokenResponse loginEmploye = restTemplate.postForEntity(
                baseUrl("/auth/login"), new LoginRequest(fixtureEmploye.username(), FIXTURE_PASSWORD), TokenResponse.class).getBody();

        String url = baseUrl("/mon-planning?dateDebut=" + jour.atStartOfDay()
                + "&dateFin=" + jour.plusDays(1).atStartOfDay());
        ResponseEntity<java.util.List> reponse = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders(loginEmploye.accessToken())), java.util.List.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).hasSize(1);
    }

    // Régression : un MANAGER dont l'employé n'a pas de département (fixture par
    // défaut, departementId = null) ne doit jamais provoquer de NPE côté
    // verifierScopeManager — juste un refus propre, jamais un 500.
    @Test
    void planningDepartement_managerSansDepartementPropre_estRefuseProprementSans500() {
        String managerToken = accessTokenFor(Role.ROLE_MANAGER);
        ResponseEntity<Map> reponse = restTemplate.exchange(
                baseUrl("/plannings/departement/1?dateDebut=2026-01-01T00:00:00&dateFin=2026-01-02T00:00:00"),
                org.springframework.http.HttpMethod.GET, new HttpEntity<>(authHeaders(managerToken)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}