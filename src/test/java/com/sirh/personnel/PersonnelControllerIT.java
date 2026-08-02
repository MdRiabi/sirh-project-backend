package com.sirh.personnel;

import com.sirh.AbstractIntegrationTest;
import com.sirh.auth.dto.LoginRequest;
import com.sirh.auth.dto.TokenResponse;
import com.sirh.auth.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersonnelControllerIT extends AbstractIntegrationTest {

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

    @Test
    void departement_crud_avecRoleRh_estAutorise() {
        String token = accessTokenFor(Role.ROLE_RH);
        Map<String, Object> body = Map.of("nom", "Département Test " + UUID.randomUUID());

        ResponseEntity<Map<String, Object>> created = restTemplate.exchange(
                baseUrl("/departements"), HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)),
                new ParameterizedTypeReference<>() {});
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Map<String, Object>> fetched = restTemplate.exchange(
                baseUrl("/departements/" + id), HttpMethod.GET, new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {});
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("nom")).isEqualTo(body.get("nom"));
    }

    @Test
    void departement_accesRefuse_pourRoleEmploye() {
        String token = accessTokenFor(Role.ROLE_EMPLOYE);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/departements"), HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void employe_creation_masqueLeMatriculeCnssParDefaut() {
        String token = accessTokenFor(Role.ROLE_RH);
        Map<String, Object> body = Map.of(
                "matriculeInterne", "MAT-" + UUID.randomUUID(),
                "matriculeCnss", "CNSS-1234-5678",
                "nom", "Dupont",
                "prenom", "Jean",
                "typeContrat", "CDI",
                "dateEmbauche", LocalDate.now().toString()
        );

        ResponseEntity<Map<String, Object>> created = restTemplate.exchange(
                baseUrl("/employes"), HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)),
                new ParameterizedTypeReference<>() {});
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("matriculeCnss")).isEqualTo("***-**-5678");
        String id = (String) created.getBody().get("id");

        ResponseEntity<Map<String, Object>> reveal = restTemplate.exchange(
                baseUrl("/employes/" + id + "/matricule-cnss"), HttpMethod.GET, new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {});
        assertThat(reveal.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reveal.getBody().get("matriculeCnss")).isEqualTo("CNSS-1234-5678");
    }

    @Test
    void employe_recherche_filtreParDepartementEtActif() {
        String token = accessTokenFor(Role.ROLE_RH);
        HttpHeaders headers = authHeaders(token);

        Map<String, Object> departement = restTemplate.exchange(
                baseUrl("/departements"), HttpMethod.POST,
                new HttpEntity<>(Map.of("nom", "Filtre Dept " + UUID.randomUUID()), headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        Long departementId = ((Number) departement.get("id")).longValue();

        createEmploye(headers, "Actif " + UUID.randomUUID(), departementId, true);
        createEmploye(headers, "Inactif " + UUID.randomUUID(), departementId, false);

        ResponseEntity<Map<String, Object>> page = restTemplate.exchange(
                baseUrl("/employes?departementId=" + departementId + "&actif=true"), HttpMethod.GET,
                new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("actif")).isEqualTo(true);
    }

    private void createEmploye(HttpHeaders headers, String nom, Long departementId, boolean actif) {
        Map<String, Object> body = Map.of(
                "matriculeInterne", "MAT-" + UUID.randomUUID(),
                "nom", nom,
                "prenom", "Test",
                "departementId", departementId,
                "typeContrat", "CDI",
                "dateEmbauche", LocalDate.now().toString(),
                "actif", actif
        );
        restTemplate.exchange(baseUrl("/employes"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    @Test
    void contrat_creation_metAJourEmployeEtGenereUnEvenementTimeline() {
        String token = accessTokenFor(Role.ROLE_RH);
        HttpHeaders headers = authHeaders(token);

        Map<String, Object> employeBody = Map.of(
                "matriculeInterne", "MAT-" + UUID.randomUUID(),
                "nom", "Renouvelable",
                "prenom", "Employe",
                "typeContrat", "CDD",
                "dateEmbauche", LocalDate.now().minusMonths(6).toString(),
                "dateFinContrat", LocalDate.now().toString()
        );
        Map<String, Object> employe = restTemplate.exchange(
                baseUrl("/employes"), HttpMethod.POST, new HttpEntity<>(employeBody, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        String employeId = (String) employe.get("id");

        LocalDate nouvelleDateDebut = LocalDate.now();
        Map<String, Object> contratBody = Map.of(
                "type", "CDI",
                "dateDebut", nouvelleDateDebut.toString()
        );
        ResponseEntity<Map<String, Object>> contratResponse = restTemplate.exchange(
                baseUrl("/employes/" + employeId + "/contrats"), HttpMethod.POST,
                new HttpEntity<>(contratBody, headers), new ParameterizedTypeReference<>() {});
        assertThat(contratResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map<String, Object>> employeAfter = restTemplate.exchange(
                baseUrl("/employes/" + employeId), HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});
        assertThat(employeAfter.getBody().get("typeContrat")).isEqualTo("CDI");
        assertThat(employeAfter.getBody().get("dateFinContrat")).isNull();

        ResponseEntity<List<Map<String, Object>>> timeline = restTemplate.exchange(
                baseUrl("/employes/" + employeId + "/timeline"), HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});
        assertThat(timeline.getBody()).anySatisfy(evt -> assertThat(evt.get("categorie")).isEqualTo("CONTRAT"));
    }

    @Test
    void evenement_manuel_refuseLaCategorieContrat() {
        String token = accessTokenFor(Role.ROLE_RH);
        HttpHeaders headers = authHeaders(token);
        var fixture = createFixtureUserWithEmploye(Role.ROLE_EMPLOYE);

        Map<String, Object> body = Map.of(
                "titre", "Tentative manuelle",
                "categorie", "CONTRAT",
                "dateEvenement", "2026-01-01T00:00:00"
        );
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/employes/" + fixture.employeId() + "/evenements"), HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}