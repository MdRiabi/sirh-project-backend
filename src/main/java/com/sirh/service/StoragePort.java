package com.sirh.service;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

// Port métier du stockage objet — indépendant d'AWS. Un seul adapter concret
// (S3StorageAdapter, AWS SDK v2) l'implémente, contre Floci en dev ou un vrai
// AWS en prod (non exécuté), seule la config d'endpoint change.
public interface StoragePort {

  URI creerUrlUploadPresignee(String bucket, String cle, String typeMime, Duration dureeValidite);

  URI creerUrlTelechargementPresignee(String bucket, String cle, Duration dureeValidite);

  Optional<Long> tailleObjet(String bucket, String cle);

  void supprimerObjet(String bucket, String cle);
}
