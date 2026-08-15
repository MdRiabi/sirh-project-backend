-- ============================================================================
-- V11 -- BaseEntity centralisee (reference/created_at/updated_at/deleted_at)
-- + soft delete sur toutes les entites sauf audit_logs (immuable, cf. CLAUDE.md).
-- Contraintes UNIQUE existantes remplacees par des index partiels
-- (WHERE deleted_at IS NULL) pour permettre la reutilisation d'une valeur
-- unique apres soft delete d'un enregistrement.
-- ============================================================================

-- 1. Colonnes BaseEntity -----------------------------------------------------

ALTER TABLE async_jobs ADD COLUMN reference UUID;
UPDATE async_jobs SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE async_jobs ALTER COLUMN reference SET NOT NULL;
ALTER TABLE async_jobs ADD CONSTRAINT uq_async_jobs_reference UNIQUE (reference);
ALTER TABLE async_jobs ADD COLUMN updated_at TIMESTAMP;
UPDATE async_jobs SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE async_jobs ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE autorisations ADD COLUMN reference UUID;
UPDATE autorisations SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE autorisations ALTER COLUMN reference SET NOT NULL;
ALTER TABLE autorisations ADD CONSTRAINT uq_autorisations_reference UNIQUE (reference);
ALTER TABLE autorisations ADD COLUMN updated_at TIMESTAMP;
UPDATE autorisations SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE autorisations ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE cloture_paie ADD COLUMN reference UUID;
UPDATE cloture_paie SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE cloture_paie ALTER COLUMN reference SET NOT NULL;
ALTER TABLE cloture_paie ADD CONSTRAINT uq_cloture_paie_reference UNIQUE (reference);
ALTER TABLE cloture_paie ADD COLUMN updated_at TIMESTAMP;
UPDATE cloture_paie SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE cloture_paie ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE contrats ADD COLUMN reference UUID;
UPDATE contrats SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE contrats ALTER COLUMN reference SET NOT NULL;
ALTER TABLE contrats ADD CONSTRAINT uq_contrats_reference UNIQUE (reference);
ALTER TABLE contrats ADD COLUMN updated_at TIMESTAMP;
UPDATE contrats SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE contrats ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE demandes_deverrouillage ADD COLUMN reference UUID;
UPDATE demandes_deverrouillage SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE demandes_deverrouillage ALTER COLUMN reference SET NOT NULL;
ALTER TABLE demandes_deverrouillage ADD CONSTRAINT uq_demandes_deverrouillage_reference UNIQUE (reference);
ALTER TABLE demandes_deverrouillage ADD COLUMN updated_at TIMESTAMP;
UPDATE demandes_deverrouillage SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE demandes_deverrouillage ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE departements ADD COLUMN reference UUID;
UPDATE departements SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE departements ALTER COLUMN reference SET NOT NULL;
ALTER TABLE departements ADD CONSTRAINT uq_departements_reference UNIQUE (reference);
ALTER TABLE departements ADD COLUMN updated_at TIMESTAMP;
UPDATE departements SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE departements ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE employes ADD COLUMN reference UUID;
UPDATE employes SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE employes ALTER COLUMN reference SET NOT NULL;
ALTER TABLE employes ADD CONSTRAINT uq_employes_reference UNIQUE (reference);
ALTER TABLE employes ADD COLUMN updated_at TIMESTAMP;
UPDATE employes SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE employes ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE evaluations ADD COLUMN reference UUID;
UPDATE evaluations SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE evaluations ALTER COLUMN reference SET NOT NULL;
ALTER TABLE evaluations ADD CONSTRAINT uq_evaluations_reference UNIQUE (reference);
ALTER TABLE evaluations ADD COLUMN updated_at TIMESTAMP;
UPDATE evaluations SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE evaluations ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE evenements_360 ADD COLUMN reference UUID;
UPDATE evenements_360 SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE evenements_360 ALTER COLUMN reference SET NOT NULL;
ALTER TABLE evenements_360 ADD CONSTRAINT uq_evenements_360_reference UNIQUE (reference);
ALTER TABLE evenements_360 ADD COLUMN updated_at TIMESTAMP;
UPDATE evenements_360 SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE evenements_360 ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE fichiers ADD COLUMN reference UUID;
UPDATE fichiers SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE fichiers ALTER COLUMN reference SET NOT NULL;
ALTER TABLE fichiers ADD CONSTRAINT uq_fichiers_reference UNIQUE (reference);
ALTER TABLE fichiers ADD COLUMN updated_at TIMESTAMP;
UPDATE fichiers SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE fichiers ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE historique_soldes ADD COLUMN reference UUID;
UPDATE historique_soldes SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE historique_soldes ALTER COLUMN reference SET NOT NULL;
ALTER TABLE historique_soldes ADD CONSTRAINT uq_historique_soldes_reference UNIQUE (reference);
ALTER TABLE historique_soldes ADD COLUMN updated_at TIMESTAMP;
UPDATE historique_soldes SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE historique_soldes ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE jours_feries ADD COLUMN reference UUID;
UPDATE jours_feries SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE jours_feries ALTER COLUMN reference SET NOT NULL;
ALTER TABLE jours_feries ADD CONSTRAINT uq_jours_feries_reference UNIQUE (reference);
ALTER TABLE jours_feries ADD COLUMN updated_at TIMESTAMP;
UPDATE jours_feries SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE jours_feries ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE notifications ADD COLUMN reference UUID;
UPDATE notifications SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE notifications ALTER COLUMN reference SET NOT NULL;
ALTER TABLE notifications ADD CONSTRAINT uq_notifications_reference UNIQUE (reference);
ALTER TABLE notifications ADD COLUMN updated_at TIMESTAMP;
UPDATE notifications SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE notifications ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE plannings ADD COLUMN reference UUID;
UPDATE plannings SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE plannings ALTER COLUMN reference SET NOT NULL;
ALTER TABLE plannings ADD CONSTRAINT uq_plannings_reference UNIQUE (reference);
ALTER TABLE plannings ADD COLUMN updated_at TIMESTAMP;
UPDATE plannings SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE plannings ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE pointages_bruts ADD COLUMN reference UUID;
UPDATE pointages_bruts SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE pointages_bruts ALTER COLUMN reference SET NOT NULL;
ALTER TABLE pointages_bruts ADD CONSTRAINT uq_pointages_bruts_reference UNIQUE (reference);
ALTER TABLE pointages_bruts ADD COLUMN updated_at TIMESTAMP;
UPDATE pointages_bruts SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE pointages_bruts ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE shifts ADD COLUMN reference UUID;
UPDATE shifts SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE shifts ALTER COLUMN reference SET NOT NULL;
ALTER TABLE shifts ADD CONSTRAINT uq_shifts_reference UNIQUE (reference);
ALTER TABLE shifts ADD COLUMN updated_at TIMESTAMP;
UPDATE shifts SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE shifts ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE taches_renouvellement ADD COLUMN reference UUID;
UPDATE taches_renouvellement SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE taches_renouvellement ALTER COLUMN reference SET NOT NULL;
ALTER TABLE taches_renouvellement ADD CONSTRAINT uq_taches_renouvellement_reference UNIQUE (reference);
ALTER TABLE taches_renouvellement ADD COLUMN updated_at TIMESTAMP;
UPDATE taches_renouvellement SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE taches_renouvellement ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE utilisateurs ADD COLUMN reference UUID;
UPDATE utilisateurs SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE utilisateurs ALTER COLUMN reference SET NOT NULL;
ALTER TABLE utilisateurs ADD CONSTRAINT uq_utilisateurs_reference UNIQUE (reference);
ALTER TABLE utilisateurs ADD COLUMN updated_at TIMESTAMP;
UPDATE utilisateurs SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE utilisateurs ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE evaluation_competences ADD COLUMN reference UUID;
UPDATE evaluation_competences SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE evaluation_competences ALTER COLUMN reference SET NOT NULL;
ALTER TABLE evaluation_competences ADD CONSTRAINT uq_evaluation_competences_reference UNIQUE (reference);
ALTER TABLE evaluation_competences ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE evaluation_competences ADD COLUMN updated_at TIMESTAMP;
UPDATE evaluation_competences SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE evaluation_competences ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE indisponibilites ADD COLUMN reference UUID;
UPDATE indisponibilites SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE indisponibilites ALTER COLUMN reference SET NOT NULL;
ALTER TABLE indisponibilites ADD CONSTRAINT uq_indisponibilites_reference UNIQUE (reference);
ALTER TABLE indisponibilites ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE indisponibilites ADD COLUMN updated_at TIMESTAMP;
UPDATE indisponibilites SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE indisponibilites ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE outillages ADD COLUMN reference UUID;
UPDATE outillages SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE outillages ALTER COLUMN reference SET NOT NULL;
ALTER TABLE outillages ADD CONSTRAINT uq_outillages_reference UNIQUE (reference);
ALTER TABLE outillages ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE outillages ADD COLUMN updated_at TIMESTAMP;
UPDATE outillages SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE outillages ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE pointeuses ADD COLUMN reference UUID;
UPDATE pointeuses SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE pointeuses ALTER COLUMN reference SET NOT NULL;
ALTER TABLE pointeuses ADD CONSTRAINT uq_pointeuses_reference UNIQUE (reference);
ALTER TABLE pointeuses ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE pointeuses ADD COLUMN updated_at TIMESTAMP;
UPDATE pointeuses SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE pointeuses ADD COLUMN deleted_at TIMESTAMP;

