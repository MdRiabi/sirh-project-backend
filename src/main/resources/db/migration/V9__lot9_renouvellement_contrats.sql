-- ============================================================================
-- V9 — Lot 9 (Module 12 : Renouvellement de contrats). Table `taches_renouvellement`
-- déjà créée par V1__init_schema.sql (schéma consolidé initial, §20) avec
-- exactement la structure du cahier-technique.md §3.20 — même situation que
-- `notifications` au Lot 8 (V8). Il manque toutefois la contrainte UNIQUE
-- (contrat_id, type_echeance) qui garantit qu'un même contrat ne génère
-- jamais deux tâches M+5 (ou deux M+11), même si le scheduler tourne deux
-- fois le même jour — absente du schéma consolidé, ajoutée ici.
--
-- MOIS_ECHEANCE_M5 / MOIS_ECHEANCE_M11 et DUREE_RENOUVELLEMENT_CDD_MOIS :
-- même principe que SEUIL_OUBLIS_BADGEAGE_HEBDO (Lot 8) et
-- TAUX_INCREMENTATION_CONGES_MENSUEL (Lot 5) — les seuils M+5/M+11 étaient
-- déjà anticipés comme paramètres dynamiques dans LOT_5_CONGES_AUTORISATIONS.md,
-- jamais en dur dans le scheduler.
-- ============================================================================

ALTER TABLE taches_renouvellement
    ADD CONSTRAINT chk_taches_renouv_type_echeance CHECK (type_echeance IN ('M+5', 'M+11')),
    ADD CONSTRAINT uq_taches_renouv_contrat_type UNIQUE (contrat_id, type_echeance);

INSERT INTO parametres_globaux (cle, valeur, type, description) VALUES
    ('MOIS_ECHEANCE_M5', '5', 'INTEGER',
     'Nombre de mois après le début du contrat CDD à partir duquel la tâche RH "Préparer renouvellement" (échéance M+5, préparation du terme à 6 mois) est générée (Module 12).'),
    ('MOIS_ECHEANCE_M11', '11', 'INTEGER',
     'Nombre de mois après le début du contrat CDD à partir duquel la tâche RH "Préparer renouvellement" (échéance M+11, préparation du terme à 12 mois) est générée (Module 12).'),
    ('DUREE_RENOUVELLEMENT_CDD_MOIS', '6', 'INTEGER',
     'Durée par défaut (en mois) du nouveau contrat créé lors de la validation d''une tâche de renouvellement quand le RH ne précise pas de date de fin explicite (Module 12).');
