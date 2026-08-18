package com.sirh.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirh.AbstractIntegrationTest;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.FichierDemandeUploadRequest;
import com.sirh.model.dto.FichierDownloadUrlDto;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.dto.FichierUploadUrlDto;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.model.enums.FichierStatut;
import com.sirh.model.enums.Role;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

// Aucun module Testcontainers Java dédié n'est publié pour Floci (vérifié sur
// Maven Central en implémentant ce lot) : GenericContainer suffit, même
// approche que POSTGRES/REDIS dans AbstractIntegrationTest.
class FichierControllerIT extends AbstractIntegrationTest {

  private static final String BUCKET_DOCUMENTS_RH = "sirh-documents-rh";
  private static final String BUCKET_UPLOADS = "sirh-uploads";

  static final GenericContainer<?> FLOCI;

  static {
    FLOCI =
        new GenericContainer<>(DockerImageName.parse("floci/floci:latest")).withExposedPorts(4566);
    FLOCI.start();
  }

  @DynamicPropertySource
  static void registerFlociProperties(DynamicPropertyRegistry registry) {
    registry.add("storage.s3.endpoint", FichierControllerIT::flociEndpoint);
    registry.add("storage.s3.public-endpoint", FichierControllerIT::flociEndpoint);
  }

  private static String flociEndpoint() {
    return "http://" + FLOCI.getHost() + ":" + FLOCI.getMappedPort(4566);
  }

  @BeforeAll
  static void creerLesBuckets() {
    S3Client s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(flociEndpoint()))
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_DOCUMENTS_RH).build());
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_UPLOADS).build());
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

  private void deposerSurUrlPresignee(String uploadUrl, byte[] contenu) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    ResponseEntity<Void> response =
        restTemplate.exchange(
            uploadUrl, HttpMethod.PUT, new HttpEntity<>(contenu, headers), Void.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
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
    byte[] contenu = "contenu de test".getBytes();
    FichierUploadUrlDto demande = demanderUpload(accessToken, FichierCategorie.JUSTIFICATIF);
    deposerSurUrlPresignee(demande.getUploadUrl(), contenu);
    FichierDto fichier = confirmer(accessToken, demande.getReference());
    assertThat(fichier.getStatut()).isEqualTo(FichierStatut.DISPONIBLE);
    return demande.getReference();
  }

  @Test
  void demandeUpload_puisConfirmation_puisTelechargement_fonctionneDeBoutEnBout() {
    String accessToken = accessTokenFor(Role.ROLE_RH);
    byte[] contenu = "contenu de test".getBytes();

    FichierUploadUrlDto demande = demanderUpload(accessToken, FichierCategorie.JUSTIFICATIF);
    deposerSurUrlPresignee(demande.getUploadUrl(), contenu);
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

    ResponseEntity<byte[]> downloadResponse =
        restTemplate.exchange(downloadUrl.getUrl(), HttpMethod.GET, null, byte[].class);
    assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(downloadResponse.getBody()).isEqualTo(contenu);
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