-- parametres_globaux a deja updated_at (V4), il manque created_at
ALTER TABLE parametres_globaux ADD COLUMN reference UUID;
UPDATE parametres_globaux SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE parametres_globaux ALTER COLUMN reference SET NOT NULL;
ALTER TABLE parametres_globaux ADD CONSTRAINT uq_parametres_globaux_reference UNIQUE (reference);
ALTER TABLE parametres_globaux ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE parametres_globaux ADD COLUMN deleted_at TIMESTAMP;

-- plans_developpement.date_creation devient plans_developpement.created_at (meme semantique : horodatage
-- de creation, deja auto-genere en base, cf. BaseEntity)
ALTER TABLE plans_developpement RENAME COLUMN date_creation TO created_at;
ALTER TABLE plans_developpement ADD COLUMN reference UUID;
UPDATE plans_developpement SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE plans_developpement ALTER COLUMN reference SET NOT NULL;
ALTER TABLE plans_developpement ADD CONSTRAINT uq_plans_developpement_reference UNIQUE (reference);
ALTER TABLE plans_developpement ADD COLUMN updated_at TIMESTAMP;
UPDATE plans_developpement SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE plans_developpement ADD COLUMN deleted_at TIMESTAMP;

-- outillage_mouvements.date_mouvement devient outillage_mouvements.created_at (meme semantique : horodatage
-- de creation, deja auto-genere en base, cf. BaseEntity)
ALTER TABLE outillage_mouvements RENAME COLUMN date_mouvement TO created_at;
ALTER TABLE outillage_mouvements ADD COLUMN reference UUID;
UPDATE outillage_mouvements SET reference = gen_random_uuid() WHERE reference IS NULL;
ALTER TABLE outillage_mouvements ALTER COLUMN reference SET NOT NULL;
ALTER TABLE outillage_mouvements ADD CONSTRAINT uq_outillage_mouvements_reference UNIQUE (reference);
ALTER TABLE outillage_mouvements ADD COLUMN updated_at TIMESTAMP;
UPDATE outillage_mouvements SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE outillage_mouvements ADD COLUMN deleted_at TIMESTAMP;

