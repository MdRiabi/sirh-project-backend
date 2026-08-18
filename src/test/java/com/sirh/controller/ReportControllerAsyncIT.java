package com.sirh.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.enums.Role;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
    return login(username, FIXTURE_PASSWORD).getAccessToken();
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

    ResponseEntity<Map> reponseGeneration =
        restTemplate.exchange(
            baseUrl("/reports/generer"),
            HttpMethod.POST,
            new HttpEntity<>(body, authHeaders(token)),
            Map.class);

    assertThat(reponseGeneration.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    Map<?, ?> jobData = (Map<?, ?>) reponseGeneration.getBody().get("data");
    String taskReference = (String) jobData.get("reference");
    assertThat(jobData.get("type")).isEqualTo("REPORT");

    attendreSucces(taskReference, token);

    ResponseEntity<byte[]> reponseTelechargement =
        restTemplate.exchange(
            baseUrl("/reports/tasks/" + taskReference + "/download"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            byte[].class);

    assertThat(reponseTelechargement.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(reponseTelechargement.getBody()).isNotEmpty();
  }

  private void attendreSucces(String taskReference, String token) {
    for (int tentative = 0; tentative < 50; tentative++) {
      ResponseEntity<Map> reponseTache =
          restTemplate.exchange(
              baseUrl("/reports/tasks/" + taskReference),
              HttpMethod.GET,
              new HttpEntity<>(authHeaders(token)),
              Map.class);
      Map<?, ?> tacheData = (Map<?, ?>) reponseTache.getBody().get("data");
      if ("SUCCESS".equals(tacheData.get("statut"))) {
        return;
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    throw new AssertionError(
        "Le job de rapport n'est jamais passé à SUCCESS dans le délai imparti.");
  }
}
