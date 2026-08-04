# CAHIER DES CHARGES TECHNIQUE (CDC-T) — v2 corrigé

**Statut** : remplace le CDC-T v1 fourni en pièce jointe. Corrige l'intégralité des constats CRITIQUE et MAJEUR de `audit-rapport.md`, ainsi que les constats MOYEN/MINEUR pertinents pour le schéma et la sécurité. **Aucun périmètre fonctionnel n'a été réduit** : les 13 modules et les 5 rôles du cahier fonctionnel sont couverts.

**Légende** : 🆕 n'existait pas en v1 · 🔧 existait, corrigé/complété · ✅ repris de v1 sans changement.

## 0. Arbitrage confirmé par le métier (§2.3 vs Modules 8/13) — corrigé le 2026-07-29

Confirmé : l'Employé ordinaire **n'a aucun accès applicatif** aux demandes d'autorisations et de congés — les Modules 8 et 13 font foi sur ce point précis (*"aucun accès employé"* / *"saisie réservée au Manager ou au RH"*). La formulation "saisie de ses demandes" en §2.3 se lit en réalité à la lumière du Workflow Nominal du Module 8 : *"L'employé formule sa demande... hors système. Le Manager ou le RH saisit la demande dans l'application."* L'employé formule sa demande **hors du SIRH** (oral, email...) ; seul le Manager ou le RH l'enregistre dans l'application.

**Le seul droit applicatif confirmé du rôle Employé est donc la consultation en lecture seule de son propre planning** (§2.3 : *"Accès à son propre planning (consultation)"*). La version précédente de ce document accordait à tort un self-service employé sur les demandes — c'est corrigé ci-dessous.

Le mécanisme d'avis du Responsable Département (*"émet un avis favorable/défavorable... transfère au Responsable RH"*, §2.3) reste pleinement conservé : il concerne le rôle Manager et n'a jamais été remis en cause. Il est simplifié en §5 pour coller exactement au Workflow Nominal des Modules 8/13, qui ne décrit pas d'étape intermédiaire séparée : l'avis est désormais capturé au moment même où le Manager saisit la demande.

---

## 1. Architecture Réseau, Matérielle & Ingestion

### 1.1 Pointeuses Hikvision — ✅ inchangé
Mode Push (`POST /api/v1/hikvision/events`) + fallback horaire `@Scheduled` sur `/ISAPI/Attendance/Search`.

**🔧 Précision liste blanche IP** (constat 13) : la table `pointeuses.adresse_ip` (déjà existante) est la source de vérité. Un filtre Spring Security (`HikvisionIpAllowlistFilter`, exécuté avant l'authentification) rejette en 403 toute requête sur `/api/v1/hikvision/events` dont l'IP source ne correspond à aucune ligne active de `pointeuses`.

### 1.2 Stockage — 🔧 clarifié (constat 14)
Un seul point de montage réseau `/mnt/company_storage/` (NFS/SMB) avec ses sous-dossiers `/contracts/`, `/reports/`, `/raw_imports/`, `/archives/`. **Il n'existe pas de second serveur physique dédié à l'archivage** : le scénario d'erreur du Module 7 est reformulé ainsi : *"Si le montage réseau devient inaccessible au moment de la tâche CRON mensuelle de purge/archivage, l'opération est annulée, retentée 3 fois, puis un rapport d'erreur d'infrastructure est émis — sachant qu'une telle panne affecte alors l'ensemble du stockage applicatif, pas seulement l'archivage."* Si un vrai isolement de l'archivage est requis à terme, cela nécessitera un second mount dédié — non retenu pour cette version.

---

## 2. Modèle de Rôles & Sécurité 🆕

Résout les constats 1, 1a, 1b, 1c, 2 de l'audit.

### 2.1 Rôles techniques

| Rôle | Correspond à (CDF §2.3) | Portée |
|---|---|---|
| `ROLE_EMPLOYE` | Employé ordinaire | **Lecture seule** de son propre planning — aucun accès (ni lecture ni écriture) aux autorisations/congés, gérés exclusivement par Manager/RH |
| `ROLE_MANAGER` | Responsable Département | Lecture planning de son équipe, avis sur les demandes de son équipe, gestion du matériel de son équipe |
| `ROLE_RH` | Responsable RH (Admin) | Gestion complète plannings/personnel/comptes utilisateurs, validation finale des demandes, clôture paie — **aucun accès aux logs** |
| `ROLE_DIRECTEUR` | Directeur | Lecture seule du dashboard KPI global — aucune autre action |
| `ROLE_SUPERUSER` | Super User | Mêmes droits que RH + **accès exclusif** aux logs système et logs utilisateurs |

`utilisateurs.role` accepte désormais ces 5 valeurs (contre 2 en v1).

### 2.2 Règle de sécurité transversale corrigée (constat 2)

Tout endpoint sécurisé `RH` en v1 reste accessible à `SUPERUSER` (superset), **sauf** `/api/v1/audit-logs` et tout endpoint de consultation des logs, qui passent de `RH` à **`SUPERUSER` exclusivement**. `/api/v1/dashboard/kpi` s'ouvre à `DIRECTEUR` en plus de `MANAGER, RH, SUPERUSER` (lecture seule, aucune mutation n'est jamais exposée à `DIRECTEUR`).

### 2.3 Claims JWT

```json
{
  "sub": "jdupont",
  "userId": 1,
  "employeId": "a3f1c2e4-9b7d-4e21-8c3a-1f2b3c4d5e6f",
  "role": "ROLE_EMPLOYE",
  "departementId": 3,
  "permissions": ["READ_OWN_PLANNING"]
}
```
*(`employeId` est un `UUID` — string, pas un entier — depuis la restructuration du §3.2.)*
`departementId` permet au backend de filtrer sans requête supplémentaire les accès `MANAGER` scopés à son équipe (déjà utilisé en v1 mais non documenté dans le token).

