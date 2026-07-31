# Rapport d'Audit — Cahiers des Charges SIRH (Phase 0)

**Date** : 2026-07-29
**Périmètre** : `CAHIER DES CHARGES FONCTIONNEL — SIRH v1` (CDF) et `CAHIER DES CHARGES TECHNIQUE (CDC-T) v1`
**Statut** : Brouillon soumis à validation — aucune correction n'a encore été appliquée aux cahiers sources. Ce document liste les constats ; les arbitrages restent à valider (voir §5).

## Méthodologie

Trois passes de lecture croisée ont été effectuées :

1. **Fonctionnel ↔ Fonctionnel** : recherche de contradictions internes au CDF (une règle d'un module qui en contredit une autre).
2. **Fonctionnel ↔ Technique** : confrontation de chaque règle métier du CDF avec le schéma SQL, les endpoints et leurs règles de sécurité du CDC-T.
3. **Technique ↔ Faisabilité** : recherche de lacunes d'infrastructure ou de modélisation qui empêcheraient une implémentation fidèle du CDC-T tel quel.

Chaque constat est classé par sévérité :
- 🔴 **CRITIQUE** : contradiction directe ou trou de sécurité — bloque le début de la Phase 1 tant qu'un arbitrage produit n'est pas rendu.
- 🟠 **MAJEUR** : fonctionnalité du CDF sans support technique, ou schéma incompatible avec les endpoints décrits — implémentation impossible sans complément de spec.
- 🟡 **MOYEN** : zone grise à trancher en Phase 1 (architecture), sans remettre en cause le périmètre.
- ⚪ **MINEUR** : clarification ou incohérence cosmétique, sans impact structurant.

---

## 1. Résumé exécutif

| # | Titre | Sévérité | Catégorie |
|---|---|---|---|
| 1 | Modèle à 5 rôles fonctionnels vs 2 rôles techniques (`ROLE_RH`/`ROLE_MANAGER`) | 🔴 CRITIQUE | Rôles/Sécurité |
| 1a | Portail "Employé" en libre-service promis mais interdit par les Modules 8 & 13, aucun rôle/endpoint technique | 🔴 CRITIQUE | Rôles/Sécurité |
| 1b | Rôle Directeur sans navigation, sans rôle technique, exclu du dashboard KPI | 🔴 CRITIQUE | Rôles/Sécurité |
| 1c | Rôle Super User sans existence technique, sidebar incomplète vs "mêmes droits que RH" | 🔴 CRITIQUE | Rôles/Sécurité |
| 2 | Inversion du droit d'accès aux logs (RH exclu au CDF, endpoint réservé au RH au CDC-T) | 🔴 CRITIQUE | Rôles/Sécurité |
| 3 | Table `shifts` absente du schéma alors que le Planning en dépend entièrement | 🟠 MAJEUR | Schéma de données |
| 4 | Table `contrats` (historique) absente, incompatible avec Vue 360° et Module 12 | 🟠 MAJEUR | Schéma de données |
| 5 | Workflow de justification des anomalies de pointage non modélisé (`pointages_bruts`) | 🟠 MAJEUR | Schéma de données |
| 6 | Table `historique_soldes` citée mais absente du schéma | 🟠 MAJEUR | Schéma de données |
| 7 | Module 9 (Évaluation Annuelle) absent du CDC-T | 🟠 MAJEUR | Périmètre technique |
| 8 | Module 11 (Notifications) absent du CDC-T malgré la cloche en UI | 🟠 MAJEUR | Périmètre technique |
| 9 | Module 12 (Renouvellement contrats) : workflow de tâches CRON non modélisé | 🟠 MAJEUR | Périmètre technique |
| 10 | Table `outillages` sans historique de mouvements ni champ d'imputation financière | 🟠 MAJEUR | Schéma de données |
| 11 | Rôle fonctionnel de Redis non défini (cache ? file de jobs ? session ?) | 🟡 MOYEN | Infrastructure |
| 12 | Absence d'outil de migration de schéma (Flyway/Liquibase) et de `.env.example` | 🟡 MOYEN | Infrastructure |
| 13 | Mécanisme de liste blanche d'IP pour le webhook Hikvision non précisé | 🟡 MOYEN | Infrastructure/Sécurité |
| 14 | Architecture d'archivage (Module 7) incohérente avec le point de montage unique | 🟡 MOYEN | Infrastructure |
| 15 | Recalcul auto des soldes congés/jours fériés (Module 6) sans endpoint/trigger | 🟡 MOYEN | Schéma/API |
| 16 | `indisponibilites` : colonnes manquantes vs contrat des endpoints | 🟡 MOYEN | Schéma de données |
| 17 | `autorisations` : colonne `type` absente du schéma | 🟡 MOYEN | Schéma de données |
| 18 | Génération de rapports async sans table `report_jobs` équivalente à `import_jobs` | 🟡 MOYEN | Schéma de données |
| 19 | Seuil de doublon de pointage : "< 5 min" (règle générale) vs "30 s" (exemple) | ⚪ MINEUR | Clarification |
| 20 | Exemple de date d'embauche future contredisant sa propre règle de validation | ⚪ MINEUR | Clarification |
| 21 | Triple matricule (matricule/RH/Paie) ajouté au schéma sans justification fonctionnelle | ⚪ MINEUR | Clarification |
| 22 | Absence de politique de verrouillage de compte après échecs de connexion | ⚪ MINEUR | Sécurité (durcissement) |

**Comptage** : 5 CRITIQUE · 8 MAJEUR · 8 MOYEN · 4 MINEUR — soit 25 constats sur 22 lignes (1, 1a-1c et 2 forment un même bloc racine).

---

## 2. Constats CRITIQUES — arbitrage produit requis avant Phase 1

### 2.1 — Modèle de rôles : 5 profils fonctionnels vs 2 rôles techniques

**Constat** : le CDF (§2.3) définit 5 profils (Employé, Responsable Département, Responsable RH, Directeur, Super User). Le CDC-T ne modélise que deux valeurs de rôle exploitables : la colonne `utilisateurs.role` est commentée `-- 'ROLE_RH', 'ROLE_MANAGER'`, et **tous** les endpoints du catalogue (§3.1 à §3.10) n'autorisent que `MANAGER` et/ou `RH` dans leur ligne "Sécurité / Rôle requis". Aucun endpoint n'accepte `ROLE_EMPLOYE`, `ROLE_DIRECTEUR` ou `ROLE_SUPERUSER`.

Cette racine produit trois symptômes distincts :

**1a. Portail Employé fantôme**
Le CDF promeut en Introduction (§1) et en §2.2/§2.3 un "portail Employé en libre-service strict" permettant la consultation du planning et **la saisie de ses propres demandes**. Or :
- Module 8 (Congés) : *"Saisie effectuée par le Manager ou le RH (aucun accès employé)."*
- Module 13 (Autorisations) : *"Accès restreint : Saisie réalisable uniquement par le Manager de département ou le Service RH."*
- Aucun endpoint CDC-T n'est accessible en écriture à un profil employé ; la table `autorisations` porte même un champ `cree_par_manager_id` (nom explicite excluant l'employé).

