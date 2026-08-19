# Lot 12 — Fichiers Cloud

**Statut** : implémenté et testé côté backend — **non committé**, en attente de validation utilisateur. Frontend non traité (hors périmètre demandé).
**Dépendances** : [Lot 0 — Socle & Infrastructure](LOT_0_INFRASTRUCTURE.md) (module `fichiers` posé initialement sur stockage disque local).
**Suite** : aucune identifiée — module transversal déjà consommé par les Lots 1/5/9 (justificatifs, contrats) via son contrat d'API, inchangé dans sa forme fonctionnelle (seul le mécanisme de stockage change).

**⚠️ Addendum (2026-08-19)** : le stockage objet posé ci-dessous (§ "Choix techniques" points 1-9, `StoragePort`/`S3StorageAdapter`/`S3Config`) est sorti du périmètre SIRH et vit désormais dans un service séparé, `s3-file-system` (repo dédié, dossier frère de `sirh-project-backend`). Le contenu original de ce document reste comme référence historique du pattern (repris tel quel dans le nouveau repo) ; voir la section "Addendum — extraction en service séparé" en fin de fichier pour ce qui a changé côté SIRH.

Hors périmètre des 13 modules du CDF initial — ce lot n'existait pas dans `BACKLOG.md` avant le 2026-08-18.

## Objectif

Faire passer le module `fichiers` (posé au Lot 0 sur un volume disque local) à un stockage objet S3-compatible, pour couvrir deux usages : documents RH sensibles (contrats, bulletins de paie...) et uploads génériques (avatars, pièces jointes), avec un flux d'upload/download 100% en URLs pré-signées — aucun binaire ne transite plus par le backend.

## Choix techniques & Dépendances

