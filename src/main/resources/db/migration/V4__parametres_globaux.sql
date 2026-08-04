-- ============================================================================
-- V4 — Table de paramétrage global (Lot 5, décision d'architecture explicite :
-- aucune règle métier variable ne doit être codée en dur). Générique par
-- conception (cle/valeur/type) pour être réutilisée par d'autres lots
-- (ex. seuils M+5/M+11 du Lot 9) sans nouvelle migration à chaque paramètre.
-- ============================================================================

CREATE TABLE parametres_globaux (
    id BIGSERIAL PRIMARY KEY,
    cle VARCHAR(100) NOT NULL UNIQUE,
    valeur VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'DECIMAL',
    description TEXT,
    modifie_par_id UUID REFERENCES employes(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO parametres_globaux (cle, valeur, type, description)
VALUES (
    'TAUX_INCREMENTATION_CONGES_MENSUEL',
    '1.5',
    'DECIMAL',
    'Nombre de jours de congé crédités automatiquement à chaque employé actif, une fois par mois (cahier-fonctionnel.md Module 8). Modifiable par le RH sans redéploiement.'
);
