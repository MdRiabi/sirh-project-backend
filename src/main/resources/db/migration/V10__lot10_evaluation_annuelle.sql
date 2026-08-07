-- ============================================================================
-- V10 : Évaluation Annuelle (Module 9, Lot 10)
-- Les tables evaluations/evaluation_competences/plans_developpement existent
-- déjà dans V1 (même situation que taches_renouvellement au Lot 9) : cette
-- migration ferme le domaine du statut evaluations et assouplit
-- evaluation_competences.note pour permettre l'enregistrement d'une grille
-- incomplète (cas exceptionnel Module 9 — la SOUMISSION bloque, pas la saisie ;
-- cf. EvaluationService#mettreAJour côté backend).
-- ============================================================================

ALTER TABLE evaluation_competences ALTER COLUMN note DROP NOT NULL;

ALTER TABLE evaluations
    ADD CONSTRAINT chk_evaluations_statut CHECK (statut IN ('EN_COURS', 'VALIDEE_RH'));

-- Écran de validation RH : liste des grilles EN_COURS à traiter (cf. GET
-- /evaluations?statut=&annee=), même besoin que idx_taches_renouv_statut au Lot 9.
CREATE INDEX idx_evaluations_statut ON evaluations(statut, annee);
