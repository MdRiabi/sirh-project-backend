-- ============================================================================
-- V1 — Schéma initial SIRH
-- Source de vérité : docs/specs/cahier-technique.md (v2, corrigé)
-- gen_random_uuid() est natif depuis PostgreSQL 13 (image postgres:16-alpine
-- du docker-compose.yml) — aucune extension (pgcrypto/uuid-ossp) requise.
-- ============================================================================

-- 1. Départements ------------------------------------------------------------
CREATE TABLE departements (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    manager_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Employés (clé UUID, cf. cahier-technique.md §3.2) -----------------------
CREATE TABLE employes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matricule_interne VARCHAR(50) NOT NULL UNIQUE,
    matricule_cnss VARCHAR(50) UNIQUE,
    matricule_paie VARCHAR(50) UNIQUE,
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

-- 3. Contrats (historique) ----------------------------------------------------
CREATE TABLE contrats (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,              -- CDI, CDD, INTERIM, STAGE, APPRENTISSAGE
    date_debut DATE NOT NULL,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, RENOUVELE, TERMINE
    motif_fin VARCHAR(30),                  -- DEMISSION, LICENCIEMENT, FIN_CDD, RETRAITE, MUTATION
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_contrats_employe ON contrats(employe_id, date_debut DESC);

-- 4. Comptes utilisateurs (5 rôles) ------------------------------------------
CREATE TABLE utilisateurs (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID UNIQUE NOT NULL REFERENCES employes(id) ON DELETE CASCADE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,              -- ROLE_EMPLOYE, ROLE_MANAGER, ROLE_RH, ROLE_DIRECTEUR, ROLE_SUPERUSER
    compte_actif BOOLEAN DEFAULT TRUE,
    dernier_login TIMESTAMP,
    echecs_login_consecutifs INT DEFAULT 0,
    verrouille_jusqu_a TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_utilisateurs_username ON utilisateurs(username);

-- 5. Fichiers (transversal, requis par indisponibilites) ---------------------
CREATE TABLE fichiers (
    id BIGSERIAL PRIMARY KEY,
    nom_original VARCHAR(255) NOT NULL,
    nom_stockage VARCHAR(255) NOT NULL UNIQUE,
    chemin_relatif VARCHAR(500) NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    taille_octets BIGINT NOT NULL,
    categorie VARCHAR(50) NOT NULL,         -- CONTRAT, DIPLOME, CIN, JUSTIFICATIF
    entite_type VARCHAR(50),                -- EMPLOYE, INDISPONIBILITE, AUTORISATION
    entite_id BIGINT,
    uploade_par_id BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_fichiers_entite ON fichiers(entite_type, entite_id);

-- 6. Vue 360° : événements personnalisés et manuels RH ------------------------
CREATE TABLE evenements_360 (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE,
    titre VARCHAR(150) NOT NULL,
    description TEXT,
    categorie VARCHAR(50) NOT NULL,         -- EVALUATION, NOTE_RH, DISCIPLINE, CONTRAT
    statut_badge VARCHAR(20) DEFAULT 'INFO',-- SUCCESS, WARNING, DANGER, INFO, PRIMARY
    saisi_par_id BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    date_evenement TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_evenements_360_emp_date ON evenements_360(employe_id, date_evenement DESC);

-- 7. Pointeuses Hikvision ------------------------------------------------------
CREATE TABLE pointeuses (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    adresse_ip VARCHAR(45) NOT NULL UNIQUE,
    port INT DEFAULT 80,
    batiment VARCHAR(50) NOT NULL,          -- Batiment A, B, C, D
    statut_en_ligne BOOLEAN DEFAULT TRUE,
    derniere_synchro TIMESTAMP
);

-- 8. Shifts (catalogue de créneaux) -------------------------------------------
CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    type_rotation VARCHAR(20) NOT NULL,     -- FIXE, 2X8, 3X8
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Plannings théoriques ------------------------------------------------------
CREATE TABLE plannings (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE,
    shift_id BIGINT NOT NULL REFERENCES shifts(id),
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'PROVISOIRE', -- PROVISOIRE, VALIDE, REPOS
    commentaire TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_plannings_emp_date ON plannings(employe_id, date_debut);

-- 10. Demandes d'autorisations (absences courtes) -----------------------------
CREATE TABLE autorisations (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,              -- ABSENCE_COURTE, SORTIE_MEDICALE, RETARD_JUSTIFIE, MISSION
    date_autorisation DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME,
    duree_minutes INT,
    motif TEXT,
    statut VARCHAR(30) DEFAULT 'EN_ATTENTE_RH', -- EN_ATTENTE_RH, VALIDE, REFUSE
    cree_par_id UUID NOT NULL REFERENCES employes(id),   -- toujours Manager ou RH, jamais l'employé concerné
    cree_par_role VARCHAR(20) NOT NULL,     -- MANAGER, RH
    avis_manager_statut VARCHAR(20),        -- FAVORABLE, DEFAVORABLE
    avis_manager_commentaire TEXT,
    valide_par_rh_id UUID REFERENCES employes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_autorisations_emp_date ON autorisations(employe_id, date_autorisation DESC);

-- 11. Pointages bruts -----------------------------------------------------------
CREATE TABLE pointages_bruts (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE,
    pointeuse_id BIGINT REFERENCES pointeuses(id) ON DELETE SET NULL,
    timestamp_pointage TIMESTAMP NOT NULL,
    type_evenement VARCHAR(20) NOT NULL,    -- ENTREE, SORTIE
    methode_validation VARCHAR(30) NOT NULL,-- FACIAL, EMPREINTE, BADGE, MANUEL
    source_import VARCHAR(20) NOT NULL,     -- HIKVISION_PUSH, EXCEL_IMPORT, MANUEL
    statut VARCHAR(30) NOT NULL DEFAULT 'OK', -- OK, ANOMALIE_NON_JUSTIFIEE, JUSTIFIE, IGNORE_DOUBLON
    autorisation_id BIGINT REFERENCES autorisations(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pointages_emp_date ON pointages_bruts(employe_id, timestamp_pointage DESC);
CREATE INDEX idx_pointages_statut ON pointages_bruts(statut);

-- 12. Indisponibilités (congés / maladies) -------------------------------------
CREATE TABLE indisponibilites (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID REFERENCES employes(id) ON DELETE CASCADE,
    type_indisponibilite VARCHAR(30) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(30) DEFAULT 'EN_ATTENTE_RH',
    commentaire TEXT,
    piece_jointe_id BIGINT REFERENCES fichiers(id),
    cree_par_id UUID NOT NULL REFERENCES employes(id),
    cree_par_role VARCHAR(20) NOT NULL,
    avis_manager_statut VARCHAR(20),
    avis_manager_commentaire TEXT,
    valide_par_rh_id UUID REFERENCES employes(id)
);
CREATE INDEX idx_indisponibilites_emp_date ON indisponibilites(employe_id, date_debut DESC);

-- 13. Historique des soldes de congés -------------------------------------------
CREATE TABLE historique_soldes (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE,
    date_mouvement DATE NOT NULL,
    delta_jours NUMERIC(5,2) NOT NULL,      -- négatif = déduction, positif = recrédit
    solde_apres NUMERIC(5,2) NOT NULL,
    motif VARCHAR(50) NOT NULL,             -- VALIDATION_CONGE, RECREDIT_FERIE, INCREMENT_MENSUEL, FORCAGE_RH
    source_type VARCHAR(30),                -- INDISPONIBILITE, JOUR_FERIE, CRON_MENSUEL
    source_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_historique_soldes_emp ON historique_soldes(employe_id, date_mouvement DESC);

-- 14. Outillages (état courant) ---------------------------------------------------
CREATE TABLE outillages (
    id BIGSERIAL PRIMARY KEY,
    code_barres VARCHAR(100) NOT NULL UNIQUE,
    nom_outil VARCHAR(150) NOT NULL,
    etat VARCHAR(30) DEFAULT 'BON_ETAT',
    attribue_a_employe_id UUID REFERENCES employes(id) ON DELETE SET NULL,
    date_attribution TIMESTAMP
);
CREATE INDEX idx_outillages_emp ON outillages(attribue_a_employe_id);

-- 15. Historique des mouvements d'outillage ---------------------------------------
CREATE TABLE outillage_mouvements (
    id BIGSERIAL PRIMARY KEY,
    outillage_id BIGINT NOT NULL REFERENCES outillages(id) ON DELETE CASCADE,
    employe_id UUID REFERENCES employes(id),
    type_mouvement VARCHAR(20) NOT NULL,    -- CHECK_OUT, CHECK_IN
    etat VARCHAR(30),                       -- BON, USE, CASSE, PERDU
    montant_imputation NUMERIC(10,2) DEFAULT 0,
    commentaire TEXT,
    effectue_par_id BIGINT REFERENCES utilisateurs(id),
    date_mouvement TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outillage_mvt_outil ON outillage_mouvements(outillage_id, date_mouvement DESC);

-- 16. Jobs asynchrones (imports Excel + rapports) ----------------------------------
CREATE TABLE async_jobs (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL,              -- IMPORT_POINTAGES, REPORT
    nom_fichier VARCHAR(255),
    statut VARCHAR(20) NOT NULL,            -- PENDING, IN_PROGRESS, SUCCESS, FAILED
    progression INT DEFAULT 0,
    parametres JSONB,
    resultat_url VARCHAR(500),
    nombre_lignes_traitees INT DEFAULT 0,
    ligne_erreur INT,
    message_erreur TEXT,
    rapport_erreurs JSONB,
    execute_par_id BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX idx_async_jobs_type_statut ON async_jobs(type, statut);

-- 17. Audit log immuable ------------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    action VARCHAR(100) NOT NULL,
    table_cible VARCHAR(50) NOT NULL,
    anciennes_valeurs JSONB,
    nouvelles_valeurs JSONB,
    adresse_ip VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 18. Notifications -------------------------------------------------------------------
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    destinataire_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    entite_type VARCHAR(50),
    entite_id BIGINT,
    lue BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_dest ON notifications(destinataire_id, lue, created_at DESC);

-- 19. Jours fériés / fermetures collectives ---------------------------------------------
CREATE TABLE jours_feries (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,              -- FERIE, FERMETURE_COLLECTIVE
    national BOOLEAN DEFAULT TRUE,
    region VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 20. Tâches de renouvellement de contrat -------------------------------------------------
CREATE TABLE taches_renouvellement (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE,
    contrat_id BIGINT REFERENCES contrats(id),
    type_echeance VARCHAR(10) NOT NULL,     -- M+5, M+11
    date_echeance DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, VALIDE, NON_RENOUVELE, URGENT_EXPIRE
    date_traitement TIMESTAMP,
    traite_par_id BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_taches_renouv_statut ON taches_renouvellement(statut, date_echeance);

-- 21. Évaluations annuelles -------------------------------------------------------------------
CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    employe_id UUID NOT NULL REFERENCES employes(id) ON DELETE CASCADE,
    manager_id UUID REFERENCES employes(id),
    annee INT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, VALIDEE_RH
    date_entretien DATE,
    date_validation_rh TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employe_id, annee)
);

-- 22. Compétences notées par évaluation --------------------------------------------------------
CREATE TABLE evaluation_competences (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    competence VARCHAR(100) NOT NULL,
    note NUMERIC(3,1) NOT NULL,
    seuil_exigence NUMERIC(3,1) NOT NULL,
    commentaire TEXT
);

-- 23. Plans de développement individuel (PDI) --------------------------------------------------
CREATE TABLE plans_developpement (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    competence VARCHAR(100) NOT NULL,
    actions TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(20) DEFAULT 'A_TRAITER'
);
