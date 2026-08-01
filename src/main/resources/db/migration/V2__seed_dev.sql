-- ============================================================================
-- V2 — Données de seed pour environnement de développement
-- Un jeu de données minimal (1 département, 1 employé, 1 compte SUPERUSER)
-- pour permettre un premier login testable de bout en bout avant l'existence
-- de tout écran de gestion des comptes (celui-ci arrive en Lot 1).
--
-- Identifiants de dev : username "admin" / mot de passe "ChangeMe123!"
-- (hash BCrypt ci-dessous). Documenté aussi dans CLAUDE.md. Ne jamais
-- appliquer cette migration sur un environnement de production.
-- ============================================================================

WITH dep AS (
    INSERT INTO departements (nom)
    VALUES ('Direction Générale')
    RETURNING id
), emp AS (
    INSERT INTO employes (
        matricule_interne, matricule_cnss, matricule_paie,
        nom, prenom, email, departement_id, type_contrat, date_embauche, actif
    )
    SELECT 'DEV-0001', 'CNSS-DEV-0001', 'PAIE-DEV-0001',
           'Admin', 'Superuser', 'admin@sirh.dev', dep.id, 'CDI', CURRENT_DATE, TRUE
    FROM dep
    RETURNING id
)
INSERT INTO utilisateurs (employe_id, username, password_hash, role, compte_actif)
SELECT emp.id, 'admin', '$2b$10$BkRl3hsY4bl14.ctu6js3O8I2DTR4aiRCvFEDmB1TdW8QMBfndaXu', 'ROLE_SUPERUSER', TRUE
FROM emp;