---

## 3. Modèle Conceptuel de Données — Schéma PostgreSQL corrigé

### 3.1 `departements` — 🔧 `manager_id` en UUID (cascade §3.2)
```sql
CREATE TABLE departements (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    manager_id UUID,                        -- 🔧 UUID (référence employes.id)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.2 `employes` — 🔧 clé primaire UUID + restructuration des matricules (constat 21 résolu, confirmé le 2026-07-29)

**Décisions actées** :
- Clé technique `id` : `UUID` auto-généré (`gen_random_uuid()`, natif depuis PostgreSQL 13, aucune extension requise — compatible avec `postgres:16-alpine` du `docker-compose.yml`).
- 3 champs métier distincts, remplaçant les 3 champs ambigus de v1 :
  - `matricule_interne` : identifiant principal, **clé métier unique**, corrélé au `badgeId` transmis par les pointeuses Hikvision et à la colonne `badgeId` des imports Excel.
  - `matricule_cnss` : numéro de sécurité sociale / affiliation légale, **optionnel**. Remplace l'ancien `matricule_rh` dont l'usage était mal défini. **Donnée sensible** — voir note de sécurité en `docs/ARCHITECTURE.md` §10.
  - `matricule_paie` : référence externe pour le logiciel de paie (Sage, ADP...), **optionnel** (v1 l'imposait à tort en `NOT NULL`).
- **Conséquence en cascade** : toute colonne référençant `employes(id)` dans les 22 autres tables du schéma passe de `BIGINT` à `UUID`. Seule la clé primaire de `employes` change de type — les autres tables conservent leur propre PK en `BIGSERIAL` (pas de généralisation globale de l'UUID à tout le schéma, non demandée).

```sql
CREATE TABLE employes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- 🔧 UUID (ex-BIGSERIAL)
    matricule_interne VARCHAR(50) NOT NULL UNIQUE, -- 🔧 renommé (ex-matricule) : clé métier unique
    matricule_cnss VARCHAR(50) UNIQUE,             -- 🔧 remplace matricule_rh, optionnel, donnée sensible
    matricule_paie VARCHAR(50) UNIQUE,             -- 🔧 devient optionnel
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE,
    departement_id BIGINT REFERENCES departements(id),
    type_contrat VARCHAR(20) NOT NULL,
    date_embauche DATE NOT NULL,
    date_fin_contrat DATE,
    solde_conges_jours NUMERIC(5,2) DEFAULT 0.0,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_employes_matricule_interne ON employes(matricule_interne);
