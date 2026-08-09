# SIRH — Système d'Information RH

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)
![Redis](https://img.shields.io/badge/Redis-7-DC382D)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED)

API REST du backend d'un SIRH (Système d'Information RH) complet : gestion du personnel, pointages biométriques, planning avec contrôle légal du repos, congés/autorisations, paie, gestion du matériel, notifications, renouvellement de contrats, évaluations annuelles, et un tableau de bord décisionnel avec export de rapports et piste d'audit immuable.

Ce dépôt est la moitié **backend** d'une architecture à deux dépôts — le frontend (React/TypeScript) vit dans [`sirh-project-frontend`](../sirh-project-frontend), cloné en dossier frère.

## Architecture full-stack

| Couche | Technologie |
|---|---|
| Frontend | React 18, TypeScript, Vite, TanStack Query, Zustand, Tailwind CSS, Recharts |
| Backend | Spring Boot 3.3, Java 21, Spring Security (JWT), Spring Data JPA |
| Base de données | PostgreSQL 16, migrations versionnées Flyway |
| Cache | Redis 7 (cache applicatif Dashboard + blacklist JWT) |
| Déploiement | Docker multi-étapes (Maven → JRE Alpine), Nginx en reverse-proxy devant le frontend |

11 lots fonctionnels livrés de bout en bout (backend + frontend + tests + documentation à chaque étape) — détail complet dans [`docs/BACKLOG.md`](docs/BACKLOG.md) et [`docs/LOTS/`](docs/LOTS/).

## Points forts techniques

- **RBAC strict à 5 rôles** (`EMPLOYE`, `MANAGER`, `RH`, `DIRECTEUR`, `SUPERUSER`), cloisonnement vérifié à chaque endpoint (`@PreAuthorize`) — ex. l'Employé n'a accès en lecture qu'à son propre planning, le Directeur ne peut jamais muter de donnée, et les logs d'audit sont réservés au SUPERUSER exclusivement (le RH, pourtant superset de tous les autres droits métier, en est explicitement exclu).
- **Piste d'audit immuable** (`audit_logs`) : écriture uniquement via un aspect Spring AOP (`@Audited`) sur les actions sensibles (clôture de paie, validation de congés, gestion du matériel, comptes utilisateurs...), lecture seule et consultable via un écran dédié filtrable/paginé.
- **Contrôles métier bloquants** : repos légal de 11h consécutives entre deux shifts, détection de doublon de pointage (fenêtre 5 min), clôture de paie irréversible avec workflow de déverrouillage exceptionnel tracé, import Excel avec rollback total au premier échec.
- **Masquage des données sensibles** : le numéro CNSS est masqué par défaut dans toutes les réponses API, dé-masquage réservé à une action explicite RH/SuperUser.
- **Génération de rapports asynchrone** : bascule automatique entre réponse synchrone (<5s) et tâche de fond suivie (`async_jobs`) au-delà, avec archivage mensuel automatisé à 12 mois.
- **Couverture de tests** : plus de 170 tests backend (intégration Testcontainers sur repositories/contrôleurs + unitaires ciblés sur la logique métier à risque — calcul d'assiduité, contrôle du repos légal, workflows de validation), 25 tests frontend (Vitest + Testing Library).
- **Conteneurisation prête pour la production** : build Docker multi-étapes optimisé (cache des dépendances Maven, image JRE Alpine, exécution non-root).

## Démarrage rapide

```bash
docker compose up --build -d
```

Guide complet (prérequis, variables d'environnement, dépannage) : **[`DEPLOIEMENT.md`](DEPLOIEMENT.md)**.

## Documentation

| Document | Contenu |
|---|---|
| [`docs/specs/cahier-technique.md`](docs/specs/cahier-technique.md) | Schéma SQL, contrat des endpoints, règles de sécurité — source de vérité technique |
| [`docs/specs/cahier-fonctionnel.md`](docs/specs/cahier-fonctionnel.md) | Besoin métier, règles de gestion, cas exceptionnels |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Vue C4, flux JWT, stratégie migrations/jobs async/stockage/audit |
| [`docs/BACKLOG.md`](docs/BACKLOG.md) | Découpage en lots, dépendances, Definition of Done |
| [`docs/LOTS/`](docs/LOTS/) | Journal de construction de chaque lot (étapes, choix techniques, cartographie API, validation) |
| [`DEPLOIEMENT.md`](DEPLOIEMENT.md) | Lancement Docker de bout en bout |

## Commandes de développement

```bash
mvn spring-boot:run     # démarrage local (nécessite docker compose up postgres redis)
mvn clean test           # tests unitaires + intégration
mvn package               # build du jar
```

## Licence

Projet privé — usage interne.
