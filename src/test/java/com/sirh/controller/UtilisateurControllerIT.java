package com.sirh.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.entity.Employe;
import com.sirh.model.enums.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

class UtilisateurControllerIT extends AbstractIntegrationTest {

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

  @Test
  void creation_pourUnEmployeExistant_permetLeLoginAvecLeMotDePasseDefini() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    Employe employe = createFixtureEmploye();
    String username = "compte-" + employe.getMatriculeInterne();

    Map<String, Object> body =
        Map.of(
            "employeId",
            employe.getId().toString(),
            "username",
            username,
            "password",
            "MonMotDePasse1!",
            "role",
            "ROLE_MANAGER");
    ResponseEntity<Map<String, Object>> created =
        restTemplate.exchange(
            baseUrl("/utilisateurs"),
            HttpMethod.POST,
            new HttpEntity<>(body, authHeaders(rhToken)),
            new ParameterizedTypeReference<>() {});
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) created.getBody().get("data");
    assertThat(data).doesNotContainKey("passwordHash");

    TokenResponse login = login(username, "MonMotDePasse1!");
    assertThat(login.getAccessToken()).isNotBlank();
  }

  @Test
  void creation_pourUnEmployeAyantDejaUnCompte_estRefusee() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    var fixture = createFixtureUserWithEmploye(Role.ROLE_MANAGER);

    Map<String, Object> body =
        Map.of(
            "employeId",
            fixture.employeId().toString(),
            "username",
            "second-compte-" + fixture.employeId(),
            "password",
            "AutreMotDePasse1!",
            "role",
            "ROLE_RH");
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl("/utilisateurs"),
            HttpMethod.POST,
            new HttpEntity<>(body, authHeaders(rhToken)),
            String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void resetPassword_permetLeLoginAvecLeNouveauMotDePasseUniquement() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    var fixture = createFixtureUserWithEmploye(Role.ROLE_MANAGER);
    UUID utilisateurReference = findUtilisateurReference(rhToken, fixture.username());

    ResponseEntity<Void> reset =
        restTemplate.exchange(
            baseUrl("/utilisateurs/" + utilisateurReference + "/reset-password"),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("newPassword", "NouveauMotDePasse1!"), authHeaders(rhToken)),
            Void.class);
    assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> oldPasswordLogin =
        restTemplate.postForEntity(
            baseUrl("/auth/login"),
            new LoginRequest(fixture.username(), FIXTURE_PASSWORD),
            String.class);
    assertThat(oldPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    TokenResponse newPasswordLogin = login(fixture.username(), "NouveauMotDePasse1!");
    assertThat(newPasswordLogin.getAccessToken()).isNotBlank();
  }

  @Test
  void desactivation_empecheLeLoginMemeAvecLeBonMotDePasse() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    var fixture = createFixtureUserWithEmploye(Role.ROLE_MANAGER);
    UUID utilisateurReference = findUtilisateurReference(rhToken, fixture.username());

    ResponseEntity<Void> update =
        restTemplate.exchange(
            baseUrl("/utilisateurs/" + utilisateurReference),
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("role", "ROLE_MANAGER", "compteActif", false), authHeaders(rhToken)),
            Void.class);
    assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> login =
        restTemplate.postForEntity(
            baseUrl("/auth/login"),
            new LoginRequest(fixture.username(), FIXTURE_PASSWORD),
            String.class);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void listing_masqueLeMatriculeCnssDeLEmployeImbrique() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    HttpHeaders headers = authHeaders(rhToken);

    Map<String, Object> employeBody =
        Map.of(
            "matriculeInterne", "MAT-" + UUID.randomUUID(),
            "matriculeCnss", "CNSS-1234-5678",
            "nom", "Dupont",
            "prenom", "Jean",
            "typeContrat", "CDI",
            "dateEmbauche", LocalDate.now().toString());
    ResponseEntity<Map<String, Object>> employeCreated =
        restTemplate.exchange(
            baseUrl("/employes"),
            HttpMethod.POST,
            new HttpEntity<>(employeBody, headers),
            new ParameterizedTypeReference<>() {});
    @SuppressWarnings("unchecked")
    String employeId =
        (String) ((Map<String, Object>) employeCreated.getBody().get("data")).get("id");

    String username = "cnss-" + UUID.randomUUID();
    Map<String, Object> utilisateurBody =
        Map.of(
            "employeId",
            employeId,
            "username",
            username,
            "password",
            "MonMotDePasse1!",
            "role",
            "ROLE_MANAGER");
    restTemplate.exchange(
        baseUrl("/utilisateurs"),
        HttpMethod.POST,
        new HttpEntity<>(utilisateurBody, headers),
        new ParameterizedTypeReference<Map<String, Object>>() {});

    ResponseEntity<Map<String, Object>> list =
        restTemplate.exchange(
            baseUrl("/utilisateurs"),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) list.getBody().get("content");
    @SuppressWarnings("unchecked")
    Map<String, Object> employeNested =
        (Map<String, Object>)
            content.stream()
                .filter(u -> username.equals(u.get("username")))
                .findFirst()
                .orElseThrow()
                .get("employe");
    assertThat(employeNested.get("matricule_cnss")).isEqualTo("***-**-5678");
  }

  private UUID findUtilisateurReference(String rhToken, String username) {
    ResponseEntity<Map<String, Object>> list =
        restTemplate.exchange(
            baseUrl("/utilisateurs"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(rhToken)),
            new ParameterizedTypeReference<>() {});
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) list.getBody().get("content");
    return content.stream()
        .filter(u -> username.equals(u.get("username")))
        .findFirst()
        .map(u -> UUID.fromString((String) u.get("reference")))
        .orElseThrow();
  }
}
