-- ============================================================================
-- V7 — Lot 7 (Module 5 : Gestion des Outillages). Deux gaps trouvés en
-- préparant le lot (même nature que jours_feries au Lot 4, cloture_paie au
-- Lot 6) :
--   - le barème d'imputation financière (0-6/6-12/>12 mois, audit-rapport.md
--     constat 10) s'applique en pourcentage d'une valeur d'achat, mais
--     `outillages` n'a ni valeur ni date d'achat ;
--   - "le matériel de son équipe" (cahier-fonctionnel.md Module 5, gestion
--     exclusive Responsable Département) n'est scopable par département que
--     via l'employé attribué, ce qui exclut tout outil en stock non attribué.
-- ============================================================================

-- NOT NULL direct sans DEFAULT : sans danger, `outillages` est vide (aucun
-- seed) à cette migration.
ALTER TABLE outillages
    ADD COLUMN departement_id BIGINT NOT NULL REFERENCES departements(id),
    ADD COLUMN valeur_achat NUMERIC(10,2) NOT NULL,
    ADD COLUMN date_achat DATE NOT NULL;

CREATE INDEX idx_outillages_departement ON outillages(departement_id);

-- Barème d'imputation (Module 5, "traitement financier" en cas de perte) :
-- même principe que TAUX_INCREMENTATION_CONGES_MENSUEL (Lot 5) et les seuils
-- du moteur de calcul (Lot 6) — aucune valeur chiffrée dans le CDF/CDC-T,
-- valeurs par défaut seedées, modifiables par le RH sans redéploiement via
-- PUT /parametres-globaux/{cle}.
INSERT INTO parametres_globaux (cle, valeur, type, description) VALUES
    ('TAUX_IMPUTATION_OUTIL_0_6_MOIS', '100', 'DECIMAL',
     'Pourcentage de la valeur d''achat imputé en cas de perte d''un outil de moins de 6 mois (Module 5).'),
    ('TAUX_IMPUTATION_OUTIL_6_12_MOIS', '50', 'DECIMAL',
     'Pourcentage de la valeur d''achat imputé en cas de perte d''un outil de 6 à 12 mois (Module 5).'),
    ('TAUX_IMPUTATION_OUTIL_PLUS_12_MOIS', '25', 'DECIMAL',
     'Pourcentage de la valeur d''achat imputé en cas de perte d''un outil de plus de 12 mois (Module 5).');
