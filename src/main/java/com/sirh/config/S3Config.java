package com.sirh.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// Un seul jeu de credentials/région pour les deux beans ci-dessous, mais deux
// endpoints distincts : S3Client (appels serveur-à-serveur : HeadObject,
// DeleteObject) reste sur l'endpoint interne au réseau Docker
// (storage.s3.endpoint, ex. http://floci:4566) ; S3Presigner signe toujours
// avec l'endpoint public (storage.s3.public-endpoint, ex.
// http://localhost:4566), seul joignable depuis le navigateur qui consomme
// l'URL pré-signée directement (ARCHITECTURE.md §8).
@Configuration
public class S3Config {

  @Bean
  public S3Client s3Client(
      @Value("${storage.s3.endpoint}") String endpoint,
      @Value("${storage.s3.region}") String region,
      @Value("${storage.s3.access-key}") String accessKey,
      @Value("${storage.s3.secret-key}") String secretKey) {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  @Bean
  public S3Presigner s3Presigner(
      @Value("${storage.s3.public-endpoint}") String publicEndpoint,
      @Value("${storage.s3.region}") String region,
      @Value("${storage.s3.access-key}") String accessKey,
      @Value("${storage.s3.secret-key}") String secretKey) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(publicEndpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