1. **Contradiction documentaire signalée avant implémentation** : `cahier-technique.md` §1.2 et `ARCHITECTURE.md` §8 documentaient explicitement *"pas de S3/objet-storage, cohérent avec la contrainte on-premise"* — une décision motivée par une contrainte métier réelle (déploiement on-premise), pas un détail arbitraire. L'utilisateur a tranché explicitement pour un pivot vers S3/Floci malgré cette contrainte documentée ; les deux documents de référence ont été corrigés en conséquence plutôt que de coder en silence contre une contradiction non résolue (cf. règle de conflit `CLAUDE.md`).
2. **Architecture hexagonale minimale, pas de nouveau package** : un port `StoragePort` et un seul adapter concret `S3StorageAdapter` vivent dans `com.sirh.service` (package plat existant, comme `FichierService`/`ReportStorage`) plutôt que dans un nouveau package `port`/`adapter`/`infrastructure` — cohérent avec la convention du repo (packages organisés par couche technique).
3. **AWS SDK v2** (`software.amazon.awssdk:bom` + module `s3`) : le module Maven `s3-presigner` **n'existe pas** en tant qu'artefact séparé (vérifié directement sur Maven Central pendant l'implémentation, malgré des exemples en ligne suggérant le contraire) — les classes `S3Presigner`/`presigner.model.*` sont packagées à l'intérieur du jar `s3` lui-même.
4. **Floci** (`floci/floci:latest`, https://github.com/floci-io/floci, image Docker Hub vérifiée à 1,6M+ pulls) comme émulateur AWS local — choisi par l'utilisateur pour sa légèreté (~13 MiB idle, ~24ms de démarrage) face à LocalStack Community. **Aucun module Testcontainers Java dédié n'est publié** (`io.floci`/`org.floci`/`com.floci` : `numFound: 0` sur Maven Central) — `GenericContainer("floci/floci:latest")` est utilisé à la place, même approche que `REDIS` dans `AbstractIntegrationTest` qui n'a lui non plus jamais eu de module dédié.
5. **Deux endpoints S3 distincts en configuration** (`storage.s3.endpoint` vs `storage.s3.public-endpoint`) : `S3Client` (appels serveur-à-serveur : `HeadObject`/`DeleteObject`) reste sur l'endpoint interne au réseau Docker (`http://floci:4566`) ; `S3Presigner` signe toujours avec l'endpoint public (`http://localhost:4566` en dev) — sans cette distinction, une URL pré-signée générée avec `floci:4566` serait invalide une fois ouverte depuis le navigateur (qui ne résout pas ce nom d'hôte).
6. **Statut de fichier ajouté** (`EN_ATTENTE`/`DISPONIBLE`/`ECHEC`) : nécessaire car l'upload en 2 temps (URL pré-signée puis dépôt côté client) introduit une fenêtre où la ligne `fichiers` existe en base sans qu'aucun objet n'existe encore côté stockage — `GET /fichiers/{reference}/download-url` refuse (409 `FICHIER_INDISPONIBLE`) tant que la confirmation n'a pas eu lieu.
7. **Table `fichiers` étendue, jamais dupliquée** : `chemin_relatif` renommé `objet_cle` (même sémantique, déjà au format `categorie/uuid.ext`) plutôt que de créer une nouvelle table parallèle — migration Liquibase `renameColumn` + `addColumn` (`bucket`, `statut`), avec backfill des lignes existantes avant de poser la contrainte `NOT NULL` sur `bucket`.
8. **`ReportStorage` (Module 7) volontairement hors périmètre** : non mentionné par l'utilisateur, continue sur le volume disque local `storage.path` — les deux mécanismes de stockage (objet pour `fichiers`, disque local pour les rapports) cohabitent désormais, documenté explicitement dans `ARCHITECTURE.md` §8 pour éviter toute confusion future.
9. **Suppression toujours logique uniquement** (`DELETE /fichiers/{reference}`) : parité avec le comportement historique du module — l'objet stocké n'est pas physiquement effacé du bucket versionné.

## Étapes de réalisation

1. Audit du module `fichiers` existant (`Fichier`/`FichierService`/`FichierController`/`FichierRepository`) et des documents de référence (`cahier-technique.md` §1.2/§3.15, `ARCHITECTURE.md` §8) — découverte de la contradiction avec la demande utilisateur (cf. Choix techniques point 1), signalée et tranchée avant tout code.
2. Plan détaillé validé (arborescence, endpoints, schéma, docker-compose) via le mode Plan.
3. Vérification factuelle de l'image Docker Floci et des coordonnées Maven `testcontainers-floci` annoncées par l'utilisateur — la première existe (`floci/floci`), la seconde n'existe pas (aucun module Testcontainers Java publié).
4. `pom.xml` : BOM `software.amazon.awssdk:bom`, dépendance `s3` (le module `s3-presigner` séparé s'est révélé inexistant en cours de route et a été retiré).
5. `application.yml` : bloc `storage.s3.*` (endpoint interne/public, région, credentials, TTL des URLs pré-signées, noms des deux buckets).
6. `docker-compose.yml` : service `floci` (port 4566, `FLOCI_HOSTNAME=floci`, `FLOCI_STORAGE_MODE=hybrid`) ; service `backend` : nouvelles variables d'environnement `STORAGE_S3_*`, dépendance sur `floci`.
7. `FichierStatut` (nouvel enum) ; `FichierCategorie` étendu (`BULLETIN_PAIE`, `AVATAR`, `PIECE_JOINTE`).
8. `com.sirh.service.StoragePort` (port) et `com.sirh.service.S3StorageAdapter` (adapter, `HeadObject`/`DeleteObject`/URLs pré-signées PUT/GET) ; `com.sirh.config.S3Config` (beans `S3Client`/`S3Presigner`, deux endpoints distincts).
9. Migration Liquibase `004-fichiers-cloud.xml` (`renameColumn chemin_relatif → objet_cle`, `addColumn bucket/statut`, backfill, `NOT NULL`).
10. `Fichier` (entité), `FichierDto`, nouveaux DTOs `FichierDemandeUploadRequest`/`FichierUploadUrlDto`/`FichierDownloadUrlDto`, `FichierService` (réécrit : `demanderUpload`/`confirmerUpload`/`genererUrlTelechargement` remplacent `upload`/`loadAsResource`), `FichierController` (nouveaux endpoints).
11. `ErrorCode.FICHIER_INDISPONIBLE` (409) + message associé.
12. `FichierControllerIT` réécrit : conteneur Floci démarré manuellement (même pattern que Postgres/Redis dans `AbstractIntegrationTest`), buckets créés en `@BeforeAll`, flux complet demande-upload → PUT direct → confirmation → download-url → GET direct testé de bout en bout avec de vrais appels HTTP contre un vrai conteneur Floci (aucun mock du SDK AWS).
13. Validation : `mvn compile`/`spotless:apply`, `mvn verify -Dit.test=FichierControllerIT` (4/4 tests passés), `mvn test` (suite unitaire complète non-régressée), `mvn verify` complet (suite IT complète — `FichierControllerIT` passe ; des échecs préexistants et sans rapport ont été identifiés sur d'autres classes IT, cf. Procédure de validation).
14. Mise à jour de `cahier-technique.md` (§1.2, §3.15, §4.2), `ARCHITECTURE.md` (§8) et `BACKLOG.md` (Lot 0 corrigé, nouveau Lot 12) pour refléter le pivot.

## Fichiers impactés & Cartographie API

```mermaid
graph LR
    subgraph Backend
        FC[FichierController] --> FS[FichierService]
        FS --> FR[FichierRepository]
        FS --> SP[StoragePort]
        SP -.implémente.-> S3A[S3StorageAdapter]
        S3A --> S3Cfg[S3Config: S3Client/S3Presigner]
    end
    subgraph Stockage objet
        S3Cfg -->|HeadObject/DeleteObject, endpoint interne| Floci[(Floci :4566)]
        S3Cfg -->|URLs pré-signées, endpoint public| Floci
    end
    Client[Client / futur frontend] -->|1. POST /fichiers/demande-upload| FC
    Client -->|2. PUT direct sur uploadUrl| Floci
    Client -->|3. POST /fichiers/reference/confirmer| FC
    Client -->|4. GET /fichiers/reference/download-url| FC
    Client -->|5. GET direct sur url| Floci
```

**Backend modifié** : `Fichier.java`, `FichierDto.java`, `FichierService.java`, `FichierController.java`, `FichierCategorie.java`, `ErrorCode.java`, `messages.properties`, `application.yml`, `docker-compose.yml`, `pom.xml`.
**Backend nouveau** : `FichierStatut.java`, `StoragePort.java`, `S3StorageAdapter.java`, `S3Config.java`, `FichierDemandeUploadRequest.java`, `FichierUploadUrlDto.java`, `FichierDownloadUrlDto.java`, `db/changelog/changes/004-fichiers-cloud.xml`.
**Tests** : `FichierControllerIT.java` (réécrit intégralement).
**Frontend** : aucun fichier touché — 🔴 à faire dans un lot séparé, cf. `BACKLOG.md` Lot 12.

## Procédure de validation

- `mvn compile` puis `mvn validate -Pspotless-apply` puis recompilation — 0 violation de formatage.
- `mvn verify -Dit.test=FichierControllerIT` : **4/4 tests passés** contre de vrais conteneurs Postgres + Redis + Floci (Testcontainers), aucun mock du SDK AWS — flux demande-upload → PUT présigné → confirmation → download-url → GET présigné → suppression logique, plus le cas 409 (téléchargement avant confirmation).
- `mvn test` (suite unitaire complète, 54 tests hors IT) : **0 échec**.
- `mvn verify` (suite complète, tous les IT) : `FichierControllerIT` **passe** (isolé et dans la suite complète). **Échecs préexistants et sans rapport détectés** sur `AssiduiteControllerIT`, `AutorisationControllerIT`, `CalendrierControllerIT`, `IndisponibiliteControllerIT`, `OutillageControllerIT`, `ParametreGlobalControllerIT` (500 `SYSTEM_ERROR` sur des endpoints métier n'ayant aucun rapport avec le stockage de fichiers) — confirmé **indépendant de ce lot** en relançant `AutorisationControllerIT` isolément (sans `FichierControllerIT` dans la JVM Failsafe) : échec identique. Non corrigé ici (hors périmètre de la demande), signalé pour investigation séparée.

---

## Addendum (2026-08-19) — extraction du stockage objet en service séparé

**Contexte** : le stockage objet posé ci-dessus sort du périmètre SIRH — devient un projet technique réutilisable au-delà du SIRH, sans notion métier RH (arbitrage utilisateur explicite).

### Choix techniques

1. **Nouveau repo `s3-file-system`**, dossier frère de `sirh-project-backend`, reprend tel quel le pattern `StoragePort`/`S3StorageAdapter` (AWS SDK v2) et `S3Config` (deux endpoints) validés ci-dessus — seul le contenu du contrat REST change (générique, pas de `FichierCategorie`).
2. **Sans base de données** (arbitré avec l'utilisateur) : le service dérive tout de Floci/S3 (`HeadObject` pour content-type/taille/date, existence de l'objet pour le statut, id technique = clé objet UUID générée à la demande d'upload). Conséquence directe : le bucket doit être refourni par l'appelant à chaque opération (confirm/download/delete), pas seulement à la création — pas de table pour le retrouver seul.
3. **Authentification par clé d'API** (`X-Api-Key`, `ApiKeyFilter`, `OncePerRequestFilter`) plutôt que Spring Security complet : le service n'a pas d'utilisateur final, une seule vérification d'en-tête suffit — éviter une dépendance et une config de filter chain pour un seul header.
4. **Côté SIRH** : `spring-cloud-starter-openfeign` + BOM `spring-cloud-dependencies:2023.0.5` — dernier train Spring Cloud encore compatible Spring Boot 3.3.x (vérifié sur Maven Central et `github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions`, pas deviné). `com.sirh.config.FeignClientConfig` (le `RequestInterceptor` posant `X-Api-Key`) volontairement **non** annotée `@Configuration` : passée via `@FeignClient(configuration = ...)`, elle vivrait dans un package couvert par le `@ComponentScan` du SIRH et s'appliquerait alors par défaut à tout futur `@FeignClient`, pas seulement `FileStorageClient` (comportement documenté par Spring Cloud OpenFeign).
5. **Retrait complet du code S3 direct du SIRH** : `S3Config`/`StoragePort`/`S3StorageAdapter` supprimés, dépendance `software.amazon.awssdk` retirée du `pom.xml`, service `floci` retiré du `docker-compose.yml` du SIRH.
6. **`Fichier` (SIRH)** : `bucket`/`objet_cle` droppés, remplacés par `fichier_externe_id` (id technique du nouveau service) — nouveau changeset Liquibase `006-fichiers-externe.xml` (jamais de modification de `004-fichiers-cloud.xml` déjà appliqué). En a profité pour nettoyer sous précondition la colonne `chemin_relatif` ré-ajoutée par erreur par `005-corrections-schema.xml` sur certains environnements (orpheline depuis le pivot 004, jamais utilisée par le code depuis).
7. **Suppression physique toujours non déclenchée par le flux métier SIRH** : `FileStorageClient` expose bien une 4e opération de suppression (le contrat REST complet est consommé), mais `FichierService.deleteFichierByReference` reste soft-delete uniquement — continuité du choix déjà présent avant l'extraction (bucket documents RH versionné), pas une régression.
8. **Tests SIRH découplés du stockage réel** : `FichierControllerIT` mocke désormais `FileStorageClient` (`@MockBean`) au lieu de démarrer un conteneur Floci — teste la logique propre au SIRH (RBAC, statuts, 409 avant confirmation), plus besoin d'AWS SDK/Testcontainers Floci côté SIRH. Le flux de bout en bout contre un vrai conteneur Floci est désormais testé côté `s3-file-system/FileControllerIT`.

### Étapes de réalisation

1. Nouveau repo `s3-file-system` scaffoldé (Spring Boot 3.3.1/Java 21, alignés SIRH) : `S3Config`/`StoragePort`/`S3StorageAdapter` repris du SIRH, `FileService` (sans état), `FileController` (4 opérations), `ApiKeyFilter`, `docker-compose.yml` (app + Floci), `Dockerfile`, `FileControllerIT` (conteneur Floci réel).
2. Vérification factuelle sur Maven Central de la compatibilité Spring Cloud OpenFeign / Spring Boot 3.3.1 (`spring-cloud-dependencies:2023.0.5`, train "Leyton").
3. SIRH : BOM + dépendance Feign ajoutés au `pom.xml`, `@EnableFeignClients` sur `SIRHApplication`, `com.sirh.client.FileStorageClient` + DTOs miroir, `com.sirh.config.FeignClientConfig`.
4. Retrait de `S3Config`/`StoragePort`/`S3StorageAdapter`/dépendance AWS SDK/service `floci` du SIRH.
5. Migration `006-fichiers-externe.xml` (drop `bucket`/`objet_cle`/`chemin_relatif`, add `fichier_externe_id`), `Fichier.java` mis à jour.
6. `FichierService` réécrit : les 3 appels `StoragePort` remplacés par 3 appels `FileStorageClient` (le 4e, delete, n'est pas invoqué par ce flux — cf. Choix techniques point 7).
7. `FichierControllerIT` (SIRH) réécrit avec `@MockBean FileStorageClient`.
8. `docker-compose.yml`/`application.yml` du SIRH mis à jour (`FILESTORAGE_API_URL`/`FILESTORAGE_API_KEY` au lieu de `STORAGE_S3_*`/service `floci`).
9. `cahier-technique.md` (§1.2/§3.15/§4.2), `ARCHITECTURE.md` (§8), `BACKLOG.md` (Lot 12) mis à jour.

### Fichiers impactés & Cartographie API

```mermaid
graph LR
    subgraph "sirh-project-backend"
        FC[FichierController] --> FS[FichierService]
        FS --> Repo[FichierRepository]
        FS --> Client[FileStorageClient - Feign]
    end
    subgraph "s3-file-system (repo séparé)"
        FileC[FileController] --> FileSvc[FileService]
        FileSvc --> SP[StoragePort]
        SP -.implémente.-> S3A[S3StorageAdapter]
        S3A --> S3Cfg[S3Config]
        S3Cfg -->|HeadObject/DeleteObject, endpoint interne| Floci[(Floci :4566)]
        S3Cfg -->|URLs pré-signées, endpoint public| Floci
    end
    Client -->|"X-Api-Key + 4 opérations"| FileC
    ClientApp[Client / futur frontend] -->|1. POST /fichiers/demande-upload| FC
    ClientApp -->|2. PUT direct sur uploadUrl| Floci
    ClientApp -->|3. POST /fichiers/reference/confirmer| FC
    ClientApp -->|4. GET /fichiers/reference/download-url| FC
    ClientApp -->|5. GET direct sur url| Floci
```

**SIRH modifié** : `Fichier.java`, `FichierService.java`, `pom.xml`, `application.yml`, `docker-compose.yml`, `SIRHApplication.java`, `masterchangelog.xml`, `FichierControllerIT.java`.
**SIRH supprimé** : `S3Config.java`, `StoragePort.java`, `S3StorageAdapter.java`.
**SIRH nouveau** : `com.sirh.client.FileStorageClient` + 4 DTOs, `com.sirh.config.FeignClientConfig`, `db/changelog/changes/006-fichiers-externe.xml`.
**Nouveau repo `s3-file-system`** : projet complet (cf. son propre rapport de lot).

### Procédure de validation

- `s3-file-system` : `mvn clean verify` — **4/4 tests** de `FileControllerIT` passés contre un vrai conteneur Floci (upload-request → PUT présigné → confirmation → download-url → GET présigné → suppression → re-confirmation 404, plus 401 sans/avec mauvaise clé d'API).
- `sirh-project-backend` : `mvn -q -B test` (suite unitaire) — **0 échec**. `mvn -q -B clean verify -Dit.test=FichierControllerIT` — **4/4 tests** passés (`FileStorageClient` mocké). `mvn -q -B clean verify` (suite complète, 30 classes de test) — **0 échec, 0 erreur** sur l'ensemble (y compris les classes citées comme ayant des échecs préexistants sans rapport dans la version précédente de ce document — non reproduits lors de cette exécution).

### Signalement multi-repo (règle `CLAUDE.md`)

`ARCHITECTURE.md` §8, `cahier-technique.md` §1.2/§3.15/§4.2 et `BACKLOG.md` (Lot 12) ont été modifiés dans ce repo — **à répercuter manuellement dans `sirh-project-frontend`** (mêmes fichiers, même contenu `docs/`). Aucun code frontend n'a été touché ici (hors périmètre demandé), mais le contrat d'upload/download reste inchangé depuis le 2026-08-18 — l'extraction en service séparé n'introduit pas de nouveau changement de contrat HTTP côté SIRH.
