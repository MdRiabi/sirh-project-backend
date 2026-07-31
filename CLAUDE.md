# SIRH Backend — Instructions

API REST du SIRH (Système d'Information RH) : Spring Boot 3 / Java 21 / PostgreSQL / Redis. Ce repo est la moitié **backend** d'une architecture multi-repo — le frontend vit dans `sirh-project-frontend` (dépôt séparé, cloné en dossier frère sous le même parent). Aucun code frontend ici.

## Documents de référence (dans cet ordre de priorité)

1. `docs/specs/cahier-technique.md` — schéma SQL, endpoints, sécurité. **Source de vérité technique.**
2. `docs/specs/cahier-fonctionnel.md` — besoin métier, règles de gestion, cas exceptionnels.
3. `docs/ARCHITECTURE.md` — C4, flux JWT, stratégie migrations/jobs async/stockage/audit.
4. `docs/BACKLOG.md` — découpage en lots, dépendances, Definition of Done.
5. `docs/specs/audit-rapport.md` — historique des écarts trouvés/corrigés (référence).

**Règle de résolution de conflit** : en cas de doute entre deux documents, le cahier technique fait foi pour schéma/API/sécurité ; le cahier fonctionnel fait foi pour règle métier/UX. Une contradiction réelle non déjà tranchée dans `cahier-technique.md` §0 doit être signalée avant de coder — ne jamais réduire le périmètre fonctionnel pour éviter un effort technique.

## ⚠️ Règle de synchronisation multi-repo (à respecter systématiquement)

`docs/` est dupliqué à l'identique dans `sirh-project-backend` et `sirh-project-frontend` — ce ne sont **pas** des fichiers partagés au sens Git (pas de submodule/symlink), donc **aucune synchronisation automatique**. À chaque modification de `docs/BACKLOG.md`, `docs/ARCHITECTURE.md`, d'une spec dans `docs/specs/`, ou de ce fichier `CLAUDE.md` (partie non spécifique au backend), il faut **explicitement signaler à l'utilisateur** quelle modification doit être répercutée manuellement dans `sirh-project-frontend`. Idem en sens inverse si un changement est fait côté frontend en premier.

## Règles métier critiques transverses (à ne jamais oublier en implémentant)

- **5 rôles** : `EMPLOYE` (lecture seule de son propre planning, *aucun* autre accès), `MANAGER`, `RH`, `DIRECTEUR` (lecture seule dashboard), `SUPERUSER` (droits RH + accès exclusif aux logs — le RH n'a **jamais** accès aux logs).
- **`employes.id` est un UUID**, pas un entier — toute FK vers `employes` dans le code (entités JPA, DTOs, query params) doit être `UUID`, jamais `Long`.
- Import Excel pointages : **rollback total au 1er échec**, jamais d'insertion partielle, traitement asynchrone (table `async_jobs`).
- Doublon de pointage : fenêtre de 5 minutes, **même employé + même pointeuse**, seul le 1er pointage compte pour les heures travaillées.
- Planning : contrôle automatique et bloquant du repos légal de 11h consécutives entre deux shifts.
- Congés/Autorisations : saisie exclusivement Manager/RH (jamais l'employé), avis Manager capturé à la création, validation finale RH uniquement.
- Clôture de paie : **irréversible**, verrouille le mois en lecture seule ; déverrouillage exceptionnel = workflow formel avec motif légal.
- Audit (`audit_logs`) : immuable, écriture seule au niveau applicatif, lecture réservée à `SUPERUSER`.
- `matricule_cnss` (NSS) : donnée sensible, masquage partiel par défaut dans toute réponse API (DTO de sérialisation), sauf action explicite RH/SuperUser.

## Stack & structure

- Spring Boot 3.3.x, Java 21, Maven, PostgreSQL 16, Redis 7 (cache dashboard + blacklist JWT uniquement — pas de file de jobs, cf. `ARCHITECTURE.md` §7), Flyway pour les migrations (`src/main/resources/db/migration/`, jamais de modification d'un script déjà appliqué — toujours un nouveau `Vn+1`).
- `spring.jpa.hibernate.ddl-auto: validate` — Flyway possède le schéma, Hibernate ne fait que le valider. Ne jamais passer en `update`/`create`.
- Packages organisés par domaine métier : `auth`, `personnel`, `pointages`, `planning`, `assiduite`, `demandes`, `outillages`, `notifications`, `evaluations`, `renouvellement`, `calendrier`, `reporting`, `audit`, `fichiers` (cf. `ARCHITECTURE.md` §3).
- Aspect Spring AOP (`@Audited`) pour la journalisation des actions sensibles — voir `ARCHITECTURE.md` §9 pour la liste des actions à couvrir.

## Commandes

- `mvn clean test` — tests unitaires + intégration
- `mvn spring-boot:run` — démarrage local (nécessite Postgres/Redis actifs, `docker compose up postgres redis` depuis ce repo)
- `mvn package` — build du jar
- `docker compose up --build` — pile complète (Postgres + Redis + backend + frontend, suppose `sirh-project-frontend` cloné en frère)

## Conventions

- Langue : anglais pour le code (classes, méthodes, variables), français pour les libellés métier exposés (messages d'erreur, enums métier) si nécessaire.
- Tests : JUnit 5 + Testcontainers (Postgres) pour les repositories/controllers, tests unitaires ciblés sur la logique métier à risque (calcul d'assiduité, contrôle repos 11h, workflow de validation).
- MapStruct pour le mapping entité/DTO (à ajouter au `pom.xml` dès le premier lot qui en a besoin).
- Springdoc OpenAPI déjà en place — c'est le contrat consommé par `sirh-project-frontend` (génération de types TS via `openapi-typescript`), donc **tout changement de endpoint doit rester conforme à `docs/specs/cahier-technique.md`** ou déclencher une mise à jour de ce document en premier.

## Git

- Conventional Commits, scopés par module : `feat(pointages): ...`, `fix(planning): ...`.
- Branches courtes `feature/<lot>-<tache>`, PR avant merge sur `main`.
- **Jamais de commit sans validation explicite de l'utilisateur**, sauf demande explicite ponctuelle comme celle qui a initialisé ce repo.
- Aucune action destructive (`reset --hard`, force-push) ni **aucun `git push`** sans confirmation explicite — le remote `origin` est un vrai dépôt GitHub partagé.
