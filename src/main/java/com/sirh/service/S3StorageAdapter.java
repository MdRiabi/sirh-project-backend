package com.sirh.service;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

// Seule implémentation de StoragePort — sert contre Floci (dev, cf.
// S3Config) et contre un vrai AWS en changeant uniquement la config
// d'endpoint, jamais ce code.
@Component
public class S3StorageAdapter implements StoragePort {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  public S3StorageAdapter(S3Client s3Client, S3Presigner s3Presigner) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
  }

  @Override
  public URI creerUrlUploadPresignee(
      String bucket, String cle, String typeMime, Duration dureeValidite) {
    PutObjectRequest putRequest =
        PutObjectRequest.builder().bucket(bucket).key(cle).contentType(typeMime).build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(dureeValidite)
            .putObjectRequest(putRequest)
            .build();
    return URI.create(s3Presigner.presignPutObject(presignRequest).url().toString());
  }

  @Override
  public URI creerUrlTelechargementPresignee(String bucket, String cle, Duration dureeValidite) {
    GetObjectRequest getRequest = GetObjectRequest.builder().bucket(bucket).key(cle).build();
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(dureeValidite)
            .getObjectRequest(getRequest)
            .build();
    return URI.create(s3Presigner.presignGetObject(presignRequest).url().toString());
  }

  @Override
  public Optional<Long> tailleObjet(String bucket, String cle) {
    try {
      HeadObjectRequest headRequest = HeadObjectRequest.builder().bucket(bucket).key(cle).build();
      return Optional.of(s3Client.headObject(headRequest).contentLength());
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    }
  }

  @Override
  public void supprimerObjet(String bucket, String cle) {
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(cle).build());
  }
}
