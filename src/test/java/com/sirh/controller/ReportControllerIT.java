package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Module 7 (CDF) : chemin synchrone (délai par défaut 5s largement suffisant
// pour un jeu de données de test) — le chemin asynchrone est couvert par
// ReportControllerAsyncIT (délai forcé à 1ms via reporting.delai-synchrone-ms).
class ReportControllerIT extends AbstractIntegrationTest {

    private String login(String username) {
        return login(username, FIXTURE_PASSWORD).accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void generer_manager_estRefuseAvec403() {
        String token = login(createFixtureUser(Role.ROLE_MANAGER));
        Map<String, Object> body = Map.of("type", "TURNOVER_ANNUEL", "format", "PDF", "annee", 2026);

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/reports/generer"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void generer_rh_turnoverPdf_renvoieLeFichierDirectement() {
        String token = login(createFixtureUser(Role.ROLE_RH));
        Map<String, Object> body = Map.of("type", "TURNOVER_ANNUEL", "format", "PDF", "annee", 2026);

        ResponseEntity<byte[]> reponse = restTemplate.exchange(baseUrl("/reports/generer"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), byte[].class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(reponse.getHeaders().getContentDisposition().getFilename()).contains("turnover-2026");
        assertThat(reponse.getBody()).isNotEmpty();
    }

    @Test
    void generer_rh_absenteismeExcel_renvoieLeFichierDirectement() {
        String token = login(createFixtureUser(Role.ROLE_RH));
        Map<String, Object> body = Map.of("type", "ABSENTEISME_ANNUEL", "format", "EXCEL", "annee", 2026);

        ResponseEntity<byte[]> reponse = restTemplate.exchange(baseUrl("/reports/generer"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), byte[].class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getHeaders().getContentDisposition().getFilename()).contains("absenteisme-2026");
        assertThat(reponse.getBody()).isNotEmpty();
    }

    @Test
    void tache_inexistante_renvoie404() {
        String token = login(createFixtureUser(Role.ROLE_RH));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/reports/tasks/" + UUID.randomUUID()), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
