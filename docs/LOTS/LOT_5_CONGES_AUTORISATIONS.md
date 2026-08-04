# Lot 5 — Congés & Autorisations *(Modules 8 et 13 du CDF)*

**Statut** : implémenté et validé (backend), en attente de commit/merge.
**Dépendances** : Lot 1 (référence `employes.id`), Lot 2 (justification d'anomalies de pointage), Lot 3 (contrôle bloquant de chevauchement congé/planning, package `com.sirh.planning`), Lot 4 (jours fériés, `Indisponibilite`/`HistoriqueSolde` posées en avance de phase).

## Objectif

Workflow de saisie hiérarchique (Manager avec avis → validation RH, ou saisie RH d'urgence) pour les autorisations d'absence courte (Module 13, justification automatique des anomalies de pointage) et les indisponibilités/congés (Module 8, déduction automatique de solde en jours ouvrés) ; règles de délai 72h et de quota mensuel 4h sur les autorisations ; gestion du solde insuffisant/forçage RH sur les congés ; incrémentation mensuelle automatique du solde via un taux paramétrable dynamiquement (aucune valeur métier codée en dur) ; mise à jour du planning comme source de vérité unique à la validation d'une demande.

## Étapes de réalisation

1. **Chaînage Git** — `feature/lot-5-conges-autorisations` créée à partir de `feature/lot-4-calendrier`.
2. **Design first (règle n°1 du DoD)** — lecture de `cahier-fonctionnel.md` (Modules 8/13), `cahier-technique.md` (§3.10-3.12, §4.6, §5) et `BACKLOG.md`, proposition d'architecture complète soumise à l'utilisateur avant tout code. Cinq ajustements actés par l'utilisateur avant le "go" : quota 4h cumulant `EN_ATTENTE_RH`+`VALIDE`, déduction de solde en **jours ouvrés** (pas calendaires — interfaçage explicitement demandé avec `jours_feries` du Lot 4), mise à jour du **planning** à la validation (créneau `CONGE`/`ABSENCE`, pas seulement le contrôle bloquant du Lot 3), gap `PUT /indisponibilites/{id}/refuser` à combler, et table de paramétrage nommée `parametres_globaux` (pas `parametres_rh`, pour rester réutilisable par d'autres lots).
3. **Migration V4 — `parametres_globaux`** — table générique `cle`/`valeur`/`type`, seedée avec `TAUX_INCREMENTATION_CONGES_MENSUEL = 1.5`. Nouveau package `com.sirh.parametres` (entity/repository/service/dto/controller), volontairement hors de `demandes` pour rester réutilisable (ex. seuils M+5/M+11 du Lot 9 à venir).
4. **Migration V5 — shift système** — `plannings.shift_id` est `NOT NULL` mais les créneaux `CONGE`/`ABSENCE` générés automatiquement n'ont pas de créneau horaire réel : un shift "système" (`type_rotation = 'SYSTEME'`) sert de support générique unique, plutôt que de rendre la colonne nullable (éviterait de casser la contrainte existante pour un cas très minoritaire).
5. **`PlanningService.ecraserCreneaux`** — nouvelle méthode partagée (Lot 3 existant) : supprime toute affectation chevauchant strictement la fenêtre puis insère un créneau `CONGE`/`ABSENCE`. Statuts `PlanningStatut.CONGE`/`ABSENCE` ajoutés, exclus des heures théoriques du rapport delta au même titre que `REPOS`. Committé séparément avant tout code de `demandes` puisqu'il s'agit d'une extension du domaine `planning`, pas de `demandes`.
6. **`Autorisation`** — entité/repository/service/dto/controller créés de zéro (contrairement à `Indisponibilite`/`HistoriqueSolde`, posées en avance pendant les Lots 3/4). `PointageBrutService.justifierAnomaliesDuJour` ajoutée côté `pointages` (appariement employé+jour, la FK `pointages_bruts.autorisation_id` reste unidirectionnelle) pour ne pas dupliquer la logique de mutation de pointage hors de son module propriétaire.
7. **`Indisponibilite`/`HistoriqueSolde`** — CRUD/workflow complétés sur les entités déjà posées. Déduction en jours ouvrés : `IndisponibiliteService` injecte `JourFerieRepository` (package `calendrier`) pour exclure week-ends et jours `type = FERIE` (jamais `FERMETURE_COLLECTIVE`, cohérent avec la règle de recrédit du Lot 4) du décompte.
8. **Gaps d'endpoints comblés et documentés** — même pattern que `/plannings/delta` (Lot 3) et `/calendrier/jours-feries` (Lot 4) : `cahier-technique.md` §4.6 ne listait aucun `GET` de listing ni `PUT /indisponibilites/{id}/refuser`. Contrat conçu à partir du besoin des écrans "liste à valider"/"historique" (`BACKLOG.md`), implémenté, puis documenté dans `cahier-technique.md` §4.6/§4.11.
9. **Bug trouvé en vérification manuelle et corrigé** — le workflow d'urgence RH (statut `VALIDE` dès la création) ne déclenchait aucun des effets d'une validation (déduction de solde, justification de pointage, mise à jour du planning), ces effets n'existant que dans `valider()`. Trouvé en testant le scénario RH-direct via `docker compose` + `curl` (Testcontainers indisponible sur cette machine, cf. point 11) : une indisponibilité `CONGE_PAYE` créée directement par le RH restait `VALIDE` sans jamais déduire le solde. Corrigé par extraction d'une méthode partagée `appliquerImpactValidation()` appelée à la fois par `creer()` (chemin RH) et `valider()` — commit dédié, tests d'intégration mis à jour pour refléter le comportement corrigé (deux tests reposaient implicitement sur l'ancien bug : ils appelaient `valider()` après une création RH déjà `VALIDE`, ce qui renvoie désormais `409` puisque la demande n'est plus `EN_ATTENTE_RH`).
10. **Choix délibéré : quota 4h non bloquant à la création RH d'urgence** — contrairement à la déduction de solde, le contrôle de quota mensuel (bloquant, avec choix `DEDUCTION_SALAIRE`/`RECUPERATION` obligatoire) reste réservé à la validation RH explicite en deux temps. Bloquer une autorisation urgente sur un total mensuel sans rapport avec l'absence à traiter irait à l'encontre de l'objectif même du workflow d'urgence (traiter vite un cas réellement urgent) — asymétrie documentée en commentaire de code.
11. **Validation** — Testcontainers toujours indisponible sur cette machine (npipe Docker Desktop, limitation documentée depuis le Lot 0 ; une tentative avec `DOCKER_HOST` explicite sur le pipe `dockerDesktopLinuxEngine` échoue avec la même erreur `BadRequestException` malgré `docker info`/`docker compose` fonctionnels en CLI). Validation via `docker compose up --build` + `curl`/`docker exec psql`, couvrant tous les scénarios métier (détail §Procédure de validation). Le job mensuel (`IncrementationSoldeScheduler`) est testé unitairement avec Mockito (pas de dépendance HTTP/DB, donc pas besoin de Testcontainers).

## Choix techniques & Dépendances

| Choix | Raison |
|---|---|
| `parametres_globaux` générique (pas `parametres_rh`) | Décision explicite de l'utilisateur : réutilisable par d'autres lots (seuils, taux) sans nouvelle migration à chaque paramètre. Colonne `type` pour supporter au-delà de `DECIMAL` sans changement de schéma. |
| Taux relu à chaque exécution du job, jamais mis en cache | Un changement de taux par le RH doit s'appliquer dès le prochain passage du cron sans redéploiement — cohérent avec `CLAUDE.md` (Redis borné au dashboard/JWT, jamais réutilisé pour ce genre de configuration). |
| Shift "système" (migration V5) plutôt que `shift_id` nullable | Évite d'affaiblir une contrainte `NOT NULL` existante pour un cas minoritaire (créneaux générés automatiquement, sans créneau horaire réel). |
| Déduction de solde en jours ouvrés, interfacée avec `jours_feries` | Décision explicite de l'utilisateur (refus du calcul en jours calendaires) : le Lot 4 existe déjà, doit être réutilisé plutôt que dupliqué. |
| Recrédit du Lot 4 limité à `CONGE_PAYE`/`FERIE` réutilisé tel quel pour l'exclusion des jours ouvrés | Cohérence : un jour férié qui recrédite un congé payé au Lot 4 ne doit pas non plus être compté comme un jour consommé au Lot 5 — même filtre `type = FERIE` (jamais `FERMETURE_COLLECTIVE`) des deux côtés. |
| Mise à jour planning en complément du contrôle Lot 3, jamais en remplacement | Décision explicite de l'utilisateur : le contrôle bloquant (refus d'affectation sur congé validé) reste utile même avec un planning à jour — deux garde-fous indépendants plutôt qu'un seul point de défaillance. |
| Justification des pointages par appariement employé+jour (pas de nouvelle colonne) | La FK `pointages_bruts.autorisation_id` posée au Lot 2 est unidirectionnelle par conception ; réutiliser l'appariement plutôt que d'ajouter un lien inverse évite un remaniement de schéma pour ce lot. |
| `PointageBrutService.justifierAnomaliesDuJour` dans `pointages`, pas `demandes` | Le module `pointages` reste le seul point de mutation des pointages bruts (cohérent avec son javadoc "point d'entrée unique") ; `demandes` appelle ce service plutôt que d'écrire directement dans `pointages_bruts`. |
| Quota 4h bloquant uniquement à la validation en deux temps, jamais à la création RH d'urgence | Choix délibéré (pas explicitement tranché par l'utilisateur) : bloquer une urgence sur un total mensuel sans rapport avec l'absence à traiter contredirait l'objectif du workflow d'urgence. Documenté en commentaire de code et dans ce lot pour traçabilité — à reconfirmer si le comportement attendu diffère. |
| `AutorisationService`/`IndisponibiliteService` : `appliquerImpactValidation()` partagée entre `creer()` (RH) et `valider()` | Bug trouvé en vérification manuelle (workflow d'urgence sans effets) : une seule méthode d'impact garantit que les deux chemins vers `VALIDE` produisent le même résultat, plutôt que deux implémentations à maintenir en synchronisation. |

## Fichiers impactés & Cartographie API

```mermaid
flowchart LR
    subgraph BE_Param["Backend — parametres/ (nouveau package)"]
        ParamController["controller/ParametreGlobalController.java"]
        ParamService["service/ParametreGlobalService.java"]
        ParamRepo["repository/ParametreGlobalRepository.java"]
    end

    subgraph BE_Demandes["Backend — demandes/"]
        AutController["controller/AutorisationController.java 🆕"]
        AutService["service/AutorisationService.java 🆕<br/>(72h, quota 4h, appliquerImpactValidation)"]
        IndispoController["controller/IndisponibiliteController.java 🆕"]
        IndispoService["service/IndisponibiliteService.java 🆕<br/>(jours ouvrés, forçage, appliquerImpactValidation)"]
        IncrementScheduler["service/IncrementationSoldeScheduler.java 🆕<br/>(@Scheduled mensuel)"]
    end

    subgraph BE_Planning["Backend — planning/"]
        PlanningService["service/PlanningService.java<br/>(+ ecraserCreneaux 🔧)"]
        PlanningStatut["entity/PlanningStatut.java<br/>(+ CONGE, ABSENCE 🔧)"]
    end

    subgraph BE_Pointages["Backend — pointages/"]
        PointageBrutService["service/PointageBrutService.java<br/>(+ justifierAnomaliesDuJour 🔧)"]
    end

    subgraph BE_Calendrier["Backend — calendrier/ (lecture)"]
        JourFerieRepo["repository/JourFerieRepository.java"]
    end

    subgraph DB["PostgreSQL"]
        T_param[(parametres_globaux 🆕)]
        T_aut[(autorisations)]
        T_indispo[(indisponibilites)]
        T_soldes[(historique_soldes)]
        T_plannings[(plannings)]
        T_pointages[(pointages_bruts)]
        T_feries[(jours_feries — lecture)]
        T_employes[(employes — solde_conges_jours)]
    end

    ParamController --> ParamService --> ParamRepo --> T_param
    IncrementScheduler -->|"lit le taux à chaque exécution"| ParamService
    IncrementScheduler -->|"INCREMENT_MENSUEL"| T_soldes
    IncrementScheduler --> T_employes

    AutController -->|"POST /autorisations"| AutService
    AutController -->|"PUT .../valider, .../refuser, GET"| AutService
    AutService --> T_aut
    AutService -->|"justifierAnomaliesDuJour"| PointageBrutService --> T_pointages
    AutService -->|"ecraserCreneaux (ABSENCE)"| PlanningService --> T_plannings

    IndispoController -->|"POST, PUT valider/refuser, GET, GET solde"| IndispoService
    IndispoService --> T_indispo
    IndispoService -->|"jours ouvrés"| JourFerieRepo --> T_feries
    IndispoService -->|"VALIDATION_CONGE / FORCAGE_RH"| T_soldes
    IndispoService --> T_employes
    IndispoService -->|"ecraserCreneaux (CONGE/ABSENCE)"| PlanningService
```

### Endpoints exposés (Lot 5)

| Méthode | Endpoint | Rôles | Contrôleur |
|---|---|---|---|
| GET/PUT | `/parametres-globaux`, `/parametres-globaux/{cle}` | RH, SUPERUSER | `ParametreGlobalController` |
| POST | `/autorisations` | MANAGER, RH, SUPERUSER | `AutorisationController` |
| PUT | `/autorisations/{id}/valider`, `/refuser` | RH, SUPERUSER | `AutorisationController` |
| GET | `/autorisations` | MANAGER (son département), RH, SUPERUSER | `AutorisationController` |
| POST | `/indisponibilites` | MANAGER, RH, SUPERUSER | `IndisponibiliteController` |
| PUT | `/indisponibilites/{id}/valider`, `/refuser` | RH, SUPERUSER | `IndisponibiliteController` |
| GET | `/indisponibilites`, `/indisponibilites/solde/{employeId}` | MANAGER (son département), RH, SUPERUSER | `IndisponibiliteController` |

## Procédure de validation

- **Compilation** : `mvn compile`/`mvn test-compile`, verts après chaque commit.
- **Tests unitaires** : `IncrementationSoldeSchedulerTest` (Mockito, 2 cas — taux appliqué à chaque employé actif, relecture du taux à chaque appel sans cache) — exécutés avec succès (`mvn test`, offline, pas de Docker requis).
- **Tests d'intégration** (`ParametreGlobalControllerIT`, `AutorisationControllerIT`, `IndisponibiliteControllerIT`, `PlanningServiceEcraserCreneauxIT`) : écrits et compilés (~25 cas au total couvrant CRUD paramètres, délai 72h, quota 4h, justification de pointage, déduction jours ouvrés, solde insuffisant/forçage, refuser, scope Manager) — **non exécutables sur cette machine** (Testcontainers/npipe Docker Desktop, limitation documentée depuis le Lot 0 ; confirmée à nouveau ce lot avec une tentative de contournement via `DOCKER_HOST` explicite, sans succès).
- **Validation réelle backend** (`docker compose up --build backend` + `curl`/`docker exec psql`), tous les scénarios rejoués manuellement après reconstruction du backend avec le correctif du point 9 :
  - `parametres-globaux` : lecture du taux seedé (`1.5`), modification RH (`1.75`), lecture confirmant la persistance, `403` pour Manager.
  - Autorisation, workflow normal : Manager crée avec avis dans les 72h → `EN_ATTENTE_RH` ; au-delà de 72h → `409 [DÉLAI DÉPASSÉ]` ; RH valide → anomalie de pointage du jour passée en `JUSTIFIE` avec `autorisation_id`, créneau `ABSENCE` généré sur le planning (vérifié en base).
  - Autorisation, workflow d'urgence RH (après correctif) : création directe → `VALIDE`, justification de pointage et créneau planning **déjà présents immédiatement**, sans appel `valider()` séparé.
  - Autorisation : `PUT .../refuser` (Manager crée `EN_ATTENTE_RH`, RH refuse → `REFUSE`), `GET /autorisations?statut=REFUSE` confirme le listing.
  - Indisponibilité, workflow normal + jours ouvrés : congé payé Lundi→Vendredi avec un jour férié (`type=FERIE`) inséré le mercredi → 4 jours ouvrés décomptés (pas 5), solde déduit en conséquence, créneau `CONGE` généré sur le planning.
  - Indisponibilité, solde insuffisant : sans `forcer` → `409 "Solde insuffisant (X jours disponibles)"`, solde inchangé en base ; avec `forcer:true` → déduction en négatif, `historique_soldes.motif = FORCAGE_RH`.
  - Indisponibilité, workflow d'urgence RH (après correctif) : même comportement forçage/blocage directement à la création (pas seulement à la validation), vérifié en base (`solde_conges_jours`, `historique_soldes`, `plannings`).
  - Indisponibilité : `PUT .../refuser` (gap comblé), `GET /indisponibilites?statut=REFUSE`.
  - Régression Lot 4 : `POST /calendrier/jours-feries` toujours `403` pour Manager (aucune régression de permissions introduite par les nouveaux modules).
  - Toutes les données de test créées pendant cette vérification (employés, comptes, autorisations, indisponibilités, pointages, jour férié, planning) ont été supprimées de la base de dev après coup ; le paramètre global a été remis à sa valeur seedée (`1.5`).