→ **Impact** : c'est une des promesses centrales de l'application ("Périmètre d'utilisation" en page 1) qui n'a aucune traduction technique, et qui est même explicitement contredite par deux modules. Sans arbitrage, impossible de savoir si l'on développe un vrai self-service employé ou un système 100% back-office.

**1b. Rôle Directeur non câblé**
Le CDF (§2.3) définit le Directeur comme lecteur seul des KPI. Mais :
- La sidebar (§2.2.B) ne liste **aucun** "Espace Directeur" (seuls RH/Admin, Resp. Département, Employé, Super User y figurent).
- L'endpoint `/dashboard/kpi`, seule fonctionnalité utile à ce rôle, est sécurisé `MANAGER, RH` — le Directeur en est techniquement exclu de son propre cas d'usage.

**1c. Rôle Super User incomplet**
Le CDF dit du Super User qu'il a *"les mêmes droits métiers et d'accès que le Responsable RH... auquel s'ajoute un accès exclusif aux logs"*. Or la sidebar Super User (§2.2.B) omet Planning, Congés & Autorisations, Statistiques et Calendrier — présents dans l'espace RH/Admin — ce qui contredit le principe de superset annoncé. Techniquement, `ROLE_SUPERUSER` n'existe nulle part.

**Recommandation** : trancher en priorité absolue (avant toute Phase 1) sur l'un des deux scénarios :
- **(A) Réduire le périmètre réel à 2 rôles techniques** (RH, Manager) pour la V1, et documenter Employé/Directeur/Super User comme rôles futurs (roadmap) — le plus rapide à livrer, mais s'écarte du CDF validé par le métier.
- **(B) Étendre le schéma et les endpoints à 5 rôles** dès la Phase 1 (ajout `ROLE_EMPLOYE`, `ROLE_DIRECTEUR`, `ROLE_SUPERUSER`, endpoints self-service employé, endpoints dashboard ouverts au Directeur, endpoints logs restreints au Super User) — fidèle au CDF, plus gros effort initial.