CREATE INDEX idx_employes_matricule_cnss ON employes(matricule_cnss);
CREATE INDEX idx_employes_matricule_paie ON employes(matricule_paie);
CREATE INDEX idx_employes_nom_prenom ON employes(nom, prenom);
ALTER TABLE departements ADD CONSTRAINT fk_manager FOREIGN KEY (manager_id) REFERENCES employes(id);
```

### 3.3 `contrats` 🆕 (constat 4 — Vue 360° et Module 12)
```sql
CREATE TABLE contrats (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    type VARCHAR(20) NOT NULL,              -- CDI, CDD, INTERIM, STAGE, APPRENTISSAGE
    date_debut DATE NOT NULL,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, RENOUVELE, TERMINE
    motif_fin VARCHAR(30),                  -- DEMISSION, LICENCIEMENT, FIN_CDD, RETRAITE, MUTATION
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_contrats_employe ON contrats(employe_id, date_debut DESC);
```
`employes.type_contrat`/`date_fin_contrat` sont mis à jour par trigger applicatif à chaque INSERT sur `contrats` (contrat le plus récent = actif).

### 3.4 `utilisateurs` — 🔧 rôle élargi
```sql
CREATE TABLE utilisateurs (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID UNIQUE NOT NULL REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,              -- ROLE_EMPLOYE, ROLE_MANAGER, ROLE_RH, ROLE_DIRECTEUR, ROLE_SUPERUSER
    compte_actif BOOLEAN DEFAULT TRUE,
    dernier_login TIMESTAMP,
    echecs_login_consecutifs INT DEFAULT 0,  -- 🆕 constat 22 : verrouillage après échecs
    verrouille_jusqu_a TIMESTAMP,            -- 🆕 constat 22
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_utilisateurs_username ON utilisateurs(username);
```

### 3.5 `evenements_360` — 🔧 structure inchangée (Vue 360°, catégories déjà génériques donc compatibles Évaluation/PDI), mais `employe_id` passe en `UUID` (référence `employes.id`, cf. §3.2)

### 3.6 `pointeuses` — ✅ inchangé

### 3.7 `pointages_bruts` — 🔧 (constat 5, workflow de justification des anomalies)
```sql
CREATE TABLE pointages_bruts (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE,  -- 🔧 UUID (cf. §3.2)
    pointeuse_id BIGINT REFERENCES pointeuses(id) ON DELETE SET NULL,
    timestamp_pointage TIMESTAMP NOT NULL,
    type_evenement VARCHAR(20) NOT NULL,
    methode_validation VARCHAR(30) NOT NULL,
    source_import VARCHAR(20) NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'OK',   -- 🆕 OK, ANOMALIE_NON_JUSTIFIEE, JUSTIFIE, IGNORE_DOUBLON
    autorisation_id BIGINT REFERENCES autorisations(id) ON DELETE SET NULL, -- 🆕 traçabilité justification
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pointages_emp_date ON pointages_bruts(employe_id, timestamp_pointage DESC);
CREATE INDEX idx_pointages_statut ON pointages_bruts(statut);
```

**🔧 Règle de détection des doublons — constat 19 résolu (confirmé le 2026-07-29)**
- **Seuil** : 5 minutes (le seuil retient cette valeur uniquement ; l'exemple "30 secondes" du Module 1 du CDF n'était qu'une illustration d'un cas extrême dans cette même fenêtre, pas un seuil distinct).
- **Portée** : même `employe_id` **et** même `pointeuse_id` (même lecteur) — deux badgeages du même employé sur deux lecteurs différents dans la même fenêtre ne sont pas des doublons.
- **Traitement** : les deux pointages sont conservés en base (aucune suppression, piste d'audit complète). Le premier chronologiquement garde `statut = 'OK'` ; le second passe en `statut = 'IGNORE_DOUBLON'`.
- **Impact sur le calcul d'assiduité (Module 3)** : le moteur de calcul (`GET /assiduite/calcul-mensuel`) **exclut** tout pointage `IGNORE_DOUBLON` de l'appariement Entrée/Sortie — seul le premier pointage de la paire est comptabilisé pour les heures travaillées.

### 3.8 `shifts` 🆕 (constat 3 — bloquant pour tout le Module 2)
```sql
CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,               -- ex: "Matin", "Après-midi", "Nuit"
    type_rotation VARCHAR(20) NOT NULL,     -- FIXE, 2X8, 3X8
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.9 `plannings` — 🔧 (constat 3)
```sql
CREATE TABLE plannings (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    shift_id BIGINT NOT NULL REFERENCES shifts(id), -- 🆕
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'PROVISOIRE', -- 🆕 PROVISOIRE, VALIDE, REPOS
    commentaire TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_plannings_emp_date ON plannings(employe_id, date_debut);
```

### 3.10 `autorisations` — 🔧 (constats 17 + workflow hiérarchique §5)
```sql
CREATE TABLE autorisations (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    type VARCHAR(30) NOT NULL,              -- 🆕 ABSENCE_COURTE, SORTIE_MEDICALE, RETARD_JUSTIFIE, MISSION
    date_autorisation DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME,
    duree_minutes INT,
    motif TEXT,
    statut VARCHAR(30) DEFAULT 'EN_ATTENTE_RH', -- 🔧 EN_ATTENTE_RH, VALIDE, REFUSE — jamais saisi par l'employé
    cree_par_id UUID NOT NULL REFERENCES employes(id), -- 🔧 UUID, toujours un Manager ou un RH, jamais l'employé concerné
    cree_par_role VARCHAR(20) NOT NULL,     -- 🔧 MANAGER, RH (RH = workflow d'urgence : statut VALIDE dès la création)
    avis_manager_statut VARCHAR(20),        -- 🆕 FAVORABLE, DEFAVORABLE — capturé à la création si cree_par_role = MANAGER
    avis_manager_commentaire TEXT,          -- 🆕
    valide_par_rh_id UUID REFERENCES employes(id), -- 🔧 UUID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_autorisations_emp_date ON autorisations(employe_id, date_autorisation DESC);
```

### 3.11 `indisponibilites` — 🔧 (constat 16 + workflow hiérarchique)
```sql
CREATE TABLE indisponibilites (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    type_indisponibilite VARCHAR(30) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(30) DEFAULT 'EN_ATTENTE_RH', -- 🔧 idem autorisations, jamais saisi par l'employé
    commentaire TEXT,                       -- 🆕
    piece_jointe_id BIGINT REFERENCES fichiers(id), -- 🆕
    cree_par_id UUID NOT NULL REFERENCES employes(id), -- 🔧 UUID, toujours un Manager ou un RH
    cree_par_role VARCHAR(20) NOT NULL,     -- 🔧 MANAGER, RH
    avis_manager_statut VARCHAR(20),        -- 🆕
    avis_manager_commentaire TEXT,          -- 🆕
    valide_par_rh_id UUID REFERENCES employes(id) -- 🔧 UUID
);
CREATE INDEX idx_indisponibilites_emp_date ON indisponibilites(employe_id, date_debut DESC);
```

### 3.12 `historique_soldes` 🆕 (constat 6)
```sql
CREATE TABLE historique_soldes (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    date_mouvement DATE NOT NULL,
    delta_jours NUMERIC(5,2) NOT NULL,      -- négatif = déduction, positif = recrédit (ex: jour férié prioritaire)
    solde_apres NUMERIC(5,2) NOT NULL,
    motif VARCHAR(50) NOT NULL,             -- VALIDATION_CONGE, RECREDIT_FERIE, INCREMENT_MENSUEL, FORCAGE_RH
    source_type VARCHAR(30),                -- INDISPONIBILITE, JOUR_FERIE, CRON_MENSUEL
    source_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_historique_soldes_emp ON historique_soldes(employe_id, date_mouvement DESC);
```

### 3.13 `outillages` — 🔧 `attribue_a_employe_id` en UUID (structure sinon inchangée)
```sql
CREATE TABLE outillages (
    id BIGSERIAL PRIMARY KEY,
    code_barres VARCHAR(100) NOT NULL UNIQUE,
    nom_outil VARCHAR(150) NOT NULL,
    etat VARCHAR(30) DEFAULT 'BON_ETAT',
    attribue_a_employe_id UUID REFERENCES employes(id) ON DELETE SET NULL, -- 🔧 UUID
    date_attribution TIMESTAMP
);
CREATE INDEX idx_outillages_emp ON outillages(attribue_a_employe_id);
```

### 3.14 `outillage_mouvements` 🆕 (constat 10)
```sql
CREATE TABLE outillage_mouvements (
    id BIGSERIAL PRIMARY KEY,
    outillage_id BIGINT NOT NULL REFERENCES outillages(id) ON DELETE CASCADE,
    employe_id UUID REFERENCES employes(id), -- 🔧 UUID
    type_mouvement VARCHAR(20) NOT NULL,    -- CHECK_OUT, CHECK_IN
    etat VARCHAR(30),                       -- BON, USE, CASSE, PERDU (uniquement pour CHECK_IN)
    montant_imputation NUMERIC(10,2) DEFAULT 0,
    commentaire TEXT,
    effectue_par_id BIGINT REFERENCES utilisateurs(id),
    date_mouvement TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outillage_mvt_outil ON outillage_mouvements(outillage_id, date_mouvement DESC);
```

### 3.15 `fichiers` — ✅ inchangé

### 3.16 `async_jobs` — 🔧 généralisation de `import_jobs` (constat 18)
```sql
CREATE TABLE async_jobs (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL,              -- 🆕 IMPORT_POINTAGES, REPORT
    nom_fichier VARCHAR(255),
    statut VARCHAR(20) NOT NULL,            -- PENDING, IN_PROGRESS, SUCCESS, FAILED
    progression INT DEFAULT 0,              -- 🆕
    parametres JSONB,                       -- 🆕 (ex: mois, departementId, format pour un report)
    resultat_url VARCHAR(500),              -- 🆕 lien de téléchargement si REPORT
    nombre_lignes_traitees INT DEFAULT 0,
    ligne_erreur INT,
    message_erreur TEXT,
    rapport_erreurs JSONB,                  -- 🆕 détail structuré (ligne/colonne/valeur/erreur)
    execute_par_id BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP                  -- 🆕
);
CREATE INDEX idx_async_jobs_type_statut ON async_jobs(type, statut);
```
*(`/api/v1/pointages/import-jobs` et `/api/v1/reports/tasks/{taskId}` interrogent désormais la même table, filtrée par `type`.)*

### 3.17 `audit_logs` — ✅ structure inchangée, accès restreint en §2.2

### 3.18 `notifications` 🆕 (constat 8 — Module 11 et cloche UI §2.2.A)
```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    destinataire_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,              -- ALERTE_POINTAGE, ALERTE_SOUS_ACTIVITE, DEMANDE_A_VALIDER, RENOUVELLEMENT_CONTRAT, ...
    message TEXT NOT NULL,
    entite_type VARCHAR(50),
    entite_id BIGINT,
    lue BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_dest ON notifications(destinataire_id, lue, created_at DESC);
```

### 3.19 `jours_feries` 🆕 (absente du DDL v1 alors que les endpoints `/calendrier/jours-feries` l'exigent — gap supplémentaire trouvé pendant la correction)
```sql
CREATE TABLE jours_feries (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,              -- FERIE, FERMETURE_COLLECTIVE
    national BOOLEAN DEFAULT TRUE,
    region VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.20 `taches_renouvellement` 🆕 (constat 9 — Module 12)
```sql
CREATE TABLE taches_renouvellement (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    contrat_id BIGINT REFERENCES contrats(id),
    type_echeance VARCHAR(10) NOT NULL,     -- M+5, M+11
    date_echeance DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, VALIDE, NON_RENOUVELE, URGENT_EXPIRE
    date_traitement TIMESTAMP,
    traite_par_id BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_taches_renouv_statut ON taches_renouvellement(statut, date_echeance);
```
Générée par une tâche `@Scheduled` quotidienne qui détecte les contrats atteignant M+5/M+11 et insère la ligne si absente ; bascule automatique en `URGENT_EXPIRE` si `date_echeance` dépassée et `statut = EN_COURS` (Module 12, cas exceptionnel).

### 3.21 `evaluations` 🆕 (constat 7 — Module 9)
```sql
CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE, -- 🔧 UUID
    manager_id UUID REFERENCES employes(id), -- 🔧 UUID
    annee INT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, VALIDEE_RH
    date_entretien DATE,
    date_validation_rh TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employe_id, annee)
);
```

### 3.22 `evaluation_competences` 🆕
```sql
CREATE TABLE evaluation_competences (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    competence VARCHAR(100) NOT NULL,
    note NUMERIC(3,1) NOT NULL,
    seuil_exigence NUMERIC(3,1) NOT NULL,
    commentaire TEXT
);
```

### 3.23 `plans_developpement` 🆕 (PDI)
```sql
CREATE TABLE plans_developpement (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    competence VARCHAR(100) NOT NULL,       -- compétence sous le seuil ayant déclenché le PDI
    actions TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(20) DEFAULT 'A_TRAITER'
);
```
Génération automatique d'une ligne par compétence sous le seuil lors de la validation RH d'une évaluation (Module 9, règle métier).

### 3.24 `parametres_globaux` 🆕 (Lot 5 — décision d'architecture explicite)

Aucune règle métier dont la valeur peut varier sans changement de comportement applicatif (ex. taux d'incrémentation mensuelle des congés, Module 8) ne doit être codée en dur. Table générique cle/valeur, volontairement hors du domaine `demandes` (package `com.sirh.parametres`) pour être réutilisable par d'autres lots (ex. seuils M+5/M+11 du Module 12) sans nouvelle migration à chaque nouveau paramètre.

```sql
CREATE TABLE parametres_globaux (
    id BIGSERIAL PRIMARY KEY,
    cle VARCHAR(100) NOT NULL UNIQUE,
    valeur VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'DECIMAL',  -- DECIMAL, INTEGER, BOOLEAN, TEXT
    description TEXT,
    modifie_par_id UUID REFERENCES employes(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Seedée à la migration (`TAUX_INCREMENTATION_CONGES_MENSUEL` = `1.5`, motif `INCREMENT_MENSUEL` de `historique_soldes` §3.12) — jamais créée/supprimée via l'API (`POST`/`DELETE` volontairement absents, cf. §4.11), uniquement lue/modifiée. Le job `@Scheduled` mensuel d'incrémentation (`ARCHITECTURE.md` §8) relit la valeur à chaque exécution, sans cache, pour qu'un changement RH s'applique dès le prochain passage sans redéploiement.

### 3.25 `cloture_paie` 🆕 (Lot 6 — gap trouvé en préparant le lot, même nature que `jours_feries` au Lot 4 : le CDC-T v1 renvoyait vers `GET /assiduite/calcul-mensuel`/`POST /assiduite/cloturer-paie` en "✅ INCHANGÉ" sans qu'aucune table de verrouillage mensuel n'ait jamais existé dans le schéma)

```sql
CREATE TABLE cloture_paie (
    id BIGSERIAL PRIMARY KEY,
    mois DATE NOT NULL UNIQUE,              -- toujours le 1er du mois (ex: 2026-07-01)
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERT', -- OUVERT, CLOTURE
    date_cloture TIMESTAMP,
    cloture_par_id UUID REFERENCES employes(id),
    taux_anomalies_justifiees NUMERIC(5,2), -- snapshot du contrôle au moment de la clôture
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Décision d'architecture confirmée (2026-08-04)** : clôture **globale par mois**, pas par département — cohérent avec le CDF ("le RH... clôture des périodes", droit global sur tous les départements). Pas de table de snapshot des résultats de calcul : `GET /assiduite/calcul-mensuel` recalcule à la volée à chaque appel (croisement `pointages_bruts`/`plannings`/`jours_feries`/`autorisations`/`indisponibilites`) — une fois le mois `CLOTURE`, ces données sources deviennent en lecture seule au niveau applicatif (409 sur `PUT /pointages/{id}/corriger`, `POST /autorisations`, `POST /indisponibilites` dont la date tombe dans le mois clôturé), donc le recalcul reste stable sans duplication à synchroniser.

### 3.26 `demandes_deverrouillage` 🆕 (Lot 6 — Module 3, cas exceptionnel "Demander Déverrouillage Exceptionnel")

```sql
CREATE TABLE demandes_deverrouillage (
    id BIGSERIAL PRIMARY KEY,
    mois DATE NOT NULL,
    motif TEXT NOT NULL,
    demande_par_id UUID NOT NULL REFERENCES employes(id),
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE', -- EN_ATTENTE, APPROUVEE, REFUSEE
    traite_par_id UUID REFERENCES employes(id),
    date_traitement TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Une demande ne peut être créée que sur un mois déjà `CLOTURE` (400 sinon). L'approbation RH (`statut = APPROUVEE`) repasse `cloture_paie.statut` à `OUVERT` pour ce mois — le RH doit ensuite reclôturer explicitement via `POST /assiduite/cloturer-paie` une fois la régularisation faite (pas de re-clôture automatique).

**Seuils du moteur de calcul (Module 3) 🆕 dans `parametres_globaux`** : aucune valeur chiffrée n'existant dans le CDF/CDC-T (segmentation heures normales/nuit/majorées, tranches de prime, seuil de clôture), même principe que `TAUX_INCREMENTATION_CONGES_MENSUEL` — valeurs par défaut seedées en migration, modifiables sans redéploiement via `PUT /parametres-globaux/{cle}` : `HEURE_DEBUT_NUIT` (`21:00`), `HEURE_FIN_NUIT` (`06:00`), `SEUIL_HEURES_MAJOREES_JOUR` (`8`), `SEUIL_PRIME_100`/`SEUIL_PRIME_50`/`SEUIL_PRIME_33` (`0`/`2`/`4`, paliers d'incidents = anomalies de pointage non justifiées sur le mois ; au-delà de `SEUIL_PRIME_33`, prime à 25%), `SEUIL_ANOMALIES_CLOTURE` (`98`, en %).

---

## 4. Catalogue des Endpoints — corrigé

Base : `/api/v1`, JWT obligatoire sauf mention contraire. Les endpoints **✅ INCHANGÉ** conservent exactement le contrat (requête/réponse) du CDC-T v1 — non reproduits ici pour ne pas dupliquer 130 pages déjà correctes. Seuls les rôles corrigés et les endpoints 🆕/🔧 sont détaillés.

**🔧 Convention transversale (constat 21, restructuration §3.2)** : partout où le CDC-T v1 utilisait un `employeId`/`id` numérique pour désigner un employé (path param, body, réponse JSON — ex. `"id": 42`), c'est désormais un **UUID string** (ex. `"id": "a3f1c2e4-9b7d-4e21-8c3a-1f2b3c4d5e6f"`). Le champ `matricule` renvoyé par les endpoints Employés devient `matriculeInterne` ; `matriculeRH` devient `matriculeCnss` (optionnel) ; `matriculePaie` reste tel quel mais devient optionnel. Le paramètre de recherche `GET /employes?matriculeRH=...` (§3.4.4 v1) devient `GET /employes?matriculeCnss=...`.

### 4.1 Auth & Utilisateurs

| Endpoint | Méthode | Rôles v2 | Statut |
|---|---|---|---|
| `/auth/login`, `/auth/refresh-token` | POST | Public | ✅ |
| `/auth/me`, `/auth/change-password` | GET/PUT | Tout rôle authentifié | 🔧 élargi (v1 limitait à MANAGER/RH) |
| `/utilisateurs` (liste/création) | GET/POST | RH, SUPERUSER | 🔧 |
| `/utilisateurs/{id}` (détail/statut/reset-password) | GET/PUT | RH, SUPERUSER | 🔧 |

### 4.2 Fichiers, Employés, Départements — ✅ INCHANGÉ (rôles v1 `MANAGER, RH` → lire implicitement `MANAGER, RH, SUPERUSER`, RH-only devient `RH, SUPERUSER`)

### 4.3 Ingestion & Pointages — ✅ INCHANGÉ, sauf :
- `GET /pointages/import-jobs` **devient** `GET /async-jobs?type=IMPORT_POINTAGES` (même contrat de réponse, table source `async_jobs`).

### 4.4 Planification — 🔧
- `GET /shifts` 🆕 — RH, SUPERUSER, MANAGER (lecture pour affectation) : liste le catalogue de créneaux (`shifts`).
- `POST /shifts` 🆕 — RH, SUPERUSER : crée un créneau réutilisable (`nom`, `typeRotation`, `heureDebut`, `heureFin`).
- `POST /plannings/affecter` — ✅ contrat inchangé, référence désormais réellement `shifts.id` (existant en base).
- `GET /plannings/departement/{id}` — ✅ inchangé.
- `GET /mon-planning` 🆕 — **EMPLOYE** : consultation stricte de son propre planning (filtre serveur `employe_id = JWT.employeId`), lecture seule.
- `GET /plannings/delta` 🆕 (gap comblé pendant le Lot 3 — absent du catalogue initial malgré cahier-fonctionnel.md Module 2 et `BACKLOG.md`) — RH, SUPERUSER, MANAGER (restreint à son propre département) : rapport croisé planning théorique / pointage réel, par jour, filtré par `employeId` OU `departementId` (exactement un des deux) + `dateDebut`/`dateFin`. Synchrone (pas d'`async_jobs`), sur le même principe que `GET /assiduite/calcul-mensuel`.

### 4.5 Calendrier — 🔧 contrat détaillé pendant le Lot 4 (gap comblé — §4.5 v1 se limitait à "INCHANGÉ" sans jamais lister les endpoints, même lacune que `/plannings/delta` en Lot 3)
- `GET /calendrier/jours-feries` — RH, SUPERUSER, MANAGER : liste le référentiel `jours_feries` sur une période (`dateDebut`/`dateFin`, obligatoires, format `YYYY-MM-DD`).
- `POST /calendrier/jours-feries` — RH, SUPERUSER : création unitaire (`date`, `libelle`, `type` = `FERIE`\|`FERMETURE_COLLECTIVE`, `national`, `region`). Si `type = FERIE` chevauche une `indisponibilite` de type `CONGE_PAYE` déjà `VALIDE` d'un employé, recrédit automatique du solde (`historique_soldes`, motif `RECREDIT_FERIE`, cf. §3.12) — pas de recrédit pour `FERMETURE_COLLECTIVE`, ni pour un autre type d'indisponibilité validée (ex. `MALADIE`) : seul le congé payé est concerné par la règle du CDF Module 6.
- `POST /calendrier/jours-feries/import` 🆕 — RH, SUPERUSER : import batch (tableau JSON de la même forme que le POST unitaire). Atomique : la moindre ligne en échec (date déjà existante, etc.) annule l'import en totalité, aucune insertion partielle — même principe que l'import Excel pointages (§4.3), mais synchrone (pas d'`async_jobs`, volumétrie annuelle trop faible pour le justifier).
- `PUT /calendrier/jours-feries/{id}` — RH, SUPERUSER : modification. Ne redéclenche pas de recrédit (celui-ci n'a lieu qu'à la création, pour ne pas recréditer deux fois un employé sur une simple correction de libellé).
- `DELETE /calendrier/jours-feries/{id}` — RH, SUPERUSER : suppression.

### 4.6 Demandes d'Autorisations & Congés — 🔧 workflow à 2 étapes (détail en §5), implémenté Lot 5, aucun accès Employé

| Endpoint | Méthode | Rôles v2 | Statut |
|---|---|---|---|
| `POST /autorisations` | POST | MANAGER, RH, SUPERUSER | 🔧 body enrichi de `avisManagerStatut: FAVORABLE\|DEFAVORABLE` (obligatoire si créateur = MANAGER, ignoré si RH/SUPERUSER). Statut initial `EN_ATTENTE_RH` (MANAGER, bloqué en 409 si `dateAutorisation`+`heureDebut` remonte à plus de 72h) ou `VALIDE` (RH/SUPERUSER, workflow d'urgence — justification des anomalies de pointage du jour et mise à jour du planning appliquées **immédiatement** à la création, pas seulement à un `valider()` ultérieur) |
| `PUT /autorisations/{id}/valider` | PUT | RH, SUPERUSER | 🔧 statut `EN_ATTENTE_RH` requis (409 sinon). Si le cumul mensuel de l'employé (statuts `EN_ATTENTE_RH`+`VALIDE`) dépasse 4h, body `traitementDepassement: DEDUCTION_SALAIRE\|RECUPERATION` obligatoire (409 sinon) — décision tracée dans `audit_logs`, pas de colonne dédiée |
| `PUT /autorisations/{id}/refuser` | PUT | RH, SUPERUSER | ✅ statut `EN_ATTENTE_RH` requis, aucun impact solde/pointage/planning |
| `GET /autorisations` 🆕 (filtres `statut`, `employeId`, `departementId`, `dateDebut`/`dateFin`, tous optionnels) | GET | MANAGER (restreint à son propre département, filtres client ignorés), RH, SUPERUSER | 🆕 gap comblé pendant le Lot 5 (même lacune que `/plannings/delta` au Lot 3 et `/calendrier/jours-feries` au Lot 4 — requis par `BACKLOG.md` pour les écrans "liste à valider"/"historique") |
| `POST /indisponibilites` | POST | MANAGER, RH, SUPERUSER | 🔧 même logique que `/autorisations` (pas de délai 72h ni de quota, propres au Module 13 uniquement). Workflow d'urgence RH/SUPERUSER : déduction de solde + mise à jour planning immédiates. Body enrichi de `forcer: boolean` (optionnel, urgence RH avec solde insuffisant) |
| `PUT /indisponibilites/{id}/valider` | PUT | RH, SUPERUSER | 🔧 statut `EN_ATTENTE_RH` requis. Body `{ bascule?: string, forcer?: boolean }` — `bascule` change `type_indisponibilite` avant déduction (ex. `CONGE_SANS_SOLDE`), `forcer` outrepasse un solde insuffisant (409 sinon, message `"Solde insuffisant (X jours disponibles)"`, motif `historique_soldes` = `FORCAGE_RH` au lieu de `VALIDATION_CONGE`). Déduction en **jours ouvrés** (week-ends + `jours_feries.type = FERIE` exclus, cf. §3.19/§3.24) uniquement si `type_indisponibilite = CONGE_PAYE` |
| `PUT /indisponibilites/{id}/refuser` 🆕 | PUT | RH, SUPERUSER | 🆕 gap symétrique comblé pendant le Lot 5 (existait pour `/autorisations` mais pas `/indisponibilites` malgré un workflow RH identique) |
| `GET /indisponibilites` 🆕 (mêmes filtres que `/autorisations`) | GET | MANAGER (son département), RH, SUPERUSER | 🆕 gap comblé pendant le Lot 5 |
| `GET /indisponibilites/solde/{employeId}` | GET | MANAGER, RH, SUPERUSER | ✅ inchangé — **pas d'accès Employé** (confirmé, Module 8) |

**Impact planning (Lot 5, décision d'architecture)** : à la validation d'une autorisation ou d'une indisponibilité (deux temps ou urgence), le planning de l'employé est mis à jour comme source de vérité unique — toute affectation existante chevauchant strictement la période est supprimée et remplacée par un créneau `CONGE` (indisponibilité de type congé) ou `ABSENCE` (autorisation, ou indisponibilité non-congé type `MALADIE`), cf. `plannings.statut` §3.9. S'ajoute au contrôle bloquant déjà posé au Lot 3 (une nouvelle affectation planning est refusée si elle chevauche un congé déjà `VALIDE`) sans le remplacer.

### 4.11 Paramètres globaux 🆕 (Lot 5 — cf. §3.24)

| Endpoint | Méthode | Rôles |
|---|---|---|
| `GET /parametres-globaux`, `GET /parametres-globaux/{cle}` | GET | RH, SUPERUSER |
| `PUT /parametres-globaux/{cle}` (body `{ valeur: string }`) | PUT | RH, SUPERUSER |

### 4.7 Outillages, Assiduité & Paie — 🔧
- `POST /outillages/check-out`, `POST /outillages/check-in` — ✅ contrat inchangé, chaque appel insère désormais une ligne dans `outillage_mouvements` (traçabilité constat 10).
- `GET /outillages/{id}/historique` 🆕 — MANAGER, RH, SUPERUSER : historique des mouvements d'un outil.
- `GET /assiduite/calcul-mensuel?mois=YYYY-MM&departementId=` 🔧 (contrat détaillé pendant le Lot 6, gap comblé — même lacune que `/plannings/delta` au Lot 3, `/calendrier/jours-feries` au Lot 4, `/autorisations` au Lot 5 : le CDC-T v1 se limitait à "✅ INCHANGÉ" sans jamais détailler le contrat) — RH, SUPERUSER, MANAGER (restreint à son propre département, `departementId` client ignoré). Réponse : un `CalculMensuelEmployeDto` par employé — `heuresNormales`/`heuresNuit`/`heuresMajorees` (aucun montant : pas de champ salaire sur `employes`, variables destinées à l'export vers un logiciel de paie externe via `matricule_paie`), `tauxPresence`, `tauxPrime` (paliers d'incidents), `incidents`, `penaliteHeures`.
- `POST /assiduite/cloturer-paie` 🔧 (body `{ mois: "YYYY-MM" }`) — RH, SUPERUSER : clôture globale du mois (§3.25), refusée en 409 si le taux d'anomalies de pointage justifiées est sous `SEUIL_ANOMALIES_CLOTURE` ou si une autorisation/indisponibilité reste `EN_ATTENTE_RH` sur le mois ; 409 si déjà `CLOTURE`.
- `POST /assiduite/demandes-deverrouillage` 🆕 (body `{ mois, motif }`) — MANAGER, RH, SUPERUSER : demande de déverrouillage exceptionnel (§3.26), 400 si le mois n'est pas `CLOTURE`.
- `GET /assiduite/demandes-deverrouillage` 🆕 (filtre `statut` optionnel) — RH, SUPERUSER.
- `PUT /assiduite/demandes-deverrouillage/{id}` 🆕 (body `{ statut: APPROUVEE|REFUSEE }`) — RH, SUPERUSER : 409 si la demande n'est pas `EN_ATTENTE`. `APPROUVEE` rouvre le mois (`cloture_paie.statut = OUVERT`).

### 4.8 Dashboard, Reports & Audit — 🔧
- `GET /dashboard/kpi` — élargi à **DIRECTEUR** (lecture seule) en plus de MANAGER, RH, SUPERUSER.
- `POST /reports/generer`, `GET /reports/tasks/{taskId}` — ✅ contrat inchangé, backés par `async_jobs` (type `REPORT`) au lieu d'une structure ad hoc.
- `GET /audit-logs` — 🔧 **SUPERUSER exclusivement** (RH retiré, constat 2).
- `GET /notifications`, `PUT /notifications/{id}/lue` 🆕 — tout rôle authentifié (scope = ses propres notifications).

### 4.9 Renouvellement de contrats 🆕 (Module 12)
- `GET /taches-renouvellement?statut=EN_COURS` — RH, SUPERUSER : liste des tâches à traiter.
- `PUT /taches-renouvellement/{id}` — RH, SUPERUSER : body `{ "statut": "VALIDE|NON_RENOUVELE" }`. `VALIDE` crée une nouvelle ligne dans `contrats` ; `NON_RENOUVELE` déclenche l'offboarding (`employes.actif = false`, cf. règle du Module 12).

### 4.10 Évaluation Annuelle 🆕 (Module 9)
- `POST /evaluations` — MANAGER : crée la grille pour un employé de son équipe (année en cours), body `{ employeId, competences: [{competence, note, seuilExigence, commentaire}] }`.
- `PUT /evaluations/{id}` — MANAGER : complète/corrige la grille avant soumission ; **rejet 400 si une compétence obligatoire n'est pas notée** (Module 9, cas exceptionnel).
- `PUT /evaluations/{id}/valider` — RH, SUPERUSER : validation finale. Génère automatiquement les lignes `plans_developpement` pour toute compétence sous le seuil.
- `GET /evaluations/{employeId}` — MANAGER (son équipe), RH, SUPERUSER *(pas d'accès Employé — non prévu par le CDF pour ce module)*.

---

## 5. Workflow des demandes (Manager saisit avec avis → Validation RH)

Corrige les Modules 8/13, conforme au Workflow Nominal exact de chaque module — aucune étape ni aucun accès employé n'y figure.

```
Employé formule sa demande HORS système (oral, email — Module 8, étape 1)
        │
        ▼
MANAGER saisit la demande dans le SIRH pour son employé, avec son avis (FAVORABLE/DEFAVORABLE)
        │  statut = EN_ATTENTE_RH
        ▼
RH valide ou refuse (droit de validation finale absolu, peut passer outre un avis défavorable)
        │  statut = VALIDE / REFUSE
        ▼
Impact : déduction du solde (congés) ou justification de l'anomalie de pointage (autorisations)
```

**Workflow d'urgence** (inchangé du CDF, Module 13) : le RH saisit et valide directement (`cree_par_role = 'RH'`), statut `VALIDE` immédiat, pas d'avis Manager à renseigner.

**L'Employé n'a aucune interaction applicative avec ce workflow** — ni saisie, ni consultation de ses demandes. Son seul point de contact avec le SIRH reste `GET /mon-planning` (lecture seule).

---

## 6. Mapping vers `audit-rapport.md`

| Constat | Statut |
|---|---|
| 1, 1a, 1b, 1c, 2 (rôles & logs) | ✅ Corrigé — §2 |
| 3 (`shifts`) | ✅ Corrigé — §3.8 |
| 4 (`contrats`) | ✅ Corrigé — §3.3 |
| 5 (justification anomalies pointage) | ✅ Corrigé — §3.7 |
| 6 (`historique_soldes`) | ✅ Corrigé — §3.12 |
| 7 (Module 9) | ✅ Corrigé — §3.21-3.23, §4.10 |
| 8 (Module 11 Notifications) | ✅ Corrigé — §3.18, §4.8 |
| 9 (Module 12 tâches renouvellement) | ✅ Corrigé — §3.20, §4.9 |
| 10 (`outillage_mouvements`) | ✅ Corrigé — §3.14 |
| 11 (rôle Redis) | À trancher en Phase 1 (architecture, pas un écart de spec) |
| 12 (Flyway, `.env.example`) | À traiter en Phase 2 (scaffolding) |
| 13 (liste blanche IP Hikvision) | ✅ Corrigé — §1.1 |
| 14 (archivage) | ✅ Corrigé — §1.2 |
| 15 (recalcul soldes/fériés) | ✅ Corrigé — `historique_soldes` porte le motif `RECREDIT_FERIE` |
| 16 (`indisponibilites` colonnes) | ✅ Corrigé — §3.11 |
| 17 (`autorisations.type`) | ✅ Corrigé — §3.10 |
| 18 (`report_jobs`) | ✅ Corrigé — `async_jobs` généralisée, §3.16 |
| 19 (seuil doublon pointage) | ✅ Résolu — §3.7 (5 min, même pointeuse, 1er pointage compté) |
| 20, 22 (mineurs restants) | ✅ Corrigés en ligne |
| 21 (triple matricule) | ✅ Résolu — §3.2 (UUID + `matricule_interne`/`matricule_cnss`/`matricule_paie`) |
| jours_feries manquante (gap trouvé pendant la correction, non listé initialement) | ✅ Corrigé — §3.19 |

**Points désormais tous résolus.**

**Précision du 2026-07-29 (a)** : le rôle Employé est confirmé comme lecture seule sur son propre planning uniquement (§2.3 + Modules 8/13) — aucun accès, ni lecture ni écriture, aux demandes d'autorisations/congés. Le mécanisme d'avis Manager (§2.3) est conservé mais simplifié : capturé au moment de la saisie par le Manager plutôt que via une étape/endpoint séparés, pour coller exactement au Workflow Nominal de ces deux modules (voir §0 et §5).

**Précision du 2026-07-29 (b)** : seuil de doublon de pointage fixé à 5 minutes, scope même employé + même pointeuse, premier pointage comptabilisé (§3.7).

**Précision du 2026-07-29 (c)** : `employes.id` passe en UUID, restructuration en `matricule_interne`/`matricule_cnss`/`matricule_paie` (§3.2), avec propagation du type UUID sur les 18 colonnes de FK concernées dans 13 tables. C'est le changement le plus structurant de cette révision — à répercuter dès la première migration Flyway (`docs/ARCHITECTURE.md` §6) puisqu'il n'y a pas de schéma existant en production à faire évoluer (le projet n'a pas encore de premier commit backend).

**Précision du 2026-08-04 (Lot 5)** : décision d'architecture explicite — aucune règle métier à valeur variable (taux d'incrémentation mensuelle des congés, Module 8) n'est codée en dur ; table générique `parametres_globaux` introduite (§3.24, §4.11), relue à chaque exécution du job mensuel sans cache. Déduction de solde congé payé calculée en jours ouvrés (week-ends + jours fériés légaux exclus, interfacée avec `jours_feries` du Lot 4) et non en jours calendaires. Le planning est mis à jour comme source de vérité unique à la validation d'une demande (créneau `CONGE`/`ABSENCE`, §4.6), en complément — pas en remplacement — du contrôle bloquant posé au Lot 3. Gaps d'endpoints comblés (même pattern que Lots 3/4) : `GET /autorisations`, `GET /indisponibilites`, `PUT /indisponibilites/{id}/refuser`.
