package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import com.sirh.model.entity.Employe;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UtilisateurControllerIT extends AbstractIntegrationTest {

    private String accessTokenFor(Role role) {
        String username = createFixtureUser(role);
        return login(username, FIXTURE_PASSWORD).accessToken();
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

        Map<String, Object> body = Map.of(
                "employeId", employe.getId().toString(),
                "username", username,
                "password", "MonMotDePasse1!",
                "role", "ROLE_MANAGER"
        );
        ResponseEntity<Map<String, Object>> created = restTemplate.exchange(
                baseUrl("/utilisateurs"), HttpMethod.POST, new HttpEntity<>(body, authHeaders(rhToken)),
                new ParameterizedTypeReference<>() {});
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) created.getBody().get("data");
        assertThat(data).doesNotContainKey("passwordHash");

        TokenResponse login = login(username, "MonMotDePasse1!");
        assertThat(login.accessToken()).isNotBlank();
    }

    @Test
    void creation_pourUnEmployeAyantDejaUnCompte_estRefusee() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        var fixture = createFixtureUserWithEmploye(Role.ROLE_MANAGER);

        Map<String, Object> body = Map.of(
                "employeId", fixture.employeId().toString(),
                "username", "second-compte-" + fixture.employeId(),
                "password", "AutreMotDePasse1!",
                "role", "ROLE_RH"
        );
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/utilisateurs"), HttpMethod.POST, new HttpEntity<>(body, authHeaders(rhToken)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void resetPassword_permetLeLoginAvecLeNouveauMotDePasseUniquement() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        var fixture = createFixtureUserWithEmploye(Role.ROLE_MANAGER);
        UUID utilisateurReference = findUtilisateurReference(rhToken, fixture.username());

        ResponseEntity<Void> reset = restTemplate.exchange(
                baseUrl("/utilisateurs/" + utilisateurReference + "/reset-password"), HttpMethod.PUT,
                new HttpEntity<>(Map.of("newPassword", "NouveauMotDePasse1!"), authHeaders(rhToken)), Void.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> oldPasswordLogin = restTemplate.postForEntity(
                baseUrl("/auth/login"), new LoginRequest(fixture.username(), FIXTURE_PASSWORD), String.class);
        assertThat(oldPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        TokenResponse newPasswordLogin = login(fixture.username(), "NouveauMotDePasse1!");
        assertThat(newPasswordLogin.accessToken()).isNotBlank();
    }

    @Test
    void desactivation_empecheLeLoginMemeAvecLeBonMotDePasse() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        var fixture = createFixtureUserWithEmploye(Role.ROLE_MANAGER);
        UUID utilisateurReference = findUtilisateurReference(rhToken, fixture.username());

        ResponseEntity<Void> update = restTemplate.exchange(
                baseUrl("/utilisateurs/" + utilisateurReference), HttpMethod.PUT,
                new HttpEntity<>(Map.of("role", "ROLE_MANAGER", "compteActif", false), authHeaders(rhToken)), Void.class);
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> login = restTemplate.postForEntity(
                baseUrl("/auth/login"), new LoginRequest(fixture.username(), FIXTURE_PASSWORD), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID findUtilisateurReference(String rhToken, String username) {
        ResponseEntity<Map<String, Object>> list = restTemplate.exchange(
                baseUrl("/utilisateurs"), HttpMethod.GET, new HttpEntity<>(authHeaders(rhToken)),
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