### 2.2 — Inversion du droit d'accès aux logs d'audit

**Constat** : le CDF est sans ambiguïté — *"Le Responsable RH... n'a aucun accès aux logs"* et le Super User dispose d'un *"accès exclusif aux logs système ainsi qu'aux logs de toutes les actions et connexions des autres comptes utilisateurs"*. Le CDC-T fait l'inverse : l'endpoint `GET /api/v1/audit-logs` (§3.10.3) est sécurisé **"RH (Réservé aux RH)"**, avec une erreur 403 explicite si *"Utilisateur connecté n'est pas RH"*.

**Impact** : c'est le contrôle d'accès le plus sensible du système (piste d'audit immuable, traçabilité légale sur 10 ans) et il est actuellement spécifié à l'envers du besoin métier exprimé. Une implémentation au fil de l'eau du CDC-T donnerait au RH un accès qui lui est explicitement refusé par le métier.

**Recommandation** : ce point doit être corrigé en même temps que 2.1 (nécessite `ROLE_SUPERUSER`) — sécuriser `/audit-logs` sur `SUPERUSER` exclusivement dès que ce rôle existe.

---

## 3. Constats MAJEURS — lacunes de modélisation bloquantes

| # | Constat | Impact | Recommandation |
|---|---|---|---|
| 3 | Aucune table `shifts` (ou `shift_templates`) dans le schéma SQL, alors que `plannings/affecter` (POST) attend un `shiftId` existant et que `plannings/departement/{id}` (GET) retourne un objet `shift {id, type, heureDebut, heureFin}` distinct, avec un `statut` (VALIDÉ/PROVISOIRE/REPOS) absent de la table `plannings`. | Le Module 2 (Planification), pourtant central, est **non implémentable** avec le schéma fourni. | Ajouter une table `shifts` (catalogue de créneaux réutilisables : MATIN/APRES-MIDI/NUIT, heures) + FK `shift_id` sur `plannings`, et une colonne `statut` sur `plannings`. |
| 4 | Pas de table `contrats` : `employes` ne porte que des colonnes scalaires (`type_contrat`, `date_fin_contrat`), alors que la Vue 360° (`GET /employes/{id}`) retourne un tableau `contrats: [...]` et que le Module 12 suppose un historique de renouvellements (CDD renouvelé en CDD, etc.). | Impossible de tracer l'historique contractuel d'un salarié (pourtant l'objet même du Module 4 "Vue 360°"). | Créer une table `contrats` (employe_id, type, date_debut, date_fin, statut) et faire de `employes.type_contrat`/`date_fin_contrat` un simple reflet du contrat actif (dénormalisation contrôlée) ou les supprimer au profit de la table dédiée. |
| 5 | `pointages_bruts` n'a ni `statut` (anomalie/justifié/ignoré-doublon), ni `autorisation_id`. Or le CDC-T décrit explicitement (§3.7.3, règle métier) que la validation d'une demande RH doit *"marquer les anomalies comme JUSTIFIÉ... avec référence à l'ID de la demande d'autorisation dans la colonne autorisation_id"*. | Le mécanisme central de justification des anomalies (cœur du Module 1 et du Module 13) n'a pas de support en base. | Ajouter `statut` (ex. `OK` / `ANOMALIE_NON_JUSTIFIEE` / `JUSTIFIE` / `IGNORE_DOUBLON`) et `autorisation_id` (FK nullable) sur `pointages_bruts`. |
| 6 | Table `historique_soldes` citée dans la règle métier de validation des congés (§3.8.2) mais absente du schéma §2. | Pas de traçabilité technique des déductions de solde, alors que le CDF l'exige explicitement pour ce cas d'usage sensible (paie). | Créer la table `historique_soldes` (employe_id, date, delta, solde_apres, motif, source_id). |
| 7 | Module 9 (Évaluation Annuelle : grilles de notation, PDI) : aucune table, aucun endpoint dans le CDC-T. | Module fonctionnel entier sans aucune traduction technique. | Décider si le module est hors périmètre V1 (à documenter comme tel) ou à spécifier en Phase 1. |
| 8 | Module 11 (Notifications) : aucune table `notifications`, aucun endpoint de consultation/marquage-lu, alors que la Top Bar (§2.2.A) affiche une "Cloche de notification" comme élément global permanent de l'UI. | Un élément d'interface central (visible sur tous les écrans) n'a aucune API pour être alimenté côté frontend. | Ajouter une table `notifications` (destinataire_id, type, message, lu, entite liée, created_at) + endpoints `GET /notifications`, `PUT /notifications/{id}/lue`. |
| 9 | Module 12 : le workflow de tâches RH généré par CRON à M+5/M+11 avec statuts `[En cours]/[Validé]/[Non renouvelé]/[URGENT-EXPIRÉ]` n'existe qu'à travers un unique `PUT /employes/{id}/renouveler-contrat`, sans table de tâches ni endpoint de liste des tâches en cours. | Le RH n'a aucun moyen technique de visualiser "ses tâches de renouvellement à traiter" (le CDF décrit un vrai objet métier "tâche", pas une simple action ponctuelle). | Ajouter une table `taches_renouvellement` (employe_id, echeance, type M+5/M+11, statut) + endpoints de liste/traitement, alimentée par le CRON. |
| 10 | `outillages` n'a qu'un état courant (`etat`), pas d'historique de mouvements (check-out/check-in horodatés) ni de champ de montant d'imputation — alors que `POST /outillages/check-in` calcule et retourne un `montantImputation` selon un barème (0-6/6-12/>12 mois), qui doit être persisté et rattaché à l'employé pour le "traitement financier" mentionné en Module 5. | Pas de traçabilité de qui a eu quel matériel à quelle période, ni de suivi des imputations financières. | Ajouter une table `outillage_mouvements` (outillage_id, employe_id, type CHECK_OUT/CHECK_IN, date, etat, montant_imputation, commentaire). |

---

## 4. Constats MOYENS — zones grises à trancher en Phase 1

| # | Constat | Recommandation proposée |
|---|---|---|
| 11 | Redis est déclaré dans le `docker-compose.yml` comme *"Cache / Traitements Asynchrones"* mais aucune configuration Spring (Spring Cache, Spring Session, file de jobs) n'est décrite. Les jobs asynchrones (import Excel, rapports) reposent en réalité sur `@Async`/`@Scheduled` + tables de suivi (`import_jobs`), pas sur Redis. | Fixer le rôle de Redis dès `docs/ARCHITECTURE.md` : cache des KPI dashboard (`@Cacheable`) + éventuellement blacklist de tokens JWT au logout. Pas de file de jobs Redis à ce stade (volumétrie mono-serveur ne le justifie pas). |
| 12 | Aucun outil de migration de schéma (Flyway/Liquibase) n'est prévu ; le DDL du §2 du CDC-T est une simple suite de `CREATE TABLE`. Pas de `.env.example` non plus pour `DB_PASSWORD`. | Introduire Flyway dès le premier commit backend (`V1__init_schema.sql` = DDL corrigé), fournir `.env.example` à la racine. |
| 13 | Le webhook `/api/v1/hikvision/events` est déclaré "Public (sécurisé par liste blanche d'IPs)" sans préciser où cette liste vit (config Spring Security ? règle Nginx ? table `pointeuses.adresse_ip` déjà existante ?). | Réutiliser la table `pointeuses` existante comme source de vérité de la liste blanche (filtre applicatif Spring), documenté dans ARCHITECTURE.md. |
| 14 | Le Module 7 décrit un *"serveur d'archivage dédié"* pouvant être *"injoignable (panne réseau)"* indépendamment du reste, mais le CDC-T (§1.2) ne monte qu'**un seul** point de montage réseau (`/mnt/company_storage/`) avec un sous-dossier `/archives/`. Si ce montage unique tombe, tout le stockage (contrats, imports, rapports) tombe avec lui — la panne "isolée" décrite dans le cas d'erreur n'est pas cohérente avec un mount point unique. | Clarifier : soit l'archivage reste un sous-dossier du même volume (et alors le scénario d'erreur du CDF doit être reformulé), soit un second mount réseau dédié doit être ajouté à l'architecture. |
| 15 | Le recalcul automatique des soldes de congés lors de la validation d'un jour férié mobile chevauchant un congé posé (Module 6) est décrit comme règle métier, sans endpoint ni mécanisme technique associé dans le CDC-T (pas de trigger, pas de job décrit). | À spécifier en Phase 1 comme logique de service déclenchée depuis `POST /calendrier/jours-feries` (recherche des congés existants sur la date, recrédit). |
| 16 | La table `indisponibilites` (§2 CDC-T) n'a pas les colonnes `commentaire`, `piece_jointe_id`, alors que les endpoints `/indisponibilites` (POST/GET) les manipulent et retournent également des champs calculés (`nbJoursOuvrables`, `soldeAvantDeduction`, `soldeApresDeduction`) non stockés. | Ajouter `commentaire TEXT`, `piece_jointe_id BIGINT REFERENCES fichiers(id)` ; les soldes calculés peuvent rester dérivés (non stockés) si `historique_soldes` (constat 6) est créé. |
| 17 | La table `autorisations` n'a pas de colonne `type` (ABSENCE_COURTE/SORTIE_MEDICALE/RETARD_JUSTIFIE/MISSION), alors que cette valeur est centrale dans les règles de validation du Module 13 (ex. RETARD_JUSTIFIE ne requiert pas de `dateFin`). | Ajouter `type VARCHAR(30) NOT NULL` sur `autorisations`. |
| 18 | Les rapports asynchrones (`POST /reports/generer`, `GET /reports/tasks/{taskId}`) utilisent un `taskId` UUID sans table de suivi décrite (contrairement aux imports Excel qui ont `import_jobs`). | Ajouter une table `report_jobs` (ou généraliser `import_jobs` en `async_jobs` avec un `type` discriminant, réutilisable pour imports ET rapports). |

---

## 5. Constats MINEURS — clarifications

| # | Constat | Recommandation |
|---|---|---|
| 19 | Le seuil de détection des doublons de pointage est donné à "< 5 minutes" en règle générale (Module 1, Traitements & Règles) mais l'exemple du cas exceptionnel "Doublon réflexe" utilise "30 secondes". | Confirmer s'il s'agit du même seuil (l'exemple est alors juste illustratif d'un cas extrême) ou de deux seuils distincts à documenter séparément. |
| 20 | L'exemple de `POST /employes` (§3.4.3) utilise `"dateEmbauche": "2026-08-01"` alors que la règle énoncée juste au-dessus interdit une date dans le futur, et que "aujourd'hui" est 2026-07-29 dans le contexte système. | Corriger l'exemple avec une date passée/présente lors de la réécriture des specs, pour éviter qu'un test copié depuis la doc échoue. |
| 21 | Trois identifiants distincts (`matricule`, `matricule_rh`, `matricule_paie`) ont été ajoutés au schéma employés ("Mise à jour") sans qu'aucune règle du CDF n'explique pourquoi trois systèmes d'identification coexistent. | Documenter la raison métier (ex. contrainte du logiciel de paie externe) dans le CDF corrigé, sinon envisager de fusionner. |
| 22 | Aucune politique de verrouillage de compte après N échecs sur `POST /auth/login` n'est spécifiée. | Recommandation de durcissement standard à ajouter en Phase 1 (ex. verrouillage 15 min après 5 échecs), sans lien avec une exigence du CDF actuel. |

---

## 6. Recommandations de stack

### Frontend — React (Vite, SPA) confirmé

| Critère | Analyse |
|---|---|
| SSR / SEO | Aucun besoin (back-office authentifié, déploiement on-premise) → écarte Next.js/Remix, qui ajouteraient un serveur Node et de la complexité de déploiement sans bénéfice. |
| Déploiement prévu | Le `docker-compose.yml` sert déjà le frontend via Nginx statique — cohérent avec un build Vite classique. |
| Densité de données / dark mode (§2.1 CDF) | Nécessite un design system de tableaux/formulaires robuste dès le départ. |
| Polling de jobs asynchrones (imports, rapports) | Pattern qui bénéficie directement d'un cache serveur avec invalidation/re-fetch automatique. |

**Bibliothèques recommandées** : TypeScript strict, **TanStack Query** (cache serveur + polling natif pour les jobs), **TanStack Table** (tableaux data-dense), **React Hook Form + Zod** (validation alignée sur les règles strictes du CDF), **Zustand** (état UI léger : session, sidebar), **React Router**, **shadcn/ui + Tailwind CSS** (thème sombre natif exigé), **Axios** avec intercepteur de refresh JWT, **openapi-typescript** pour générer les types depuis l'OpenAPI backend (contrat unique de vérité), **Recharts** pour les widgets de dashboard.

### Backend — Spring Boot confirmé, compléments nécessaires

**Manques à combler dès l'initialisation** (cf. constats 12) : **Flyway** pour versionner le schéma corrigé, **Springdoc OpenAPI** pour générer le contrat consommé par le frontend, **MapStruct** pour le mapping entité/DTO vu le volume d'endpoints (~50), **Testcontainers** (Postgres + Redis) pour les tests d'intégration, **Bean Validation** pour les règles de contraintes (déjà largement documentées dans le CDC-T, à traduire en annotations).

---

## 7. Décisions attendues avant d'ouvrir la Phase 1

1. **Rôles (constats 1, 1a-1c, 2)** : scénario (A) réduit à 2 rôles pour la V1, ou (B) 5 rôles fidèles au CDF ? *(bloquant, à trancher en premier)*
2. **Modules 9, 11, 12** *(constats 7, 8, 9)* : inclus dans le périmètre de développement immédiat, ou repoussés en V2 avec mention explicite dans les specs corrigées ?
3. **Modélisation planning** *(constat 3)* : validation de l'ajout d'une table `shifts`.
4. **Modélisation contrats** *(constat 4)* : validation de l'ajout d'une table `contrats` historisée.
5. **Rôle de Redis** *(constat 11)* : accord sur cache + éventuel blacklist JWT, pas de file de jobs à ce stade.
6. **Architecture d'archivage** *(constat 14)* : sous-dossier unique ou second point de montage réseau ?

Dès réception de tes arbitrages sur ces 6 points, je mets à jour `docs/specs/cahier-fonctionnel.md` et `docs/specs/cahier-technique.md` (versions corrigées et annotées), puis j'ouvre `docs/ARCHITECTURE.md` en Phase 1.

---

## 8. Addendum — suivi des résolutions (mis à jour au fil de la Phase 1)

Tous les constats CRITIQUE, MAJEUR et MOYEN de ce rapport sont résolus dans `docs/specs/cahier-technique.md` (v2) et `docs/ARCHITECTURE.md`. Sur les constats MINEUR restés ouverts à l'issue de la première correction :

| # | Constat | Résolution | Où |
|---|---|---|---|
| 19 | Seuil de doublon "< 5 min" vs "30 s" | **Résolu (2026-07-29)** : seuil unique de 5 minutes, scope même employé + même pointeuse, seul le premier pointage compte pour le calcul des heures travaillées, le second est conservé en base avec `statut = IGNORE_DOUBLON`. | `cahier-technique.md` §3.7 |
| 21 | Triple matricule non justifié | **Résolu (2026-07-29)** : restructuré en `matricule_interne` (unique, clé métier), `matricule_cnss` (optionnel, remplace `matricule_rh`, donnée sensible), `matricule_paie` (optionnel). Décision complémentaire non anticipée par l'audit : la clé technique `employes.id` passe de `BIGSERIAL` à `UUID`, avec propagation sur les 18 colonnes de FK concernées dans 13 tables du schéma. | `cahier-technique.md` §3.2, `ARCHITECTURE.md` §4 et §10 |

Il ne reste aucun point ouvert issu de cet audit. La Phase 1 (Exploration) est close ; la Phase 2 (Planification) peut démarrer.
