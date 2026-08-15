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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Force la bascule asynchrone (délai synchrone réduit à 1ms, cf.
// ReportGenerationService) pour couvrir tout le cycle de vie async_jobs :
// 202 Accepted -> polling GET /reports/tasks/{id} -> SUCCESS -> téléchargement.
// Contexte Spring séparé de ReportControllerIT (propriété différente), les
// containers Postgres/Redis restent partagés (champs statiques de la classe
// abstraite commune).
class ReportControllerAsyncIT extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void reportingProperties(DynamicPropertyRegistry registry) {
        registry.add("reporting.delai-synchrone-ms", () -> "1");
    }

    private String login(String username) {
        return restTemplate.postForEntity(baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class)
                .getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void generer_delaiDepasse_bascculeEnAsyncPuisTelechargeable() {
        String token = login(createFixtureUser(Role.ROLE_RH));
        Map<String, Object> body = Map.of("type", "TURNOVER_ANNUEL", "format", "PDF", "annee", 2026);

        ResponseEntity<Map> reponseGeneration = restTemplate.exchange(baseUrl("/reports/generer"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);

        assertThat(reponseGeneration.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        Number taskId = (Number) reponseGeneration.getBody().get("id");
        assertThat(reponseGeneration.getBody().get("type")).isEqualTo("REPORT");

        attendreSucces(taskId, token);

        ResponseEntity<byte[]> reponseTelechargement = restTemplate.exchange(baseUrl("/reports/tasks/" + taskId + "/download"),
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), byte[].class);

        assertThat(reponseTelechargement.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponseTelechargement.getBody()).isNotEmpty();
    }

    private void attendreSucces(Number taskId, String token) {
        for (int tentative = 0; tentative < 50; tentative++) {
            ResponseEntity<Map> reponseTache = restTemplate.exchange(baseUrl("/reports/tasks/" + taskId), HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)), Map.class);
            if ("SUCCESS".equals(reponseTache.getBody().get("statut"))) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("Le job de rapport n'est jamais passé à SUCCESS dans le délai imparti.");
    }
}
