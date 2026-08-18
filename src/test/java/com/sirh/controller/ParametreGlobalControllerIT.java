package com.sirh.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.enums.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Couvre le mécanisme de paramétrage global dynamique (Lot 5, décision d'architecture explicite) :
 * aucune règle métier variable codée en dur, lecture/écriture RH/SUPERUSER uniquement.
 */
class ParametreGlobalControllerIT extends AbstractIntegrationTest {

  private String accessTokenFor(Role role) {
    String username = createFixtureUser(role);
    return login(username, FIXTURE_PASSWORD).getAccessToken();
  }

  private HttpHeaders authHeaders(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private Map<?, ?> dataDe(ResponseEntity<Map> reponse) {
    return (Map<?, ?>) reponse.getBody().get("data");
  }

  @Test
  void get_tauxIncrementationConges_estSeedeA1point5() {
    String token = accessTokenFor(Role.ROLE_RH);
    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/parametres-globaux/TAUX_INCREMENTATION_CONGES_MENSUEL"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(dataDe(reponse).get("valeur")).isEqualTo("1.5");
  }

  @Test
  void update_parRH_persisteLaNouvelleValeur() {
    String token = accessTokenFor(Role.ROLE_RH);
    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/parametres-globaux/TAUX_INCREMENTATION_CONGES_MENSUEL"),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("valeur", "2.08"), authHeaders(token)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(dataDe(reponse).get("valeur")).isEqualTo("2.08");

    ResponseEntity<Map> relu =
        restTemplate.exchange(
            baseUrl("/parametres-globaux/TAUX_INCREMENTATION_CONGES_MENSUEL"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            Map.class);
    assertThat(dataDe(relu).get("valeur")).isEqualTo("2.08");
  }

  @Test
  void update_parManager_estRefuseAvec403() {
    String token = accessTokenFor(Role.ROLE_MANAGER);
    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/parametres-globaux/TAUX_INCREMENTATION_CONGES_MENSUEL"),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("valeur", "99"), authHeaders(token)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void get_cleInexistante_estRefuseAvec404() {
    String token = accessTokenFor(Role.ROLE_RH);
    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/parametres-globaux/CLE_INCONNUE"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void list_retourneAuMoinsLeParametreSeed() {
    String token = accessTokenFor(Role.ROLE_SUPERUSER);
    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/parametres-globaux"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) reponse.getBody().get("content")).isNotEmpty();
  }
}
