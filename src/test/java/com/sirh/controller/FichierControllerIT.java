package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import com.sirh.model.dto.FichierDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

class FichierControllerIT extends AbstractIntegrationTest {

    private String accessTokenFor(Role role) {
        String username = createFixtureUser(role);
        TokenResponse login = restTemplate.postForEntity(
                baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class).getBody();
        return login.accessToken();
    }

    private ResponseEntity<FichierDto> uploadTestFile(String accessToken) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("contenu de test".getBytes()) {
            @Override
            public String getFilename() {
                return "justificatif.txt";
            }
        });
        body.add("categorie", "JUSTIFICATIF");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        return restTemplate.postForEntity(baseUrl("/fichiers/upload"),
                new HttpEntity<>(body, headers), FichierDto.class);
    }

    @Test
    void upload_puisDownload_fonctionneDeBoutEnBout() {
        String accessToken = accessTokenFor(Role.ROLE_RH);

        ResponseEntity<FichierDto> uploadResponse = uploadTestFile(accessToken);
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long fichierId = uploadResponse.getBody().id();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<byte[]> downloadResponse = restTemplate.exchange(
                baseUrl("/fichiers/" + fichierId + "/download"), HttpMethod.GET,
                new HttpEntity<>(headers), byte[].class);

        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getBody()).isEqualTo("contenu de test".getBytes());
    }

    @Test
    void delete_avecRoleEmploye_estRefuse() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Long fichierId = uploadTestFile(rhToken).getBody().id();

        String employeToken = accessTokenFor(Role.ROLE_EMPLOYE);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(employeToken);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/fichiers/" + fichierId), HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_avecRoleRh_estAutorise() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Long fichierId = uploadTestFile(rhToken).getBody().id();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(rhToken);
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl("/fichiers/" + fichierId), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
