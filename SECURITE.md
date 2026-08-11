# Sécurité — SIRH

Ce document centralise l'architecture de sécurité du SIRH (Système d'Information RH) et la manière dont elle est vérifiée en continu. Il est identique dans les deux dépôts (`sirh-project-backend` et `sirh-project-frontend`) — voir la règle de synchronisation multi-repo dans `CLAUDE.md`.

## Secure by design

### Authentification & sessions

- **JWT + refresh token** : access token de 15 minutes (claims `role`, `employeId`, `departementId`), refresh token de 7 jours. Détail du flux complet dans [`docs/ARCHITECTURE.md` §5](docs/ARCHITECTURE.md).
- **Révocation immédiate au logout** : le `jti` de l'access token est ajouté à une blacklist Redis avec un TTL égal au temps restant avant expiration naturelle.
- **Verrouillage de compte** : après 5 échecs de connexion consécutifs, le compte est verrouillé 15 minutes (`utilisateurs.echecs_login_consecutifs` / `verrouille_jusqu_a`), y compris si le bon mot de passe est ensuite saisi. Le compteur est réinitialisé à la connexion réussie.
  - *Note de fiabilité* : ce mécanisme a eu une régression réelle corrigée au Lot 12 — `AuthService.login()` était annoté `@Transactional` sans `noRollbackFor`, donc l'incrémentation du compteur d'échecs était systématiquement annulée dès que la méthode se terminait par une exception d'identifiants invalides, rendant le verrouillage totalement inopérant en pratique malgré un code apparemment correct. Couvert depuis par un test d'intégration dédié (`AuthControllerIT.login_apresCinqEchecs_verrouilleLeCompte`).

### Contrôle d'accès (RBAC)

5 rôles strictement cloisonnés, vérifiés à chaque endpoint (`@PreAuthorize`) :

| Rôle | Portée |
|---|---|
| `EMPLOYE` | Lecture seule de son propre planning uniquement — aucun autre accès. |
| `MANAGER` | Gestion de son équipe/département (saisie congés/autorisations, matériel, plannings). |
| `RH` | Superset des droits Manager + validation finale congés, clôture de paie, gestion des comptes utilisateurs. |
| `DIRECTEUR` | Lecture seule du tableau de bord décisionnel — ne peut jamais muter de donnée. |
| `SUPERUSER` | Droits RH + accès exclusif aux logs d'audit. Le RH, pourtant superset de tous les autres droits métier, en est explicitement exclu. |

### Piste d'audit immuable

`audit_logs` est alimenté uniquement par un aspect Spring AOP (`@Audited`) sur les actions sensibles (clôture de paie, validation de congés, gestion du matériel, comptes utilisateurs...). Aucun repository n'expose de méthode `update`/`delete` sur cette entité — écriture seule au niveau applicatif. Lecture réservée à `SUPERUSER` exclusivement. Détail en [`docs/ARCHITECTURE.md` §9](docs/ARCHITECTURE.md).

### Données sensibles

Le numéro CNSS (`employes.matricule_cnss`) est masqué par défaut dans toutes les réponses API (DTO de sérialisation) ; le démasquage complet est réservé à une action explicite RH/SuperUser. Détail en [`docs/ARCHITECTURE.md` §10](docs/ARCHITECTURE.md).

## Vérification continue (CI/CD)

Deux gates de sécurité automatisés tournent sur chaque push/PR vers `main`, dans les deux dépôts :

| Contrôle | Portée | Comportement | Résultats |
|---|---|---|---|
| **CodeQL** (SAST) | Code source (Java côté backend, JavaScript/TypeScript côté frontend) | **Non-bloquant** — alimente l'onglet Security sans faire échouer la CI, le temps qu'une baseline soit triée | Onglet *Security → Code scanning* du dépôt GitHub |
| **Trivy** (scan de conteneur) | Image Docker buildée en CI (dépendances applicatives + paquets OS de l'image de base) | **Bloquant** sur les vulnérabilités `CRITICAL`/`HIGH` — une CVE connue sur une dépendance ou une image de base a un correctif direct (bump de version), donc bloquer est actionnable immédiatement | Onglet *Security → Code scanning* + échec du job `docker-build` si non résolu |

Les deux workflows sont déclenchés en plus sur un cron hebdomadaire (CodeQL) pour capter les nouvelles CVE publiées sur du code qui n'a pas changé.

Badges de statut en tête du `README.md` de chaque dépôt.

## Divulgation responsable

Ce projet est un dépôt privé à usage interne — pas de programme de bug bounty public. Toute vulnérabilité constatée doit être signalée directement à l'équipe technique plutôt que divulguée publiquement.