-- 2. Contraintes UNIQUE -> index partiels (WHERE deleted_at IS NULL) --------
-- Une valeur devient reutilisable une fois l'enregistrement qui la portait
-- soft-delete (cf. directive d'architecture, migration BaseEntity).

ALTER TABLE departements DROP CONSTRAINT departements_nom_key;
CREATE UNIQUE INDEX idx_departements_nom ON departements(nom) WHERE deleted_at IS NULL;

ALTER TABLE employes DROP CONSTRAINT employes_matricule_interne_key;
CREATE UNIQUE INDEX idx_employes_matricule_interne_uq ON employes(matricule_interne) WHERE deleted_at IS NULL;

ALTER TABLE employes DROP CONSTRAINT employes_matricule_cnss_key;
CREATE UNIQUE INDEX idx_employes_matricule_cnss_uq ON employes(matricule_cnss) WHERE deleted_at IS NULL;

ALTER TABLE employes DROP CONSTRAINT employes_matricule_paie_key;
CREATE UNIQUE INDEX idx_employes_matricule_paie_uq ON employes(matricule_paie) WHERE deleted_at IS NULL;

ALTER TABLE employes DROP CONSTRAINT employes_email_key;
CREATE UNIQUE INDEX idx_employes_email ON employes(email) WHERE deleted_at IS NULL;

ALTER TABLE utilisateurs DROP CONSTRAINT utilisateurs_employe_id_key;
CREATE UNIQUE INDEX idx_utilisateurs_employe_id ON utilisateurs(employe_id) WHERE deleted_at IS NULL;

ALTER TABLE utilisateurs DROP CONSTRAINT utilisateurs_username_key;
CREATE UNIQUE INDEX idx_utilisateurs_username_uq ON utilisateurs(username) WHERE deleted_at IS NULL;

ALTER TABLE fichiers DROP CONSTRAINT fichiers_nom_stockage_key;
CREATE UNIQUE INDEX idx_fichiers_nom_stockage ON fichiers(nom_stockage) WHERE deleted_at IS NULL;

ALTER TABLE pointeuses DROP CONSTRAINT pointeuses_adresse_ip_key;
CREATE UNIQUE INDEX idx_pointeuses_adresse_ip ON pointeuses(adresse_ip) WHERE deleted_at IS NULL;

ALTER TABLE outillages DROP CONSTRAINT outillages_code_barres_key;
CREATE UNIQUE INDEX idx_outillages_code_barres ON outillages(code_barres) WHERE deleted_at IS NULL;

ALTER TABLE jours_feries DROP CONSTRAINT jours_feries_date_key;
CREATE UNIQUE INDEX idx_jours_feries_date_uq ON jours_feries(date) WHERE deleted_at IS NULL;

ALTER TABLE parametres_globaux DROP CONSTRAINT parametres_globaux_cle_key;
CREATE UNIQUE INDEX idx_parametres_globaux_cle ON parametres_globaux(cle) WHERE deleted_at IS NULL;

ALTER TABLE cloture_paie DROP CONSTRAINT cloture_paie_mois_key;
CREATE UNIQUE INDEX idx_cloture_paie_mois ON cloture_paie(mois) WHERE deleted_at IS NULL;

ALTER TABLE evaluations DROP CONSTRAINT evaluations_employe_id_annee_key;
CREATE UNIQUE INDEX idx_evaluations_employe_annee ON evaluations(employe_id, annee) WHERE deleted_at IS NULL;

ALTER TABLE taches_renouvellement DROP CONSTRAINT uq_taches_renouv_contrat_type;
CREATE UNIQUE INDEX idx_taches_renouv_contrat_type ON taches_renouvellement(contrat_id, type_echeance) WHERE deleted_at IS NULL;

