package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.RefreshTokenRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.dto.UserSummary;
import com.sirh.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIntegrationTest {

    @Test
    void login_avecIdentifiantsValides_retourneDesTokens() {
        String username = createFixtureUser(Role.ROLE_MANAGER);

        TokenResponse body = login(username, FIXTURE_PASSWORD);

        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.user().role()).isEqualTo("ROLE_MANAGER");
    }

    @Test
    void login_avecMauvaisMotDePasse_retourne401() {
        String username = createFixtureUser(Role.ROLE_EMPLOYE);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl("/auth/login"), new LoginRequest(username, "MauvaisMotDePasse"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_apresCinqEchecs_verrouilleLeCompte() {
        String username = createFixtureUser(Role.ROLE_EMPLOYE);

        // AuthService.MAX_ECHECS_CONSECUTIFS = 5 : les 5 premiers échecs répondent
        // 401 (et arment le verrou côté serveur au 5e) ; seule la tentative
        // suivante est effectivement bloquée en 423.
        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity(baseUrl("/auth/login"),
                    new LoginRequest(username, "faux"), String.class);
        }
        ResponseEntity<String> sixthAttempt = restTemplate.postForEntity(baseUrl("/auth/login"),
                new LoginRequest(username, "faux"), String.class);
        assertThat(sixthAttempt.getStatusCode()).isEqualTo(HttpStatus.LOCKED);

        // même avec le bon mot de passe, le compte reste verrouillé
        ResponseEntity<String> withCorrectPassword = restTemplate.postForEntity(baseUrl("/auth/login"),
                new LoginRequest(username, FIXTURE_PASSWORD), String.class);
        assertThat(withCorrectPassword.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void refreshToken_avecTokenValide_retourneNouveauxTokens() {
        String username = createFixtureUser(Role.ROLE_RH);
        TokenResponse login = login(username, FIXTURE_PASSWORD);

        ResponseEntity<SingleObjectDto<TokenResponse>> refreshed = restTemplate.exchange(
                baseUrl("/auth/refresh-token"), HttpMethod.POST,
                new HttpEntity<>(new RefreshTokenRequest(login.refreshToken())),
                new ParameterizedTypeReference<SingleObjectDto<TokenResponse>>() {});

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().data().accessToken()).isNotBlank();
    }

    @Test
    void me_sansToken_retourne401() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl("/auth/me"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_avecTokenValide_retourneLeProfil() {
        String username = createFixtureUser(Role.ROLE_DIRECTEUR);
        TokenResponse login = login(username, FIXTURE_PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.accessToken());
        ResponseEntity<SingleObjectDto<UserSummary>> response = restTemplate.exchange(
                baseUrl("/auth/me"), HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<SingleObjectDto<UserSummary>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().username()).isEqualTo(username);
    }

    @Test
    void logout_revoqueImmediatementLeToken() {
        String username = createFixtureUser(Role.ROLE_SUPERUSER);
        TokenResponse login = login(username, FIXTURE_PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.accessToken());
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                baseUrl("/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(new RefreshTokenRequest(login.refreshToken()), headers), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> meAfterLogout = restTemplate.exchange(
                baseUrl("/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(meAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
