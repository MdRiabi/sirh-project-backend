package com.sirh.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirh.AbstractIntegrationTest;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class FichierControllerIT extends AbstractIntegrationTest {

  private String accessTokenFor(Role role) {
    String username = createFixtureUser(role);
    return login(username, FIXTURE_PASSWORD).getAccessToken();
  }

  private ResponseEntity<SingleObjectDto<FichierDto>> uploadTestFile(String accessToken) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file",
        new ByteArrayResource("contenu de test".getBytes()) {
          @Override
          public String getFilename() {
            return "justificatif.txt";
          }
        });
    body.add("categorie", "JUSTIFICATIF");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(accessToken);

    return restTemplate.exchange(
        baseUrl("/fichiers/upload"),
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        new ParameterizedTypeReference<SingleObjectDto<FichierDto>>() {});
  }

  @Test
  void upload_puisDownload_fonctionneDeBoutEnBout() {
    String accessToken = accessTokenFor(Role.ROLE_RH);

    ResponseEntity<SingleObjectDto<FichierDto>> uploadResponse = uploadTestFile(accessToken);
    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID fichierReference = uploadResponse.getBody().data().getReference();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<byte[]> downloadResponse =
        restTemplate.exchange(
            baseUrl("/fichiers/" + fichierReference + "/download"),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            byte[].class);

    assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(downloadResponse.getBody()).isEqualTo("contenu de test".getBytes());
  }

  @Test
  void delete_avecRoleEmploye_estRefuse() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    UUID fichierReference = uploadTestFile(rhToken).getBody().data().getReference();

    String employeToken = accessTokenFor(Role.ROLE_EMPLOYE);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(employeToken);
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl("/fichiers/" + fichierReference),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void delete_avecRoleRh_estAutorise() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    UUID fichierReference = uploadTestFile(rhToken).getBody().data().getReference();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(rhToken);
    ResponseEntity<Void> response =
        restTemplate.exchange(
            baseUrl("/fichiers/" + fichierReference),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }
}
