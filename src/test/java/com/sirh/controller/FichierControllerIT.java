package com.sirh.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.sirh.AbstractIntegrationTest;
import com.sirh.client.FileStorageClient;
import com.sirh.client.FileStorageDownloadUrl;
import com.sirh.client.FileStorageMetadata;
import com.sirh.client.FileStorageUploadRequest;
import com.sirh.client.FileStorageUploadUrl;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.FichierDemandeUploadRequest;
import com.sirh.model.dto.FichierDownloadUrlDto;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.dto.FichierUploadUrlDto;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.model.enums.FichierStatut;
import com.sirh.model.enums.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

// FileStorageClient (Feign, vers le service séparé s3-file-system) est mocké
// : ce test couvre la logique propre au SIRH (RBAC, statuts, 409 avant
// confirmation), pas le stockage objet réel — désormais testé côté
// s3-file-system/FileControllerIT contre un vrai conteneur Floci.
class FichierControllerIT extends AbstractIntegrationTest {

  private static final String FAKE_UPLOAD_URL = "http://fake-storage.test/upload";
  private static final String FAKE_DOWNLOAD_URL = "http://fake-storage.test/download";

  @MockBean private FileStorageClient fileStorageClient;

  @BeforeEach
  void stubFileStorageClient() {
    when(fileStorageClient.demanderUpload(any(FileStorageUploadRequest.class)))
        .thenAnswer(
            invocation -> {
              String id = UUID.randomUUID().toString();
              FileStorageUploadRequest request = invocation.getArgument(0);
              return new FileStorageUploadUrl(
                  id, request.bucket(), id, FAKE_UPLOAD_URL, Instant.now().plusSeconds(900));
            });
    when(fileStorageClient.confirmer(anyString(), anyString()))
        .thenReturn(
            new FileStorageMetadata(
                "peu-importe", "peu-importe", "text/plain", 15L, Instant.now(), "CONFIRMED"));
    when(fileStorageClient.genererUrlTelechargement(anyString(), anyString()))
        .thenReturn(
            new FileStorageDownloadUrl(
                "peu-importe", FAKE_DOWNLOAD_URL, Instant.now().plusSeconds(300)));
  }

  private String accessTokenFor(Role role) {
    String username = createFixtureUser(role);
    return login(username, FIXTURE_PASSWORD).getAccessToken();
  }

  private FichierUploadUrlDto demanderUpload(String accessToken, FichierCategorie categorie) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    var request =
        new FichierDemandeUploadRequest(
            "justificatif.txt", "text/plain", 15L, categorie, null, null);
    return restTemplate
        .exchange(
            baseUrl("/fichiers/demande-upload"),
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<SingleObjectDto<FichierUploadUrlDto>>() {})
        .getBody()
        .data();
  }

  private FichierDto confirmer(String accessToken, UUID reference) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return restTemplate
        .exchange(
            baseUrl("/fichiers/" + reference + "/confirmer"),
            HttpMethod.POST,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<SingleObjectDto<FichierDto>>() {})
        .getBody()
        .data();
  }

  private UUID uploaderEtConfirmerFichierDeTest(String accessToken) {
    FichierUploadUrlDto demande = demanderUpload(accessToken, FichierCategorie.JUSTIFICATIF);
    FichierDto fichier = confirmer(accessToken, demande.getReference());
    assertThat(fichier.getStatut()).isEqualTo(FichierStatut.DISPONIBLE);
    return demande.getReference();
  }

  @Test
  void demandeUpload_puisConfirmation_puisTelechargement_fonctionneDeBoutEnBout() {
    String accessToken = accessTokenFor(Role.ROLE_RH);

    FichierUploadUrlDto demande = demanderUpload(accessToken, FichierCategorie.JUSTIFICATIF);
    assertThat(demande.getUploadUrl()).isEqualTo(FAKE_UPLOAD_URL);

    FichierDto fichier = confirmer(accessToken, demande.getReference());
    assertThat(fichier.getStatut()).isEqualTo(FichierStatut.DISPONIBLE);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    FichierDownloadUrlDto downloadUrl =
        restTemplate
            .exchange(
                baseUrl("/fichiers/" + demande.getReference() + "/download-url"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<SingleObjectDto<FichierDownloadUrlDto>>() {})
            .getBody()
            .data();
    assertThat(downloadUrl.getUrl()).isEqualTo(FAKE_DOWNLOAD_URL);
  }

  @Test
  void telechargement_avantConfirmation_estRefuse() {
    String accessToken = accessTokenFor(Role.ROLE_RH);
    FichierUploadUrlDto demande = demanderUpload(accessToken, FichierCategorie.JUSTIFICATIF);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl("/fichiers/" + demande.getReference() + "/download-url"),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void delete_avecRoleEmploye_estRefuse() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    UUID fichierReference = uploaderEtConfirmerFichierDeTest(rhToken);

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
    UUID fichierReference = uploaderEtConfirmerFichierDeTest(rhToken);

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
